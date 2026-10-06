package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.OrderPodPhoto;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Shipper;
import com.mycompany.myapp.domain.enumeration.DeliveryAttemptResult;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.ShipperRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.AddPaymentRequest;
import com.mycompany.myapp.service.dto.order.AssignShipperRequest;
import com.mycompany.myapp.service.dto.order.FailDeliveryRequest;
import com.mycompany.myapp.service.dto.order.FailDeliveryResponse;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.order.PodRequest;
import com.mycompany.myapp.service.dto.order.PodResponse;
import com.mycompany.myapp.service.partner.AhamoveDispatchService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DeliveryFacadeService {

    private static final String ENTITY = "order";
    private static final int MAX_FAILS = 3;

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderPodPhotoRepository podPhotoRepository;
    private final OrderPaymentRepository paymentRepository;
    private final OrderDeliveryAttemptRepository deliveryAttemptRepository;
    private final OrderFacadeService orderFacadeService;
    private final DayClosureGuard dayClosureGuard;
    private final ShipperRepository shipperRepository;

    public DeliveryFacadeService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderPodPhotoRepository podPhotoRepository,
        OrderPaymentRepository paymentRepository,
        OrderDeliveryAttemptRepository deliveryAttemptRepository,
        OrderFacadeService orderFacadeService,
        DayClosureGuard dayClosureGuard,
        ShipperRepository shipperRepository
    ) {
        this.shipperRepository = shipperRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.podPhotoRepository = podPhotoRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.orderFacadeService = orderFacadeService;
        this.dayClosureGuard = dayClosureGuard;
    }

    public PodResponse pod(String orderCode, PodRequest req) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertCollectionMutable(order);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestAlertException("Already delivered (E-POD-057)", ENTITY, "alreadyDelivered");
        }
        assertPartnerPodAllowed(order, SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN));
        assertArrivedForPod(order);
        if (req.getPhotos() == null || req.getPhotos().isEmpty()) {
            throw new BadRequestAlertException("At least 1 POD photo required", ENTITY, "podPhotoRequired");
        }
        if (req.getPhotos().size() > 3) {
            throw new BadRequestAlertException("Max 3 POD photos", ENTITY, "podPhotoMax");
        }
        BigDecimal collected = req.getCollectedAmount() == null ? BigDecimal.ZERO : req.getCollectedAmount();
        assertReceiverCollectAllowed(order, collected);

        String channel = req.getChannel() == null ? "COUNTER" : req.getChannel().trim().toUpperCase();
        String actor = currentActor();
        Instant now = Instant.now();

        order.setReceiverActualName(req.getActualRecipientName().trim());
        if (req.getActualRecipientPhone() != null && !req.getActualRecipientPhone().isBlank()) {
            order.setReceiverActualPhone(req.getActualRecipientPhone().trim());
        }

        List<String> savedUrls = new ArrayList<>();
        int seq = (int) podPhotoRepository.countByOrder_Id(order.getId()) + 1;
        for (String photo : req.getPhotos()) {
            String url = truncateUrl(photo);
            OrderPodPhoto row = new OrderPodPhoto();
            row.setPhotoUrl(url);
            row.setCapturedAt(now);
            row.setCapturedByUsername(actor);
            row.setSequenceNo(Math.min(seq, 3));
            row.setOrder(order);
            podPhotoRepository.save(row);
            savedUrls.add(url);
            seq++;
        }

        if (collected.compareTo(BigDecimal.ZERO) > 0) {
            PaymentMethod method = req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethod.TM;
            addPaymentInternal(order, collected, method, PaymentKind.SAU, "POD " + channel, actor);
        }

        shipmentOrderRepository.save(order);

        String action = "HOME".equals(channel) ? "POD" : "POD_QUAY";
        ensureStatusThenDeliver(order, action, req.getActualRecipientName());

        order = requireOrder(order.getOrderCode());
        PodResponse res = new PodResponse();
        res.setOk(true);
        res.setOrderCode(order.getOrderCode());
        res.setStatus(order.getStatus());
        res.setPaidAmount(order.getPaidAmount());
        res.setFareAmount(order.getFareAmount());
        res.setDueAmount(OrderMoney.collectDue(order));
        res.setPhotoUrls(savedUrls);
        return res;
    }

    public FailDeliveryResponse failDelivery(String orderCode, FailDeliveryRequest req) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new BadRequestAlertException("Fail only from OUT_FOR_DELIVERY", ENTITY, "failInvalidStatus");
        }
        AhamoveDispatchService.assertNoActivePartner(order);

        int nextFail = (order.getFailCount() == null ? 0 : order.getFailCount()) + 1;
        order.setFailCount(nextFail);
        shipmentOrderRepository.save(order);

        OrderDeliveryAttempt attempt = new OrderDeliveryAttempt();
        attempt.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
        attempt.setAttemptAt(Instant.now());
        attempt.setResult(DeliveryAttemptResult.FAILED);
        attempt.setReason(req.getReason());
        attempt.setHandledByUsername(currentActor());
        attempt.setOrder(order);
        deliveryAttemptRepository.save(attempt);

        OrderTransitionRequest toFail = new OrderTransitionRequest();
        toFail.setToStatus(OrderStatus.FAILED_DELIVERY);
        toFail.setAction("FAIL");
        toFail.setDetail(req.getReason());
        orderFacadeService.transition(order.getOrderCode(), toFail);

        boolean returned = false;
        Instant firstFail = deliveryAttemptRepository
            .findByOrder_IdOrderByAttemptAtAsc(order.getId())
            .stream()
            .filter(a -> a.getResult() == DeliveryAttemptResult.FAILED)
            .map(OrderDeliveryAttempt::getAttemptAt)
            .findFirst()
            .orElse(Instant.now());
        boolean windowExpired = firstFail.isBefore(Instant.now().minus(java.time.Duration.ofHours(48)));
        if (nextFail >= MAX_FAILS || windowExpired) {
            OrderTransitionRequest toBranch = new OrderTransitionRequest();
            toBranch.setToStatus(OrderStatus.AT_DEST);
            toBranch.setAction(windowExpired && nextFail < MAX_FAILS ? "FAIL_48H" : "FAIL_MAX");
            toBranch.setDetail(windowExpired && nextFail < MAX_FAILS ? "Hết 48h — về AT_DEST" : "3 lần — về AT_DEST");
            orderFacadeService.transition(order.getOrderCode(), toBranch);

            OrderDeliveryAttempt ret = new OrderDeliveryAttempt();
            ret.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
            ret.setAttemptAt(Instant.now());
            ret.setResult(DeliveryAttemptResult.RETURNED_TO_BRANCH);
            ret.setReason(windowExpired && nextFail < MAX_FAILS ? "FAIL_48H" : "FAIL_MAX");
            ret.setHandledByUsername(currentActor());
            ret.setOrder(requireOrder(order.getOrderCode()));
            deliveryAttemptRepository.save(ret);
            returned = true;
        }

        order = requireOrder(order.getOrderCode());
        FailDeliveryResponse res = new FailDeliveryResponse();
        res.setOk(true);
        res.setOrderCode(order.getOrderCode());
        res.setStatus(order.getStatus());
        res.setFailCount(order.getFailCount() == null ? nextFail : order.getFailCount());
        res.setReturnedToBranch(returned);
        return res;
    }

    public OrderDetailDTO assignShipper(String orderCode, AssignShipperRequest req) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        if (order.getStatus() != OrderStatus.AT_DEST && order.getStatus() != OrderStatus.FAILED_DELIVERY) {
            throw new BadRequestAlertException("Assign shipper from AT_DEST or FAILED_DELIVERY", ENTITY, "assignShipperStatus");
        }
        PartnerAdvance.assertNoRefundDue(order, ENTITY);

        String mode = req.getMode() == null ? "INTERNAL" : req.getMode().trim().toUpperCase();
        String action;
        String detail;
        if ("PARTNER".equals(mode) || req.getPartner() != null) {
            if (req.getPartner() != null) {
                // store partner code on order
            }
            if (req.getPartnerCode() != null) {
                order.setPartnerCode(req.getPartnerCode());
            } else if (req.getPartner() != null) {
                order.setPartnerCode(req.getPartner().name());
            }
            if (req.getPartnerFeeAmount() != null) {
                order.setPartnerFeeAmount(req.getPartnerFeeAmount());
            }
            order.setShipper(null);
            shipmentOrderRepository.save(order);
            action = "PUSH_SHIP";
            detail =
                (req.getPartner() != null ? req.getPartner().name() : "PARTNER") +
                (req.getPartnerCode() != null ? " · " + req.getPartnerCode() : "");

            OrderDeliveryAttempt attempt = new OrderDeliveryAttempt();
            attempt.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
            attempt.setAttemptAt(Instant.now());
            attempt.setResult(DeliveryAttemptResult.SUCCESS); // accepted / out for delivery
            attempt.setHandledByUsername(currentActor());
            attempt.setDeliveryPartner(req.getPartner());
            attempt.setReason("ASSIGN_PARTNER");
            attempt.setOrder(order);
            deliveryAttemptRepository.save(attempt);
        } else {
            action = "TAKE_JOB";
            detail = "Internal shipper";
            String reason = "TAKE_JOB";
            if (req.getShipperId() != null) {
                Shipper shipper = requireAssignableShipper(order, req.getShipperId());
                order.setShipper(shipper);
                shipmentOrderRepository.save(order);
                String who = shipper.getFullName() + (shipper.getPhone() != null ? " · " + shipper.getPhone() : "");
                detail = "Shipper nội bộ: " + who;
                reason = "TAKE_JOB · " + who;
            }
            if (req.getNote() != null && !req.getNote().isBlank()) {
                detail = detail + " — " + req.getNote().trim();
            }
            OrderDeliveryAttempt attempt = new OrderDeliveryAttempt();
            attempt.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
            attempt.setAttemptAt(Instant.now());
            attempt.setResult(DeliveryAttemptResult.SUCCESS);
            attempt.setHandledByUsername(currentActor());
            attempt.setReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
            attempt.setOrder(order);
            deliveryAttemptRepository.save(attempt);
        }

        OrderTransitionRequest tr = new OrderTransitionRequest();
        tr.setToStatus(OrderStatus.OUT_FOR_DELIVERY);
        tr.setAction(action);
        tr.setDetail(detail);
        orderFacadeService.transition(order.getOrderCode(), tr);

        return orderFacadeService.getByCode(order.getOrderCode());
    }

    public OrderDetailDTO addPayment(String orderCode, AddPaymentRequest req) {
        ShipmentOrder order = requireOrder(orderCode);
        addPaymentInternal(
            order,
            req.getAmount(),
            req.getMethod(),
            req.getPaymentKind() != null ? req.getPaymentKind() : PaymentKind.SAU,
            req.getNote(),
            currentActor()
        );
        shipmentOrderRepository.save(order);
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    /** Đơn người gửi trả: cước còn nợ thuộc phần VP gửi lập phiếu thu, không thu người nhận lúc giao. */
    static void assertReceiverCollectAllowed(ShipmentOrder order, BigDecimal collected) {
        if (collected != null && collected.signum() > 0 && order.getPaymentTerm() == PaymentTerm.GUI_TRA) {
            throw new BadRequestAlertException(
                "Đơn người gửi trả — cước còn nợ do VP gửi lập phiếu thu, không thu người nhận khi giao. Để số thu = 0.",
                ENTITY,
                "senderPaysNoCollect"
            );
        }
    }

    /**
     * Ahamove đang giao → POD do webhook (COMPLETED + ảnh). Ahamove đã COMPLETED (không ảnh) thì POD tay được;
     * webhook không về thì chỉ Admin POD tay.
     */
    static void assertPartnerPodAllowed(ShipmentOrder order, boolean admin) {
        if (AhamoveDispatchService.partnerActive(order) && !admin) {
            throw new BadRequestAlertException(
                "Đơn đang giao qua Ahamove — chờ Ahamove báo giao xong (ảnh POD tự lưu)",
                ENTITY,
                "ahamoveDeliveringPod"
            );
        }
    }

    /** CONFIRMED/WAITING = hàng còn ở VP gửi: chỉ giao tại chỗ khi VP gửi cũng là VP nhận. */
    static void assertArrivedForPod(ShipmentOrder order) {
        OrderStatus status = order.getStatus();
        if ((status == OrderStatus.CONFIRMED || status == OrderStatus.WAITING) && !sameOriginAndDestination(order)) {
            throw new BadRequestAlertException(
                "Order has not arrived at destination office (status " + status + ")",
                ENTITY,
                "podNotArrived"
            );
        }
    }

    private static boolean sameOriginAndDestination(ShipmentOrder order) {
        Long from = order.getFromOffice() != null ? order.getFromOffice().getId() : null;
        if (from == null) {
            return false;
        }
        Long to = order.getToOffice() != null ? order.getToOffice().getId() : null;
        Long finalTo = order.getFinalToOffice() != null ? order.getFinalToOffice().getId() : null;
        return from.equals(finalTo != null ? finalTo : to);
    }

    private void ensureStatusThenDeliver(ShipmentOrder order, String action, String detail) {
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.DELIVERED || status == OrderStatus.RETURNED) {
            return;
        }
        // Đơn đang hoàn (RETURNING): POD = trả người gửi → RETURNED (không DELIVERED / không KPI giao đi).
        if (status == OrderStatus.RETURNING) {
            OrderTransitionRequest tr = new OrderTransitionRequest();
            tr.setToStatus(OrderStatus.RETURNED);
            tr.setAction(action != null && !action.isBlank() ? action : "RT_DONE");
            tr.setDetail(detail != null && !detail.isBlank() ? detail : "Hoàn thành công");
            orderFacadeService.transition(order.getOrderCode(), tr);
            OrderDeliveryAttempt attempt = new OrderDeliveryAttempt();
            attempt.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
            attempt.setAttemptAt(Instant.now());
            attempt.setResult(DeliveryAttemptResult.SUCCESS);
            attempt.setHandledByUsername(currentActor());
            attempt.setReason("RETURN_POD");
            attempt.setOrder(requireOrder(order.getOrderCode()));
            deliveryAttemptRepository.save(attempt);
            return;
        }
        // FE / app: AT_DEST, OUT_FOR_DELIVERY, CONFIRMED, WAITING → DELIVERED
        if (
            status != OrderStatus.AT_DEST &&
            status != OrderStatus.OUT_FOR_DELIVERY &&
            status != OrderStatus.CONFIRMED &&
            status != OrderStatus.WAITING
        ) {
            throw new BadRequestAlertException("Cannot POD from status " + status, ENTITY, "podInvalidStatus");
        }
        assertArrivedForPod(order);
        OrderTransitionRequest tr = new OrderTransitionRequest();
        tr.setToStatus(OrderStatus.DELIVERED);
        tr.setAction(action);
        tr.setDetail(detail);
        orderFacadeService.transition(order.getOrderCode(), tr);

        OrderDeliveryAttempt attempt = new OrderDeliveryAttempt();
        attempt.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
        attempt.setAttemptAt(Instant.now());
        attempt.setResult(DeliveryAttemptResult.SUCCESS);
        attempt.setHandledByUsername(currentActor());
        attempt.setReason(action);
        attempt.setOrder(requireOrder(order.getOrderCode()));
        deliveryAttemptRepository.save(attempt);
    }

    private void addPaymentInternal(
        ShipmentOrder order,
        BigDecimal amount,
        PaymentMethod method,
        PaymentKind kind,
        String note,
        String actor
    ) {
        assertPaymentAllowed(order, amount);
        OrderPayment payment = new OrderPayment();
        payment.setPaymentAt(Instant.now());
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setPaymentKind(kind);
        payment.setNote(note);
        payment.setCollectorUsername(actor);
        payment.setOrder(order);
        paymentRepository.save(payment);

        BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        order.setPaidAmount(paid.add(amount));
    }

    /**
     * NV quầy nhận tiền mặt tài xế đối tác ứng: đánh dấu đã nhận rồi ghi khoản thu (người thu = NV đang thao tác).
     * Vẫn qua guard chốt ngày + không vượt số còn nợ.
     */
    /** {@code collector} = người nhận nợ khoản ứng (người bàn giao Ahamove); người bấm xác nhận ghi ở partnerCodCollectedBy. */
    public void recordPartnerAdvance(ShipmentOrder order, BigDecimal amount, String note, String collector) {
        String actor = currentActor();
        order.setPartnerCodCollectedAt(Instant.now());
        order.setPartnerCodCollectedBy(actor);
        addPaymentInternal(order, amount, PaymentMethod.TM, PaymentKind.SAU, note, collector != null ? collector : actor);
        shipmentOrderRepository.save(order);
    }

    private void assertPaymentAllowed(ShipmentOrder order, BigDecimal amount) {
        dayClosureGuard.assertCollectionMutable(order);
        PartnerAdvance.assertNotPending(order, ENTITY);
        BigDecimal due = OrderMoney.collectDue(order);
        if (amount.compareTo(due) > 0) {
            throw new BadRequestAlertException(
                "amount exceeds due for " + order.getOrderCode() + " (due=" + due + ")",
                ENTITY,
                "amountExceedsDue"
            );
        }
    }

    /** Shipper phải đang hoạt động và thuộc VP nhận của đơn (VP đích cuối nếu có trung chuyển). */
    Shipper requireAssignableShipper(ShipmentOrder order, Long shipperId) {
        Shipper shipper = shipperRepository
            .findById(shipperId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy shipper", ENTITY, "shipperNotFound"));
        if (!Boolean.TRUE.equals(shipper.getActive())) {
            throw new BadRequestAlertException("Shipper " + shipper.getFullName() + " đã ngừng hoạt động", ENTITY, "shipperInactive");
        }
        Office dest = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
        if (dest == null || shipper.getOffice() == null || !Objects.equals(dest.getId(), shipper.getOffice().getId())) {
            throw new BadRequestAlertException(
                "Shipper " + shipper.getFullName() + " không thuộc VP nhận của đơn",
                ENTITY,
                "shipperOfficeMismatch"
            );
        }
        return shipper;
    }

    private ShipmentOrder requireOrder(String code) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(code.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + code));
    }

    private static String truncateUrl(String url) {
        if (url == null) {
            return "";
        }
        // Cho phép data-URL ảnh POD từ app (LONGTEXT). Giới hạn cứng tránh payload quá lớn.
        String trimmed = url.trim();
        final int max = 1_500_000;
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static String currentActor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }
}
