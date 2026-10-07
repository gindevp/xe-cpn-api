package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.PartnerFeeExpense;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.DeliveryAttemptResult;
import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.PartnerFeeExpenseRepository;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.autocall.AutoCallService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.FailDeliveryRequest;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.order.PodRequest;
import com.mycompany.myapp.service.order.DeliveryFacadeService;
import com.mycompany.myapp.service.order.OrderFacadeService;
import com.mycompany.myapp.service.order.OrderMoney;
import com.mycompany.myapp.service.order.PartnerAdvance;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Giao tận nơi qua Ahamove (không COD hàng). Đơn còn nợ cước (người gửi hay người nhận trả) → tài xế ứng cước
 * ({@code partnerCodAmount}) cho người bàn giao rồi thu lại người nhận; người bàn giao nhận nợ nộp về công ty. Phí Ahamove = chi phí đối tác ({@code partnerFeeAmount}),
 * không đụng cước khách.
 */
@Service
public class AhamoveDispatchService {

    private static final Logger LOG = LoggerFactory.getLogger(AhamoveDispatchService.class);
    private static final String ENTITY = "ahamove";
    public static final String PARTNER_CODE = "AHAMOVE";
    private static final java.util.Set<String> PARTNER_DONE = java.util.Set.of("CANCELLED", "COMPLETED", "FAILED");
    /** Tài xế đã lấy hàng — phí Ahamove phát sinh (huỷ trước lúc này thì không mất phí). */
    static final java.util.Set<String> PARTNER_PICKED_UP = java.util.Set.of("IN PROCESS", "IN_PROCESS", "COMPLETED", "FAILED");

    /** Đơn Ahamove còn chạy bên đối tác (chưa huỷ / giao xong / thất bại) — không được chuyển giao thất bại thủ công. */
    public static boolean partnerActive(ShipmentOrder order) {
        if (order == null || !PARTNER_CODE.equals(order.getPartnerCode()) || !notBlank(order.getPartnerOrderId())) {
            return false;
        }
        String st = order.getPartnerStatus();
        return notBlank(st) && !PARTNER_DONE.contains(st.trim().toUpperCase());
    }

    public static void assertNoActivePartner(ShipmentOrder order) {
        if (partnerActive(order)) {
            throw new BadRequestAlertException(
                "Đơn đang giao Ahamove (" + order.getPartnerOrderId() + ") — bấm Hủy Ahamove trước khi báo giao không thành công",
                ENTITY,
                "ahamoveActive"
            );
        }
    }

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final OrderDeliveryAttemptRepository deliveryAttemptRepository;
    private final AhamoveOrderClient ahamoveOrderClient;
    private final DeliveryFacadeService deliveryFacadeService;
    private final OrderFacadeService orderFacadeService;
    private final DayClosureGuard dayClosureGuard;
    private final OrderPaymentRepository orderPaymentRepository;
    private final ReceiptOrderLineRepository receiptOrderLineRepository;
    private final PartnerFeeExpenseRepository partnerFeeExpenseRepository;
    private final TransactionTemplate tx;

    public AhamoveDispatchService(
        ShipmentOrderRepository shipmentOrderRepository,
        IntegrationConfigRepository integrationConfigRepository,
        OrderDeliveryAttemptRepository deliveryAttemptRepository,
        AhamoveOrderClient ahamoveOrderClient,
        DeliveryFacadeService deliveryFacadeService,
        OrderFacadeService orderFacadeService,
        DayClosureGuard dayClosureGuard,
        OrderPaymentRepository orderPaymentRepository,
        ReceiptOrderLineRepository receiptOrderLineRepository,
        PartnerFeeExpenseRepository partnerFeeExpenseRepository,
        PlatformTransactionManager transactionManager
    ) {
        this.partnerFeeExpenseRepository = partnerFeeExpenseRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.receiptOrderLineRepository = receiptOrderLineRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.ahamoveOrderClient = ahamoveOrderClient;
        this.deliveryFacadeService = deliveryFacadeService;
        this.orderFacadeService = orderFacadeService;
        this.dayClosureGuard = dayClosureGuard;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public static class DispatchRequest {

        private Double lat;
        private Double lng;
        private String address;
        private String remarks;

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }

    /** Gọi Ahamove giao đơn: AT_DEST / FAILED_DELIVERY → OUT_FOR_DELIVERY. */
    public OrderDetailDTO dispatch(String orderCode, DispatchRequest request) {
        final DispatchRequest req = request != null ? request : new DispatchRequest();
        if ((req.getLat() == null) != (req.getLng() == null)) {
            throw new BadRequestAlertException("Toạ độ giao thiếu lat hoặc lng", ENTITY, "ahamovePinInvalid");
        }
        ShipmentOrder order = requireOrder(orderCode);
        Dispatchable ok = assertDispatchable(order);
        Office pickupOffice = ok.office();
        BigDecimal advance = ok.advance();
        IntegrationConfig cfg = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        String pickupMobile = cfg != null ? AhamoveAuthClient.normalizeMobile(cfg.getAhamoveMobile()) : null;
        if (pickupMobile == null) {
            throw new BadRequestAlertException("Chưa cấu hình SĐT tài khoản Ahamove (Tích hợp)", ENTITY, "ahamoveMobileMissing");
        }
        String paymentMethod = cfg.getAhamovePaymentMethod() != null ? cfg.getAhamovePaymentMethod() : "BALANCE";
        String dropAddress = notBlank(req.getAddress()) ? req.getAddress().trim() : order.getDeliveryAddress();
        if (!notBlank(dropAddress)) {
            throw new BadRequestAlertException("Đơn chưa có địa chỉ giao", ENTITY, "ahamoveAddressMissing");
        }

        AhamoveOrderClient.Stop pickup = new AhamoveOrderClient.Stop(
            pickupOffice.getLatitude().doubleValue(),
            pickupOffice.getLongitude().doubleValue(),
            notBlank(pickupOffice.getAddress()) ? pickupOffice.getAddress() : pickupOffice.getName(),
            "CPN " + pickupOffice.getName(),
            pickupMobile,
            null,
            order.getOrderCode(),
            0L
        );
        AhamoveOrderClient.Stop drop = new AhamoveOrderClient.Stop(
            req.getLat(),
            req.getLng(),
            dropAddress,
            order.getReceiverName(),
            order.getReceiverPhone(),
            req.getRemarks(),
            order.getOrderCode(),
            advance.longValue()
        );
        AhamoveCargo cargo = AhamoveCargo.from(order);
        cargo.assertFitsBike();
        AhamoveOrderClient.CreatedOrder created = ahamoveOrderClient.createOrder(pickup, drop, paymentMethod, cargo);

        String actor = currentActor();
        try {
            tx.executeWithoutResult(status -> saveDispatched(order.getOrderCode(), created, req, advance, actor));
        } catch (RuntimeException e) {
            LOG.warn("Ahamove dispatch: lưu DB lỗi sau khi tạo đơn {} — hủy đơn Ahamove: {}", created.orderId(), e.getMessage());
            try {
                ahamoveOrderClient.cancelOrder(created.orderId(), "CPN lỗi hệ thống — hủy tự động");
            } catch (RuntimeException ce) {
                LOG.error("Ahamove dispatch: hủy đơn {} thất bại — cần hủy tay trên Ahamove", created.orderId(), ce);
            }
            throw e;
        }
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    /** Hủy đơn Ahamove khi tài xế chưa lấy hàng: OUT_FOR_DELIVERY → FAILED_DELIVERY (không tính lần giao thất bại). */
    public OrderDetailDTO cancel(String orderCode, String reason) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        if (!PARTNER_CODE.equals(order.getPartnerCode()) || !notBlank(order.getPartnerOrderId())) {
            throw new BadRequestAlertException("Đơn không giao qua Ahamove", ENTITY, "ahamoveNotDispatched");
        }
        boolean lateCancel = order.getStatus() == OrderStatus.FAILED_DELIVERY && partnerActive(order);
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY && !lateCancel) {
            throw new BadRequestAlertException("Chỉ hủy khi đơn đang giao", ENTITY, "ahamoveCancelStatus");
        }
        String comment = notBlank(reason) ? reason.trim() : "CPN hủy giao";
        ahamoveOrderClient.cancelOrder(order.getPartnerOrderId(), comment);
        String actor = currentActor();
        tx.executeWithoutResult(status -> markCancelled(order.getOrderCode(), comment, actor));
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    /**
     * Callback Ahamove (POST toàn bộ order JSON). Luôn lưu thông tin đối tác; chuyển trạng thái CPN khi:
     * giao xong có ảnh → POD tự động; giao thất bại → fail delivery; Ahamove hủy → FAILED_DELIVERY.
     *
     * @return mã đơn CPN, hoặc empty nếu không khớp đơn nào.
     */
    public Optional<String> applyWebhook(JsonNode body) {
        WebhookUpdate u = parseWebhook(body);
        if (u == null || u.orderId() == null) {
            return Optional.empty();
        }
        Optional<ShipmentOrder> found = shipmentOrderRepository.findFirstByPartnerOrderId(u.orderId());
        if (found.isEmpty()) {
            LOG.info("Ahamove webhook: không có đơn CPN cho {}", u.orderId());
            return Optional.empty();
        }
        String code = found.get().getOrderCode();
        tx.executeWithoutResult(status -> savePartnerInfo(code, u));

        ShipmentOrder order = requireOrder(code);
        savePickupPhotos(order, u);
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            if (order.getStatus() == OrderStatus.DELIVERED && !u.podUrls().isEmpty()) {
                try {
                    deliveryFacadeService.appendPodPhotos(order, u.podUrls(), "Giao");
                } catch (RuntimeException e) {
                    LOG.warn("Ahamove webhook {}: không lưu ảnh giao đơn {} — {}", u.orderId(), code, e.getMessage());
                }
            }
            return Optional.of(code);
        }
        try {
            if ("COMPLETED".equals(u.dropStatus())) {
                if (!u.podUrls().isEmpty()) {
                    PodRequest pod = new PodRequest();
                    pod.setChannel("HOME");
                    String name = notBlank(order.getReceiverName()) ? order.getReceiverName().trim() : "Người nhận";
                    pod.setActualRecipientName(name.length() > 100 ? name.substring(0, 100) : name);
                    pod.setPhotos(u.podUrls());
                    pod.setCaption("Giao");
                    pod.setCollectedAmount(BigDecimal.ZERO);
                    deliveryFacadeService.pod(code, pod);
                }
            } else if ("FAILED".equals(u.dropStatus())) {
                FailDeliveryRequest fail = new FailDeliveryRequest();
                String reason = "Ahamove: " + (notBlank(u.failReason()) ? u.failReason() : "giao thất bại");
                fail.setReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
                deliveryFacadeService.failDelivery(code, fail);
            } else if ("CANCELLED".equals(u.status())) {
                String reason = notBlank(u.failReason()) ? u.failReason() : "Ahamove hủy đơn";
                tx.executeWithoutResult(status -> markCancelled(code, reason, "ahamove"));
            }
        } catch (RuntimeException e) {
            LOG.warn("Ahamove webhook {}: không tự chuyển trạng thái đơn {} — {}", u.orderId(), code, e.getMessage());
        }
        return Optional.of(code);
    }

    record WebhookUpdate(
        String orderId,
        String status,
        String subStatus,
        String dropStatus,
        String driverName,
        String driverPhone,
        String sharedLink,
        BigDecimal totalPay,
        List<String> podUrls,
        String failReason,
        List<String> pickupPodUrls
    ) {}

    static WebhookUpdate parseWebhook(JsonNode body) {
        if (body == null || !body.isObject()) {
            return null;
        }
        String id = text(body, "_id");
        if (id == null) {
            id = text(body, "order_id");
        }
        id = stripStopSuffix(id);
        JsonNode drop = null;
        JsonNode path = body.get("path");
        List<String> pickupPods = new ArrayList<>();
        if (path != null && path.isArray() && path.size() > 1) {
            JsonNode pickup = path.get(0);
            if (pickup != null) {
                collectUrls(pickup.get("pod_info"), pickupPods);
            }
            drop = path.get(path.size() - 1);
        }
        List<String> pods = new ArrayList<>();
        if (drop != null) {
            collectUrls(drop.get("pod_info"), pods);
        }
        String fail = drop != null ? text(drop, "fail_comment") : null;
        if (fail == null) {
            fail = text(body, "cancel_comment");
        }
        if (fail == null) {
            fail = text(body, "comment");
        }
        Double pay = number(body, "total_pay");
        return new WebhookUpdate(
            id,
            upper(text(body, "status")),
            text(body, "sub_status"),
            drop != null ? upper(text(drop, "status")) : null,
            text(body, "supplier_name"),
            text(body, "supplier_id"),
            text(body, "shared_link"),
            pay == null ? null : BigDecimal.valueOf(pay).setScale(0, java.math.RoundingMode.HALF_UP),
            pods,
            fail,
            pickupPods
        );
    }

    /** Ảnh tài xế đến lấy hàng — lưu vào POD, không đổi trạng thái đơn. */
    private void savePickupPhotos(ShipmentOrder order, WebhookUpdate u) {
        if (u.pickupPodUrls().isEmpty()) {
            return;
        }
        try {
            int added = deliveryFacadeService.appendPodPhotos(order, u.pickupPodUrls(), "Nhận");
            if (added > 0) {
                orderFacadeService.recordEvent(order, "AHAMOVE_PICKUP_POD", "Ảnh tài xế nhận hàng (" + added + ")", "ahamove");
            }
        } catch (RuntimeException e) {
            LOG.warn("Ahamove webhook {}: không lưu ảnh nhận hàng đơn {} — {}", u.orderId(), order.getOrderCode(), e.getMessage());
        }
    }

    /** Ahamove gửi sự kiện theo điểm dừng dạng {@code ORDERID-1}; mã đơn gốc không có hậu tố. */
    static String stripStopSuffix(String id) {
        if (id == null) {
            return null;
        }
        String s = id.trim();
        int dash = s.lastIndexOf('-');
        if (dash > 0 && dash < s.length() - 1 && s.substring(dash + 1).matches("\\d{1,2}")) {
            return s.substring(0, dash);
        }
        return s.isEmpty() ? null : s;
    }

    private static void collectUrls(JsonNode node, List<String> out) {
        if (node == null || node.isNull() || out.size() >= 3) {
            return;
        }
        if (node.isTextual()) {
            String s = node.asText().trim();
            if (s.startsWith("http://") || s.startsWith("https://")) {
                if (!out.contains(s)) {
                    out.add(s.length() > 1000 ? s.substring(0, 1000) : s);
                }
            }
            return;
        }
        if (node.isArray() || node.isObject()) {
            for (JsonNode child : node) {
                collectUrls(child, out);
            }
        }
    }

    record Dispatchable(Office office, BigDecimal advance) {}

    private Dispatchable assertDispatchable(ShipmentOrder order) {
        dayClosureGuard.assertOrderMutable(order);
        if (order.getStatus() != OrderStatus.AT_DEST && order.getStatus() != OrderStatus.FAILED_DELIVERY) {
            throw new BadRequestAlertException("Chỉ gọi Ahamove khi hàng ở kho VP nhận / giao thất bại", ENTITY, "ahamoveStatus");
        }
        if (!Boolean.TRUE.equals(order.getHomeDelivery())) {
            throw new BadRequestAlertException("Đơn không phải giao tận nơi", ENTITY, "ahamoveNotHomeDelivery");
        }
        if (OrderMoney.nz(order.getCodAmount()).signum() > 0) {
            throw new BadRequestAlertException("Đơn có COD — chưa hỗ trợ giao COD qua Ahamove", ENTITY, "ahamoveCod");
        }
        PartnerAdvance.assertNoRefundDue(order, ENTITY);
        BigDecimal advance = advanceFor(order);
        if (advance.signum() > 0) {
            dayClosureGuard.assertCollectionMutable(order);
        }
        Office office = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
        if (office == null || office.getLatitude() == null || office.getLongitude() == null) {
            throw new BadRequestAlertException("VP nhận chưa có toạ độ GPS (Danh mục VP)", ENTITY, "ahamoveOfficeGps");
        }
        return new Dispatchable(office, advance);
    }

    /**
     * Số tài xế ứng = toàn bộ cước còn nợ, cả đơn người gửi trả (vd. người nhận xin giao tận nơi sau khi hàng tới).
     * Đơn ghi nợ cước thì chặn.
     */
    static BigDecimal advanceFor(ShipmentOrder order) {
        BigDecimal due = OrderMoney.due(order);
        if (due.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (Boolean.TRUE.equals(order.getOnCredit())) {
            throw new BadRequestAlertException("Đơn ghi nợ cước — không gọi Ahamove ứng cước", ENTITY, "ahamoveOnCredit");
        }
        return due.setScale(0, RoundingMode.HALF_UP);
    }

    /** Người bấm bàn giao Ahamove gần nhất — nhận nợ tiền tài xế ứng để nộp về công ty. */
    String dispatcherOf(ShipmentOrder order) {
        String who = null;
        for (OrderDeliveryAttempt a : deliveryAttemptRepository.findByOrder_IdOrderByAttemptAtAsc(order.getId())) {
            if (
                a.getDeliveryPartner() == DeliveryPartner.AHAMOVE &&
                "ASSIGN_PARTNER".equals(a.getReason()) &&
                notBlank(a.getHandledByUsername())
            ) {
                who = a.getHandledByUsername();
            }
        }
        return who != null ? who : currentActor();
    }

    /** Đơn gọi Ahamove từ trước khi ghi nhận tự động, chưa bấm nhận tiền ứng. */
    public OrderDetailDTO confirmAdvance(String orderCode) {
        tx.executeWithoutResult(status -> {
            ShipmentOrder order = requireOrder(orderCode);
            BigDecimal amount = PartnerAdvance.amount(order);
            if (!PARTNER_CODE.equals(order.getPartnerCode()) || amount.signum() <= 0) {
                throw new BadRequestAlertException("Đơn không có tiền tài xế Ahamove ứng", ENTITY, "ahamoveNoAdvance");
            }
            if (order.getPartnerCodCollectedAt() != null) {
                throw new BadRequestAlertException("Đã xác nhận nhận tiền ứng trước đó", ENTITY, "ahamoveAdvanceDone");
            }
            OrderStatus st = order.getStatus();
            if (st != OrderStatus.OUT_FOR_DELIVERY && st != OrderStatus.DELIVERED && st != OrderStatus.FAILED_DELIVERY) {
                throw new BadRequestAlertException("Trạng thái đơn không nhận tiền ứng được", ENTITY, "ahamoveAdvanceStatus");
            }
            String driver = notBlank(order.getPartnerDriverName()) ? " · tài xế " + order.getPartnerDriverName().trim() : "";
            String note = truncate(PartnerAdvance.PAYMENT_NOTE_PREFIX + driver, 255);
            String debtor = dispatcherOf(order);
            deliveryFacadeService.recordPartnerAdvance(order, amount, note, debtor);
            orderFacadeService.recordEvent(
                order,
                "AHAMOVE_ADVANCE_IN",
                "Nhận " + PartnerAdvance.money(amount) + "đ tài xế Ahamove ứng" + driver + " · người nhận nợ " + debtor,
                currentActor()
            );
        });
        return orderFacadeService.getByCode(orderCode);
    }

    /**
     * Trả lại tiền ứng cho tài xế khi giao không được (hàng đã về VP). Xoá khoản thu ứng → đơn quay lại còn nợ.
     * Không cho nếu tiền đã vào phiếu thu nộp quỹ hoặc ngày ghi thu đã chốt.
     */
    public OrderDetailDTO refundAdvance(String orderCode) {
        tx.executeWithoutResult(status -> {
            ShipmentOrder order = requireOrder(orderCode);
            if (!PartnerAdvance.needsRefund(order)) {
                throw new BadRequestAlertException("Đơn không có tiền ứng cần hoàn", ENTITY, "ahamoveNoRefund");
            }
            dayClosureGuard.assertCollectionMutable(order);
            OrderPayment payment = orderPaymentRepository
                .findByOrder_IdOrderByPaymentAtDesc(order.getId())
                .stream()
                .filter(p -> p.getNote() != null && p.getNote().startsWith(PartnerAdvance.PAYMENT_NOTE_PREFIX))
                .findFirst()
                .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy khoản thu tiền ứng", ENTITY, "ahamoveAdvanceMissing"));
            Instant paidAt = payment.getPaymentAt() != null ? payment.getPaymentAt() : order.getPartnerCodCollectedAt();
            if (receiptOrderLineRepository.existsByOrder_IdAndReceipt_CreatedAtGreaterThanEqual(order.getId(), paidAt)) {
                throw new BadRequestAlertException(
                    "Tiền ứng đã nằm trong phiếu thu nộp quỹ — hủy phiếu thu trước khi hoàn ứng",
                    ENTITY,
                    "ahamoveAdvanceInReceipt"
                );
            }
            Office receiving = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
            dayClosureGuard.assertOfficeOpen(receiving, LocalDate.ofInstant(paidAt, DayClosureGuard.VN));

            BigDecimal amount = OrderMoney.nz(payment.getAmount());
            orderPaymentRepository.delete(payment);
            order.setPaidAmount(OrderMoney.nz(order.getPaidAmount()).subtract(amount).max(BigDecimal.ZERO));
            order.setPartnerCodAmount(null);
            order.setPartnerCodCollectedAt(null);
            order.setPartnerCodCollectedBy(null);
            shipmentOrderRepository.save(order);
            orderFacadeService.recordEvent(
                order,
                "AHAMOVE_ADVANCE_REFUND",
                "Hoàn " +
                PartnerAdvance.money(amount) +
                "đ tiền ứng cho tài xế Ahamove (thu ngày " +
                LocalDate.ofInstant(paidAt, DayClosureGuard.VN) +
                ")",
                currentActor()
            );
        });
        return orderFacadeService.getByCode(orderCode);
    }

    private void saveDispatched(
        String code,
        AhamoveOrderClient.CreatedOrder created,
        DispatchRequest req,
        BigDecimal advanceSent,
        String actor
    ) {
        ShipmentOrder order = requireOrder(code);
        BigDecimal advance = assertDispatchable(order).advance();
        if (advance.compareTo(advanceSent) != 0) {
            throw new BadRequestAlertException(
                "Cước còn nợ vừa thay đổi (" + PartnerAdvance.money(advance) + "đ) — gọi lại Ahamove",
                ENTITY,
                "ahamoveAdvanceChanged"
            );
        }
        Instant now = Instant.now();
        order.setPartnerCodAmount(advance.signum() > 0 ? advance : null);
        order.setPartnerCodCollectedAt(null);
        order.setPartnerCodCollectedBy(null);
        order.setPartnerCode(PARTNER_CODE);
        order.setPartnerOrderId(created.orderId());
        order.setPartnerStatus(created.status());
        order.setPartnerTrackingUrl(truncate(created.sharedLink(), 500));
        order.setPartnerFeeAmount(created.totalPay());
        order.setPartnerDriverName(null);
        order.setPartnerDriverPhone(null);
        order.setPartnerPodUrl(null);
        order.setPartnerFailReason(null);
        order.setPartnerUpdatedAt(now);
        order.setShipper(null);
        boolean pinned = req.getLat() != null && req.getLng() != null;
        order.setDeliveryLat(pinned ? BigDecimal.valueOf(req.getLat()).setScale(7, java.math.RoundingMode.HALF_UP) : null);
        order.setDeliveryLng(pinned ? BigDecimal.valueOf(req.getLng()).setScale(7, java.math.RoundingMode.HALF_UP) : null);
        shipmentOrderRepository.save(order);

        OrderDeliveryAttempt attempt = new OrderDeliveryAttempt();
        attempt.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
        attempt.setAttemptAt(now);
        attempt.setResult(DeliveryAttemptResult.SUCCESS);
        attempt.setHandledByUsername(actor);
        attempt.setDeliveryPartner(DeliveryPartner.AHAMOVE);
        attempt.setReason("ASSIGN_PARTNER");
        attempt.setOrder(order);
        deliveryAttemptRepository.save(attempt);

        // Người bàn giao nhận nợ ngay — không chờ bấm "Đã nhận tiền ứng".
        if (advance.signum() > 0) {
            String note = truncate(PartnerAdvance.PAYMENT_NOTE_PREFIX, 255);
            deliveryFacadeService.recordPartnerAdvance(order, advance, note, actor);
            orderFacadeService.recordEvent(
                order,
                "AHAMOVE_ADVANCE_IN",
                "Nhận " + PartnerAdvance.money(advance) + "đ tài xế Ahamove ứng · người nhận nợ " + actor,
                actor
            );
        }

        OrderTransitionRequest tr = new OrderTransitionRequest();
        tr.setToStatus(OrderStatus.OUT_FOR_DELIVERY);
        tr.setAction("PUSH_SHIP");
        tr.setDetail(
            "AHAMOVE · " +
            created.orderId() +
            (created.totalPay() != null ? " · phí " + created.totalPay() : "") +
            (advance.signum() > 0 ? " · tài xế ứng " + PartnerAdvance.money(advance) : "") +
            (pinned ? "" : " · không ghim GPS, Ahamove tự dò địa chỉ")
        );
        orderFacadeService.transition(code, tr);
    }

    private void markCancelled(String code, String reason, String actor) {
        ShipmentOrder order = requireOrder(code);
        order.setPartnerStatus("CANCELLED");
        order.setPartnerFailReason(truncate(reason, 500));
        order.setPartnerUpdatedAt(Instant.now());
        shipmentOrderRepository.save(order);
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            return;
        }
        OrderDeliveryAttempt attempt = new OrderDeliveryAttempt();
        attempt.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
        attempt.setAttemptAt(Instant.now());
        attempt.setResult(DeliveryAttemptResult.FAILED);
        attempt.setHandledByUsername(actor);
        attempt.setDeliveryPartner(DeliveryPartner.AHAMOVE);
        attempt.setReason(truncate("AHAMOVE_CANCEL: " + reason, 255));
        attempt.setOrder(order);
        deliveryAttemptRepository.save(attempt);

        OrderTransitionRequest tr = new OrderTransitionRequest();
        tr.setToStatus(OrderStatus.FAILED_DELIVERY);
        tr.setAction("AHAMOVE_CANCEL");
        tr.setDetail(truncate(reason, 255));
        orderFacadeService.transition(code, tr);
    }

    private void savePartnerInfo(String code, WebhookUpdate u) {
        ShipmentOrder order = requireOrder(code);
        String before = order.getPartnerStatus();
        String newStatus = u.dropStatus() != null && ("COMPLETED".equals(u.dropStatus()) || "FAILED".equals(u.dropStatus()))
            ? u.dropStatus()
            : u.status();
        if (newStatus != null) {
            order.setPartnerStatus(truncate(newStatus, 40));
        }
        if (notBlank(u.driverName())) {
            order.setPartnerDriverName(truncate(u.driverName(), 100));
        }
        if (notBlank(u.driverPhone())) {
            order.setPartnerDriverPhone(truncate(u.driverPhone(), 32));
        }
        if (notBlank(u.sharedLink())) {
            order.setPartnerTrackingUrl(truncate(u.sharedLink(), 500));
        }
        if (u.totalPay() != null && u.totalPay().signum() > 0) {
            order.setPartnerFeeAmount(u.totalPay());
        }
        if (!u.podUrls().isEmpty()) {
            order.setPartnerPodUrl(truncate(u.podUrls().get(0), 1000));
        }
        if (notBlank(u.failReason())) {
            order.setPartnerFailReason(truncate(u.failReason(), 500));
        }
        order.setPartnerUpdatedAt(Instant.now());
        shipmentOrderRepository.save(order);

        if (newStatus != null && !newStatus.equals(before)) {
            String detail =
                "Ahamove " +
                newStatus +
                (notBlank(u.driverName()) ? " · tài xế " + u.driverName() : "") +
                (notBlank(u.driverPhone()) ? " " + u.driverPhone() : "") +
                (notBlank(u.failReason()) ? " · " + u.failReason() : "");
            orderFacadeService.recordEvent(order, "AHAMOVE_STATUS", detail, "ahamove");
        }
        recordCashFeeIfPickedUp(order, newStatus);
    }

    /**
     * Thanh toán Ahamove tiền mặt: tài xế lấy hàng thì người bàn giao trả phí (báo giá lúc tạo đơn) — ghi 1 khoản/đơn Ahamove,
     * trừ vào phiếu thu tiếp theo của người đó.
     */
    void recordCashFeeIfPickedUp(ShipmentOrder order, String partnerStatus) {
        String partnerOrderId = order.getPartnerOrderId();
        BigDecimal fee = OrderMoney.nz(order.getPartnerFeeAmount());
        if (
            partnerStatus == null ||
            !PARTNER_PICKED_UP.contains(partnerStatus.trim().toUpperCase()) ||
            !PARTNER_CODE.equals(order.getPartnerCode()) ||
            !notBlank(partnerOrderId) ||
            fee.signum() <= 0
        ) {
            return;
        }
        IntegrationConfig cfg = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        if (cfg == null || !"CASH".equalsIgnoreCase(cfg.getAhamovePaymentMethod())) {
            return;
        }
        if (partnerFeeExpenseRepository.existsByPartnerOrderId(partnerOrderId)) {
            return;
        }
        String payer = dispatcherOf(order);
        PartnerFeeExpense e = new PartnerFeeExpense();
        e.setOrder(order);
        e.setPartnerCode(PARTNER_CODE);
        e.setPartnerOrderId(partnerOrderId);
        e.setAmount(fee);
        e.setPayerUsername(truncate(payer, 50));
        e.setIncurredAt(Instant.now());
        partnerFeeExpenseRepository.save(e);
        orderFacadeService.recordEvent(
            order,
            "AHAMOVE_FEE",
            "Phí Ahamove " + PartnerAdvance.money(fee) + "đ (" + partnerOrderId + ") · " + payer + " trả tài xế, trừ vào phiếu thu",
            "ahamove"
        );
    }

    /** Token webhook: query {@code ?token=} hoặc header {@code apikey}. */
    public boolean webhookTokenValid(String token, String apikeyHeader) {
        IntegrationConfig cfg = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        String expected = cfg != null ? cfg.getAhamoveWebhookToken() : null;
        if (!notBlank(expected)) {
            return false;
        }
        return AutoCallService.tokenMatches(expected, token) || AutoCallService.tokenMatches(expected, apikeyHeader);
    }

    private ShipmentOrder requireOrder(String code) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(code.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + code));
    }

    private static String currentActor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static String upper(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return s == null || s.isBlank() ? null : s;
    }

    private static Double number(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isNumber()) {
            return v.asDouble();
        }
        if (v.isTextual()) {
            try {
                return Double.parseDouble(v.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
