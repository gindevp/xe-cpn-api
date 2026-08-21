package com.mycompany.myapp.service.finance;

import com.mycompany.myapp.domain.DayClosure;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ReceiptOrderLine;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.order.OrderMoney;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FinanceFacadeService {

    private static final String ENTITY = "finance";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptOrderLineRepository receiptOrderLineRepository;
    private final DayClosureRepository dayClosureRepository;
    private final OfficeRepository officeRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final OrderEventRepository orderEventRepository;
    private final DayClosureGuard dayClosureGuard;

    public FinanceFacadeService(
        ShipmentOrderRepository shipmentOrderRepository,
        ReceiptRepository receiptRepository,
        ReceiptOrderLineRepository receiptOrderLineRepository,
        DayClosureRepository dayClosureRepository,
        OfficeRepository officeRepository,
        OrderPaymentRepository orderPaymentRepository,
        OrderEventRepository orderEventRepository,
        DayClosureGuard dayClosureGuard
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.receiptRepository = receiptRepository;
        this.receiptOrderLineRepository = receiptOrderLineRepository;
        this.dayClosureRepository = dayClosureRepository;
        this.officeRepository = officeRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.orderEventRepository = orderEventRepository;
        this.dayClosureGuard = dayClosureGuard;
    }

    @Transactional(readOnly = true)
    public List<CandidateDTO> candidates(String officeCode, String keyword) {
        Specification<ShipmentOrder> spec = (root, q, cb) ->
            cb.and(
                cb.notEqual(root.get("status"), OrderStatus.CANCELLED),
                cb.notEqual(root.get("status"), OrderStatus.DRAFT),
                cb.greaterThan(root.get("fareAmount"), root.get("paidAmount"))
            );
        if (officeCode != null && !officeCode.isBlank()) {
            String scoped = officeCode.trim().toUpperCase();
            spec = spec.and((root, q, cb) -> cb.equal(root.get("fromOffice").get("code"), scoped));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("orderCode")), like),
                    cb.like(cb.lower(root.get("receiverPhone")), like),
                    cb.like(cb.lower(root.get("senderPhone")), like)
                )
            );
        }
        return shipmentOrderRepository
            .findAll(spec)
            .stream()
            .limit(200)
            .map(o -> {
                BigDecimal due = OrderMoney.due(o);
                return new CandidateDTO(
                    o.getOrderCode(),
                    o.getReceiverName(),
                    o.getReceiverPhone(),
                    o.getFareAmount(),
                    o.getPaidAmount(),
                    due,
                    o.getStatus().name(),
                    o.getFromOffice() != null ? o.getFromOffice().getCode() : null,
                    resolveDebtOwner(o)
                );
            })
            .toList();
    }

    public ReceiptDTO createReceipt(CreateReceiptRequest req) {
        if (req == null || req.lines() == null || req.lines().isEmpty()) {
            throw new BadRequestAlertException("Receipt lines required", ENTITY, "receiptLinesRequired");
        }
        Office office = null;
        if (req.officeCode() != null && !req.officeCode().isBlank()) {
            office = officeRepository
                .findOneByCode(req.officeCode().trim().toUpperCase())
                .orElseThrow(() -> new BadRequestAlertException("Office not found", ENTITY, "officeNotFound"));
        }
        Instant now = Instant.now();
        String actor = actor();
        if (office != null) {
            dayClosureGuard.assertOfficeOpen(office);
        }
        BigDecimal total = BigDecimal.ZERO;
        List<ReceiptOrderLine> lines = new ArrayList<>();
        for (ReceiptLineRequest line : req.lines()) {
            if (line == null || line.orderCode() == null || line.orderCode().isBlank()) {
                throw new BadRequestAlertException("orderCode required on receipt line", ENTITY, "orderCodeRequired");
            }
            ShipmentOrder order = shipmentOrderRepository
                .findOneByOrderCodeOrDraftCode(line.orderCode().trim())
                .orElseThrow(() -> new BadRequestAlertException("Order not found: " + line.orderCode(), ENTITY, "orderNotFound"));
            dayClosureGuard.assertCollectionMutable(order);
            BigDecimal amount = line.amountCollected();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestAlertException("amountCollected must be > 0", ENTITY, "amountInvalid");
            }
            BigDecimal paid = OrderMoney.nz(order.getPaidAmount());
            BigDecimal due = OrderMoney.due(order);
            if (amount.compareTo(due) > 0) {
                throw new BadRequestAlertException(
                    "amountCollected exceeds due for " + order.getOrderCode() + " (due=" + due + ")",
                    ENTITY,
                    "amountExceedsDue"
                );
            }
            total = total.add(amount);

            OrderPayment payment = new OrderPayment();
            payment.setPaymentAt(now);
            payment.setAmount(amount);
            payment.setMethod(PaymentMethod.TM);
            payment.setPaymentKind(PaymentKind.SAU);
            payment.setNote("RECEIPT");
            payment.setCollectorUsername(actor);
            payment.setOrder(order);
            orderPaymentRepository.save(payment);

            order.setPaidAmount(paid.add(amount));
            shipmentOrderRepository.save(order);

            ReceiptOrderLine rol = new ReceiptOrderLine();
            rol.setAmountCollected(amount);
            rol.setOrder(order);
            lines.add(rol);
        }

        Receipt receipt = new Receipt();
        receipt.setReceiptCode(nextReceiptCode(office));
        receipt.setPayerName(req.payerName() == null || req.payerName().isBlank() ? "Khách" : req.payerName().trim());
        receipt.setPayerCode(req.payerCode());
        receipt.setTotalAmount(total);
        receipt.setCreatedAt(now);
        receipt.setCreatedByUsername(actor);
        receipt.setOffice(office);
        receipt = receiptRepository.save(receipt);
        for (ReceiptOrderLine rol : lines) {
            rol.setReceipt(receipt);
            receiptOrderLineRepository.save(rol);
        }
        return toReceiptDto(receipt, lines);
    }

    @Transactional(readOnly = true)
    public Page<ReceiptDTO> listReceipts(String officeCode, String createdBy, Pageable pageable) {
        Specification<Receipt> spec = Specification.where(null);
        if (officeCode != null && !officeCode.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("office").get("code"), officeCode.trim().toUpperCase()));
        }
        if (createdBy != null && !createdBy.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("createdByUsername"), createdBy.trim()));
        }
        return receiptRepository.findAll(spec, pageable).map(r -> toReceiptDto(r, receiptOrderLineRepository.findByReceipt_Id(r.getId())));
    }

    @Transactional(readOnly = true)
    public DayClosureDTO getDay(String officeCode, LocalDate businessDate) {
        Office office = requireOffice(officeCode);
        LocalDate date = businessDate != null ? businessDate : LocalDate.now(VN);
        return dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(office.getId(), date).map(this::toDayDto).orElse(null);
    }

    public DayClosureDTO closeDay(String officeCode, LocalDate businessDate) {
        Office office = requireOffice(officeCode);
        LocalDate date = businessDate != null ? businessDate : LocalDate.now(VN);
        DayClosure existing = dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(office.getId(), date).orElse(null);
        if (existing != null && existing.getStatus() == DayClosureStatus.CLOSED) {
            throw new BadRequestAlertException("Day already closed", ENTITY, "dayAlreadyClosed");
        }
        DayClosure closure = existing != null ? existing : new DayClosure();
        closure.setOffice(office);
        closure.setBusinessDate(date);
        closure.setStatus(DayClosureStatus.CLOSED);
        closure.setConfirmedByUsername(actor());
        closure.setConfirmedAt(Instant.now());
        closure.setReopenedAt(null);
        closure.setReopenedByUsername(null);
        closure = dayClosureRepository.save(closure);
        return toDayDto(closure);
    }

    public DayClosureDTO reopenDay(String officeCode, LocalDate businessDate) {
        Office office = requireOffice(officeCode);
        LocalDate date = businessDate != null ? businessDate : LocalDate.now(VN);
        DayClosure closure = dayClosureRepository
            .findFirstByOffice_IdAndBusinessDateOrderByIdDesc(office.getId(), date)
            .orElseThrow(() -> new BadRequestAlertException("No day closure found", ENTITY, "dayNotFound"));
        if (closure.getStatus() != DayClosureStatus.CLOSED) {
            throw new BadRequestAlertException("Day is not CLOSED", ENTITY, "dayNotClosed");
        }
        closure.setStatus(DayClosureStatus.REOPENED);
        closure.setReopenedByUsername(actor());
        closure.setReopenedAt(Instant.now());
        closure = dayClosureRepository.save(closure);
        return toDayDto(closure);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> collectionsReport(String officeCode, LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now(VN);
        Instant from = d.atStartOfDay(VN).toInstant();
        Instant to = d.plusDays(1).atStartOfDay(VN).toInstant();

        List<Receipt> receipts = receiptRepository.findAll((root, q, cb) -> {
            var pred = cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), from), cb.lessThan(root.get("createdAt"), to));
            if (officeCode != null && !officeCode.isBlank()) {
                pred = cb.and(pred, cb.equal(root.get("office").get("code"), officeCode.trim().toUpperCase()));
            }
            return pred;
        });

        Map<String, BigDecimal> byCollector = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Receipt r : receipts) {
            total = total.add(r.getTotalAmount());
            byCollector.merge(r.getCreatedByUsername(), r.getTotalAmount(), BigDecimal::add);
        }

        DayClosureDTO day = null;
        if (officeCode != null && !officeCode.isBlank()) {
            day = officeRepository
                .findOneByCode(officeCode.trim().toUpperCase())
                .flatMap(o -> dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(o.getId(), d))
                .map(this::toDayDto)
                .orElse(null);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("date", d.toString());
        out.put("officeCode", officeCode);
        out.put("totalAmount", total);
        out.put("receiptCount", receipts.size());
        out.put("byCollector", byCollector);
        out.put("dayClosure", day);
        out.put("paymentCount", orderPaymentRepository.countByPaymentAtGreaterThanEqualAndPaymentAtLessThan(from, to));

        // M1: unpaid residue (including DELIVERED underpay) — does not change totalAmount semantics
        Specification<ShipmentOrder> unpaidSpec = (root, q, cb) ->
            cb.and(
                cb.notEqual(root.get("status"), OrderStatus.CANCELLED),
                cb.notEqual(root.get("status"), OrderStatus.DRAFT),
                cb.greaterThan(root.get("fareAmount"), root.get("paidAmount"))
            );
        if (officeCode != null && !officeCode.isBlank()) {
            unpaidSpec = unpaidSpec.and((root, q, cb) ->
                cb.or(
                    cb.equal(root.get("fromOffice").get("code"), officeCode.trim().toUpperCase()),
                    cb.equal(root.get("toOffice").get("code"), officeCode.trim().toUpperCase())
                )
            );
        }
        List<ShipmentOrder> unpaidOrders = shipmentOrderRepository.findAll(unpaidSpec);
        BigDecimal unpaidDueTotal = BigDecimal.ZERO;
        long unpaidDeliveredCount = 0;
        for (ShipmentOrder o : unpaidOrders) {
            unpaidDueTotal = unpaidDueTotal.add(OrderMoney.due(o));
            if (o.getStatus() == OrderStatus.DELIVERED) {
                unpaidDeliveredCount++;
            }
        }
        out.put("unpaidOrderCount", unpaidOrders.size());
        out.put("unpaidDueTotal", unpaidDueTotal);
        out.put("unpaidDeliveredCount", unpaidDeliveredCount);
        return out;
    }

    private String nextReceiptCode(Office office) {
        String oc = office != null ? office.getCode() : "XX";
        String stamp = LocalDate.now(VN).format(DateTimeFormatter.ofPattern("yyMMdd"));
        String prefix = "PT" + oc + stamp;
        long seq = receiptRepository.countByReceiptCodeStartingWith(prefix) + 1;
        return prefix + "-" + String.format("%03d", seq);
    }

    private ReceiptDTO toReceiptDto(Receipt r, List<ReceiptOrderLine> lines) {
        List<Map<String, Object>> lineViews = new ArrayList<>();
        for (ReceiptOrderLine l : lines) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderCode", l.getOrder() != null ? l.getOrder().getOrderCode() : null);
            m.put("amountCollected", l.getAmountCollected());
            lineViews.add(m);
        }
        return new ReceiptDTO(
            r.getId(),
            r.getReceiptCode(),
            r.getPayerName(),
            r.getPayerCode(),
            r.getTotalAmount(),
            r.getCreatedAt(),
            r.getCreatedByUsername(),
            r.getOffice() != null ? r.getOffice().getCode() : null,
            lineViews
        );
    }

    private DayClosureDTO toDayDto(DayClosure c) {
        return new DayClosureDTO(
            c.getId(),
            c.getBusinessDate(),
            c.getStatus(),
            c.getOffice() != null ? c.getOffice().getCode() : null,
            c.getConfirmedByUsername(),
            c.getConfirmedAt(),
            c.getReopenedByUsername(),
            c.getReopenedAt()
        );
    }

    private Office requireOffice(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestAlertException("officeCode is required", ENTITY, "officeRequired");
        }
        return officeRepository
            .findOneByCode(code.trim().toUpperCase())
            .orElseThrow(() -> new BadRequestAlertException("Office not found", ENTITY, "officeNotFound"));
    }

    private String resolveDebtOwner(ShipmentOrder order) {
        if (order.getId() != null) {
            var lastPay = orderPaymentRepository.findFirstByOrder_IdOrderByPaymentAtDesc(order.getId());
            if (lastPay.isPresent()) {
                String collector = lastPay.get().getCollectorUsername();
                if (collector != null && !collector.isBlank()) {
                    return collector.trim();
                }
            }
        }
        if (order.getPickupStaffUsername() != null && !order.getPickupStaffUsername().isBlank()) {
            return order.getPickupStaffUsername().trim();
        }
        if (order.getId() != null) {
            for (OrderEvent event : orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId())) {
                if ("CREATED".equalsIgnoreCase(event.getAction())) {
                    String actor = event.getActorUsername();
                    if (actor != null && !actor.isBlank()) {
                        return actor.trim();
                    }
                    break;
                }
            }
        }
        return null;
    }

    private static String actor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }

    public record CandidateDTO(
        String orderCode,
        String receiverName,
        String receiverPhone,
        BigDecimal fareAmount,
        BigDecimal paidAmount,
        BigDecimal dueAmount,
        String status,
        String fromOfficeCode,
        String debtOwnerUsername
    ) {}

    public record ReceiptLineRequest(String orderCode, BigDecimal amountCollected) {}

    public record CreateReceiptRequest(String payerName, String payerCode, String officeCode, List<ReceiptLineRequest> lines) {}

    public record ReceiptDTO(
        Long id,
        String receiptCode,
        String payerName,
        String payerCode,
        BigDecimal totalAmount,
        Instant createdAt,
        String createdByUsername,
        String officeCode,
        List<Map<String, Object>> lines
    ) {}

    public record DayClosureDTO(
        Long id,
        LocalDate businessDate,
        DayClosureStatus status,
        String officeCode,
        String confirmedByUsername,
        Instant confirmedAt,
        String reopenedByUsername,
        Instant reopenedAt
    ) {}
}
