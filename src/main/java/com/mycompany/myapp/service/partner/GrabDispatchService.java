package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import com.mycompany.myapp.domain.PartnerFeeExpense;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.DeliveryAttemptResult;
import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.repository.PartnerFeeExpenseRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gọi GrabExpress cùng lối Ahamove: phí là chi phí đối tác, cước khách không đổi.
 * Cước còn nợ gửi Grab thu hộ (COD advanced) để tài xế ứng cho người bàn giao.
 * Tip chọn lúc book, Grab chỉ nhận sau khi giao xong rồi cộng vào phí đối tác.
 */
@Service
public class GrabDispatchService {

    private static final Logger LOG = LoggerFactory.getLogger(GrabDispatchService.class);
    private static final String ENTITY = "grab";
    public static final String PARTNER_CODE = "GRAB";

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final OrderDeliveryAttemptRepository deliveryAttemptRepository;
    private final PartnerFeeExpenseRepository partnerFeeExpenseRepository;
    private final GrabExpressClient grab;
    private final OsmNominatimService osmNominatimService;
    private final GoongPlacesService goongPlacesService;
    private final DeliveryFacadeService deliveryFacadeService;
    private final OrderFacadeService orderFacadeService;
    private final DayClosureGuard dayClosureGuard;
    private final TransactionTemplate tx;

    public GrabDispatchService(
        ShipmentOrderRepository shipmentOrderRepository,
        IntegrationConfigRepository integrationConfigRepository,
        OrderDeliveryAttemptRepository deliveryAttemptRepository,
        PartnerFeeExpenseRepository partnerFeeExpenseRepository,
        GrabExpressClient grab,
        OsmNominatimService osmNominatimService,
        GoongPlacesService goongPlacesService,
        DeliveryFacadeService deliveryFacadeService,
        OrderFacadeService orderFacadeService,
        DayClosureGuard dayClosureGuard,
        PlatformTransactionManager transactionManager
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.partnerFeeExpenseRepository = partnerFeeExpenseRepository;
        this.grab = grab;
        this.osmNominatimService = osmNominatimService;
        this.goongPlacesService = goongPlacesService;
        this.deliveryFacadeService = deliveryFacadeService;
        this.orderFacadeService = orderFacadeService;
        this.dayClosureGuard = dayClosureGuard;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public GrabExpressClient.Quote estimate(
        Double officeLat,
        Double officeLng,
        Double pinLat,
        Double pinLng,
        String address,
        String officeAddress,
        String bulkyTier,
        String orderCode
    ) {
        if (officeLat == null || officeLng == null) {
            throw new BadRequestAlertException("VP chưa có toạ độ GPS", ENTITY, "grabOfficeGps");
        }
        String drop = blank(address) ? "" : address.trim();
        double[] pin = coords(pinLat, pinLng, drop);
        ShipmentOrder order = orderCode == null || orderCode.isBlank()
            ? null
            : shipmentOrderRepository.findOneByOrderCodeOrDraftCode(orderCode.trim()).orElse(null);
        AhamoveCargo cargo = AhamoveCargo.from(order);
        String officeText = blank(officeAddress) ? "Văn phòng" : officeAddress.trim();
        GrabExpressClient.Stop office = new GrabExpressClient.Stop(officeLat, officeLng, officeText, "CPN", null, null);
        GrabExpressClient.Stop point = new GrabExpressClient.Stop(pin[0], pin[1], blank(drop) ? "Điểm giao" : drop, "Khách", null, null);
        return grab.quote(office, point, GrabExpressClient.vehicleOf(bulkyTier), cargo);
    }

    public OrderDetailDTO dispatch(String orderCode, AhamoveDispatchService.DispatchRequest request) {
        AhamoveDispatchService.DispatchRequest req = request != null ? request : new AhamoveDispatchService.DispatchRequest();
        ShipmentOrder order = require(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        if (order.getStatus() != OrderStatus.AT_DEST && order.getStatus() != OrderStatus.FAILED_DELIVERY) {
            throw new BadRequestAlertException("Chỉ gọi Grab khi hàng ở kho VP nhận / giao thất bại", ENTITY, "grabStatus");
        }
        if (!Boolean.TRUE.equals(order.getHomeDelivery())) {
            throw new BadRequestAlertException("Đơn không phải giao tận nơi", ENTITY, "grabNotHome");
        }
        if (OrderMoney.nz(order.getCodAmount()).signum() > 0) {
            throw new BadRequestAlertException("Đơn có COD hàng — chưa hỗ trợ qua Grab", ENTITY, "grabCod");
        }
        PartnerAdvance.assertNoRefundDue(order, ENTITY);
        AhamoveDispatchService.assertNoActivePartner(order);
        BigDecimal advance = AhamoveDispatchService.advanceFor(order);
        if (advance.signum() > 0) {
            dayClosureGuard.assertCollectionMutable(order);
        }
        Office office = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
        if (office == null || office.getLatitude() == null || office.getLongitude() == null) {
            throw new BadRequestAlertException("VP nhận chưa có toạ độ GPS", ENTITY, "grabOfficeGps");
        }
        String dropAddress = blank(req.getAddress()) ? order.getDeliveryAddress() : req.getAddress().trim();
        if (blank(dropAddress)) {
            throw new BadRequestAlertException("Đơn chưa có địa chỉ giao", ENTITY, "grabAddress");
        }
        String phone = GrabExpressClient.phone(order.getReceiverPhone());
        if (phone == null) {
            throw new BadRequestAlertException("SĐT người nhận không hợp lệ", ENTITY, "grabPhone");
        }
        int tip = tipUnits(req);
        if (advance.signum() > 0 && tip > 0) {
            tip = 0;
        }
        AhamoveCargo cargo = AhamoveCargo.from(order);
        String note = note(req.getRemarks(), advance, tip);
        GrabExpressClient.Stop origin = new GrabExpressClient.Stop(
            office.getLatitude().doubleValue(),
            office.getLongitude().doubleValue(),
            blank(office.getAddress()) ? office.getName() : office.getAddress(),
            "CPN " + office.getName(),
            senderPhone(),
            null
        );
        double[] dropPin = coords(req.getLat(), req.getLng(), dropAddress);
        req.setLat(dropPin[0]);
        req.setLng(dropPin[1]);
        GrabExpressClient.Stop dest = new GrabExpressClient.Stop(dropPin[0], dropPin[1], dropAddress, order.getReceiverName(), phone, note);
        String merchantId = merchantId(order.getOrderCode());
        GrabExpressClient.Created created = grab.create(
            origin,
            dest,
            GrabExpressClient.vehicleOf(req.getBulkyTier()),
            cargo,
            merchantId,
            advance.signum() > 0 ? advance.longValue() : null
        );
        int tipSaved = tip;
        String actor = actor();
        try {
            tx.executeWithoutResult(status -> saveBook(order.getOrderCode(), created, req, advance, actor, false, tipSaved));
        } catch (RuntimeException e) {
            cancelQuiet(created.deliveryId());
            throw e;
        }
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    public OrderDetailDTO dispatchPickup(String orderCode, AhamoveDispatchService.DispatchRequest request) {
        AhamoveDispatchService.DispatchRequest req = request != null ? request : new AhamoveDispatchService.DispatchRequest();
        ShipmentOrder order = require(orderCode);
        Office office = assertPickup(order);
        String pickupAddress = blank(req.getAddress()) ? order.getPickupAddress() : req.getAddress().trim();
        if (blank(pickupAddress)) {
            throw new BadRequestAlertException("Đơn chưa có địa chỉ lấy", ENTITY, "grabAddress");
        }
        String phone = GrabExpressClient.phone(order.getSenderPhone());
        if (phone == null) {
            throw new BadRequestAlertException("SĐT người gửi không hợp lệ", ENTITY, "grabPhone");
        }
        int tip = tipUnits(req);
        AhamoveCargo cargo = AhamoveCargo.from(order);
        double[] pickupPin = coords(req.getLat(), req.getLng(), pickupAddress);
        GrabExpressClient.Stop origin = new GrabExpressClient.Stop(
            pickupPin[0],
            pickupPin[1],
            pickupAddress,
            order.getSenderName(),
            phone,
            note(req.getRemarks(), BigDecimal.ZERO, tip)
        );
        GrabExpressClient.Stop dest = new GrabExpressClient.Stop(
            office.getLatitude().doubleValue(),
            office.getLongitude().doubleValue(),
            blank(office.getAddress()) ? office.getName() : office.getAddress(),
            "CPN " + office.getName(),
            senderPhone(),
            null
        );
        GrabExpressClient.Created created = grab.create(
            origin,
            dest,
            GrabExpressClient.vehicleOf(req.getBulkyTier()),
            cargo,
            merchantId(order.getOrderCode()),
            null
        );
        String actor = actor();
        try {
            tx.executeWithoutResult(status -> saveBook(order.getOrderCode(), created, req, BigDecimal.ZERO, actor, true, tip));
        } catch (RuntimeException e) {
            cancelQuiet(created.deliveryId());
            throw e;
        }
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    public OrderDetailDTO cancel(String orderCode, String reason) {
        ShipmentOrder order = require(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        if (!PARTNER_CODE.equals(order.getPartnerCode()) || blank(order.getPartnerOrderId())) {
            throw new BadRequestAlertException("Đơn không giao qua Grab", ENTITY, "grabNotDispatched");
        }
        OrderStatus st = order.getStatus();
        boolean waiting =
            st == OrderStatus.AT_DEST ||
            st == OrderStatus.FAILED_DELIVERY ||
            (Boolean.TRUE.equals(order.getHomePickup()) &&
                order.getPickedUpAt() == null &&
                (st == OrderStatus.CONFIRMED || st == OrderStatus.WAITING));
        if (st != OrderStatus.OUT_FOR_DELIVERY && !waiting) {
            throw new BadRequestAlertException("Chỉ hủy khi tài xế chưa lấy hàng hoặc đơn đang giao", ENTITY, "grabCancelStatus");
        }
        String comment = blank(reason) ? "CPN hủy giao" : reason.trim();
        grab.cancel(order.getPartnerOrderId());
        String actor = actor();
        tx.executeWithoutResult(status -> markCancelled(order.getOrderCode(), comment, actor));
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    public Optional<String> applyWebhook(JsonNode body) {
        if (body == null || !body.isObject()) {
            return Optional.empty();
        }
        String deliveryId = text(body, "deliveryID");
        if (deliveryId == null) {
            return Optional.empty();
        }
        Optional<ShipmentOrder> found = shipmentOrderRepository.findFirstByPartnerOrderId(deliveryId);
        if (found.isEmpty()) {
            LOG.info("Grab webhook: không có đơn CPN cho {}", deliveryId);
            return Optional.empty();
        }
        String code = found.get().getOrderCode();
        String status = normalize(text(body, "status"));
        tx.executeWithoutResult(s -> saveInfo(code, body, status));
        ShipmentOrder order = require(code);
        savePhotos(order, body);
        if (AhamoveDispatchService.isDoorPickup(order)) {
            applyDoorPickup(code, status, body);
            return Optional.of(code);
        }
        if (pickedUp(status)) {
            startDelivery(order, status);
            recordCashFee(code);
            order = require(code);
        }
        try {
            if ("COMPLETED".equals(status) && order.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
                List<String> photos = proof(body, "dropoffProofURL");
                if (!photos.isEmpty()) {
                    PodRequest pod = new PodRequest();
                    pod.setChannel("HOME");
                    String name = blank(order.getReceiverName()) ? "Người nhận" : order.getReceiverName().trim();
                    pod.setActualRecipientName(name.length() > 100 ? name.substring(0, 100) : name);
                    pod.setPhotos(photos);
                    pod.setCaption(AhamoveDispatchService.CAPTION_DROP);
                    pod.setCollectedAmount(BigDecimal.ZERO);
                    deliveryFacadeService.pod(code, pod);
                }
                submitTip(code);
            } else if ("FAILED".equals(status) && order.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
                FailDeliveryRequest fail = new FailDeliveryRequest();
                String reason = "Grab: " + (text(body, "failedReason") == null ? "giao thất bại" : text(body, "failedReason"));
                fail.setReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
                deliveryFacadeService.failDelivery(code, fail);
            } else if ("CANCELLED".equals(status)) {
                tx.executeWithoutResult(s ->
                    markCancelled(code, text(body, "failedReason") == null ? "Grab hủy đơn" : text(body, "failedReason"), "grab")
                );
            }
        } catch (RuntimeException e) {
            LOG.warn("Grab webhook {}: không tự chuyển {} — {}", deliveryId, code, e.getMessage());
        }
        return Optional.of(code);
    }

    public void pullPhotos(String orderCode) {
        if (blank(orderCode)) {
            return;
        }
        ShipmentOrder order;
        try {
            order = require(orderCode.trim());
        } catch (RuntimeException e) {
            return;
        }
        if (!PARTNER_CODE.equals(order.getPartnerCode()) || blank(order.getPartnerOrderId())) {
            return;
        }
        try {
            JsonNode body = grab.fetch(order.getPartnerOrderId());
            if (body != null) {
                savePhotos(order, body);
            }
        } catch (RuntimeException e) {
            LOG.warn("Grab pull photos {}: {}", orderCode, e.getMessage());
        }
    }

    public boolean webhookAuthorized(String token, String authorization) {
        var cfg = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        if (cfg == null) {
            return false;
        }
        if (notBlank(cfg.getGrabWebhookToken()) && AutoCallService.tokenMatches(cfg.getGrabWebhookToken(), token)) {
            return true;
        }
        String secret = cfg.getGrabClientSecret();
        if (!notBlank(secret) || !notBlank(authorization)) {
            return false;
        }
        String header = authorization.trim();
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            header = header.substring(7).trim();
        }
        return AutoCallService.tokenMatches(secret, header);
    }

    private void saveBook(
        String code,
        GrabExpressClient.Created created,
        AhamoveDispatchService.DispatchRequest req,
        BigDecimal advance,
        String actor,
        boolean pickup,
        int tip
    ) {
        ShipmentOrder order = require(code);
        Instant now = Instant.now();
        order.setPartnerCode(PARTNER_CODE);
        order.setPartnerOrderId(created.deliveryId());
        order.setPartnerStatus(normalize(created.status()));
        order.setPartnerSubStatus(null);
        order.setPartnerTrackingUrl(truncate(created.trackingUrl(), 500));
        order.setPartnerFeeAmount(created.amount());
        String payMethod = GrabExpressClient.storedPayMethod(paymentMethod());
        order.setPartnerPayMethod(payMethod);
        order.setPartnerDriverName(null);
        order.setPartnerDriverPhone(null);
        order.setPartnerPodUrl(null);
        order.setPartnerFailReason(null);
        order.setPartnerUpdatedAt(now);
        order.setPartnerTipUnits(tip > 0 ? tip : null);
        order.setShipper(null);
        if (!pickup) {
            order.setPartnerCodAmount(advance.signum() > 0 ? advance : null);
            order.setPartnerCodCollectedAt(null);
            order.setPartnerCodCollectedBy(null);
            order.setDeliveryLat(BigDecimal.valueOf(req.getLat()).setScale(7, RoundingMode.HALF_UP));
            order.setDeliveryLng(BigDecimal.valueOf(req.getLng()).setScale(7, RoundingMode.HALF_UP));
        }
        shipmentOrderRepository.save(order);
        OrderDeliveryAttempt attempt = new OrderDeliveryAttempt();
        attempt.setAttemptNo((int) deliveryAttemptRepository.countByOrder_Id(order.getId()) + 1);
        attempt.setAttemptAt(now);
        attempt.setResult(DeliveryAttemptResult.SUCCESS);
        attempt.setHandledByUsername(actor);
        attempt.setDeliveryPartner(DeliveryPartner.GRAB);
        attempt.setReason("ASSIGN_PARTNER");
        attempt.setOrder(order);
        deliveryAttemptRepository.save(attempt);
        String fee = created.amount() == null
            ? ""
            : " · phí " + created.amount().toPlainString() + ("CASH".equals(payMethod) ? " (tiền mặt)" : " (ví Grab)");
        String tipNote = tip > 0 ? " · tip " + (tip * AhamoveCargo.TIP_UNIT_VND) + "đ sau khi giao" : "";
        String advanceNote = !pickup && advance.signum() > 0 ? " · sẽ ứng " + PartnerAdvance.money(advance) + " khi bàn giao ship" : "";
        orderFacadeService.recordEvent(
            order,
            "GRAB_BOOK",
            "Book Grab " +
            (pickup ? "lấy tận nơi " : "") +
            created.deliveryId() +
            " · " +
            GrabExpressClient.vehicleOf(req.getBulkyTier()) +
            fee +
            tipNote +
            advanceNote,
            actor
        );
    }

    private void saveInfo(String code, JsonNode body, String status) {
        ShipmentOrder order = require(code);
        String before = order.getPartnerStatus();
        if (status != null) {
            order.setPartnerStatus(truncate(status, 40));
        }
        JsonNode driver = body.get("driver");
        if (driver != null && driver.isObject()) {
            if (text(driver, "name") != null) {
                order.setPartnerDriverName(truncate(text(driver, "name"), 100));
            }
            if (text(driver, "phone") != null) {
                order.setPartnerDriverPhone(truncate(text(driver, "phone"), 32));
            }
        }
        if (text(body, "trackURL") != null) {
            order.setPartnerTrackingUrl(truncate(text(body, "trackURL"), 500));
        }
        if (text(body, "failedReason") != null) {
            order.setPartnerFailReason(truncate(text(body, "failedReason"), 500));
        }
        order.setPartnerUpdatedAt(Instant.now());
        shipmentOrderRepository.save(order);
        if (status != null && !status.equals(before)) {
            orderFacadeService.recordEvent(order, "GRAB_STATUS", "Grab " + status + driverNote(driver), "grab");
        }
    }

    private void applyDoorPickup(String code, String status, JsonNode body) {
        List<String> drop = proof(body, "dropoffProofURL");
        if (!drop.isEmpty()) {
            try {
                deliveryFacadeService.appendPodPhotos(require(code), drop, AhamoveDispatchService.CAPTION_DROP);
            } catch (RuntimeException e) {
                LOG.warn("Grab lấy {}: không lưu ảnh — {}", code, e.getMessage());
            }
        }
        if ("COMPLETED".equals(status)) {
            try {
                orderFacadeService.warehouseReceive(code);
            } catch (RuntimeException e) {
                LOG.warn("Grab lấy {}: chưa nhập kho — {}", code, e.getMessage());
            }
            submitTip(code);
            return;
        }
        if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
            tx.executeWithoutResult(s -> {
                ShipmentOrder order = require(code);
                order.setPickingAt(null);
                shipmentOrderRepository.save(order);
            });
            return;
        }
        if (pickedUp(status)) {
            try {
                orderFacadeService.pickupStart(code);
            } catch (RuntimeException e) {
                LOG.warn("Grab lấy {}: chưa chuyển đang lấy — {}", code, e.getMessage());
            }
            recordCashFee(code);
        }
    }

    private void startDelivery(ShipmentOrder order, String status) {
        OrderStatus st = order.getStatus();
        if (st != OrderStatus.AT_DEST && st != OrderStatus.FAILED_DELIVERY) {
            return;
        }
        try {
            OrderTransitionRequest tr = new OrderTransitionRequest();
            tr.setToStatus(OrderStatus.OUT_FOR_DELIVERY);
            tr.setAction("GRAB_PICKUP");
            tr.setDetail("Tài xế Grab đã lấy hàng · " + status);
            orderFacadeService.transition(order.getOrderCode(), tr);
        } catch (RuntimeException e) {
            LOG.warn("Grab {}: chưa chuyển đang giao — {}", order.getOrderCode(), e.getMessage());
        }
    }

    private void markCancelled(String code, String reason, String actor) {
        ShipmentOrder order = require(code);
        if ("CANCELLED".equals(order.getPartnerStatus()) && order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            return;
        }
        order.setPartnerStatus("CANCELLED");
        order.setPartnerFailReason(truncate(reason, 500));
        order.setPartnerTipUnits(null);
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
        attempt.setDeliveryPartner(DeliveryPartner.GRAB);
        attempt.setReason(truncate("GRAB_CANCEL: " + reason, 255));
        attempt.setOrder(order);
        deliveryAttemptRepository.save(attempt);
        OrderTransitionRequest tr = new OrderTransitionRequest();
        tr.setToStatus(OrderStatus.FAILED_DELIVERY);
        tr.setAction("GRAB_CANCEL");
        tr.setDetail(truncate(reason, 255));
        orderFacadeService.transition(code, tr);
    }

    private void submitTip(String code) {
        ShipmentOrder order = require(code);
        int units = order.getPartnerTipUnits() == null ? 0 : order.getPartnerTipUnits();
        if (units <= 0 || blank(order.getPartnerOrderId())) {
            return;
        }
        long amount = (long) units * AhamoveCargo.TIP_UNIT_VND;
        try {
            grab.tip(order.getPartnerOrderId(), amount);
        } catch (RuntimeException e) {
            LOG.warn("Grab tip {} chưa gửi được — {}", code, e.getMessage());
            return;
        }
        tx.executeWithoutResult(s -> {
            ShipmentOrder row = require(code);
            BigDecimal fee = OrderMoney.nz(row.getPartnerFeeAmount()).add(BigDecimal.valueOf(amount));
            row.setPartnerFeeAmount(fee);
            row.setPartnerTipUnits(null);
            shipmentOrderRepository.save(row);
            orderFacadeService.recordEvent(row, "GRAB_TIP", "Tip Grab " + amount + "đ đã cộng vào phí đối tác", "grab");
        });
        recordCashFee(code);
    }

    private String bookerOf(ShipmentOrder order) {
        String who = null;
        for (OrderDeliveryAttempt a : deliveryAttemptRepository.findByOrder_IdOrderByAttemptAtAsc(order.getId())) {
            if (
                a.getDeliveryPartner() == DeliveryPartner.GRAB &&
                "ASSIGN_PARTNER".equals(a.getReason()) &&
                notBlank(a.getHandledByUsername())
            ) {
                who = a.getHandledByUsername();
            }
        }
        return who != null ? who : "system";
    }

    private String paymentMethod() {
        return integrationConfigRepository.findAll().stream().findFirst().map(cfg -> cfg.getGrabPaymentMethod()).orElse(null);
    }

    /**
     * Tiền mặt: người gọi xe trả tài xế lúc lấy hàng. Ghi một khoản để trừ phiếu thu.
     * Tip cộng sau khi giao thì cập nhật khoản chưa vào phiếu.
     */
    private void recordCashFee(String code) {
        tx.executeWithoutResult(s -> {
            ShipmentOrder order = require(code);
            String partnerOrderId = order.getPartnerOrderId();
            BigDecimal fee = OrderMoney.nz(order.getPartnerFeeAmount());
            if (!"CASH".equals(order.getPartnerPayMethod()) || !notBlank(partnerOrderId) || fee.signum() <= 0) {
                return;
            }
            PartnerFeeExpense open = partnerFeeExpenseRepository
                .findByOrder_Id(order.getId())
                .stream()
                .filter(e -> partnerOrderId.equals(e.getPartnerOrderId()))
                .findFirst()
                .orElse(null);
            if (open != null) {
                if (open.getReceipt() == null && fee.compareTo(OrderMoney.nz(open.getAmount())) != 0) {
                    open.setAmount(fee);
                    partnerFeeExpenseRepository.save(open);
                }
                return;
            }
            String confirmed = order.getPartnerShipConfirmedBy();
            String payer = notBlank(confirmed) ? confirmed.trim() : bookerOf(order);
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
                "GRAB_FEE",
                "Phí Grab " + PartnerAdvance.money(fee) + "đ (" + partnerOrderId + ") · " + payer + " trả tài xế, trừ vào phiếu thu",
                "grab"
            );
        });
    }

    private void savePhotos(ShipmentOrder order, JsonNode body) {
        List<String> pickup = proof(body, "pickupProofURL");
        List<String> drop = proof(body, "dropoffProofURL");
        if (!pickup.isEmpty()) {
            try {
                deliveryFacadeService.appendPodPhotos(order, pickup, AhamoveDispatchService.CAPTION_PICKUP);
            } catch (RuntimeException e) {
                LOG.warn("Grab ảnh nhận {}: {}", order.getOrderCode(), e.getMessage());
            }
        }
        if (!drop.isEmpty() && order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            try {
                deliveryFacadeService.appendPodPhotos(order, drop, AhamoveDispatchService.CAPTION_DROP);
            } catch (RuntimeException e) {
                LOG.warn("Grab ảnh giao {}: {}", order.getOrderCode(), e.getMessage());
            }
        }
    }

    private List<String> proof(JsonNode body, String field) {
        List<String> out = new ArrayList<>();
        String url = text(body, field);
        if (url == null) {
            return out;
        }
        String data = grab.downloadDataUrl(url);
        if (data != null) {
            out.add(data);
        }
        return out;
    }

    private Office assertPickup(ShipmentOrder order) {
        dayClosureGuard.assertOrderMutable(order);
        if (!AhamoveDispatchService.isDoorPickup(order)) {
            throw new BadRequestAlertException("Chỉ gọi Grab lấy hàng khi đơn đang chờ lấy", ENTITY, "grabPickupStatus");
        }
        if (order.getPaymentTerm() != PaymentTerm.NHAN_TRA) {
            throw new BadRequestAlertException("Đơn lấy tận nơi chỉ chọn người nhận thanh toán", ENTITY, "pickupReceiverPays");
        }
        AhamoveDispatchService.assertNoActivePartner(order);
        Office office = order.getFromOffice();
        if (office == null || office.getLatitude() == null || office.getLongitude() == null) {
            throw new BadRequestAlertException("VP gửi chưa có toạ độ GPS", ENTITY, "grabOfficeGps");
        }
        return office;
    }

    private String senderPhone() {
        var cfg = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        String raw = cfg == null ? null : cfg.getAhamoveSenderMobile();
        if (blank(raw) && cfg != null) {
            raw = cfg.getAhamoveMobile();
        }
        String phone = GrabExpressClient.phone(raw);
        if (phone == null) {
            throw new BadRequestAlertException("Chưa có SĐT người gửi Grab — dùng SĐT Ahamove trong Tích hợp", ENTITY, "grabSenderPhone");
        }
        return phone;
    }

    private void cancelQuiet(String deliveryId) {
        try {
            grab.cancel(deliveryId);
        } catch (RuntimeException e) {
            LOG.error("Grab: hủy {} thất bại — cần hủy tay", deliveryId, e);
        }
    }

    private ShipmentOrder require(String code) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(code)
            .orElseThrow(() -> new BadRequestAlertException("Không thấy đơn", ENTITY, "orderNotFound"));
    }

    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase();
        if ("CANCELED".equals(s)) {
            return "CANCELLED";
        }
        return s;
    }

    static boolean pickedUp(String status) {
        return (
            "PENDING_DROP_OFF".equals(status) ||
            "IN_DELIVERY".equals(status) ||
            "IN_RETURN".equals(status) ||
            "RETURNED".equals(status) ||
            "COMPLETED".equals(status) ||
            "FAILED".equals(status)
        );
    }

    private static int tipUnits(AhamoveDispatchService.DispatchRequest req) {
        if (req == null || req.getTipUnits() == null || req.getTipUnits() <= 0) {
            return 0;
        }
        return Math.min(req.getTipUnits(), AhamoveCargo.TIP_MAX);
    }

    private static String note(String remarks, BigDecimal advance, int tip) {
        StringBuilder sb = new StringBuilder();
        if (!blank(remarks)) {
            sb.append(remarks.trim());
        }
        if (advance != null && advance.signum() > 0) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append("Ứng cước cho quầy ").append(PartnerAdvance.money(advance)).append("đ rồi thu người nhận");
        }
        if (tip > 0 && (advance == null || advance.signum() == 0)) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append("Tip sau giao ").append(tip * AhamoveCargo.TIP_UNIT_VND).append("đ");
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static String merchantId(String orderCode) {
        String id = orderCode + "-" + (System.currentTimeMillis() % 1_000_000);
        return id.length() > 64 ? id.substring(0, 64) : id;
    }

    private static String driverNote(JsonNode driver) {
        if (driver == null || !driver.isObject()) {
            return "";
        }
        String name = text(driver, "name");
        String phone = text(driver, "phone");
        if (name == null && phone == null) {
            return "";
        }
        return " · tài xế " + (name == null ? "" : name) + (phone == null ? "" : " " + phone);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        String v = node.get(field).asText();
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Có GPS thì dùng GPS. Chỉ có địa chỉ thì định vị rồi gửi Grab, cùng lối Ahamove nhận địa chỉ. */
    private double[] coords(Double lat, Double lng, String address) {
        if (lat != null && lng != null) {
            return new double[] { lat, lng };
        }
        String addr = address == null ? "" : address.trim();
        if (addr.length() < 8) {
            throw new BadRequestAlertException("Nhập địa chỉ hoặc ghim GPS để Grab báo giá", ENTITY, "grabPin");
        }
        double[] goong = goongPoint(addr);
        if (goong != null) {
            return goong;
        }
        double[] found = osmNominatimService.locate(addr);
        if (found != null) {
            return found;
        }
        throw new BadRequestAlertException(
            "Không định vị được địa chỉ để Grab báo giá. Ghim bản đồ hoặc dán link Google Maps.",
            ENTITY,
            "grabGeocode"
        );
    }

    private double[] goongPoint(String address) {
        if (!goongPlacesService.hasApiKey()) {
            return null;
        }
        try {
            java.util.Map<String, Object> hit = goongPlacesService.geocode(address);
            Object lat = hit.get("lat");
            Object lng = hit.get("lng");
            if (lat instanceof BigDecimal la && lng instanceof BigDecimal ln) {
                return new double[] { la.doubleValue(), ln.doubleValue() };
            }
        } catch (RuntimeException e) {
            LOG.warn("Goong không định vị được cho Grab: {}", e.getMessage());
        }
        return null;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean notBlank(String s) {
        return !blank(s);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static String actor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }
}
