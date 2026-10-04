package com.mycompany.myapp.service.finance;

import com.mycompany.myapp.domain.AuditLog;
import com.mycompany.myapp.domain.DayClosure;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ReceiptOrderLine;
import com.mycompany.myapp.domain.ReceiptWaiver;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.AuditLogRepository;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.ReceiptListRow;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.repository.ReceiptWaiverRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.audit.AuditRecorder;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.order.OrderMoney;
import com.mycompany.myapp.service.order.OrderStatusTransitions;
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
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FinanceFacadeService {

    private static final String ENTITY = "finance";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String AUDIT_RECEIPT = "Receipt";
    private static final String AUDIT_DUE = "ReceiptDue";
    private static final Set<String> CUSTOMER_PAID_EVENT_ACTIONS = Set.of(
        "POD",
        "POD_QUAY",
        "POD_HOME",
        "DELIVERED",
        "WAREHOUSE_RECEIVE",
        "WH_IN"
    );

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptOrderLineRepository receiptOrderLineRepository;
    private final DayClosureRepository dayClosureRepository;
    private final OfficeRepository officeRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final OrderEventRepository orderEventRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final DayClosureGuard dayClosureGuard;
    private final ReceiptWaiverRepository receiptWaiverRepository;
    private final AuditRecorder auditRecorder;
    private final AuditLogRepository auditLogRepository;
    private final StaffAccessService staffAccessService;

    public FinanceFacadeService(
        ShipmentOrderRepository shipmentOrderRepository,
        ReceiptRepository receiptRepository,
        ReceiptOrderLineRepository receiptOrderLineRepository,
        DayClosureRepository dayClosureRepository,
        OfficeRepository officeRepository,
        OrderPaymentRepository orderPaymentRepository,
        OrderEventRepository orderEventRepository,
        StaffProfileRepository staffProfileRepository,
        DayClosureGuard dayClosureGuard,
        ReceiptWaiverRepository receiptWaiverRepository,
        AuditRecorder auditRecorder,
        AuditLogRepository auditLogRepository,
        StaffAccessService staffAccessService
    ) {
        this.receiptWaiverRepository = receiptWaiverRepository;
        this.auditRecorder = auditRecorder;
        this.auditLogRepository = auditLogRepository;
        this.staffAccessService = staffAccessService;
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
        return candidates(officeCode, keyword, null);
    }

    /**
     * Tiền NV đang giữ ở mọi VP (NV đổi VP vẫn thấy tiền VP cũ). Chỉ quét đơn NV có dính vào — người giữ tiền luôn
     * suy ra từ actor sự kiện / người thu / NV lấy hàng; lọc đúng chủ nợ do bên gọi làm.
     */
    @Transactional(readOnly = true)
    public List<CandidateDTO> candidatesInvolving(String login) {
        if (login == null || login.isBlank()) {
            return List.of();
        }
        return candidates(null, null, login.trim().toLowerCase());
    }

    private List<CandidateDTO> candidates(String officeCode, String keyword, String involvedLogin) {
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
                cb.or(cb.equal(status, OrderStatus.DELIVERED), cb.greaterThan(root.get("paidAmount"), BigDecimal.ZERO), guiTraPastWh),
                notFullySettled(root, q, cb)
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
        if (involvedLogin != null) {
            spec = spec.and((root, q, cb) -> {
                var ev = q.subquery(Long.class);
                var e = ev.from(OrderEvent.class);
                ev.select(e.get("id")).where(cb.equal(e.get("order"), root), cb.equal(cb.lower(e.get("actorUsername")), involvedLogin));
                var pay = q.subquery(Long.class);
                var p = pay.from(OrderPayment.class);
                pay
                    .select(p.get("id"))
                    .where(cb.equal(p.get("order"), root), cb.equal(cb.lower(p.get("collectorUsername")), involvedLogin));
                return cb.or(cb.equal(cb.lower(root.get("pickupStaffUsername")), involvedLogin), cb.exists(ev), cb.exists(pay));
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
        List<Long> ids = orders.stream().map(ShipmentOrder::getId).filter(Objects::nonNull).toList();
        Map<Long, ReceiptSettlement.Totals> totals = loadSettlementTotals(ids);
        Map<Long, BigDecimal[]> waived = loadWaived(ids);
        List<CandidateDTO> out = new ArrayList<>();
        Map<String, String> ownerNames = new HashMap<>();
        for (ShipmentOrder o : orders) {
            BigDecimal[] outs = outstanding(o, totals.getOrDefault(o.getId(), ReceiptSettlement.Totals.ZERO), waived.get(o.getId()));
            String fromCode = o.getFromOffice() != null ? o.getFromOffice().getCode() : null;
            if (outs[0].signum() > 0 && (scoped == null || scoped.equals(fromCode))) {
                String owner = resolveSenderOwner(o);
                out.add(toCandidate(o, outs[0], ReceiptSettlement.SENDER, owner, ownerName(ownerNames, owner)));
            }
            if (outs[1].signum() > 0 && (scoped == null || scoped.equals(fromCode) || atReceiverOffice(o, scoped))) {
                String owner = resolveDeliveryActor(o);
                out.add(toCandidate(o, outs[1], ReceiptSettlement.DELIVERY, owner, ownerName(ownerNames, owner)));
            }
        }
        return out;
    }

    private String ownerName(Map<String, String> cache, String login) {
        if (login == null || login.isBlank()) {
            return null;
        }
        return cache.computeIfAbsent(login.toLowerCase(), k -> {
            String name = displayNameOf(login);
            return name == null || name.isBlank() ? "" : name.trim();
        });
    }

    /**
     * Lọc sẵn trong DB: đã lập phiếu + hủy nộp < mức tối đa có thể phải nộp (chặn trên của
     * {@link ReceiptSettlement#split}). Đơn đã nộp đủ không bị nạp lại mỗi lần mở màn.
     */
    private static jakarta.persistence.criteria.Predicate notFullySettled(
        jakarta.persistence.criteria.Root<ShipmentOrder> root,
        jakarta.persistence.criteria.CriteriaQuery<?> q,
        jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        var receipted = q.subquery(BigDecimal.class);
        var line = receipted.from(ReceiptOrderLine.class);
        receipted
            .select(cb.coalesce(cb.sum(line.<BigDecimal>get("amountCollected")), BigDecimal.ZERO))
            .where(cb.equal(line.get("order"), root));
        var waivedSum = q.subquery(BigDecimal.class);
        var waiver = waivedSum.from(ReceiptWaiver.class);
        waivedSum.select(cb.coalesce(cb.sum(waiver.<BigDecimal>get("amount")), BigDecimal.ZERO)).where(cb.equal(waiver.get("order"), root));

        var status = root.get("status");
        jakarta.persistence.criteria.Expression<BigDecimal> paid = cb.coalesce(root.<BigDecimal>get("paidAmount"), BigDecimal.ZERO);
        jakarta.persistence.criteria.Expression<BigDecimal> fare = cb.coalesce(root.<BigDecimal>get("fareAmount"), BigDecimal.ZERO);
        jakarta.persistence.criteria.Expression<BigDecimal> cod = cb.coalesce(root.<BigDecimal>get("codAmount"), BigDecimal.ZERO);
        jakarta.persistence.criteria.Expression<BigDecimal> paidOrFare = cb
            .<BigDecimal>selectCase()
            .when(cb.greaterThan(paid, fare), paid)
            .otherwise(fare);
        var delivered = cb.equal(status, OrderStatus.DELIVERED);
        var credit = cb.isTrue(root.get("onCredit"));
        jakarta.persistence.criteria.Expression<BigDecimal> bound = cb
            .<BigDecimal>selectCase()
            .when(status.in(OrderStatus.RETURNING, OrderStatus.RETURNED), paid)
            .when(cb.and(credit, delivered), cb.sum(paid, cod))
            .when(credit, paid)
            .when(delivered, cb.sum(paidOrFare, cod))
            .otherwise(paidOrFare);
        return cb.lessThan(cb.sum(receipted, waivedSum), bound);
    }

    /**
     * Chặn huỷ đơn khi nhân viên còn giữ tiền đã thu của khách mà chưa lập phiếu thu
     * (cước khách chưa trả không tính). Đơn huỷ rời "Đơn cần nộp" nên tiền sẽ mất dấu.
     */
    @Transactional(readOnly = true)
    public void assertNoHeldMoney(String orderCode) {
        ShipmentOrder order = shipmentOrderRepository.findOneByOrderCodeOrDraftCode(orderCode.trim()).orElse(null);
        if (order == null || order.getId() == null) {
            return;
        }
        BigDecimal[] held = heldOutstanding(order);
        BigDecimal total = held[0].add(held[1]);
        if (total.signum() > 0) {
            throw new BadRequestAlertException(
                "Đơn " + order.getOrderCode() + " còn " + money(total) + " đã thu chưa nộp — lập phiếu thu hoặc hủy nộp trước khi hủy đơn",
                ENTITY,
                "cancelHasHeldMoney"
            );
        }
    }

    /**
     * Trước khi hủy đơn: admin thì tự hủy nộp khoản đã thu chưa nộp (coi như đã hoàn tiền khách);
     * vai trò khác vẫn bị chặn như {@link #assertNoHeldMoney}.
     */
    public void settleHeldMoneyForCancel(String orderCode, String cancelReason) {
        if (!staffAccessService.isSystemAdmin()) {
            assertNoHeldMoney(orderCode);
            return;
        }
        ShipmentOrder order = shipmentOrderRepository.findOneByOrderCodeOrDraftCode(orderCode.trim()).orElse(null);
        if (order == null || order.getId() == null || !OrderStatusTransitions.canTransition(order.getStatus(), OrderStatus.CANCELLED)) {
            return;
        }
        BigDecimal[] held = heldOutstanding(order);
        if (held[0].add(held[1]).signum() <= 0) {
            return;
        }
        dayClosureGuard.assertCollectionMutable(order);
        String detail = cancelReason == null ? "" : cancelReason.trim();
        String reason = detail.isEmpty() ? "Huỷ đơn" : "Huỷ đơn · " + detail;
        if (reason.length() > 255) {
            reason = reason.substring(0, 255);
        }
        Instant now = Instant.now();
        String actor = actor();
        for (int i = 0; i < 2; i++) {
            BigDecimal amount = held[i];
            if (amount.signum() <= 0) {
                continue;
            }
            String portion = i == 0 ? ReceiptSettlement.SENDER : ReceiptSettlement.DELIVERY;
            String owner = i == 0 ? resolveSenderOwner(order) : resolveDeliveryActor(order);
            ReceiptWaiver w = new ReceiptWaiver();
            w.setOrder(order);
            w.setPortion(portion);
            w.setAmount(amount);
            w.setOwnerUsername(owner);
            w.setReason(reason);
            w.setWaivedAt(now);
            w.setWaivedByUsername(actor);
            receiptWaiverRepository.save(w);
            auditRecorder.record(
                "RECEIPT_DUE_WAIVE",
                AUDIT_DUE,
                order.getOrderCode(),
                (i == 0 ? "Phần VP gửi" : "Phần giao") +
                " · " +
                money(amount) +
                " · người nộp: " +
                (owner == null ? "—" : owner) +
                " · lý do: " +
                reason
            );
        }
    }

    /** [phần VP gửi, phần giao] tiền đã thu nhưng còn phải nộp (chưa lập phiếu, chưa hủy nộp). */
    private BigDecimal[] heldOutstanding(ShipmentOrder order) {
        ReceiptSettlement.Totals totals = loadSettlementTotals(List.of(order.getId())).getOrDefault(
            order.getId(),
            ReceiptSettlement.Totals.ZERO
        );
        ReceiptSettlement.Split s = ReceiptSettlement.split(order, totals);
        BigDecimal[] outs = outstanding(order, totals, loadWaived(List.of(order.getId())).get(order.getId()));
        return new BigDecimal[] { s.senderHeldOut().min(outs[0]), s.deliveryHeldOut().min(outs[1]) };
    }

    /** Còn phải nộp [phần VP gửi, phần giao] sau khi trừ khoản admin đã hủy nộp. */
    private static BigDecimal[] outstanding(ShipmentOrder o, ReceiptSettlement.Totals totals, BigDecimal[] waived) {
        ReceiptSettlement.Split s = ReceiptSettlement.split(o, totals);
        BigDecimal[] w = waived == null ? zeros2() : waived;
        return new BigDecimal[] { nonNegative(s.senderOut().subtract(w[0])), nonNegative(s.deliveryOut().subtract(w[1])) };
    }

    /** orderId → [đã hủy nộp phần VP gửi, phần giao]. */
    private Map<Long, BigDecimal[]> loadWaived(List<Long> orderIds) {
        Map<Long, BigDecimal[]> out = new HashMap<>();
        for (int i = 0; i < orderIds.size(); i += 500) {
            List<Long> chunk = orderIds.subList(i, Math.min(orderIds.size(), i + 500));
            for (Object[] row : receiptWaiverRepository.sumByOrderIds(chunk)) {
                BigDecimal[] a = out.computeIfAbsent((Long) row[0], k -> zeros2());
                int idx = ReceiptSettlement.DELIVERY.equals(row[1]) ? 1 : 0;
                a[idx] = a[idx].add(toBigDecimal(row[2]));
            }
        }
        return out;
    }

    private static BigDecimal[] zeros2() {
        return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
    }

    private static BigDecimal nonNegative(BigDecimal v) {
        return v.signum() < 0 ? BigDecimal.ZERO : v;
    }

    private CandidateDTO toCandidate(ShipmentOrder o, BigDecimal amount, String portion, String owner, String ownerName) {
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
            ownerName == null || ownerName.isEmpty() ? null : ownerName,
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
            ReceiptSettlement.Totals orderTotals = order.getId() == null
                ? ReceiptSettlement.Totals.ZERO
                : loadSettlementTotals(List.of(order.getId())).getOrDefault(order.getId(), ReceiptSettlement.Totals.ZERO);
            ReceiptSettlement.Split s = ReceiptSettlement.split(order, orderTotals);
            BigDecimal[] outs = order.getId() == null
                ? new BigDecimal[] { s.senderOut(), s.deliveryOut() }
                : outstanding(order, orderTotals, loadWaived(List.of(order.getId())).get(order.getId()));
            String portion = line.portion() == null || line.portion().isBlank() ? null : line.portion().trim().toUpperCase();
            if (portion != null && !ReceiptSettlement.SENDER.equals(portion) && !ReceiptSettlement.DELIVERY.equals(portion)) {
                throw new BadRequestAlertException("Invalid receipt portion: " + line.portion(), ENTITY, "portionInvalid");
            }
            BigDecimal collectable = ReceiptSettlement.SENDER.equals(portion)
                ? outs[0]
                : ReceiptSettlement.DELIVERY.equals(portion) ? outs[1] : outs[0].add(outs[1]);
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
                : ReceiptSettlement.DELIVERY.equals(portion) ? amount : amount.min(outs[1]);
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
        auditRecorder.record(
            "RECEIPT_CREATE",
            AUDIT_RECEIPT,
            receipt.getReceiptCode(),
            "Người nộp: " + receipt.getPayerName() + " · " + lines.size() + " đơn · " + money(total) + " · " + orderCodes(lines)
        );
        return toReceiptDto(receipt, lines);
    }

    private static String money(BigDecimal v) {
        return String.format("%,d đ", OrderMoney.nz(v).longValue()).replace(',', '.');
    }

    private static String orderCodes(List<ReceiptOrderLine> lines) {
        return String.join(
            ", ",
            lines.stream().map(l -> l.getOrder() != null ? l.getOrder().getOrderCode() : null).filter(Objects::nonNull).toList()
        );
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

    /**
     * Không trả ảnh chứng từ (xem {@link #receiptProofImage}) và nạp dòng/đơn/thời điểm thu theo lô — tránh vài nghìn query
     * + vài chục MB mỗi lần mở danh sách khi phiếu thu nhiều lên.
     */
    @Transactional(readOnly = true)
    public Page<ReceiptDTO> listReceipts(String officeCode, String createdBy, Pageable pageable) {
        return listReceipts(officeCode, createdBy, ReceiptListFilter.NONE, pageable);
    }

    /** Lọc danh sách phiếu thu phía server; {@code day} = ngày lập phiếu (yyyy-MM-dd, giờ VN). */
    public record ReceiptListFilter(String code, String payer, String creator, String day) {
        public static final ReceiptListFilter NONE = new ReceiptListFilter(null, null, null, null);
    }

    private static String likeParam(String v) {
        return v == null || v.isBlank() ? null : "%" + v.trim().toLowerCase() + "%";
    }

    private static Instant[] receiptDayRange(ReceiptListFilter f) {
        if (f.day() == null || f.day().isBlank()) {
            return new Instant[] { null, null };
        }
        java.time.LocalDate d = java.time.LocalDate.parse(f.day().trim());
        java.time.ZoneId vn = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        return new Instant[] { d.atStartOfDay(vn).toInstant(), d.plusDays(1).atStartOfDay(vn).toInstant() };
    }

    private static String officeParam(String officeCode) {
        return officeCode == null || officeCode.isBlank() ? null : officeCode.trim().toUpperCase();
    }

    private static String createdByParam(String createdBy) {
        return createdBy == null || createdBy.isBlank() ? null : createdBy.trim();
    }

    /** Tổng tiền mọi phiếu khớp bộ lọc (không chỉ trang hiện tại). */
    @Transactional(readOnly = true)
    public BigDecimal sumReceipts(String officeCode, String createdBy, ReceiptListFilter filter) {
        ReceiptListFilter f = filter == null ? ReceiptListFilter.NONE : filter;
        Instant[] range = receiptDayRange(f);
        return receiptRepository.sumListTotal(
            officeParam(officeCode),
            createdByParam(createdBy),
            likeParam(f.code()),
            likeParam(f.payer()),
            likeParam(f.creator()),
            range[0],
            range[1]
        );
    }

    @Transactional(readOnly = true)
    public Page<ReceiptDTO> listReceipts(String officeCode, String createdBy, ReceiptListFilter filter, Pageable pageable) {
        Pageable paging = pageable.getSort().isSorted()
            ? pageable
            : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        ReceiptListFilter f = filter == null ? ReceiptListFilter.NONE : filter;
        Instant[] range = receiptDayRange(f);
        Page<ReceiptListRow> rows = receiptRepository.findListRows(
            officeParam(officeCode),
            createdByParam(createdBy),
            likeParam(f.code()),
            likeParam(f.payer()),
            likeParam(f.creator()),
            range[0],
            range[1],
            paging
        );
        List<Long> receiptIds = rows.getContent().stream().map(ReceiptListRow::id).toList();
        Map<Long, List<ReceiptOrderLine>> linesByReceipt = new HashMap<>();
        Set<Long> orderIds = new HashSet<>();
        if (!receiptIds.isEmpty()) {
            for (ReceiptOrderLine l : receiptOrderLineRepository.findByReceiptIdsWithOrder(receiptIds)) {
                linesByReceipt.computeIfAbsent(l.getReceipt().getId(), k -> new ArrayList<>()).add(l);
                if (l.getOrder() != null && l.getOrder().getId() != null) {
                    orderIds.add(l.getOrder().getId());
                }
            }
        }
        Map<Long, Instant> paidAt = customerPaidAtByOrderIds(orderIds);
        Map<String, Optional<String>> names = new HashMap<>();
        return rows.map(row -> {
            List<ReceiptOrderLine> lines = linesByReceipt.getOrDefault(row.id(), List.of());
            List<Map<String, Object>> lineViews = new ArrayList<>();
            Instant customerPaidAt = null;
            for (ReceiptOrderLine l : lines) {
                Map<String, Object> m = new HashMap<>();
                m.put("orderCode", l.getOrder() != null ? l.getOrder().getOrderCode() : null);
                m.put("amountCollected", l.getAmountCollected());
                lineViews.add(m);
                Instant paid = l.getOrder() != null ? paidAt.get(l.getOrder().getId()) : null;
                if (paid != null && (customerPaidAt == null || paid.isAfter(customerPaidAt))) {
                    customerPaidAt = paid;
                }
            }
            return new ReceiptDTO(
                row.id(),
                row.receiptCode(),
                row.payerName(),
                row.payerCode(),
                row.totalAmount(),
                row.createdAt(),
                row.createdByUsername(),
                row.officeCode(),
                lineViews,
                row.confirmedAt(),
                row.confirmedByUsername(),
                customerPaidAt != null ? customerPaidAt : row.createdAt(),
                null,
                Boolean.TRUE.equals(row.hasConfirmProof()),
                names.computeIfAbsent("P:" + row.payerCode(), k -> Optional.ofNullable(staffNameOf(row.payerCode(), true))).orElse(null),
                names
                    .computeIfAbsent("U:" + row.createdByUsername(), k -> Optional.ofNullable(staffNameOf(row.createdByUsername(), false)))
                    .orElse(null),
                Boolean.TRUE.equals(row.hasTransferProof())
            );
        });
    }

    @Transactional(readOnly = true)
    public String receiptProofImage(String receiptCode) {
        if (receiptCode == null || receiptCode.isBlank()) {
            throw new BadRequestAlertException("receiptCode is required", ENTITY, "receiptCodeRequired");
        }
        return receiptRepository.findProofImageByCode(receiptCode.trim()).orElse(null);
    }

    /** Cùng quy tắc {@link #resolveCustomerPaidAtForOrder} nhưng cho cả lô đơn (2 query). */
    Map<Long, Instant> customerPaidAtByOrderIds(Set<Long> orderIds) {
        Map<Long, Instant> out = new HashMap<>();
        if (orderIds.isEmpty()) {
            return out;
        }
        for (Object[] row : orderPaymentRepository.latestCustomerPaymentAtByOrderIds(orderIds)) {
            if (row[0] != null && row[1] != null) {
                out.put((Long) row[0], (Instant) row[1]);
            }
        }
        Set<Long> missing = new HashSet<>(orderIds);
        missing.removeAll(out.keySet());
        if (!missing.isEmpty()) {
            for (Object[] row : orderEventRepository.latestEventAtByOrderIds(missing, CUSTOMER_PAID_EVENT_ACTIONS)) {
                if (row[0] != null && row[1] != null) {
                    out.put((Long) row[0], (Instant) row[1]);
                }
            }
        }
        return out;
    }

    public ReceiptDTO confirmReceipt(String receiptCode, ConfirmReceiptRequest body) {
        if (receiptCode == null || receiptCode.isBlank()) {
            throw new BadRequestAlertException("receiptCode is required", ENTITY, "receiptCodeRequired");
        }
        String proof = body != null ? body.proofImage() : null;
        if (proof != null && proof.length() > 2_500_000) {
            throw new BadRequestAlertException("Proof image too large", ENTITY, "receiptProofTooLarge");
        }
        Receipt receipt = receiptRepository
            .findOneByReceiptCode(receiptCode.trim())
            .orElseThrow(() -> new BadRequestAlertException("Receipt not found", ENTITY, "receiptNotFound"));
        if (receipt.getConfirmedAt() != null) {
            throw new BadRequestAlertException("Receipt already confirmed", ENTITY, "receiptAlreadyConfirmed");
        }
        if (proof == null || proof.isBlank()) {
            proof = receipt.getTransferProofImage();
        }
        if (proof == null || proof.isBlank()) {
            throw new BadRequestAlertException("Transaction proof image is required", ENTITY, "receiptProofRequired");
        }
        receipt.setConfirmedAt(Instant.now());
        receipt.setConfirmedByUsername(actor());
        receipt.setConfirmProofImage(proof.trim());
        receipt = receiptRepository.save(receipt);
        auditRecorder.record(
            "RECEIPT_CONFIRM",
            AUDIT_RECEIPT,
            receipt.getReceiptCode(),
            "Người nộp: " + receipt.getPayerName() + " · " + money(receipt.getTotalAmount())
        );
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
        auditRecorder.record(
            "RECEIPT_UNCONFIRM",
            AUDIT_RECEIPT,
            receipt.getReceiptCode(),
            "Người nộp: " + receipt.getPayerName() + " · " + money(receipt.getTotalAmount())
        );
        return toReceiptDto(receipt, receiptOrderLineRepository.findByReceipt_Id(receipt.getId()));
    }

    /**
     * Admin hủy phiếu thu (bắt buộc lý do): gỡ các khoản RECEIPT* phiếu đã ghi vào đơn (cùng thời điểm + người tạo phiếu),
     * trừ lại paidAmount phần cước, xoá dòng + phiếu. Đơn quay lại "Đơn cần nộp".
     */
    public void cancelReceipt(String receiptCode, String rawReason) {
        requireAdmin();
        if (receiptCode == null || receiptCode.isBlank()) {
            throw new BadRequestAlertException("receiptCode is required", ENTITY, "receiptCodeRequired");
        }
        String reason = rawReason == null ? "" : rawReason.trim();
        if (reason.isEmpty()) {
            throw new BadRequestAlertException("Reason is required", ENTITY, "receiptCancelReasonRequired");
        }
        if (reason.length() > 255) {
            reason = reason.substring(0, 255);
        }
        Receipt receipt = receiptRepository
            .findOneByReceiptCode(receiptCode.trim())
            .orElseThrow(() -> new BadRequestAlertException("Receipt not found", ENTITY, "receiptNotFound"));
        List<ReceiptOrderLine> lines = receiptOrderLineRepository.findByReceipt_Id(receipt.getId());
        Instant createdAt = receipt.getCreatedAt();
        String creator = receipt.getCreatedByUsername();
        for (ReceiptOrderLine line : lines) {
            ShipmentOrder order = line.getOrder();
            if (order == null || order.getId() == null) {
                continue;
            }
            dayClosureGuard.assertCollectionMutable(order);
            BigDecimal fareRemoved = BigDecimal.ZERO;
            for (OrderPayment p : orderPaymentRepository.findByOrder_IdOrderByPaymentAtDesc(order.getId())) {
                if (!isPaymentOfReceipt(p, createdAt, creator)) {
                    continue;
                }
                if (p.getPaymentKind() == PaymentKind.SAU) {
                    fareRemoved = fareRemoved.add(OrderMoney.nz(p.getAmount()));
                }
                orderPaymentRepository.delete(p);
            }
            if (fareRemoved.signum() > 0) {
                order.setPaidAmount(OrderMoney.nz(order.getPaidAmount()).subtract(fareRemoved).max(BigDecimal.ZERO));
                shipmentOrderRepository.save(order);
            }
        }
        String detail =
            "Người nộp: " +
            receipt.getPayerName() +
            " · " +
            lines.size() +
            " đơn · " +
            money(receipt.getTotalAmount()) +
            (receipt.getConfirmedAt() != null ? " · đã xác nhận bởi " + receipt.getConfirmedByUsername() : "") +
            " · " +
            orderCodes(lines) +
            " · lý do: " +
            reason;
        receiptOrderLineRepository.deleteAll(lines);
        receiptRepository.delete(receipt);
        auditRecorder.record("RECEIPT_CANCEL", AUDIT_RECEIPT, receiptCode.trim(), detail);
    }

    private static boolean isPaymentOfReceipt(OrderPayment p, Instant receiptCreatedAt, String creator) {
        String note = p.getNote() == null ? "" : p.getNote().trim().toUpperCase();
        if (!note.startsWith("RECEIPT") || p.getPaymentAt() == null || receiptCreatedAt == null) {
            return false;
        }
        long diffMs = Math.abs(p.getPaymentAt().toEpochMilli() - receiptCreatedAt.toEpochMilli());
        return diffMs < 1000 && (creator == null || creator.equalsIgnoreCase(p.getCollectorUsername()));
    }

    /** Admin hủy nộp: bỏ khoản còn phải nộp của đơn khỏi danh sách "Đơn cần nộp" (bắt buộc lý do). */
    public WaiveResult waiveDues(WaiveRequest req) {
        requireAdmin();
        String reason = req == null || req.reason() == null ? "" : req.reason().trim();
        if (reason.isEmpty()) {
            throw new BadRequestAlertException("Reason is required", ENTITY, "waiveReasonRequired");
        }
        if (reason.length() > 255) {
            reason = reason.substring(0, 255);
        }
        if (req.items() == null || req.items().isEmpty()) {
            throw new BadRequestAlertException("Items required", ENTITY, "waiveItemsRequired");
        }
        Instant now = Instant.now();
        String actor = actor();
        int count = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (WaiveItem item : req.items()) {
            if (item == null || item.orderCode() == null || item.orderCode().isBlank()) {
                continue;
            }
            ShipmentOrder order = shipmentOrderRepository
                .findOneByOrderCodeOrDraftCode(item.orderCode().trim())
                .orElseThrow(() -> new BadRequestAlertException("Order not found: " + item.orderCode(), ENTITY, "orderNotFound"));
            dayClosureGuard.assertCollectionMutable(order);
            String portion = item.portion() == null || item.portion().isBlank() ? null : item.portion().trim().toUpperCase();
            if (portion != null && !ReceiptSettlement.SENDER.equals(portion) && !ReceiptSettlement.DELIVERY.equals(portion)) {
                throw new BadRequestAlertException("Invalid portion: " + item.portion(), ENTITY, "portionInvalid");
            }
            BigDecimal[] outs = outstanding(
                order,
                loadSettlementTotals(List.of(order.getId())).getOrDefault(order.getId(), ReceiptSettlement.Totals.ZERO),
                loadWaived(List.of(order.getId())).get(order.getId())
            );
            for (String p : List.of(ReceiptSettlement.SENDER, ReceiptSettlement.DELIVERY)) {
                if (portion != null && !portion.equals(p)) {
                    continue;
                }
                BigDecimal amount = ReceiptSettlement.SENDER.equals(p) ? outs[0] : outs[1];
                if (amount.signum() <= 0) {
                    continue;
                }
                String owner = ReceiptSettlement.SENDER.equals(p) ? resolveSenderOwner(order) : resolveDeliveryActor(order);
                ReceiptWaiver w = new ReceiptWaiver();
                w.setOrder(order);
                w.setPortion(p);
                w.setAmount(amount);
                w.setOwnerUsername(owner);
                w.setReason(reason);
                w.setWaivedAt(now);
                w.setWaivedByUsername(actor);
                receiptWaiverRepository.save(w);
                auditRecorder.record(
                    "RECEIPT_DUE_WAIVE",
                    AUDIT_DUE,
                    order.getOrderCode(),
                    (ReceiptSettlement.SENDER.equals(p) ? "Phần VP gửi" : "Phần giao") +
                    " · " +
                    money(amount) +
                    " · người nộp: " +
                    (owner == null ? "—" : owner) +
                    " · lý do: " +
                    reason
                );
                count++;
                total = total.add(amount);
            }
        }
        if (count == 0) {
            throw new BadRequestAlertException("Nothing to waive", ENTITY, "waiveNothing");
        }
        return new WaiveResult(count, total);
    }

    /** Lịch sử thao tác màn phiếu thu (tạo / xác nhận / hoàn tác / hủy nộp) — chỉ admin. */
    @Transactional(readOnly = true)
    public List<HistoryDTO> history(LocalDate from, LocalDate to) {
        requireAdmin();
        LocalDate end = to == null ? LocalDate.now(VN) : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isAfter(end)) {
            throw new BadRequestAlertException("from must be <= to", ENTITY, "rangeInvalid");
        }
        if (start.plusDays(92).isBefore(end)) {
            throw new BadRequestAlertException("Range too large (max 92 days)", ENTITY, "rangeTooLarge");
        }
        Map<String, String> names = new HashMap<>();
        List<HistoryDTO> out = new ArrayList<>();
        for (AuditLog a : auditLogRepository.findByEntityTypeInAndActedAtGreaterThanEqualAndActedAtLessThanOrderByActedAtDesc(
            List.of(AUDIT_RECEIPT, AUDIT_DUE),
            start.atStartOfDay(VN).toInstant(),
            end.plusDays(1).atStartOfDay(VN).toInstant()
        )) {
            String by = a.getActedByUsername();
            String name = by == null ? null : names.computeIfAbsent(by, this::displayNameOf);
            out.add(new HistoryDTO(a.getId(), a.getActedAt(), by, name, a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetail()));
        }
        return out;
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
        // Phiếu trong ngày có thể đã bị xoá → count+1 có thể trùng mã còn tồn tại.
        long seq = receiptRepository.countByReceiptCodeStartingWith(prefix) + 1;
        String code = prefix + "-" + String.format("%03d", seq);
        while (receiptRepository.existsByReceiptCode(code)) {
            code = prefix + "-" + String.format("%03d", ++seq);
        }
        return code;
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
            r.getConfirmProofImage(),
            notBlank(r.getConfirmProofImage()) || notBlank(r.getTransferProofImage()),
            staffNameOf(r.getPayerCode(), true),
            staffNameOf(r.getCreatedByUsername(), false),
            notBlank(r.getTransferProofImage())
        );
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String staffNameOf(String key, boolean orStaffCode) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String k = key.trim();
        Optional<StaffProfile> profile = staffProfileRepository.findOneByUserLoginIgnoreCase(k);
        if (profile.isEmpty() && orStaffCode) {
            profile = staffProfileRepository.findOneByStaffCodeIgnoreCase(k);
        }
        return profile.map(StaffProfile::getDisplayName).filter(n -> !n.isBlank()).orElse(null);
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
                    p.getAmount() != null &&
                    p.getAmount().signum() > 0 &&
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

    /** Người nhập kho gửi (WAREHOUSE_RECEIVE); bỏ qua nhập kho → người quét lên xe đầu tiên; rồi tạo đơn / lấy hàng. */
    private String resolveSenderWhActor(ShipmentOrder order) {
        if (order.getId() != null) {
            List<OrderEvent> events = orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId());
            for (int i = events.size() - 1; i >= 0; i--) {
                OrderEvent event = events.get(i);
                String action = event.getAction() == null ? "" : event.getAction().trim().toUpperCase();
                if ("WAREHOUSE_RECEIVE".equals(action) || "WH_IN".equals(action) || "CONFIRM".equals(action)) {
                    String actor = staffActor(event);
                    if (actor != null) {
                        return actor;
                    }
                }
            }
            for (OrderEvent event : events) {
                if ("SCAN_OUT".equalsIgnoreCase(event.getAction() == null ? "" : event.getAction().trim())) {
                    String actor = staffActor(event);
                    if (actor != null) {
                        return actor;
                    }
                }
            }
        }
        return resolveDebtOwner(order);
    }

    /** Actor là nhân viên (bỏ khách tự tạo đơn / hệ thống). */
    private static String staffActor(OrderEvent event) {
        String actor = event.getActorUsername();
        if (actor == null || actor.isBlank()) {
            return null;
        }
        String a = actor.trim();
        return "customer".equalsIgnoreCase(a) || "system".equalsIgnoreCase(a) || "anonymousUser".equalsIgnoreCase(a) ? null : a;
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
                if ("CREATE".equalsIgnoreCase(event.getAction()) || "CREATED".equalsIgnoreCase(event.getAction())) {
                    return staffActor(event);
                }
            }
        }
        return null;
    }

    private static String actor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }

    private void requireAdmin() {
        if (!staffAccessService.isSystemAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }

    private String displayNameOf(String login) {
        return staffProfileRepository.findOneByUserLoginIgnoreCase(login).map(StaffProfile::getDisplayName).orElse(null);
    }

    public record WaiveItem(String orderCode, /** SENDER | DELIVERY | null = cả hai */String portion) {}

    public record WaiveRequest(String reason, List<WaiveItem> items) {}

    public record WaiveResult(int count, BigDecimal totalAmount) {}

    public record HistoryDTO(
        Long id,
        Instant at,
        String username,
        String displayName,
        String action,
        String entityType,
        String entityId,
        String detail
    ) {}

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
        /** Tên hiển thị của người chịu nợ (hồ sơ nhân viên), null nếu chưa có. */
        String debtOwnerName,
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
        /** Ảnh chứng từ khi xác nhận thu (data-URL). Danh sách để null — lấy qua GET /api/receipts/{code}/proof-image. */
        String confirmProofImage,
        boolean hasConfirmProof,
        /** Họ tên hồ sơ nhân viên theo payerCode; null nếu người nộp không có hồ sơ. */
        String payerDisplayName,
        String createdByDisplayName,
        /** NV đã gửi ảnh chuyển khoản từ app — KT xác nhận có thể dùng luôn ảnh này. */
        boolean hasTransferProof
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
