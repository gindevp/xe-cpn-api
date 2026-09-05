package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderLeg;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.LegStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.ServiceType;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.CreateDraftOrderRequest;
import com.mycompany.myapp.service.dto.order.CreateDraftOrderResponse;
import com.mycompany.myapp.service.dto.order.CreateOrderRequest;
import com.mycompany.myapp.service.dto.order.MarkCodExportedRequest;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.OrderSummaryDTO;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.order.OrderTransitionResponse;
import com.mycompany.myapp.service.dto.order.PatchOrderRequest;
import com.mycompany.myapp.service.dto.order.TrackOrderRequest;
import com.mycompany.myapp.service.dto.order.TrackOrderResponse;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderFacadeService {

    private static final String ENTITY = "order";
    private static final Duration DRAFT_TTL = Duration.ofHours(24);

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderPodPhotoRepository orderPodPhotoRepository;
    private final OfficeRepository officeRepository;
    private final OrderCodeGenerator orderCodeGenerator;
    private final SimpleFareCalculator fareCalculator;
    private final StaffAccessService staffAccessService;
    private final OrderLegRepository orderLegRepository;
    private final DayClosureGuard dayClosureGuard;
    private final OrderIssueRepository orderIssueRepository;
    private final DraftExpiryService draftExpiryService;

    public OrderFacadeService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderEventRepository orderEventRepository,
        OrderPodPhotoRepository orderPodPhotoRepository,
        OfficeRepository officeRepository,
        OrderCodeGenerator orderCodeGenerator,
        SimpleFareCalculator fareCalculator,
        StaffAccessService staffAccessService,
        OrderLegRepository orderLegRepository,
        DayClosureGuard dayClosureGuard,
        OrderIssueRepository orderIssueRepository,
        DraftExpiryService draftExpiryService
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderEventRepository = orderEventRepository;
        this.orderPodPhotoRepository = orderPodPhotoRepository;
        this.officeRepository = officeRepository;
        this.orderCodeGenerator = orderCodeGenerator;
        this.fareCalculator = fareCalculator;
        this.staffAccessService = staffAccessService;
        this.orderLegRepository = orderLegRepository;
        this.dayClosureGuard = dayClosureGuard;
        this.orderIssueRepository = orderIssueRepository;
        this.draftExpiryService = draftExpiryService;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> list(OrderStatus status, String fromOfficeCode, String toOfficeCode, String keyword, Pageable pageable) {
        return list(status, fromOfficeCode, toOfficeCode, null, keyword, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> list(
        OrderStatus status,
        String fromOfficeCode,
        String toOfficeCode,
        String receiverOfficeCode,
        String keyword,
        Pageable pageable
    ) {
        return list(status, fromOfficeCode, toOfficeCode, receiverOfficeCode, keyword, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> list(
        OrderStatus status,
        String fromOfficeCode,
        String toOfficeCode,
        String receiverOfficeCode,
        String keyword,
        PaymentTerm paymentTerm,
        String createdFrom,
        String createdTo,
        String routeLabel,
        String itineraryLabel,
        Pageable pageable
    ) {
        Specification<ShipmentOrder> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (paymentTerm != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("paymentTerm"), paymentTerm));
        }
        if (fromOfficeCode != null && !fromOfficeCode.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("fromOffice").get("code"), fromOfficeCode.trim().toUpperCase()));
        }
        if (toOfficeCode != null && !toOfficeCode.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("toOffice").get("code"), toOfficeCode.trim().toUpperCase()));
        }
        if (receiverOfficeCode != null && !receiverOfficeCode.isBlank()) {
            String receiver = receiverOfficeCode.trim().toUpperCase();
            spec = spec.and((root, q, cb) -> cb.equal(receiverOfficePath(root, cb), receiver));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("orderCode")), like),
                    cb.like(cb.lower(root.get("draftCode")), like),
                    cb.like(cb.lower(root.get("senderPhone")), like),
                    cb.like(cb.lower(root.get("receiverPhone")), like),
                    cb.like(cb.lower(root.get("receiverName")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("senderName"), "")), like)
                )
            );
        }
        Instant rangeStart = parseDayStart(createdFrom);
        Instant rangeEndExclusive = parseDayEndExclusive(createdTo);
        if (rangeStart != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), rangeStart));
        }
        if (rangeEndExclusive != null) {
            spec = spec.and((root, q, cb) -> cb.lessThan(root.get("createdAt"), rangeEndExclusive));
        }
        if (routeLabel != null && !routeLabel.isBlank()) {
            String route = routeLabel.trim();
            spec = spec.and((root, q, cb) -> cb.equal(root.get("routeLabel"), route));
        }
        if (itineraryLabel != null && !itineraryLabel.isBlank()) {
            String it = itineraryLabel.trim();
            spec = spec.and((root, q, cb) -> cb.equal(root.get("itineraryLabel"), it));
        }
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        if (scoped != null) {
            spec = spec.and((root, q, cb) ->
                cb.or(
                    cb.equal(root.get("fromOffice").get("code"), scoped),
                    cb.equal(root.get("toOffice").get("code"), scoped),
                    cb.equal(receiverOfficePath(root, cb), scoped)
                )
            );
        }
        return shipmentOrderRepository.findAll(spec, pageable).map(this::toSummary);
    }

    public int markCodExported(MarkCodExportedRequest req) {
        if (req == null || req.getOrderCodes() == null || req.getOrderCodes().isEmpty()) {
            throw new BadRequestAlertException("orderCodes required", ENTITY, "orderCodesRequired");
        }
        Instant now = Instant.now();
        int updated = 0;
        for (String code : req.getOrderCodes()) {
            if (code == null || code.isBlank()) {
                continue;
            }
            ShipmentOrder order = shipmentOrderRepository.findOneByOrderCodeOrDraftCode(code.trim()).orElse(null);
            if (order == null) {
                continue;
            }
            if (order.getPaymentTerm() != PaymentTerm.COD) {
                continue;
            }
            if (order.getStatus() != OrderStatus.DELIVERED) {
                continue;
            }
            order.setCodExportedAt(now);
            shipmentOrderRepository.save(order);
            updated++;
        }
        return updated;
    }

    private static Instant parseDayStart(String day) {
        if (day == null || day.isBlank()) {
            return null;
        }
        LocalDate d = LocalDate.parse(day.trim());
        return d.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
    }

    private static Instant parseDayEndExclusive(String day) {
        if (day == null || day.isBlank()) {
            return null;
        }
        LocalDate d = LocalDate.parse(day.trim());
        return d.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
    }

    /** VP nhận thật: finalToOffice khi có (đơn qua hub), ngược lại toOffice. */
    private static jakarta.persistence.criteria.Expression<String> receiverOfficePath(
        jakarta.persistence.criteria.Root<ShipmentOrder> root,
        jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        var finalTo = root.join("finalToOffice", jakarta.persistence.criteria.JoinType.LEFT);
        var to = root.join("toOffice", jakarta.persistence.criteria.JoinType.LEFT);
        return cb.coalesce(finalTo.get("code"), to.get("code"));
    }

    @Transactional(readOnly = true)
    public OrderDetailDTO getByCode(String code) {
        ShipmentOrder order = requireByCode(code);
        return toDetail(order);
    }

    public CreateDraftOrderResponse createDraft(CreateDraftOrderRequest req) {
        boolean homeDelivery = Boolean.TRUE.equals(req.getHomeDelivery());
        boolean homePickup = Boolean.TRUE.equals(req.getHomePickup());
        if (homeDelivery && (isBlank(req.getDeliveryAddress()) || isBlank(req.getHubOfficeCode()))) {
            throw new BadRequestAlertException("Home delivery requires address and hubOfficeCode", ENTITY, "homedelivery");
        }
        if (!homeDelivery && isBlank(req.getToOfficeCode())) {
            throw new BadRequestAlertException("toOfficeCode is required when not home delivery", ENTITY, "toofficerequired");
        }
        if (isBlank(req.getFromOfficeCode())) {
            throw new BadRequestAlertException("fromOfficeCode is required", ENTITY, "fromofficerequired");
        }

        String fromCode = req.getFromOfficeCode().trim().toUpperCase();
        Office from = requireOffice(fromCode);
        Office to;
        Office hub = null;
        if (homeDelivery) {
            hub = requireOffice(req.getHubOfficeCode());
            to = from;
        } else {
            to = requireOffice(req.getToOfficeCode());
        }

        Office fareTo = hub != null ? hub : to;
        SimpleFareCalculator.FareBreakdown fare = fareCalculator.estimate(
            req.getEstimatedWeightKg(),
            homePickup,
            homeDelivery,
            from,
            fareTo,
            req.getBranchCode()
        );
        String draftCode = orderCodeGenerator.nextDraftCode(fromCode);

        ShipmentOrder order = newBlankOrder();
        order.setOrderCode(draftCode);
        order.setDraftCode(draftCode);
        order.setStatus(OrderStatus.DRAFT);
        order.setPaymentTerm(req.getPaymentTerm());
        order.setGoodsType(req.getGoodsType());
        order.setServiceType(resolveServiceType(homePickup, homeDelivery));
        order.setSenderName(req.getSenderName());
        order.setSenderPhone(req.getSenderPhone().trim());
        order.setReceiverName(req.getReceiverName().trim());
        order.setReceiverPhone(req.getReceiverPhone().trim());
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setPickupAddress(req.getPickupAddress());
        order.setHomePickup(homePickup);
        order.setHomeDelivery(homeDelivery);
        order.setWeightKg(req.getEstimatedWeightKg());
        order.setQuantity(1);
        order.setFareAmount(fare.total());
        order.setPickupFeeAmount(fare.pickupFee());
        order.setDeliveryFeeAmount(fare.deliveryFee());
        order.setNote(req.getNote());
        order.setFromOffice(from);
        order.setToOffice(to);
        order.setHubOffice(hub);
        order.setFinalToOffice(hub != null ? hub : to);
        order.setPublicTrackingAllowed(true);

        order = shipmentOrderRepository.save(order);
        appendEvent(order, "DRAFT_CREATE", "Public draft", "customer");

        Instant expiresAt = Instant.now().plus(DRAFT_TTL);
        return new CreateDraftOrderResponse(draftCode, order.getOrderCode(), OrderStatus.DRAFT, fare.total(), expiresAt);
    }

    public OrderSummaryDTO createConfirmed(CreateOrderRequest req) {
        if (!isBlank(req.getDraftCode())) {
            return confirmDraft(req);
        }
        return createNewConfirmed(req);
    }

    public OrderTransitionResponse transition(String code, OrderTransitionRequest req) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        OrderStatus from = order.getStatus();
        OrderStatus to = req.getToStatus();
        if (!OrderStatusTransitions.canTransition(from, to)) {
            throw new BadRequestAlertException("Invalid transition " + from + " -> " + to, ENTITY, "invalidtransition");
        }
        if (to == OrderStatus.CANCELLED && !isBlank(req.getDetail())) {
            order.setCancelReason(req.getDetail());
        }
        if (from == OrderStatus.DRAFT && to == OrderStatus.CONFIRMED) {
            if (order.getFromOffice() == null) {
                throw new BadRequestAlertException("fromOfficeCode is required", ENTITY, "fromofficerequired");
            }
            String office = order.getFromOffice().getCode();
            if (order.getOrderCode() != null && order.getOrderCode().startsWith("N-")) {
                order.setOrderCode(orderCodeGenerator.nextOrderCode(office));
            }
        }
        order.setStatus(to);
        // Leave warehouse pipeline: stale DEST_WH_IN/DELIVERING must not keep listing the order in Nhập kho giao.
        if (to == OrderStatus.DELIVERED || to == OrderStatus.CANCELLED || to == OrderStatus.RETURNED || to == OrderStatus.RETURNING) {
            order.setForwardStage(null);
        }
        shipmentOrderRepository.save(order);
        String action = isBlank(req.getAction()) ? "TRANSITION_" + to.name() : req.getAction();
        appendEvent(order, action, req.getDetail(), currentActor());
        return new OrderTransitionResponse(true, to, order.getOrderCode());
    }

    public OrderTransitionResponse restore(String code) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new BadRequestAlertException("Only CANCELLED orders can be restored", ENTITY, "restoreinvalid");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCancelReason(null);
        shipmentOrderRepository.save(order);
        appendEvent(order, "RESTORE", "Restored to CONFIRMED", currentActor());
        return new OrderTransitionResponse(true, OrderStatus.CONFIRMED, order.getOrderCode());
    }

    @Transactional(readOnly = true)
    public TrackOrderResponse track(TrackOrderRequest req) {
        TrackOrderResponse res = new TrackOrderResponse();
        String code = req.getCode().trim();
        String phone = req.getPhone().trim();
        ShipmentOrder order = shipmentOrderRepository.findOneByOrderCodeOrDraftCode(code).orElse(null);
        if (order == null) {
            res.setFound(false);
            return res;
        }
        boolean phoneOk = phone.equals(order.getSenderPhone()) || phone.equals(order.getReceiverPhone());
        if (!phoneOk || !Boolean.TRUE.equals(order.getPublicTrackingAllowed())) {
            res.setFound(false);
            return res;
        }
        res.setFound(true);
        res.setOrderCode(order.getOrderCode());
        res.setDraftCode(order.getDraftCode());
        res.setStatus(order.getStatus());
        res.setFromOfficeCode(officeCode(order.getFromOffice()));
        res.setToOfficeCode(officeCode(order.getToOffice()));
        res.setReceiverName(order.getReceiverName());
        res.setEvents(mapEvents(order.getId()));
        return res;
    }

    private OrderSummaryDTO confirmDraft(CreateOrderRequest req) {
        ShipmentOrder order = shipmentOrderRepository
            .findOneByDraftCode(req.getDraftCode().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BadRequestAlertException("Order is not DRAFT", ENTITY, "notdraft");
        }
        if (draftExpiryService.isDraftExpired(order)) {
            draftExpiryService.cancelExpiredDraft(order);
            throw new BadRequestAlertException("Draft expired (>24h)", ENTITY, "draftExpired");
        }
        applyCreateFields(order, req);
        String fromCode = order.getFromOffice().getCode();
        order.setOrderCode(orderCodeGenerator.nextOrderCode(fromCode));
        order.setStatus(OrderStatus.CONFIRMED);
        order = shipmentOrderRepository.save(order);
        ensureLegs(order);
        appendEvent(order, "CONFIRM", "Confirmed from draft", currentActor());
        return toSummary(order);
    }

    private OrderSummaryDTO createNewConfirmed(CreateOrderRequest req) {
        boolean homeDelivery = Boolean.TRUE.equals(req.getHomeDelivery());
        boolean homePickup = Boolean.TRUE.equals(req.getHomePickup());
        Office from = requireOffice(req.getFromOfficeCode());
        Office to = requireOffice(req.getToOfficeCode());
        Office hub = isBlank(req.getHubOfficeCode()) ? null : requireOffice(req.getHubOfficeCode());

        SimpleFareCalculator.FareBreakdown fare = fareCalculator.estimate(
            req.getWeightKg(),
            homePickup,
            homeDelivery,
            from,
            to,
            req.getBranchCode()
        );
        BigDecimal total;
        if (fare.pricingRuleId() != null) {
            total = fare.total();
        } else if (req.getFareAmount() != null) {
            total = req.getFareAmount();
        } else {
            total = fare.total();
        }

        ShipmentOrder order = newBlankOrder();
        order.setOrderCode(orderCodeGenerator.nextOrderCode(from.getCode()));
        order.setStatus(OrderStatus.CONFIRMED);
        applyCreateFields(order, req);
        order.setFromOffice(from);
        order.setToOffice(to);
        order.setHubOffice(hub);
        Office dest = !isBlank(req.getFinalToOfficeCode()) ? requireOffice(req.getFinalToOfficeCode()) : to;
        order.setFinalToOffice(dest);
        order.setServiceType(resolveServiceType(homePickup, homeDelivery));
        order.setFareAmount(total);
        order.setPickupFeeAmount(fare.pickupFee());
        order.setDeliveryFeeAmount(fare.deliveryFee());
        order.setPublicTrackingAllowed(true);

        order = shipmentOrderRepository.save(order);
        ensureLegs(order);
        appendEvent(order, "CREATE", "Internal create", currentActor());
        return toSummary(order);
    }

    public OrderDetailDTO patch(String code, PatchOrderRequest req) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        if (req == null) {
            return getByCode(order.getOrderCode());
        }
        if (req.getSenderName() != null) {
            order.setSenderName(req.getSenderName());
        }
        if (req.getSenderPhone() != null) {
            order.setSenderPhone(req.getSenderPhone());
        }
        if (req.getReceiverName() != null) {
            order.setReceiverName(req.getReceiverName());
        }
        if (req.getReceiverPhone() != null) {
            order.setReceiverPhone(req.getReceiverPhone());
        }
        if (req.getNote() != null) {
            order.setNote(req.getNote());
        }
        if (req.getPickupAddress() != null) {
            order.setPickupAddress(req.getPickupAddress());
        }
        if (req.getDeliveryAddress() != null) {
            order.setDeliveryAddress(req.getDeliveryAddress());
        }
        if (req.getWeightKg() != null) {
            order.setWeightKg(req.getWeightKg());
        }
        if (req.getQuantity() != null) {
            order.setQuantity(req.getQuantity());
        }
        if (req.getFareAmount() != null) {
            assertFareNotBelowPaid(req.getFareAmount(), order.getPaidAmount());
            order.setFareAmount(req.getFareAmount());
        }
        boolean doorChanged = false;
        if (req.getHomePickup() != null) {
            order.setHomePickup(req.getHomePickup());
            doorChanged = true;
        }
        if (req.getHomeDelivery() != null) {
            order.setHomeDelivery(req.getHomeDelivery());
            doorChanged = true;
        }
        if (doorChanged) {
            boolean hp = Boolean.TRUE.equals(order.getHomePickup());
            boolean hd = Boolean.TRUE.equals(order.getHomeDelivery());
            order.setServiceType(resolveServiceType(hp, hd));
            SimpleFareCalculator.FareBreakdown fees = fareCalculator.estimate(
                order.getWeightKg(),
                hp,
                hd,
                order.getFromOffice(),
                order.getToOffice()
            );
            order.setPickupFeeAmount(fees.pickupFee());
            order.setDeliveryFeeAmount(fees.deliveryFee());
            // Recalc total only when client did not send explicit fareAmount in this PATCH
            if (req.getFareAmount() == null) {
                assertFareNotBelowPaid(fees.total(), order.getPaidAmount());
                order.setFareAmount(fees.total());
            }
            // Legs: do not rebuild/wipe existing OrderLeg rows on door-flag PATCH (LEG regression safe)
        }
        if (req.getPickingAt() != null) {
            order.setPickingAt(parseInstant(req.getPickingAt()));
        }
        if (req.getPickedUpAt() != null) {
            order.setPickedUpAt(parseInstant(req.getPickedUpAt()));
        }
        if (req.getPickupStaffUsername() != null) {
            order.setPickupStaffUsername(req.getPickupStaffUsername());
        }
        if (req.getPartnerCode() != null) {
            order.setPartnerCode(req.getPartnerCode());
        }
        if (req.getPartnerFeeAmount() != null) {
            order.setPartnerFeeAmount(req.getPartnerFeeAmount());
        }
        if (req.getFromOfficeCode() != null) {
            order.setFromOffice(requireOffice(req.getFromOfficeCode()));
        }
        if (req.getToOfficeCode() != null) {
            order.setToOffice(requireOffice(req.getToOfficeCode()));
        }
        if (req.getHubOfficeCode() != null) {
            order.setHubOffice(requireOffice(req.getHubOfficeCode()));
        }
        if (req.getFinalToOfficeCode() != null) {
            order.setFinalToOffice(requireOffice(req.getFinalToOfficeCode()));
        }
        if (req.getCodAmount() != null) {
            order.setCodAmount(req.getCodAmount());
        }
        if (req.getCodFeeAmount() != null) {
            order.setCodFeeAmount(req.getCodFeeAmount());
        }
        if (req.getBankName() != null) {
            order.setBankName(blankToNull(req.getBankName()));
        }
        if (req.getBankAccountNo() != null) {
            order.setBankAccountNo(blankToNull(req.getBankAccountNo()));
        }
        if (req.getBankAccountName() != null) {
            order.setBankAccountName(blankToNull(req.getBankAccountName()));
        }
        if (req.getRouteLabel() != null) {
            order.setRouteLabel(blankToNull(req.getRouteLabel()));
        }
        if (req.getItineraryLabel() != null) {
            order.setItineraryLabel(blankToNull(req.getItineraryLabel()));
        }
        shipmentOrderRepository.save(order);
        appendEvent(order, "PATCH", "Order fields updated", currentActor());
        return getByCode(order.getOrderCode());
    }

    public OrderDetailDTO pickupStart(String code) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        if (order.getPickingAt() == null) {
            order.setPickingAt(Instant.now());
        }
        if (order.getPickupStaffUsername() == null) {
            order.setPickupStaffUsername(currentActor());
        }
        shipmentOrderRepository.save(order);
        appendEvent(order, "PICKUP_START", "Pickup started", currentActor());
        return getByCode(order.getOrderCode());
    }

    public OrderDetailDTO warehouseReceive(String code) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        order.setPickedUpAt(Instant.now());
        shipmentOrderRepository.save(order);
        appendEvent(order, "WAREHOUSE_RECEIVE", "Received at warehouse", currentActor());
        return getByCode(order.getOrderCode());
    }

    public OrderDetailDTO advanceLeg(String code) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        java.util.List<OrderLeg> legs = ensureLegs(order);
        if (legs.isEmpty()) {
            throw new BadRequestAlertException("Order has no legs to advance", ENTITY, "noLegs");
        }
        OrderLeg current = legs
            .stream()
            .filter(l -> l.getStatus() == LegStatus.PENDING || l.getStatus() == LegStatus.IN_TRANSIT)
            .findFirst()
            .orElse(null);
        if (current == null) {
            throw new BadRequestAlertException("No pending leg", ENTITY, "noPendingLeg");
        }
        Instant now = Instant.now();
        boolean last = current.getLegIndex() != null && current.getLegIndex() >= legs.size() - 1;
        current.setArrivedAt(now);
        current.setStatus(last ? LegStatus.AT_DEST : LegStatus.AT_HUB);
        orderLegRepository.save(current);
        if (last) {
            order.setStatus(OrderStatus.AT_DEST);
            appendEvent(order, "LEG_ARRIVE_DEST", "Last leg arrived", currentActor());
        } else {
            OrderLeg next = legs.get(current.getLegIndex() + 1);
            order.setFromOffice(next.getFromOffice());
            order.setToOffice(next.getToOffice());
            order.setCurrentTrip(null);
            order.setStatus(OrderStatus.CONFIRMED);
            appendEvent(order, "LEG_ADVANCE", "Advanced to next leg", currentActor());
        }
        shipmentOrderRepository.save(order);
        return getByCode(order.getOrderCode());
    }

    private void applyCreateFields(ShipmentOrder order, CreateOrderRequest req) {
        boolean homeDelivery = Boolean.TRUE.equals(req.getHomeDelivery());
        boolean homePickup = Boolean.TRUE.equals(req.getHomePickup());
        order.setPaymentTerm(req.getPaymentTerm());
        order.setGoodsType(req.getGoodsType());
        order.setSenderName(req.getSenderName());
        order.setSenderPhone(req.getSenderPhone().trim());
        order.setReceiverName(req.getReceiverName().trim());
        order.setReceiverPhone(req.getReceiverPhone().trim());
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setPickupAddress(req.getPickupAddress());
        order.setHomePickup(homePickup);
        order.setHomeDelivery(homeDelivery);
        order.setQrDropOff(Boolean.TRUE.equals(req.getQrDropOff()));
        order.setWeightKg(req.getWeightKg());
        order.setQuantity(req.getQuantity() == null ? 1 : req.getQuantity());
        order.setNote(req.getNote());
        if (!isBlank(req.getFromOfficeCode())) {
            order.setFromOffice(requireOffice(req.getFromOfficeCode()));
        }
        if (!isBlank(req.getToOfficeCode())) {
            order.setToOffice(requireOffice(req.getToOfficeCode()));
        }
        if (!isBlank(req.getHubOfficeCode())) {
            order.setHubOffice(requireOffice(req.getHubOfficeCode()));
        }
        if (!isBlank(req.getFinalToOfficeCode())) {
            order.setFinalToOffice(requireOffice(req.getFinalToOfficeCode()));
        }
        if (req.getFareAmount() != null) {
            order.setFareAmount(req.getFareAmount());
        }
        if (req.getCodAmount() != null) {
            order.setCodAmount(req.getCodAmount());
        }
        if (req.getCodFeeAmount() != null) {
            order.setCodFeeAmount(req.getCodFeeAmount());
        }
        if (req.getBankName() != null) {
            order.setBankName(blankToNull(req.getBankName()));
        }
        if (req.getBankAccountNo() != null) {
            order.setBankAccountNo(blankToNull(req.getBankAccountNo()));
        }
        if (req.getBankAccountName() != null) {
            order.setBankAccountName(blankToNull(req.getBankAccountName()));
        }
        if (req.getRouteLabel() != null) {
            order.setRouteLabel(blankToNull(req.getRouteLabel()));
        }
        if (req.getItineraryLabel() != null) {
            order.setItineraryLabel(blankToNull(req.getItineraryLabel()));
        }
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private ShipmentOrder newBlankOrder() {
        ShipmentOrder order = new ShipmentOrder();
        order.setQrDropOff(false);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setFailCount(0);
        order.setLabelReprintCount(0);
        order.setPublicTrackingAllowed(true);
        return order;
    }

    private void appendEvent(ShipmentOrder order, String action, String detail, String actor) {
        OrderEvent event = new OrderEvent();
        event.setEventAt(Instant.now());
        event.setAction(action);
        event.setDetail(detail);
        event.setActorUsername(actor == null ? "system" : actor);
        event.setOrder(order);
        orderEventRepository.save(event);
    }

    private ShipmentOrder requireByCode(String code) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(code.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + code));
    }

    private Office requireOffice(String code) {
        return officeRepository
            .findOneByCode(code.trim().toUpperCase())
            .orElseThrow(() -> new BadRequestAlertException("Unknown office: " + code, ENTITY, "officeNotFound"));
    }

    private OrderSummaryDTO toSummary(ShipmentOrder o) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        fillSummary(dto, o);
        return dto;
    }

    private OrderDetailDTO toDetail(ShipmentOrder o) {
        OrderDetailDTO dto = new OrderDetailDTO();
        fillSummary(dto, o);
        dto.setCancelReason(o.getCancelReason());
        dto.setReceiverActualName(o.getReceiverActualName());
        dto.setReceiverActualPhone(o.getReceiverActualPhone());
        dto.setFailCount(o.getFailCount());
        dto.setEvents(mapEvents(o.getId()));
        dto.setPodPhotos(orderPodPhotoRepository.findByOrder_IdOrderBySequenceNoAsc(o.getId()).stream().map(p -> p.getPhotoUrl()).toList());
        dto.setIssues(
            orderIssueRepository.findByOrder_IdOrderByOpenedAtAscIdAsc(o.getId()).stream().map(ExceptionFacadeService::toIssueView).toList()
        );
        dto.setCurrentIssueId(o.getIssue() != null ? o.getIssue().getId() : null);
        return dto;
    }

    private void fillSummary(OrderSummaryDTO dto, ShipmentOrder o) {
        dto.setId(o.getId());
        dto.setOrderCode(o.getOrderCode());
        dto.setDraftCode(o.getDraftCode());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());
        dto.setStatus(o.getStatus());
        dto.setForwardStage(o.getForwardStage());
        dto.setReturnStage(o.getReturnStage());
        dto.setSenderName(o.getSenderName());
        dto.setSenderPhone(o.getSenderPhone());
        dto.setReceiverName(o.getReceiverName());
        dto.setReceiverPhone(o.getReceiverPhone());
        dto.setFromOfficeCode(officeCode(o.getFromOffice()));
        dto.setToOfficeCode(officeCode(o.getToOffice()));
        dto.setHubOfficeCode(officeCode(o.getHubOffice()));
        dto.setFinalToOfficeCode(officeCode(o.getFinalToOffice()));
        dto.setGoodsType(o.getGoodsType());
        dto.setPaymentTerm(o.getPaymentTerm());
        dto.setWeightKg(o.getWeightKg());
        dto.setQuantity(o.getQuantity());
        dto.setFareAmount(o.getFareAmount());
        dto.setPaidAmount(o.getPaidAmount());
        dto.setDueAmount(OrderMoney.due(o));
        dto.setPickupFeeAmount(o.getPickupFeeAmount());
        dto.setDeliveryFeeAmount(o.getDeliveryFeeAmount());
        dto.setHomePickup(o.getHomePickup());
        dto.setHomeDelivery(o.getHomeDelivery());
        dto.setQrDropOff(o.getQrDropOff());
        dto.setCurrentTripCode(o.getCurrentTrip() != null ? o.getCurrentTrip().getTripCode() : null);
        dto.setShelfNumber(o.getShelfNumber());
        dto.setNote(o.getNote());
        dto.setPickingAt(o.getPickingAt());
        dto.setPickedUpAt(o.getPickedUpAt());
        dto.setPickupStaffUsername(o.getPickupStaffUsername());
        dto.setPartnerCode(o.getPartnerCode());
        dto.setPartnerFeeAmount(o.getPartnerFeeAmount());
        dto.setCodAmount(o.getCodAmount());
        dto.setCodFeeAmount(o.getCodFeeAmount());
        dto.setBankName(o.getBankName());
        dto.setBankAccountNo(o.getBankAccountNo());
        dto.setBankAccountName(o.getBankAccountName());
        dto.setRouteLabel(o.getRouteLabel());
        dto.setItineraryLabel(o.getItineraryLabel());
        dto.setCodExportedAt(o.getCodExportedAt());
        if (o.getCurrentTrip() != null) {
            if (o.getCurrentTrip().getVehicle() != null) {
                dto.setVehiclePlate(o.getCurrentTrip().getVehicle().getPlateNumber());
            }
            if (o.getCurrentTrip().getDriver() != null) {
                dto.setDriverName(o.getCurrentTrip().getDriver().getFullName());
            }
        }
        java.util.List<OrderLeg> legs = o.getId() == null
            ? java.util.List.of()
            : orderLegRepository.findByOrder_IdOrderByLegIndexAsc(o.getId());
        dto.setLegs(legs.stream().map(this::toLegView).toList());
        dto.setCurrentLegIndex(currentLegIndex(legs));
        if ((dto.getVehiclePlate() == null || dto.getDriverName() == null) && !legs.isEmpty()) {
            for (int i = legs.size() - 1; i >= 0; i--) {
                OrderLeg leg = legs.get(i);
                if (leg.getTrip() == null) {
                    continue;
                }
                if (dto.getVehiclePlate() == null && leg.getTrip().getVehicle() != null) {
                    dto.setVehiclePlate(leg.getTrip().getVehicle().getPlateNumber());
                }
                if (dto.getDriverName() == null && leg.getTrip().getDriver() != null) {
                    dto.setDriverName(leg.getTrip().getDriver().getFullName());
                }
                if (dto.getVehiclePlate() != null && dto.getDriverName() != null) {
                    break;
                }
            }
        }
        if (o.getStatus() == OrderStatus.DELIVERED && o.getId() != null) {
            dto.setPodPhotos(
                orderPodPhotoRepository.findByOrder_IdOrderBySequenceNoAsc(o.getId()).stream().map(p -> p.getPhotoUrl()).toList()
            );
            dto.setReceiverActualName(o.getReceiverActualName());
            dto.setReceiverActualPhone(o.getReceiverActualPhone());
        }
    }

    private OrderSummaryDTO.OrderLegViewDTO toLegView(OrderLeg leg) {
        OrderSummaryDTO.OrderLegViewDTO v = new OrderSummaryDTO.OrderLegViewDTO();
        v.setIndex(leg.getLegIndex());
        v.setFromOfficeCode(officeCode(leg.getFromOffice()));
        v.setToOfficeCode(officeCode(leg.getToOffice()));
        v.setTripCode(leg.getTrip() != null ? leg.getTrip().getTripCode() : null);
        v.setStatus(leg.getStatus());
        v.setDepartedAt(leg.getDepartedAt());
        v.setArrivedAt(leg.getArrivedAt());
        return v;
    }

    private static Integer currentLegIndex(java.util.List<OrderLeg> legs) {
        if (legs == null || legs.isEmpty()) {
            return null;
        }
        int current = 0;
        for (int i = 0; i < legs.size(); i++) {
            LegStatus status = legs.get(i).getStatus();
            current = i;
            if (status == LegStatus.PENDING || status == LegStatus.IN_TRANSIT) {
                break;
            }
        }
        return current;
    }

    private java.util.List<OrderLeg> ensureLegs(ShipmentOrder order) {
        java.util.List<OrderLeg> existing = orderLegRepository.findByOrder_IdOrderByLegIndexAsc(order.getId());
        if (!existing.isEmpty()) {
            return existing;
        }
        Office from = order.getFromOffice();
        Office to = order.getToOffice();
        Office hub = order.getHubOffice();
        Office dest = order.getFinalToOffice();
        if (from == null || to == null || hub == null || dest == null || sameOffice(hub, dest)) {
            return existing;
        }
        Office hopTo = sameOffice(to, hub) ? to : hub;
        OrderLeg first = new OrderLeg();
        first.setOrder(order);
        first.setLegIndex(0);
        first.setStatus(LegStatus.PENDING);
        first.setFromOffice(from);
        first.setToOffice(hopTo);
        OrderLeg second = new OrderLeg();
        second.setOrder(order);
        second.setLegIndex(1);
        second.setStatus(LegStatus.PENDING);
        second.setFromOffice(hopTo);
        second.setToOffice(dest);
        orderLegRepository.save(first);
        orderLegRepository.save(second);
        return orderLegRepository.findByOrder_IdOrderByLegIndexAsc(order.getId());
    }

    private static boolean sameOffice(Office a, Office b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (Exception ex) {
            throw new BadRequestAlertException("Invalid instant: " + raw, ENTITY, "invalidInstant");
        }
    }

    private List<OrderDetailDTO.OrderEventViewDTO> mapEvents(Long orderId) {
        return orderEventRepository
            .findByOrder_IdOrderByEventAtAsc(orderId)
            .stream()
            .map(e -> {
                OrderDetailDTO.OrderEventViewDTO v = new OrderDetailDTO.OrderEventViewDTO();
                v.setAt(e.getEventAt());
                v.setAction(e.getAction());
                v.setDetail(e.getDetail());
                v.setBy(e.getActorUsername());
                return v;
            })
            .toList();
    }

    /**
     * H1: PATCH must not set fare below already-collected paidAmount.
     * Equal is allowed; null paid is treated as zero.
     */
    static void assertFareNotBelowPaid(BigDecimal newFare, BigDecimal paidAmount) {
        BigDecimal paid = OrderMoney.nz(paidAmount);
        if (newFare.compareTo(paid) < 0) {
            throw new BadRequestAlertException(
                "fareAmount must be >= paidAmount (fare=" + newFare + ", paid=" + paid + ")",
                ENTITY,
                "fareBelowPaid"
            );
        }
    }

    private static ServiceType resolveServiceType(boolean homePickup, boolean homeDelivery) {
        if (homePickup && homeDelivery) {
            return ServiceType.HOME_TO_HOME;
        }
        if (homePickup) {
            return ServiceType.HOME_TO_COUNTER;
        }
        if (homeDelivery) {
            return ServiceType.COUNTER_TO_HOME;
        }
        return ServiceType.COUNTER_TO_COUNTER;
    }

    private static String officeCode(Office office) {
        return office == null ? null : office.getCode();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String currentActor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }
}
