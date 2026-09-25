package com.mycompany.myapp.service.finance;

import com.mycompany.myapp.domain.DayClosure;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ReceiptOrderLine;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.order.OrderMoney;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final StaffProfileRepository staffProfileRepository;
    private final DayClosureGuard dayClosureGuard;

    public FinanceFacadeService(
        ShipmentOrderRepository shipmentOrderRepository,
        ReceiptRepository receiptRepository,
        ReceiptOrderLineRepository receiptOrderLineRepository,
        DayClosureRepository dayClosureRepository,
        OfficeRepository officeRepository,
        OrderPaymentRepository orderPaymentRepository,
        OrderEventRepository orderEventRepository,
        StaffProfileRepository staffProfileRepository,
        DayClosureGuard dayClosureGuard
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.receiptRepository = receiptRepository;
        this.receiptOrderLineRepository = receiptOrderLineRepository;
        this.dayClosureRepository = dayClosureRepository;
        this.officeRepository = officeRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.orderEventRepository = orderEventRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.dayClosureGuard = dayClosureGuard;
    }

    @Transactional(readOnly = true)
    public List<CandidateDTO> candidates(String officeCode, String keyword) {
        // Mỗi đơn tối đa 2 dòng: phần VP gửi (SENDER) và phần giao (DELIVERY) — xem ReceiptSettlement.
        Specification<ShipmentOrder> spec = (root, q, cb) -> {
            var status = root.get("status");
            var guiTraPastWh = cb.and(
                cb.equal(root.get("paymentTerm"), PaymentTerm.GUI_TRA),
                cb.or(
                    cb.isNotNull(root.get("forwardStage")),
                    status.in(
                        OrderStatus.IN_TRANSIT,
                        OrderStatus.WAITING,
                        OrderStatus.AT_DEST,
                        OrderStatus.OUT_FOR_DELIVERY,
                        OrderStatus.FAILED_DELIVERY
                    )
                )
            );
            return cb.and(
                status.in(OrderStatus.DRAFT, OrderStatus.CANCELLED).not(),
                cb.or(cb.equal(status, OrderStatus.DELIVERED), cb.greaterThan(root.get("paidAmount"), BigDecimal.ZERO), guiTraPastWh)
            );
        };
        String scoped = officeCode == null || officeCode.isBlank() ? null : officeCode.trim().toUpperCase();
        if (scoped != null) {
            spec = spec.and((root, q, cb) -> {
                var to = root.join("toOffice", JoinType.LEFT);
                var fin = root.join("finalToOffice", JoinType.LEFT);
                var atFrom = cb.equal(root.get("fromOffice").get("code"), scoped);
                var atTo = cb.or(cb.equal(to.get("code"), scoped), cb.equal(fin.get("code"), scoped));
                return cb.or(atFrom, cb.and(cb.equal(root.get("status"), OrderStatus.DELIVERED), atTo));
            });
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
        List<ShipmentOrder> orders = shipmentOrderRepository.findAll(spec);
        Map<Long, ReceiptSettlement.Totals> totals = loadSettlementTotals(
            orders.stream().map(ShipmentOrder::getId).filter(Objects::nonNull).toList()
        );
        List<CandidateDTO> out = new ArrayList<>();
        for (ShipmentOrder o : orders) {
            if (out.size() >= 300) {
                break;
            }
            ReceiptSettlement.Split s = ReceiptSettlement.split(o, totals.getOrDefault(o.getId(), ReceiptSettlement.Totals.ZERO));
            String fromCode = o.getFromOffice() != null ? o.getFromOffice().getCode() : null;
            if (s.senderOut().signum() > 0 && (scoped == null || scoped.equals(fromCode))) {
                out.add(toCandidate(o, s.senderOut(), ReceiptSettlement.SENDER, resolveSenderOwner(o)));
            }
            if (s.deliveryOut().signum() > 0 && (scoped == null || scoped.equals(fromCode) || atReceiverOffice(o, scoped))) {
                out.add(toCandidate(o, s.deliveryOut(), ReceiptSettlement.DELIVERY, resolveDeliveryActor(o)));
            }
        }
        return out;
    }

    private CandidateDTO toCandidate(ShipmentOrder o, BigDecimal amount, String portion, String owner) {
        return new CandidateDTO(
            o.getOrderCode(),
            o.getReceiverName(),
            o.getReceiverPhone(),
            o.getFareAmount(),
            o.getPaidAmount(),
            amount,
            o.getStatus().name(),
            o.getFromOffice() != null ? o.getFromOffice().getCode() : null,
            owner,
            portion,
            resolveCollectedAt(o, portion)
        );
    }

    /**
     * Thời điểm nhận tiền khách theo phần SENDER/DELIVERY (paymentAt / event POD / WH_IN).
     * Không dùng order.createdAt trừ khi không còn nguồn nào khác.
     */
    private Instant resolveCollectedAt(ShipmentOrder order, String portion) {
        if (order.getId() == null) {
            return order.getCreatedAt();
        }
        boolean delivery = ReceiptSettlement.DELIVERY.equals(portion);
        for (OrderPayment p : orderPaymentRepository.findByOrder_IdOrderByPaymentAtDesc(order.getId())) {
            boolean delSide =
                ReceiptSettlement.isDeliverySidePayment(p.getPaymentKind(), p.getNote()) || p.getPaymentKind() == PaymentKind.COD;
            if (delivery != delSide) {
                continue;
            }
            String note = p.getNote() == null ? "" : p.getNote().trim().toUpperCase();
            if (note.startsWith("RECEIPT")) {
                continue;
            }
            if (p.getPaymentAt() != null) {
                return p.getPaymentAt();
            }
        }
        List<OrderEvent> events = orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId());
        for (int i = events.size() - 1; i >= 0; i--) {
            OrderEvent event = events.get(i);
            String action = event.getAction() == null ? "" : event.getAction().trim().toUpperCase();
            boolean match = delivery
                ? ("POD".equals(action) || "POD_QUAY".equals(action) || "POD_HOME".equals(action) || "DELIVERED".equals(action))
                : ("WAREHOUSE_RECEIVE".equals(action) ||
                    "WH_IN".equals(action) ||
                    "CONFIRM".equals(action) ||
                    "CREATED".equals(action) ||
                    "CREATE".equals(action));
            if (match && event.getEventAt() != null) {
                return event.getEventAt();
            }
        }
        return order.getCreatedAt();
    }

    private static boolean atReceiverOffice(ShipmentOrder o, String code) {
        return (
            (o.getToOffice() != null && code.equals(o.getToOffice().getCode())) ||
            (o.getFinalToOffice() != null && code.equals(o.getFinalToOffice().getCode()))
        );
    }

    private Map<Long, ReceiptSettlement.Totals> loadSettlementTotals(List<Long> orderIds) {
        Map<Long, BigDecimal[]> acc = new HashMap<>();
        for (int i = 0; i < orderIds.size(); i += 500) {
            List<Long> chunk = orderIds.subList(i, Math.min(orderIds.size(), i + 500));
            for (Object[] row : orderPaymentRepository.sumGroupedByOrderIds(chunk)) {
                BigDecimal[] a = acc.computeIfAbsent((Long) row[0], k -> zeros3());
                PaymentKind kind = (PaymentKind) row[1];
                BigDecimal amount = toBigDecimal(row[3]);
                if (ReceiptSettlement.isDeliverySidePayment(kind, (String) row[2])) {
                    a[0] = a[0].add(amount);
                } else if (kind == PaymentKind.COD) {
                    a[1] = a[1].add(amount);
                }
            }
            for (Object[] row : receiptOrderLineRepository.sumAmountByOrderIds(chunk)) {
                BigDecimal[] a = acc.computeIfAbsent((Long) row[0], k -> zeros3());
                a[2] = a[2].add(toBigDecimal(row[1]));
            }
        }
        Map<Long, ReceiptSettlement.Totals> out = new HashMap<>();
        acc.forEach((id, a) -> out.put(id, new ReceiptSettlement.Totals(a[0], a[1], a[2])));
        return out;
    }

    private static BigDecimal[] zeros3() {
        return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }

    public ReceiptDTO createReceipt(CreateReceiptRequest req) {
        if (req == null || req.lines() == null || req.lines().isEmpty()) {
            throw new BadRequestAlertException("Receipt lines required", ENTITY, "receiptLinesRequired");
        }
        Office office = resolveReceiptOffice(req.officeCode(), req.payerCode());
        Instant now = Instant.now();
        String actor = actor();
        if (office != null) {
            dayClosureGuard.assertOfficeOpen(office);
        }
        BigDecimal total = BigDecimal.ZERO;
        List<ReceiptOrderLine> lines = new ArrayList<>();
        Set<Long> seenOrderIds = new HashSet<>();
        for (ReceiptLineRequest line : req.lines()) {
            if (line == null || line.orderCode() == null || line.orderCode().isBlank()) {
                throw new BadRequestAlertException("orderCode required on receipt line", ENTITY, "orderCodeRequired");
            }
            ShipmentOrder order = shipmentOrderRepository
                .findOneByOrderCodeOrDraftCode(line.orderCode().trim())
                .orElseThrow(() -> new BadRequestAlertException("Order not found: " + line.orderCode(), ENTITY, "orderNotFound"));
            dayClosureGuard.assertCollectionMutable(order);
            BigDecimal amount = line.amountCollected();
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestAlertException("amountCollected must be >= 0", ENTITY, "amountInvalid");
            }
            if (order.getId() != null && !seenOrderIds.add(order.getId())) {
                throw new BadRequestAlertException("Duplicate order on receipt: " + order.getOrderCode(), ENTITY, "duplicateOrderLine");
            }
            ReceiptSettlement.Split s = ReceiptSettlement.split(
                order,
                order.getId() == null
                    ? ReceiptSettlement.Totals.ZERO
                    : loadSettlementTotals(List.of(order.getId())).getOrDefault(order.getId(), ReceiptSettlement.Totals.ZERO)
            );
            String portion = line.portion() == null || line.portion().isBlank() ? null : line.portion().trim().toUpperCase();
            if (portion != null && !ReceiptSettlement.SENDER.equals(portion) && !ReceiptSettlement.DELIVERY.equals(portion)) {
                throw new BadRequestAlertException("Invalid receipt portion: " + line.portion(), ENTITY, "portionInvalid");
            }
            BigDecimal collectable = ReceiptSettlement.SENDER.equals(portion)
                ? s.senderOut()
                : ReceiptSettlement.DELIVERY.equals(portion) ? s.deliveryOut() : s.totalOut();
            if (amount.compareTo(collectable) > 0) {
                throw new BadRequestAlertException(
                    "amountCollected exceeds collectable for " +
                    order.getOrderCode() +
                    " (portion=" +
                    (portion == null ? "ALL" : portion) +
                    ", collectable=" +
                    collectable +
                    ", sender=" +
                    s.senderOut() +
                    ", delivery=" +
                    s.deliveryOut() +
                    ")",
                    ENTITY,
                    "amountExceedsDue"
                );
            }
            total = total.add(amount);

            // Không chỉ định phần: ưu tiên phần giao, dư mới vào phần VP gửi.
            BigDecimal toDelivery = ReceiptSettlement.SENDER.equals(portion)
                ? BigDecimal.ZERO
                : ReceiptSettlement.DELIVERY.equals(portion) ? amount : amount.min(s.deliveryOut());
            BigDecimal toSender = amount.subtract(toDelivery);
            // Tiền đã thu (đã vào paidAmount) chỉ lập dòng phiếu; phần còn nợ mới ghi payment mới.
            BigDecimal senderFare = toSender.subtract(toSender.min(s.senderHeldOut())).min(s.senderFareDue());
            BigDecimal deliveryRest = toDelivery.subtract(toDelivery.min(s.deliveryHeldOut()));
            BigDecimal deliveryFare = deliveryRest.min(s.deliveryFareDue());
            BigDecimal toCod = deliveryRest.subtract(deliveryFare).min(s.codDue());

            savePayment(order, senderFare, PaymentKind.SAU, ReceiptSettlement.NOTE_RECEIPT_SENDER, actor, now);
            savePayment(order, deliveryFare, PaymentKind.SAU, ReceiptSettlement.NOTE_RECEIPT_DELIVERY, actor, now);
            savePayment(order, toCod, PaymentKind.COD, ReceiptSettlement.NOTE_RECEIPT_COD, actor, now);
            BigDecimal fareAdded = senderFare.add(deliveryFare);
            if (fareAdded.signum() > 0) {
                order.setPaidAmount(OrderMoney.nz(order.getPaidAmount()).add(fareAdded));
                shipmentOrderRepository.save(order);
            }

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

    private void savePayment(ShipmentOrder order, BigDecimal amount, PaymentKind kind, String note, String actor, Instant at) {
        if (amount.signum() <= 0) {
            return;
        }
        OrderPayment payment = new OrderPayment();
        payment.setPaymentAt(at);
        payment.setAmount(amount);
        payment.setMethod(PaymentMethod.TM);
        payment.setPaymentKind(kind);
        payment.setNote(note);
        payment.setCollectorUsername(actor);
        payment.setOrder(order);
        orderPaymentRepository.save(payment);
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

    public ReceiptDTO confirmReceipt(String receiptCode, ConfirmReceiptRequest body) {
        if (receiptCode == null || receiptCode.isBlank()) {
            throw new BadRequestAlertException("receiptCode is required", ENTITY, "receiptCodeRequired");
        }
        String proof = body != null ? body.proofImage() : null;
        if (proof == null || proof.isBlank()) {
            throw new BadRequestAlertException("Transaction proof image is required", ENTITY, "receiptProofRequired");
        }
        if (proof.length() > 2_500_000) {
            throw new BadRequestAlertException("Proof image too large", ENTITY, "receiptProofTooLarge");
        }
        Receipt receipt = receiptRepository
            .findOneByReceiptCode(receiptCode.trim())
            .orElseThrow(() -> new BadRequestAlertException("Receipt not found", ENTITY, "receiptNotFound"));
        if (receipt.getConfirmedAt() != null) {
            throw new BadRequestAlertException("Receipt already confirmed", ENTITY, "receiptAlreadyConfirmed");
        }
        receipt.setConfirmedAt(Instant.now());
        receipt.setConfirmedByUsername(actor());
        receipt.setConfirmProofImage(proof.trim());
        receipt = receiptRepository.save(receipt);
        return toReceiptDto(receipt, receiptOrderLineRepository.findByReceipt_Id(receipt.getId()));
    }

    /**
     * Hoàn tác xác nhận thu — chỉ trong cùng ngày lịch (Asia/Ho_Chi_Minh) với lúc xác nhận.
     * Sau 0h đêm không hoàn tác phiếu đã tích ngày hôm trước. Idempotent nếu chưa confirm.
     */
    public ReceiptDTO unconfirmReceipt(String receiptCode) {
        if (receiptCode == null || receiptCode.isBlank()) {
            throw new BadRequestAlertException("receiptCode is required", ENTITY, "receiptCodeRequired");
        }
        Receipt receipt = receiptRepository
            .findOneByReceiptCode(receiptCode.trim())
            .orElseThrow(() -> new BadRequestAlertException("Receipt not found", ENTITY, "receiptNotFound"));
        Instant confirmedAt = receipt.getConfirmedAt();
        if (confirmedAt == null) {
            // Đã ở trạng thái chưa xác nhận (double-click / FE lệch BE) — coi như thành công.
            return toReceiptDto(receipt, receiptOrderLineRepository.findByReceipt_Id(receipt.getId()));
        }
        LocalDate confirmDay = confirmedAt.atZone(VN).toLocalDate();
        LocalDate today = LocalDate.now(VN);
        if (!confirmDay.equals(today)) {
            throw new BadRequestAlertException("Cannot unconfirm after midnight of confirmation day", ENTITY, "receiptUnconfirmExpired");
        }
        receipt.setConfirmedAt(null);
        receipt.setConfirmedByUsername(null);
        receipt.setConfirmProofImage(null);
        receipt = receiptRepository.save(receipt);
        return toReceiptDto(receipt, receiptOrderLineRepository.findByReceipt_Id(receipt.getId()));
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

    /**
     * VP phiếu thu = VP người nộp tiền khi admin/KT (ALL) tạo hộ.
     * Ưu tiên officeCode request; nếu trống/ALL thì lấy từ StaffProfile theo payerCode (login hoặc mã NV).
     */
    private Office resolveReceiptOffice(String officeCode, String payerCode) {
        if (officeCode != null && !officeCode.isBlank() && !"ALL".equalsIgnoreCase(officeCode.trim())) {
            return officeRepository
                .findOneByCode(officeCode.trim().toUpperCase())
                .orElseThrow(() -> new BadRequestAlertException("Office not found", ENTITY, "officeNotFound"));
        }
        if (payerCode == null || payerCode.isBlank()) {
            return null;
        }
        String key = payerCode.trim();
        StaffProfile profile = staffProfileRepository
            .findOneByUserLoginIgnoreCase(key)
            .or(() -> staffProfileRepository.findOneByStaffCodeIgnoreCase(key))
            .orElse(null);
        if (profile == null || profile.getOffice() == null) {
            return null;
        }
        if (Boolean.TRUE.equals(profile.getScopeAllOffices())) {
            return null;
        }
        return profile.getOffice();
    }

    private ReceiptDTO toReceiptDto(Receipt r, List<ReceiptOrderLine> lines) {
        List<Map<String, Object>> lineViews = new ArrayList<>();
        Instant customerPaidAt = null;
        for (ReceiptOrderLine l : lines) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderCode", l.getOrder() != null ? l.getOrder().getOrderCode() : null);
            m.put("amountCollected", l.getAmountCollected());
            lineViews.add(m);
            if (l.getOrder() != null && l.getOrder().getId() != null) {
                Instant paid = resolveCustomerPaidAtForOrder(l.getOrder().getId());
                if (paid != null && (customerPaidAt == null || paid.isAfter(customerPaidAt))) {
                    customerPaidAt = paid;
                }
            }
        }
        if (customerPaidAt == null) {
            customerPaidAt = r.getCreatedAt();
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
            lineViews,
            r.getConfirmedAt(),
            r.getConfirmedByUsername(),
            customerPaidAt,
            r.getConfirmProofImage()
        );
    }

    /** Thời điểm nhận tiền khách gần nhất trên đơn (bỏ RECEIPT_* nộp quỹ). */
    private Instant resolveCustomerPaidAtForOrder(Long orderId) {
        for (OrderPayment p : orderPaymentRepository.findByOrder_IdOrderByPaymentAtDesc(orderId)) {
            String note = p.getNote() == null ? "" : p.getNote().trim().toUpperCase();
            if (note.startsWith("RECEIPT")) {
                continue;
            }
            if (p.getPaymentAt() != null) {
                return p.getPaymentAt();
            }
        }
        List<OrderEvent> events = orderEventRepository.findByOrder_IdOrderByEventAtAsc(orderId);
        for (int i = events.size() - 1; i >= 0; i--) {
            OrderEvent event = events.get(i);
            String action = event.getAction() == null ? "" : event.getAction().trim().toUpperCase();
            if (
                ("POD".equals(action) ||
                    "POD_QUAY".equals(action) ||
                    "POD_HOME".equals(action) ||
                    "DELIVERED".equals(action) ||
                    "WAREHOUSE_RECEIVE".equals(action) ||
                    "WH_IN".equals(action)) &&
                event.getEventAt() != null
            ) {
                return event.getEventAt();
            }
        }
        return null;
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

    /** Chủ phần SENDER: người thu tiền phía gửi gần nhất; chưa thu → NV nhập kho gửi / tạo đơn. */
    private String resolveSenderOwner(ShipmentOrder order) {
        if (order.getId() != null) {
            for (OrderPayment p : orderPaymentRepository.findByOrder_IdOrderByPaymentAtDesc(order.getId())) {
                boolean senderSide =
                    (p.getPaymentKind() == PaymentKind.TRUOC || p.getPaymentKind() == PaymentKind.SAU) &&
                    !ReceiptSettlement.isDeliverySidePayment(p.getPaymentKind(), p.getNote()) &&
                    !ReceiptSettlement.NOTE_RECEIPT_SENDER.equals(p.getNote());
                String collector = p.getCollectorUsername();
                if (senderSide && collector != null && !collector.isBlank()) {
                    return collector.trim();
                }
            }
        }
        return resolveSenderWhActor(order);
    }

    /** Người nhập kho gửi (WAREHOUSE_RECEIVE) hoặc fallback tạo đơn / lấy hàng. */
    private String resolveSenderWhActor(ShipmentOrder order) {
        if (order.getId() != null) {
            List<OrderEvent> events = orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId());
            for (int i = events.size() - 1; i >= 0; i--) {
                OrderEvent event = events.get(i);
                String action = event.getAction() == null ? "" : event.getAction().trim().toUpperCase();
                if ("WAREHOUSE_RECEIVE".equals(action) || "WH_IN".equals(action) || "CONFIRM".equals(action)) {
                    String actor = event.getActorUsername();
                    if (actor != null && !actor.isBlank()) {
                        return actor.trim();
                    }
                }
            }
        }
        return resolveDebtOwner(order);
    }

    /**
     * Người làm đơn ra khỏi kho giao (POD / giao thành công) — chịu trách nhiệm trên phiếu thu nhận trả/COD.
     */
    private String resolveDeliveryActor(ShipmentOrder order) {
        if (order.getId() != null) {
            List<OrderEvent> events = orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId());
            for (int i = events.size() - 1; i >= 0; i--) {
                OrderEvent event = events.get(i);
                String action = event.getAction() == null ? "" : event.getAction().trim().toUpperCase();
                if ("POD".equals(action) || "POD_QUAY".equals(action) || "POD_HOME".equals(action) || "DELIVERED".equals(action)) {
                    String actor = event.getActorUsername();
                    if (actor != null && !actor.isBlank()) {
                        return actor.trim();
                    }
                }
            }
            var lastPay = orderPaymentRepository.findFirstByOrder_IdOrderByPaymentAtDesc(order.getId());
            if (lastPay.isPresent()) {
                String note = lastPay.get().getNote() == null ? "" : lastPay.get().getNote().toUpperCase();
                String collector = lastPay.get().getCollectorUsername();
                if (collector != null && !collector.isBlank() && note.contains("POD")) {
                    return collector.trim();
                }
            }
        }
        return resolveDebtOwner(order);
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
        String debtOwnerUsername,
        /** SENDER | DELIVERY */
        String portion,
        /** Thời điểm nhận tiền khách (payment/POD/WH), ISO instant. */
        Instant collectedAt
    ) {}

    /** portion null = tự phân bổ (phần giao trước). */
    public record ReceiptLineRequest(String orderCode, BigDecimal amountCollected, String portion) {}

    public record CreateReceiptRequest(String payerName, String payerCode, String officeCode, List<ReceiptLineRequest> lines) {}

    public record ConfirmReceiptRequest(String proofImage) {}

    public record ReceiptDTO(
        Long id,
        String receiptCode,
        String payerName,
        String payerCode,
        BigDecimal totalAmount,
        Instant createdAt,
        String createdByUsername,
        String officeCode,
        List<Map<String, Object>> lines,
        Instant confirmedAt,
        String confirmedByUsername,
        /** Thời điểm nhận tiền khách (payment/POD), fallback createdAt. */
        Instant customerPaidAt,
        /** Ảnh chứng từ khi xác nhận thu (data-URL). */
        String confirmProofImage
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
