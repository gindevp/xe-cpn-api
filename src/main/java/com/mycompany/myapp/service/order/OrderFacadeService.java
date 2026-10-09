package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderGoodsPhoto;
import com.mycompany.myapp.domain.OrderLeg;
import com.mycompany.myapp.domain.OrderPodPhoto;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.LegStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.ServiceType;
import com.mycompany.myapp.repository.ItineraryRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderGoodsPhotoRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.OfficeItineraryPoints;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.CreateDraftOrderRequest;
import com.mycompany.myapp.service.dto.order.CreateDraftOrderResponse;
import com.mycompany.myapp.service.dto.order.CreateOrderRequest;
import com.mycompany.myapp.service.dto.order.LogOrderEventRequest;
import com.mycompany.myapp.service.dto.order.MarkCodExportedRequest;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.OrderSummaryDTO;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.order.OrderTransitionResponse;
import com.mycompany.myapp.service.dto.order.PatchOrderRequest;
import com.mycompany.myapp.service.dto.order.TrackOrderRequest;
import com.mycompany.myapp.service.dto.order.TrackOrderResponse;
import com.mycompany.myapp.service.invoice.InvoicePolicy;
import com.mycompany.myapp.service.invoice.OrderDeliveredEvent;
import com.mycompany.myapp.service.invoice.VietnamTaxCode;
import com.mycompany.myapp.service.partner.AhamoveDispatchService;
import com.mycompany.myapp.service.storage.StoredMedia;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderPodPhotoRepository orderPodPhotoRepository;
    private final OfficeRepository officeRepository;
    private final OrderCodeGenerator orderCodeGenerator;
    private final SimpleFareCalculator fareCalculator;
    private com.mycompany.myapp.service.partner.DoorKmEstimator doorKmEstimator;
    private final StaffAccessService staffAccessService;
    private final OrderLegRepository orderLegRepository;
    private final DayClosureGuard dayClosureGuard;
    private final OrderIssueRepository orderIssueRepository;
    private final DraftExpiryService draftExpiryService;
    private final ApplicationEventPublisher eventPublisher;
    private ItineraryRepository itineraryRepository;
    private OrderGoodsPhotoRepository goodsPhotoRepository;

    static final int MAX_GOODS_PHOTO_LENGTH = 2_500_000;

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
        DraftExpiryService draftExpiryService,
        ApplicationEventPublisher eventPublisher
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
        this.eventPublisher = eventPublisher;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setDoorKmEstimator(com.mycompany.myapp.service.partner.DoorKmEstimator doorKmEstimator) {
        this.doorKmEstimator = doorKmEstimator;
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
        return list(
            status,
            fromOfficeCode,
            toOfficeCode,
            receiverOfficeCode,
            keyword,
            paymentTerm,
            createdFrom,
            createdTo,
            routeLabel,
            itineraryLabel,
            null,
            pageable
        );
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
        java.util.Collection<String> codes,
        Pageable pageable
    ) {
        return list(
            status,
            fromOfficeCode,
            toOfficeCode,
            receiverOfficeCode,
            keyword,
            paymentTerm,
            createdFrom,
            createdTo,
            routeLabel,
            itineraryLabel,
            codes,
            OrderListExtra.NONE,
            pageable
        );
    }

    /**
     * Bộ lọc thêm cho danh sách: {@code anyOfficeCode} = VP gửi / đến / nhận; {@code statuses} = một trong các trạng thái;
     * {@code openOrUpdatedWithinDays} = đơn chưa kết thúc hoặc cập nhật trong N ngày gần nhất (tập làm việc của màn vận hành);
     * {@code updatedFrom/updatedTo} = khoảng ngày cập nhật (yyyy-MM-dd, giờ VN);
     * {@code successOfficeCode} = VP thao tác thành công (DELIVERED → VP nhận, RETURNED → VP gửi);
     * {@code homeDelivery} = giao tận nơi;
     * {@code searchAllOffices} = ô tìm đơn: có từ khoá thì NV thấy đơn mọi VP (danh sách nghiệp vụ vẫn theo VP);
     * {@code cancelRequests} = đơn chờ duyệt huỷ: HIDE ẩn (trừ khi tìm theo từ khoá / mã), ONLY chỉ lấy các đơn đó.
     */
    public record OrderListExtra(
        String anyOfficeCode,
        java.util.Collection<OrderStatus> statuses,
        Integer openOrUpdatedWithinDays,
        String updatedFrom,
        String updatedTo,
        String successOfficeCode,
        Boolean homeDelivery,
        boolean searchAllOffices,
        CancelRequestMode cancelRequests,
        Boolean codOnly
    ) {
        public static final OrderListExtra NONE = new OrderListExtra(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            CancelRequestMode.INCLUDE,
            null
        );
    }

    public enum CancelRequestMode {
        HIDE,
        INCLUDE,
        ONLY,
    }

    private static final List<OrderStatus> TERMINAL_STATUSES = List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.RETURNED);

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
        java.util.Collection<String> codes,
        OrderListExtra extra,
        Pageable pageable
    ) {
        Specification<ShipmentOrder> spec = Specification.where(null);
        OrderListExtra ex = extra == null ? OrderListExtra.NONE : extra;
        if (ex.anyOfficeCode() != null && !ex.anyOfficeCode().isBlank()) {
            String any = ex.anyOfficeCode().trim().toUpperCase();
            spec = spec.and((root, q, cb) ->
                cb.or(
                    cb.equal(root.get("fromOffice").get("code"), any),
                    cb.equal(root.get("toOffice").get("code"), any),
                    cb.equal(receiverOfficePath(root, cb), any)
                )
            );
        }
        if (ex.statuses() != null && !ex.statuses().isEmpty()) {
            List<OrderStatus> wanted = ex.statuses().stream().filter(java.util.Objects::nonNull).distinct().toList();
            spec = spec.and((root, q, cb) -> root.get("status").in(wanted));
        }
        if (ex.openOrUpdatedWithinDays() != null && ex.openOrUpdatedWithinDays() >= 0) {
            Instant since = Instant.now().minus(java.time.Duration.ofDays(ex.openOrUpdatedWithinDays()));
            spec = spec.and((root, q, cb) ->
                cb.or(cb.not(root.get("status").in(TERMINAL_STATUSES)), cb.greaterThanOrEqualTo(root.get("updatedAt"), since))
            );
        }
        Instant updatedStart = parseDayStart(ex.updatedFrom());
        Instant updatedEndExclusive = parseDayEndExclusive(ex.updatedTo());
        if (updatedStart != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("updatedAt"), updatedStart));
        }
        if (updatedEndExclusive != null) {
            spec = spec.and((root, q, cb) -> cb.lessThan(root.get("updatedAt"), updatedEndExclusive));
        }
        if (ex.successOfficeCode() != null && !ex.successOfficeCode().isBlank()) {
            String so = ex.successOfficeCode().trim().toUpperCase();
            spec = spec.and((root, q, cb) ->
                cb.or(
                    cb.and(cb.equal(root.get("status"), OrderStatus.DELIVERED), cb.equal(receiverOfficePath(root, cb), so)),
                    cb.and(cb.equal(root.get("status"), OrderStatus.RETURNED), cb.equal(root.get("fromOffice").get("code"), so))
                )
            );
        }
        if (ex.homeDelivery() != null) {
            boolean home = ex.homeDelivery();
            spec = spec.and((root, q, cb) ->
                home
                    ? cb.isTrue(root.get("homeDelivery"))
                    : cb.or(cb.isNull(root.get("homeDelivery")), cb.isFalse(root.get("homeDelivery")))
            );
        }
        if (codes != null && !codes.isEmpty()) {
            List<String> wanted = codes.stream().filter(c -> c != null && !c.isBlank()).map(String::trim).distinct().limit(500).toList();
            if (wanted.isEmpty()) {
                return Page.empty(pageable);
            }
            spec = spec.and((root, q, cb) -> cb.or(root.get("orderCode").in(wanted), root.get("draftCode").in(wanted)));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (Boolean.TRUE.equals(ex.codOnly()) || paymentTerm == PaymentTerm.COD) {
            // Đơn có thu hộ: nhận theo tiền COD; PaymentTerm.COD chỉ còn ở đơn cũ.
            spec = spec.and((root, q, cb) ->
                cb.or(cb.equal(root.get("paymentTerm"), PaymentTerm.COD), cb.greaterThan(root.get("codAmount"), BigDecimal.ZERO))
            );
        }
        if (paymentTerm != null && paymentTerm != PaymentTerm.COD) {
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
            String phone = phoneSearchDigits(keyword);
            String phoneLike = phone != null ? "%" + phone + "%" : like;
            spec = spec.and((root, q, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("orderCode")), like),
                    cb.like(cb.lower(root.get("draftCode")), like),
                    cb.like(cb.lower(root.get("senderPhone")), phoneLike),
                    cb.like(cb.lower(root.get("receiverPhone")), phoneLike),
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
        CancelRequestMode cancelMode = ex.cancelRequests() == null ? CancelRequestMode.INCLUDE : ex.cancelRequests();
        boolean lookup = (keyword != null && !keyword.isBlank()) || (codes != null && !codes.isEmpty());
        if (cancelMode == CancelRequestMode.ONLY) {
            spec = spec.and((root, q, cb) -> pendingCancelRequest(root, q, cb));
        } else if (cancelMode == CancelRequestMode.HIDE && !lookup) {
            spec = spec.and((root, q, cb) -> cb.not(pendingCancelRequest(root, q, cb)));
        }
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        boolean crossOfficeSearch = ex.searchAllOffices() && keyword != null && !keyword.isBlank();
        if (scoped != null && !crossOfficeSearch) {
            spec = spec.and((root, q, cb) ->
                cb.or(
                    cb.equal(root.get("fromOffice").get("code"), scoped),
                    cb.equal(root.get("toOffice").get("code"), scoped),
                    cb.equal(receiverOfficePath(root, cb), scoped)
                )
            );
        }
        Page<ShipmentOrder> page = shipmentOrderRepository.findAll(spec, pageable);
        java.util.Map<Long, List<OrderLeg>> legsByOrder = legsByOrderId(page.getContent());
        List<Long> pageIds = page.getContent().stream().map(ShipmentOrder::getId).filter(java.util.Objects::nonNull).toList();
        java.util.Map<Long, java.util.Map<String, Instant>> stageTimes = stageEventTimes(pageIds);
        java.util.Set<Long> withGoodsPhoto = goodsPhotoRepository == null || pageIds.isEmpty()
            ? java.util.Set.of()
            : new java.util.HashSet<>(goodsPhotoRepository.findOrderIdsIn(pageIds));
        java.util.Map<String, String> codProcessorNames = staffNamesByLogin(
            page.getContent().stream().map(ShipmentOrder::getCodExportedBy).toList()
        );
        java.util.Map<Long, String> creators = creatorLogins(pageIds);
        java.util.Map<String, java.util.Optional<com.mycompany.myapp.domain.StaffProfile>> creatorProfiles = new java.util.HashMap<>();
        return page.map(o -> {
            OrderSummaryDTO dto = new OrderSummaryDTO();
            fillSummary(dto, o, legsByOrder.getOrDefault(o.getId(), List.of()));
            applyCreator(dto, creators.get(o.getId()), creatorProfiles);
            applyStageTimes(dto, stageTimes.getOrDefault(o.getId(), java.util.Map.of()));
            dto.setHasGoodsPhoto(withGoodsPhoto.contains(o.getId()));
            if (o.getCodExportedBy() != null) {
                dto.setCodExportedByName(codProcessorNames.get(o.getCodExportedBy().trim().toLowerCase()));
            }
            return dto;
        });
    }

    private static final List<String> WAREHOUSE_IN_ACTIONS = List.of("WAREHOUSE_RECEIVE", "WH_IN", "CONFIRM", "CREATE");
    private static final List<String> TRIP_ASSIGN_ACTIONS = List.of("ASSIGN_TRIP");
    private static final List<String> DRIVER_SIGN_ACTIONS = List.of("KY_BAN_GIAO_TAI_XE", "SCAN_OUT", "HANDOVER");
    private static final List<String> DEST_WAREHOUSE_IN_ACTIONS = List.of("SCAN_IN", "HUB_IN", "DEST_WH_IN");
    private static final List<String> SHIPPER_ASSIGN_ACTIONS = List.of("DELIVERING");
    private static final List<String> STAGE_TIME_ACTIONS = java.util.stream.Stream.of(
        WAREHOUSE_IN_ACTIONS,
        TRIP_ASSIGN_ACTIONS,
        DRIVER_SIGN_ACTIONS,
        DEST_WAREHOUSE_IN_ACTIONS,
        SHIPPER_ASSIGN_ACTIONS
    )
        .flatMap(List::stream)
        .toList();

    /** orderId → (ACTION → thời điểm mới nhất). */
    private java.util.Map<Long, java.util.Map<String, Instant>> stageEventTimes(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<Long, java.util.Map<String, Instant>> out = new java.util.HashMap<>();
        for (Object[] row : orderEventRepository.latestEventAtByOrderIdsAndAction(orderIds, STAGE_TIME_ACTIONS)) {
            if (row[0] == null || row[1] == null || row[2] == null) {
                continue;
            }
            out.computeIfAbsent((Long) row[0], k -> new java.util.HashMap<>()).put((String) row[1], (Instant) row[2]);
        }
        return out;
    }

    private static Instant latestOf(java.util.Map<String, Instant> times, List<String> actions) {
        Instant best = null;
        for (String a : actions) {
            Instant t = times.get(a);
            if (t != null && (best == null || t.isAfter(best))) {
                best = t;
            }
        }
        return best;
    }

    private static void applyStageTimes(OrderSummaryDTO dto, java.util.Map<String, Instant> times) {
        dto.setWarehouseInAt(latestOf(times, WAREHOUSE_IN_ACTIONS));
        dto.setTripAssignedAt(latestOf(times, TRIP_ASSIGN_ACTIONS));
        dto.setDriverSignedAt(latestOf(times, DRIVER_SIGN_ACTIONS));
        dto.setDestWarehouseInAt(latestOf(times, DEST_WAREHOUSE_IN_ACTIONS));
        dto.setShipperAssignedAt(latestOf(times, SHIPPER_ASSIGN_ACTIONS));
    }

    private java.util.Map<Long, List<OrderLeg>> legsByOrderId(List<ShipmentOrder> orders) {
        List<Long> ids = orders.stream().map(ShipmentOrder::getId).filter(java.util.Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<Long, List<OrderLeg>> out = new java.util.HashMap<>();
        for (OrderLeg leg : orderLegRepository.findByOrderIdsWithTrip(ids)) {
            out.computeIfAbsent(leg.getOrder().getId(), k -> new java.util.ArrayList<>()).add(leg);
        }
        return out;
    }

    /**
     * Đơn có thu hộ: nhận biết theo tiền COD; hình thức thanh toán vẫn là người gửi / người nhận trả cước.
     * PaymentTerm.COD chỉ còn ở đơn cũ (coi như người nhận trả cước).
     */
    static boolean isCodOrder(ShipmentOrder order) {
        return order.getPaymentTerm() == PaymentTerm.COD || OrderMoney.nz(order.getCodAmount()).signum() > 0;
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
            if (!isCodOrder(order)) {
                continue;
            }
            if (order.getStatus() != OrderStatus.DELIVERED || order.getCodExportedAt() != null) {
                continue;
            }
            String actor = currentActor();
            order.setCodExportedAt(now);
            order.setCodExportedBy(actor);
            shipmentOrderRepository.save(order);
            appendEvent(
                order,
                "COD_PROCESSED",
                "Xác nhận đã xử lý COD " + OrderMoney.nz(order.getCodAmount()).toPlainString() + "đ",
                actor
            );
            updated++;
        }
        return updated;
    }

    /**
     * Từ khoá dạng SĐT ("0912 456 874", "0912.456.874", "+84912456874") → chỉ chữ số, đầu 84 đổi về 0.
     * Không phải SĐT (có chữ cái, &lt; 3 số) → null, giữ cách so khớp cũ.
     */
    static String phoneSearchDigits(String keyword) {
        if (keyword == null) return null;
        String compact = keyword.trim().replaceAll("[\\s.\\-()]", "");
        if (!compact.matches("\\+?\\d{3,}")) return null;
        String digits = compact.startsWith("+") ? compact.substring(1) : compact;
        if (digits.startsWith("84") && digits.length() >= 11) digits = "0" + digits.substring(2);
        return digits;
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

    private static jakarta.persistence.criteria.Predicate pendingCancelRequest(
        jakarta.persistence.criteria.Root<ShipmentOrder> root,
        jakarta.persistence.criteria.CriteriaQuery<?> q,
        jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        jakarta.persistence.criteria.Subquery<Long> sub = q.subquery(Long.class);
        jakarta.persistence.criteria.Root<com.mycompany.myapp.domain.OrderIssue> i = sub.from(com.mycompany.myapp.domain.OrderIssue.class);
        sub
            .select(i.get("id"))
            .where(
                cb.equal(i.get("order"), root),
                cb.equal(i.get("issueType"), com.mycompany.myapp.domain.enumeration.IssueType.CANCEL_REQUEST),
                cb.equal(i.get("issueStatus"), IssueStatus.OPEN)
            );
        return cb.exists(sub);
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

    /**
     * Public guest create (POST /api/orders/guest, alias /drafts).
     * Business no longer uses DRAFT: always CONFIRMED + real order code.
     * Guest drop-off (!homePickup) → qrDropOff so đơn vào Chờ nhận hàng.
     */
    public CreateDraftOrderResponse createDraft(CreateDraftOrderRequest req) {
        String goodsPhoto = validGoodsPhoto(req.getGoodsPhoto());
        boolean homeDelivery = Boolean.TRUE.equals(req.getHomeDelivery());
        boolean homePickup = Boolean.TRUE.equals(req.getHomePickup());
        // GTN: VP nhận = toOffice (không còn hub trung chuyển). Chấp nhận hubOfficeCode legacy làm toOffice.
        String destOfficeCode = !isBlank(req.getToOfficeCode()) ? req.getToOfficeCode() : (homeDelivery ? req.getHubOfficeCode() : null);
        if (homeDelivery && (isBlank(req.getDeliveryAddress()) || isBlank(destOfficeCode))) {
            throw new BadRequestAlertException("Home delivery requires address and toOfficeCode", ENTITY, "homedelivery");
        }
        if (!homeDelivery && isBlank(req.getToOfficeCode())) {
            throw new BadRequestAlertException("toOfficeCode is required when not home delivery", ENTITY, "toofficerequired");
        }
        if (isBlank(req.getFromOfficeCode())) {
            throw new BadRequestAlertException("fromOfficeCode is required", ENTITY, "fromofficerequired");
        }

        String fromCode = req.getFromOfficeCode().trim().toUpperCase();
        Office from = requireOffice(fromCode);
        Office to = requireOffice(destOfficeCode);

        Office fareTo = to;
        SimpleFareCalculator.FareBreakdown fare = fareCalculator.estimate(
            req.getEstimatedWeightKg(),
            homePickup,
            homeDelivery,
            from,
            fareTo,
            req.getPickupKm(),
            req.getDeliveryKm(),
            req.getBranchCode()
        );

        // Guest public create: auto-confirm soft daily overflow (no staff dialog).
        String orderCode = orderCodeGenerator.nextOrderCode(fromCode, true);

        ShipmentOrder order = newBlankOrder();
        order.setOrderCode(orderCode);
        order.setStatus(OrderStatus.CONFIRMED);
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
        // Khách mang hàng đến bưu cục / quét QR — không lấy tận nơi.
        order.setQrDropOff(!homePickup);
        order.setWeightKg(req.getEstimatedWeightKg());
        int qty = req.getQuantity() != null && req.getQuantity() > 0 ? req.getQuantity() : 1;
        order.setQuantity(qty);

        // FE gửi tổng theo kiện → ưu tiên; không thì ước lượng BE theo cân.
        BigDecimal fareTotal = req.getFareAmount() != null ? req.getFareAmount() : fare.total();
        order.setFareAmount(fareTotal);
        if (req.getGoodsFareAmount() != null) {
            order.setGoodsFareAmount(req.getGoodsFareAmount());
        }
        order.setPickupFeeAmount(req.getPickupFeeAmount() != null ? req.getPickupFeeAmount() : fare.pickupFee());
        order.setDeliveryFeeAmount(req.getDeliveryFeeAmount() != null ? req.getDeliveryFeeAmount() : fare.deliveryFee());
        if (req.getDeclaredFeeAmount() != null) {
            order.setDeclaredFeeAmount(req.getDeclaredFeeAmount());
        }
        if (req.getDiscountAmount() != null) {
            order.setDiscountAmount(req.getDiscountAmount());
        }
        if (req.getCodAmount() != null) {
            order.setCodAmount(req.getCodAmount());
        }
        if (req.getCodFeeAmount() != null) {
            order.setCodFeeAmount(req.getCodFeeAmount());
        }
        order.setBankName(blankToNull(req.getBankName()));
        order.setBankAccountNo(blankToNull(req.getBankAccountNo()));
        order.setBankAccountName(blankToNull(req.getBankAccountName()));
        // Endpoint công khai: không tin paidAmount từ client — tiền chỉ ghi qua order_payment do nhân viên thu.
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRouteLabel(blankToNull(req.getRouteLabel()));
        order.setItineraryLabel(blankToNull(req.getItineraryLabel()));
        order.setNote(req.getNote());
        order.setFromOffice(from);
        order.setToOffice(to);
        order.setHubOffice(null);
        order.setFinalToOffice(to);
        order.setPublicTrackingAllowed(true);
        fillItineraryIfMissing(order);

        order = shipmentOrderRepository.save(order);
        ensureLegs(order);
        appendEvent(order, "CREATE", "Tạo đơn hàng", "customer");
        if (goodsPhoto != null && goodsPhotoRepository != null) {
            OrderGoodsPhoto photo = new OrderGoodsPhoto();
            photo.setOrderId(order.getId());
            photo.setPhotoUrl(storeMedia(goodsPhoto, "goods"));
            photo.setCapturedAt(Instant.now());
            photo.setCapturedByUsername("customer");
            goodsPhotoRepository.save(photo);
            appendEvent(order, "GOODS_PHOTO", "Khách gửi ảnh đơn hàng", "customer");
        }

        return new CreateDraftOrderResponse(null, order.getOrderCode(), OrderStatus.CONFIRMED, fareTotal, null);
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
        if (to == OrderStatus.FAILED_DELIVERY) {
            AhamoveDispatchService.assertNoActivePartner(order);
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
                order.setOrderCode(orderCodeGenerator.nextOrderCode(office, Boolean.TRUE.equals(req.getConfirmDailyOverflow())));
            }
        }
        order.setStatus(to);
        // Leave warehouse pipeline — but RETURNING keeps forwardStage (pipeline hoàn dùng chung tab kho).
        if (to == OrderStatus.DELIVERED || to == OrderStatus.CANCELLED || to == OrderStatus.RETURNED) {
            order.setForwardStage(null);
        } else {
            // Stage đi cùng trạng thái đặt ngay tại đây: gọi forward-stage song song dễ bị lần lưu này ghi đè.
            ForwardStage synced = forwardStageFor(to, order.getForwardStage());
            if (synced != null) {
                order.setForwardStage(synced);
            }
        }
        shipmentOrderRepository.save(order);
        String action = isBlank(req.getAction()) ? "TRANSITION_" + to.name() : req.getAction();
        appendEvent(order, action, req.getDetail(), currentActor());
        if (to == OrderStatus.DELIVERED) {
            eventPublisher.publishEvent(new OrderDeliveredEvent(order.getOrderCode()));
        }
        if (to == OrderStatus.AT_DEST) {
            onArrivedAtDest(order, action);
        }
        return new OrderTransitionResponse(true, to, order.getOrderCode());
    }

    /** Tab pipeline bắt buộc theo trạng thái mới; null = giữ nguyên. */
    static ForwardStage forwardStageFor(OrderStatus to, ForwardStage current) {
        return switch (to) {
            case OUT_FOR_DELIVERY -> ForwardStage.DELIVERING;
            case FAILED_DELIVERY -> current == ForwardStage.FAILED || current == ForwardStage.REDELIVER_WAIT ? null : ForwardStage.FAILED;
            case AT_DEST -> ForwardStage.DEST_WH_IN;
            default -> null;
        };
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
        appendEvent(order, "RESTORE", "Khôi phục về đã xác nhận", currentActor());
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
        boolean phoneOk = phoneMatchesSenderOrReceiver(phone, order.getSenderPhone(), order.getReceiverPhone());
        if (!phoneOk || !Boolean.TRUE.equals(order.getPublicTrackingAllowed())) {
            res.setFound(false);
            return res;
        }
        res.setFound(true);
        res.setOrderCode(order.getOrderCode());
        res.setDraftCode(order.getDraftCode());
        res.setStatus(order.getStatus());
        res.setStatusLabel(CustomerTrackStatus.labelOf(order));
        res.setFromOfficeCode(officeCode(order.getFromOffice()));
        res.setToOfficeCode(officeCode(order.getToOffice()));
        res.setReceiverName(order.getReceiverName());
        res.setReceiverPhone(order.getReceiverPhone());
        res.setDeliveryAddress(order.getDeliveryAddress());
        res.setGoodsType(order.getGoodsType() != null ? order.getGoodsType().name() : null);
        res.setNote(order.getNote());
        res.setHomeDelivery(order.getHomeDelivery());
        res.setHomePickup(order.getHomePickup());
        res.setFareAmount(order.getFareAmount());
        res.setGoodsFareAmount(order.getGoodsFareAmount());
        res.setDeliveryFeeAmount(order.getDeliveryFeeAmount());
        res.setPickupFeeAmount(order.getPickupFeeAmount());
        Office dest = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
        res.setFromOfficeName(order.getFromOffice() != null ? order.getFromOffice().getName() : null);
        res.setToOfficeName(dest != null ? dest.getName() : null);
        res.setRouteLabel(order.getRouteLabel());
        res.setItineraryLabel(order.getItineraryLabel());
        List<OrderEvent> events = order.getId() == null ? List.of() : orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId());
        res.setEvents(
            events
                .stream()
                .filter(e -> CustomerTrackJourney.PUBLIC_EVENT_ACTIONS.contains(e.getAction()))
                .map(e -> {
                    OrderDetailDTO.OrderEventViewDTO v = new OrderDetailDTO.OrderEventViewDTO();
                    v.setAt(e.getEventAt());
                    v.setAction(e.getAction());
                    return v;
                })
                .toList()
        );
        res.setJourney(CustomerTrackJourney.build(order, events));
        String inv = order.getInvoiceStatus();
        if ("ISSUED".equals(inv) || "DUPLICATE".equals(inv)) {
            res.setInvoiceState("ISSUED");
            res.setInvoiceNo(order.getInvoiceNo());
        } else if ("MANUAL".equals(inv) || "PENDING".equals(inv) || "FAILED".equals(inv)) {
            res.setInvoiceState("OFFICE");
        } else {
            res.setInvoiceState(Boolean.TRUE.equals(order.getInvoiceRequested()) ? "REQUESTED" : "NONE");
        }
        return res;
    }

    /**
     * Public track: full phone OR last 4 digits must match sender or receiver (digits only).
     */
    static boolean phoneMatchesSenderOrReceiver(String input, String senderPhone, String receiverPhone) {
        String in = digitsOnly(input);
        if (in.isEmpty()) {
            return false;
        }
        String sender = digitsOnly(senderPhone);
        String receiver = digitsOnly(receiverPhone);
        if (in.length() == 4) {
            return (!sender.isEmpty() && sender.endsWith(in)) || (!receiver.isEmpty() && receiver.endsWith(in));
        }
        return in.equals(sender) || in.equals(receiver);
    }

    private static String digitsOnly(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.replaceAll("\\D+", "");
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
        order.setOrderCode(orderCodeGenerator.nextOrderCode(fromCode, Boolean.TRUE.equals(req.getConfirmDailyOverflow())));
        order.setStatus(OrderStatus.CONFIRMED);
        fillItineraryIfMissing(order);
        order = shipmentOrderRepository.save(order);
        ensureLegs(order);
        appendEvent(order, "CONFIRM", "Xác nhận từ nháp", currentActor());
        return toSummary(order);
    }

    private OrderSummaryDTO createNewConfirmed(CreateOrderRequest req) {
        boolean homeDelivery = Boolean.TRUE.equals(req.getHomeDelivery());
        boolean homePickup = Boolean.TRUE.equals(req.getHomePickup());
        Office from = requireOffice(req.getFromOfficeCode());
        Office to = requireOffice(req.getToOfficeCode());

        SimpleFareCalculator.FareBreakdown fare = fareCalculator.estimate(
            req.getWeightKg(),
            homePickup,
            homeDelivery,
            from,
            to,
            req.getPickupKm(),
            req.getDeliveryKm(),
            req.getBranchCode()
        );
        // FE gửi tổng theo kiện (goodsFare từng dòng + phí COD/tận nơi/khai giá/giảm giá) → SoT.
        // Không ghi đè bằng ước lượng BE theo tổng cân 1 lần — với ≥2 kiện sẽ lệch (surcharge/band).
        // Khi không có fareAmount: bảng giá master + phí dịch vụ từ request (COD/khai giá/giảm giá).
        BigDecimal serviceFees = OrderMoney.nz(req.getCodFeeAmount())
            .add(OrderMoney.nz(req.getDeclaredFeeAmount()))
            .subtract(OrderMoney.nz(req.getDiscountAmount()));
        BigDecimal total;
        if (req.getFareAmount() != null) {
            total = req.getFareAmount();
        } else {
            total = fare.total().add(serviceFees);
        }
        total = total.max(BigDecimal.ZERO);

        ShipmentOrder order = newBlankOrder();
        order.setOrderCode(orderCodeGenerator.nextOrderCode(from.getCode(), Boolean.TRUE.equals(req.getConfirmDailyOverflow())));
        order.setStatus(OrderStatus.CONFIRMED);
        applyCreateFields(order, req);
        order.setFromOffice(from);
        order.setToOffice(to);
        // Không còn trung chuyển: bỏ hub; finalTo mặc định = toOffice.
        order.setHubOffice(null);
        Office dest = !isBlank(req.getFinalToOfficeCode()) ? requireOffice(req.getFinalToOfficeCode()) : to;
        order.setFinalToOffice(dest);
        order.setServiceType(resolveServiceType(homePickup, homeDelivery));
        order.setFareAmount(total);
        if (req.getGoodsFareAmount() != null) {
            order.setGoodsFareAmount(req.getGoodsFareAmount());
        } else if (fare.pricingRuleId() != null) {
            order.setGoodsFareAmount(fare.base().add(OrderMoney.nz(fare.surcharge())));
        }
        // Ưu tiên phí tận nơi FE đã tính theo bảng /phu-phi × KM; không thì BE (có KM nếu gửi).
        order.setPickupFeeAmount(req.getPickupFeeAmount() != null ? req.getPickupFeeAmount() : fare.pickupFee());
        order.setDeliveryFeeAmount(req.getDeliveryFeeAmount() != null ? req.getDeliveryFeeAmount() : fare.deliveryFee());
        order.setPublicTrackingAllowed(true);
        fillItineraryIfMissing(order);

        order = shipmentOrderRepository.save(order);
        ensureLegs(order);
        appendEvent(order, "CREATE", "Tạo đơn nội bộ", currentActor());
        return toSummary(order);
    }

    public OrderDetailDTO patch(String code, PatchOrderRequest req) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        if (req == null) {
            return getByCode(order.getOrderCode());
        }
        String endpointsBefore = itineraryEndpointsKey(order);
        java.util.Map<String, String> fieldsBefore = OrderEditDiff.snapshot(order);
        BigDecimal fareBefore = OrderMoney.nz(order.getFareAmount());
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
        boolean hpBefore = Boolean.TRUE.equals(order.getHomePickup());
        boolean hdBefore = Boolean.TRUE.equals(order.getHomeDelivery());
        String pickupAddrBefore = order.getPickupAddress();
        String deliveryAddrBefore = order.getDeliveryAddress();
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
        SimpleFareCalculator.FareBreakdown doorFees = null;
        BigDecimal estimatedPickupKm = null;
        BigDecimal estimatedDeliveryKm = null;
        // Cờ đổi, hoặc đổi địa chỉ khi đang bật tận nơi: tính lại phí. Gửi lại cùng cờ thì giữ phí đã tính theo km.
        if (req.getHomePickup() != null) {
            order.setHomePickup(req.getHomePickup());
        }
        if (req.getHomeDelivery() != null) {
            order.setHomeDelivery(req.getHomeDelivery());
        }
        boolean hp = Boolean.TRUE.equals(order.getHomePickup());
        boolean hd = Boolean.TRUE.equals(order.getHomeDelivery());
        boolean pickupFlagChanged = hp != hpBefore;
        boolean deliveryFlagChanged = hd != hdBefore;
        boolean pickupAddrChanged = !sameText(pickupAddrBefore, order.getPickupAddress());
        boolean deliveryAddrChanged = !sameText(deliveryAddrBefore, order.getDeliveryAddress());
        boolean recomputeDoor = pickupFlagChanged || deliveryFlagChanged || (hp && pickupAddrChanged) || (hd && deliveryAddrChanged);
        if (recomputeDoor) {
            order.setServiceType(resolveServiceType(hp, hd));
            BigDecimal prevPickupFee = order.getPickupFeeAmount();
            BigDecimal prevDeliveryFee = order.getDeliveryFeeAmount();
            if (hp && (pickupFlagChanged || pickupAddrChanged)) {
                estimatedPickupKm = requireDoorKm(order.getFromOffice(), order.getPickupAddress(), "lấy");
            }
            if (hd && (deliveryFlagChanged || deliveryAddrChanged)) {
                Office dest = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
                estimatedDeliveryKm = requireDoorKm(dest, order.getDeliveryAddress(), "giao");
            }
            doorFees = estimatedPickupKm != null || estimatedDeliveryKm != null
                ? fareCalculator.estimate(
                    order.getWeightKg(),
                    hp,
                    hd,
                    order.getFromOffice(),
                    order.getToOffice(),
                    estimatedPickupKm,
                    estimatedDeliveryKm
                )
                : fareCalculator.estimate(order.getWeightKg(), hp, hd, order.getFromOffice(), order.getToOffice());
            // Phía không đổi km thì giữ phí cũ — null km sẽ bị tính nhầm mức 0–4 km.
            order.setPickupFeeAmount(hp ? (estimatedPickupKm != null ? doorFees.pickupFee() : prevPickupFee) : BigDecimal.ZERO);
            order.setDeliveryFeeAmount(hd ? (estimatedDeliveryKm != null ? doorFees.deliveryFee() : prevDeliveryFee) : BigDecimal.ZERO);
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
            // Chuỗi rỗng = xóa hub (hết trung chuyển).
            if (isBlank(req.getHubOfficeCode())) {
                order.setHubOffice(null);
            } else {
                order.setHubOffice(requireOffice(req.getHubOfficeCode()));
            }
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
        if (req.getGoodsFareAmount() != null) {
            order.setGoodsFareAmount(req.getGoodsFareAmount());
        }
        if (req.getDeclaredFeeAmount() != null) {
            order.setDeclaredFeeAmount(req.getDeclaredFeeAmount());
        }
        if (req.getDiscountAmount() != null) {
            order.setDiscountAmount(req.getDiscountAmount());
        }
        if (doorFees != null) {
            // Phí tận nơi vừa tính lại → tổng cước dựng lại từ các khoản (fareAmount client gửi kèm có thể còn phí cũ).
            BigDecimal recalc = recalcFareAfterDoorChange(order, doorFees);
            BigDecimal paid = OrderMoney.nz(order.getPaidAmount());
            if (recalc.compareTo(paid) < 0) {
                throw new BadRequestAlertException(
                    "Đơn đã thu " +
                    paid.toPlainString() +
                    "đ — bỏ lấy/giao tận nơi làm cước còn " +
                    recalc.toPlainString() +
                    "đ, thấp hơn số đã thu. Cần hoàn tiền cho khách trước khi đổi.",
                    ENTITY,
                    "fareBelowPaid"
                );
            }
            order.setFareAmount(recalc);
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
        applyInvoiceFields(
            order,
            req.getInvoiceRequested(),
            req.getInvoiceTaxCode(),
            req.getInvoiceCompanyName(),
            req.getInvoiceEmail(),
            req.getInvoiceCompanyAddress(),
            req.getInvoiceBuyerName(),
            req.getInvoiceBuyerIdNumber(),
            req.getInvoiceBuyerPhone(),
            true
        );
        if (req.getRouteLabel() != null) {
            order.setRouteLabel(blankToNull(req.getRouteLabel()));
        }
        if (req.getItineraryLabel() != null) {
            order.setItineraryLabel(blankToNull(req.getItineraryLabel()));
        }
        if (req.getRouteLabel() == null && req.getItineraryLabel() == null && !endpointsBefore.equals(itineraryEndpointsKey(order))) {
            refillItinerary(order);
        }
        if (OrderMoney.nz(order.getFareAmount()).compareTo(fareBefore) != 0) {
            PartnerAdvance.assertNotPending(order, ENTITY);
        }
        shipmentOrderRepository.save(order);
        if (!Boolean.TRUE.equals(req.getSkipHistory())) {
            String eventAction = !isBlank(req.getEventAction()) ? req.getEventAction().trim() : "PATCH";
            String eventDetail = !isBlank(req.getEventDetail()) ? req.getEventDetail().trim() : "Cập nhật thông tin đơn";
            if ("PATCH".equals(eventAction) || "ORDER_EDIT".equals(eventAction)) {
                String changes = OrderEditDiff.describe(fieldsBefore, OrderEditDiff.snapshot(order));
                if (!changes.isEmpty()) {
                    eventDetail = changes;
                }
                String kmNote = doorKmNote(estimatedPickupKm, estimatedDeliveryKm);
                if (!kmNote.isEmpty()) {
                    eventDetail = eventDetail + " · " + kmNote;
                }
            }
            appendEvent(order, eventAction, eventDetail, currentActor());
        }
        return getByCode(order.getOrderCode());
    }

    /**
     * Append a history row without changing order fields.
     * PRINT also updates labelPrintedAt / labelReprintCount.
     */
    public OrderDetailDTO logEvent(String code, LogOrderEventRequest req) {
        ShipmentOrder order = requireByCode(code);
        if (req == null || isBlank(req.getAction())) {
            throw new BadRequestAlertException("action is required", ENTITY, "eventActionRequired");
        }
        String action = req.getAction().trim();
        String detail = req.getDetail();
        if ("PRINT".equalsIgnoreCase(action)) {
            Instant now = Instant.now();
            Integer prev = order.getLabelReprintCount();
            if (order.getLabelPrintedAt() == null) {
                order.setLabelPrintedAt(now);
                order.setLabelReprintCount(0);
            } else {
                order.setLabelPrintedAt(now);
                order.setLabelReprintCount(prev == null ? 1 : prev + 1);
            }
            shipmentOrderRepository.save(order);
        }
        appendEvent(order, action, detail, currentActor());
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
        appendEvent(order, "PICKUP_START", "Bắt đầu lấy hàng", currentActor());
        return getByCode(order.getOrderCode());
    }

    public OrderDetailDTO warehouseReceive(String code) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        if (order.getStatus() == OrderStatus.DRAFT) {
            if (order.getFromOffice() == null) {
                throw new BadRequestAlertException("fromOffice is required", ENTITY, "fromofficerequired");
            }
            if (draftExpiryService.isDraftExpired(order)) {
                draftExpiryService.cancelExpiredDraft(order);
                throw new BadRequestAlertException("Draft expired (>24h)", ENTITY, "draftExpired");
            }
            String office = order.getFromOffice().getCode();
            if (order.getOrderCode() != null && order.getOrderCode().startsWith("N-")) {
                order.setOrderCode(orderCodeGenerator.nextOrderCode(office, false));
            }
            order.setStatus(OrderStatus.CONFIRMED);
            ensureLegs(order);
            appendEvent(order, "CONFIRM", "Xác nhận nhập kho tại bưu cục", currentActor());
        }
        order.setPickedUpAt(Instant.now());
        if (order.getForwardStage() == null) {
            order.setForwardStage(ForwardStage.WH_IN);
        }
        shipmentOrderRepository.save(order);
        appendEvent(order, "WAREHOUSE_RECEIVE", "Nhập kho gửi", currentActor());
        collectSenderFareOnWarehouseIn(order);
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
            appendEvent(order, "LEG_ARRIVE_DEST", "Chặng cuối đã đến", currentActor());
            onArrivedAtDest(order, "LEG_ARRIVE_DEST");
        } else {
            OrderLeg next = legs.get(current.getLegIndex() + 1);
            order.setFromOffice(next.getFromOffice());
            order.setToOffice(next.getToOffice());
            order.setCurrentTrip(null);
            order.setStatus(OrderStatus.CONFIRMED);
            appendEvent(order, "LEG_ADVANCE", "Chuyển sang chặng tiếp", currentActor());
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
        order.setOnCredit(Boolean.TRUE.equals(req.getOnCredit()));
        order.setWeightKg(req.getWeightKg());
        order.setQuantity(req.getQuantity() == null ? 1 : req.getQuantity());
        order.setNote(req.getNote());
        if (!isBlank(req.getFromOfficeCode())) {
            order.setFromOffice(requireOffice(req.getFromOfficeCode()));
        }
        if (!isBlank(req.getToOfficeCode())) {
            order.setToOffice(requireOffice(req.getToOfficeCode()));
        }
        // Bỏ ghi hub trên create — không còn trung chuyển.
        order.setHubOffice(null);
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
        if (req.getGoodsFareAmount() != null) {
            order.setGoodsFareAmount(req.getGoodsFareAmount());
        }
        if (req.getDeclaredFeeAmount() != null) {
            order.setDeclaredFeeAmount(req.getDeclaredFeeAmount());
        }
        if (req.getDiscountAmount() != null) {
            order.setDiscountAmount(req.getDiscountAmount());
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
        applyInvoiceFields(
            order,
            req.getInvoiceRequested(),
            req.getInvoiceTaxCode(),
            req.getInvoiceCompanyName(),
            req.getInvoiceEmail(),
            req.getInvoiceCompanyAddress(),
            req.getInvoiceBuyerName(),
            req.getInvoiceBuyerIdNumber(),
            req.getInvoiceBuyerPhone(),
            false
        );
        if (req.getRouteLabel() != null) {
            order.setRouteLabel(blankToNull(req.getRouteLabel()));
        }
        if (req.getItineraryLabel() != null) {
            order.setItineraryLabel(blankToNull(req.getItineraryLabel()));
        }
    }

    @Autowired(required = false)
    void setItineraryRepository(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
    }

    @Autowired(required = false)
    void setGoodsPhotoRepository(OrderGoodsPhotoRepository goodsPhotoRepository) {
        this.goodsPhotoRepository = goodsPhotoRepository;
    }

    private StoredMedia storedMedia;

    @Autowired(required = false)
    void setStoredMedia(StoredMedia storedMedia) {
        this.storedMedia = storedMedia;
    }

    private String storeMedia(String value, String folder) {
        return storedMedia == null || value == null ? value : storedMedia.store(value, folder);
    }

    private String showMedia(String value) {
        return storedMedia == null || value == null ? value : storedMedia.expose(value);
    }

    /** Ảnh đơn hàng khách gửi khi tạo đơn; null nếu không có. */
    @Transactional(readOnly = true)
    public String goodsPhoto(String code) {
        ShipmentOrder order = requireByCode(code);
        if (goodsPhotoRepository == null) {
            return null;
        }
        return goodsPhotoRepository.findOneByOrderId(order.getId()).map(p -> showMedia(p.getPhotoUrl())).orElse(null);
    }

    /** Ảnh từ endpoint công khai: chỉ nhận data-URL ảnh, giới hạn dung lượng. */
    static String validGoodsPhoto(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String photo = raw.trim();
        if (!photo.startsWith("data:image/") || !photo.contains(";base64,")) {
            throw new BadRequestAlertException("Goods photo must be an image", ENTITY, "goodsPhotoInvalid");
        }
        if (photo.length() > MAX_GOODS_PHOTO_LENGTH) {
            throw new BadRequestAlertException("Goods photo too large", ENTITY, "goodsPhotoTooLarge");
        }
        return photo;
    }

    private com.mycompany.myapp.repository.StaffProfileRepository staffProfileRepository;

    @Autowired(required = false)
    void setStaffProfileRepository(com.mycompany.myapp.repository.StaffProfileRepository staffProfileRepository) {
        this.staffProfileRepository = staffProfileRepository;
    }

    private com.mycompany.myapp.repository.OrderPaymentRepository orderPaymentRepository;

    @Autowired(required = false)
    void setOrderPaymentRepository(com.mycompany.myapp.repository.OrderPaymentRepository orderPaymentRepository) {
        this.orderPaymentRepository = orderPaymentRepository;
    }

    private com.mycompany.myapp.service.autocall.AutoCallService autoCallService;

    @Autowired(required = false)
    void setAutoCallService(com.mycompany.myapp.service.autocall.AutoCallService autoCallService) {
        this.autoCallService = autoCallService;
    }

    private void onArrivedAtDest(ShipmentOrder order, String action) {
        if (autoCallService != null) {
            autoCallService.onArrivedAtDest(order, action);
        }
    }

    static final String NOTE_SENDER_PREPAID = "Thu đầu gửi (người gửi thanh toán)";
    static final String NOTE_PAYMENT_TERM_REVERSAL = "Đảo khoản thu do đổi hình thức thanh toán";

    private com.mycompany.myapp.repository.ReceiptOrderLineRepository receiptOrderLineRepository;

    @Autowired(required = false)
    void setReceiptOrderLineRepository(com.mycompany.myapp.repository.ReceiptOrderLineRepository receiptOrderLineRepository) {
        this.receiptOrderLineRepository = receiptOrderLineRepository;
    }

    private static String paymentMethodLabel(PaymentTerm term, boolean onCredit) {
        if (onCredit) {
            return "Công nợ";
        }
        return term == PaymentTerm.GUI_TRA ? "Người gửi trả" : "Người nhận trả";
    }

    /** Đã qua nhập kho gửi: người gửi không còn ở quầy để thu cước. */
    static boolean senderWarehouseDone(ShipmentOrder order) {
        if (order.getPickedUpAt() != null || order.getForwardStage() != null) {
            return true;
        }
        OrderStatus st = order.getStatus();
        return (
            st == OrderStatus.IN_TRANSIT ||
            st == OrderStatus.AT_DEST ||
            st == OrderStatus.OUT_FOR_DELIVERY ||
            st == OrderStatus.FAILED_DELIVERY
        );
    }

    /** Hàng còn nằm kho VP gửi, chưa gán xe. */
    static boolean atSenderWarehouse(ShipmentOrder order) {
        OrderStatus st = order.getStatus();
        if (st != OrderStatus.CONFIRMED && st != OrderStatus.WAITING) {
            return false;
        }
        if (order.getCurrentTrip() != null) {
            return false;
        }
        ForwardStage stage = order.getForwardStage();
        return stage == null || stage == ForwardStage.PICKED || stage == ForwardStage.WH_IN;
    }

    /** Từ Nhập kho giao trở đi (đang giao / giao thất bại / chờ giao lại). */
    static boolean atReceiverSide(ShipmentOrder order) {
        OrderStatus st = order.getStatus();
        if (st == OrderStatus.AT_DEST || st == OrderStatus.OUT_FOR_DELIVERY || st == OrderStatus.FAILED_DELIVERY) {
            return true;
        }
        ForwardStage stage = order.getForwardStage();
        return (
            stage == ForwardStage.DEST_WH_IN ||
            stage == ForwardStage.DELIVERING ||
            stage == ForwardStage.FAILED ||
            stage == ForwardStage.REDELIVER_WAIT
        );
    }

    /** Quầy chỉ đổi HTTT ở Chờ lấy hàng (shipper chưa nhận lấy) / Chờ nhận hàng / Nhập kho gửi (chưa gán xe). */
    static boolean counterMayChangePaymentTerm(ShipmentOrder order) {
        boolean notPickedUp = order.getPickedUpAt() == null && order.getForwardStage() == null;
        if (notPickedUp) {
            boolean picking =
                order.getPickingAt() != null || (order.getPickupStaffUsername() != null && !order.getPickupStaffUsername().isBlank());
            if (picking || order.getCurrentTrip() != null) {
                return false;
            }
            if (order.getStatus() == OrderStatus.DRAFT) {
                return true;
            }
        }
        return atSenderWarehouse(order);
    }

    /**
     * Đổi hình thức thanh toán. Điều phối VP gửi đổi được khi hàng còn ở kho gửi chưa gán xe (kể cả đã thu — khoản thu
     * được đảo bằng payment âm); sau đó điều phối chỉ đổi khi đơn chưa thu đồng nào, admin đổi được cả khi đã ghi thu.
     * Đã lên phiếu thu / đã giao / hoàn / huỷ thì không ai đổi.
     */
    /**
     * Đổi VP nhận khi hàng đang nằm ở kho VP nhận (nhập kho giao / giao thất bại / chờ giao lại) vì hàng đã được chuyển
     * tay sang VP khác. Giữ trạng thái, lộ trình và cước; chỉ admin hoặc điều phối của VP đang giữ hàng.
     */
    public OrderDetailDTO rerouteDestination(String code, com.mycompany.myapp.service.dto.order.RerouteDestinationRequest req) {
        ShipmentOrder order = requireByCode(code);
        dayClosureGuard.assertOrderMutable(order);
        PartnerAdvance.assertNoRefundDue(order, ENTITY);
        String reason = req == null || req.reason() == null ? "" : req.reason().trim();
        if (reason.length() < 3) {
            throw new BadRequestAlertException("Nhập lý do đổi VP nhận", ENTITY, "rerouteReasonRequired");
        }
        String targetCode = req.officeCode() == null ? "" : req.officeCode().trim();
        if (targetCode.isEmpty()) {
            throw new BadRequestAlertException("Chọn VP nhận mới", ENTITY, "rerouteOfficeRequired");
        }
        Office current = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();

        if (!staffAccessService.isSystemAdmin()) {
            var profile = staffAccessService
                .current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ admin / điều phối được đổi VP nhận"));
            if (profile.getRoleCode() != com.mycompany.myapp.domain.enumeration.RoleCode.AD) {
                if (profile.getRoleCode() != com.mycompany.myapp.domain.enumeration.RoleCode.DH) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ admin / điều phối được đổi VP nhận");
                }
                String scoped = staffAccessService.scopedOfficeCode().orElse(null);
                if (scoped != null && (current == null || !scoped.equalsIgnoreCase(current.getCode()))) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ điều phối VP đang giữ hàng được đổi VP nhận");
                }
            }
        }

        if (!rerouteAllowed(order)) {
            throw new BadRequestAlertException(
                "Chỉ đổi VP nhận khi hàng đang ở kho VP nhận (nhập kho giao / giao thất bại / chờ giao lại)",
                ENTITY,
                "rerouteStatus"
            );
        }
        Office target = requireOffice(targetCode);
        if (Boolean.FALSE.equals(target.getActive())) {
            throw new BadRequestAlertException("VP nhận mới đã ngừng hoạt động", ENTITY, "rerouteOfficeInactive");
        }
        if (current != null && current.getId() != null && current.getId().equals(target.getId())) {
            throw new BadRequestAlertException("VP nhận mới trùng VP hiện tại", ENTITY, "rerouteSameOffice");
        }

        order.setToOffice(target);
        order.setFinalToOffice(target);
        order.setHubOffice(null);
        order.setPartnerCode(null);
        order.setPartnerFeeAmount(null);
        order.setPartnerOrderId(null);
        order.setPartnerStatus(null);
        shipmentOrderRepository.save(order);
        String detail =
            "Đổi VP nhận " + (current == null ? "—" : current.getName()) + " → " + target.getName() + " (hàng chuyển tay) · " + reason;
        appendEvent(order, "DEST_REROUTE", detail.length() > 255 ? detail.substring(0, 255) : detail, currentActor());
        return getByCode(order.getOrderCode());
    }

    /** Hàng đang nằm ở kho VP nhận, chưa giao cho shipper. */
    static boolean rerouteAllowed(ShipmentOrder order) {
        OrderStatus st = order.getStatus();
        if (st != OrderStatus.AT_DEST && st != OrderStatus.FAILED_DELIVERY) {
            return false;
        }
        return order.getForwardStage() != ForwardStage.DELIVERING;
    }

    public OrderDetailDTO changePaymentTerm(String code, com.mycompany.myapp.service.dto.order.ChangePaymentTermRequest req) {
        ShipmentOrder order = requireByCode(code);
        PartnerAdvance.assertNotPending(order, ENTITY);
        String reason = req == null || req.reason() == null ? "" : req.reason().trim();
        if (reason.length() < 3) {
            throw new BadRequestAlertException("Nhập lý do đổi hình thức thanh toán", ENTITY, "paymentTermReasonRequired");
        }
        String method = req.method() == null ? "" : req.method().trim().toUpperCase();
        PaymentTerm target;
        boolean targetCredit;
        switch (method) {
            case "GUI_TRA" -> {
                target = PaymentTerm.GUI_TRA;
                targetCredit = false;
            }
            case "NHAN_TRA" -> {
                target = PaymentTerm.NHAN_TRA;
                targetCredit = false;
            }
            case "CONG_NO" -> {
                target = PaymentTerm.GUI_TRA;
                targetCredit = true;
            }
            default -> throw new BadRequestAlertException("Hình thức thanh toán không hợp lệ", ENTITY, "paymentTermInvalid");
        }

        boolean admin = staffAccessService.isSystemAdmin();
        boolean senderOfficeStaff = admin;
        if (!admin) {
            var profile = staffAccessService
                .current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ admin / điều phối được đổi"));
            if (profile.getRoleCode() == com.mycompany.myapp.domain.enumeration.RoleCode.AD) {
                admin = true;
                senderOfficeStaff = true;
            } else if (profile.getRoleCode() == com.mycompany.myapp.domain.enumeration.RoleCode.Q) {
                String scoped = staffAccessService.scopedOfficeCode().orElse(null);
                boolean ownSender =
                    scoped != null && order.getFromOffice() != null && scoped.equalsIgnoreCase(order.getFromOffice().getCode());
                if (!ownSender) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Quầy chỉ đổi được đơn của VP gửi mình");
                }
                if (!counterMayChangePaymentTerm(order)) {
                    throw new BadRequestAlertException(
                        "Quầy chỉ đổi hình thức thanh toán khi đơn ở Chờ lấy hàng / Chờ nhận hàng / Nhập kho gửi",
                        ENTITY,
                        "paymentTermCounterStage"
                    );
                }
                senderOfficeStaff = true;
            } else if (profile.getRoleCode() != com.mycompany.myapp.domain.enumeration.RoleCode.DH) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ admin / điều phối / quầy được đổi hình thức thanh toán");
            } else {
                String scoped = staffAccessService.scopedOfficeCode().orElse(null);
                Office receiver = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
                senderOfficeStaff =
                    scoped == null || (order.getFromOffice() != null && scoped.equalsIgnoreCase(order.getFromOffice().getCode()));
                boolean inScope = senderOfficeStaff || (receiver != null && scoped.equalsIgnoreCase(receiver.getCode()));
                if (!inScope) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Đơn không thuộc VP của bạn");
                }
            }
        }
        boolean atSenderWh = atSenderWarehouse(order) && senderOfficeStaff;

        OrderStatus st = order.getStatus();
        if (st == OrderStatus.DELIVERED || st == OrderStatus.RETURNING || st == OrderStatus.RETURNED || st == OrderStatus.CANCELLED) {
            throw new BadRequestAlertException(
                "Đơn đã giao / hoàn / huỷ — không đổi được hình thức thanh toán",
                ENTITY,
                "paymentTermLocked"
            );
        }
        if (atReceiverSide(order)) {
            throw new BadRequestAlertException(
                "Hàng đã về VP nhận — không đổi được hình thức thanh toán",
                ENTITY,
                "paymentTermReceiverSide"
            );
        }
        if (receiptOrderLineRepository != null && order.getId() != null && receiptOrderLineRepository.existsByOrder_Id(order.getId())) {
            throw new BadRequestAlertException(
                "Đơn đã lên phiếu thu — huỷ phiếu thu trước khi đổi hình thức thanh toán",
                ENTITY,
                "paymentTermReceipted"
            );
        }

        PaymentTerm currentTerm = order.getPaymentTerm();
        boolean currentCredit = Boolean.TRUE.equals(order.getOnCredit());
        if (currentTerm == target && currentCredit == targetCredit) {
            throw new BadRequestAlertException("Đơn đang ở hình thức này rồi", ENTITY, "paymentTermUnchanged");
        }
        if (target == PaymentTerm.GUI_TRA && !targetCredit && senderWarehouseDone(order) && !atSenderWh) {
            throw new BadRequestAlertException(
                "Đơn đã gán xe — chỉ đổi được sang Người nhận trả hoặc Công nợ",
                ENTITY,
                "paymentTermSenderGone"
            );
        }

        BigDecimal paid = OrderMoney.nz(order.getPaidAmount());
        boolean reverse = paid.signum() > 0 && !(target == PaymentTerm.GUI_TRA && !targetCredit);
        if (paid.signum() > 0 && !admin && !atSenderWh) {
            throw new BadRequestAlertException(
                "Đơn đã thu " + paid.toPlainString() + "đ và đã gán xe — chỉ admin đổi được hình thức thanh toán",
                ENTITY,
                "paymentTermCollected"
            );
        }

        dayClosureGuard.assertOrderMutable(order);
        String actor = currentActor();
        if (reverse) {
            dayClosureGuard.assertCollectionMutable(order);
            if (orderPaymentRepository != null) {
                com.mycompany.myapp.domain.OrderPayment payment = new com.mycompany.myapp.domain.OrderPayment();
                payment.setPaymentAt(Instant.now());
                payment.setAmount(paid.negate());
                payment.setMethod(com.mycompany.myapp.domain.enumeration.PaymentMethod.TM);
                payment.setPaymentKind(com.mycompany.myapp.domain.enumeration.PaymentKind.TRUOC);
                payment.setNote(NOTE_PAYMENT_TERM_REVERSAL);
                payment.setCollectorUsername(actor);
                payment.setOrder(order);
                orderPaymentRepository.save(payment);
            }
            order.setPaidAmount(BigDecimal.ZERO);
        }

        String from = paymentMethodLabel(currentTerm, currentCredit);
        String to = paymentMethodLabel(target, targetCredit);
        order.setPaymentTerm(target);
        order.setOnCredit(targetCredit);
        shipmentOrderRepository.save(order);

        StringBuilder detail = new StringBuilder("Đổi hình thức thanh toán: ").append(from).append(" → ").append(to);
        if (reverse) {
            detail.append(" · Đảo khoản đã thu ").append(paid.toPlainString()).append("đ");
        }
        detail.append(" · Lý do: ").append(reason);
        appendEvent(order, "PAYMENT_TERM_CHANGE", detail.toString(), actor);
        if (target == PaymentTerm.GUI_TRA && !targetCredit && (senderWarehouseDone(order) || goodsAtSenderCounter(order))) {
            collectSenderFareOnWarehouseIn(order);
        }
        return getByCode(order.getOrderCode());
    }

    /**
     * Đơn quầy tạo tại VP gửi, hàng còn nằm kho, chưa gán xe: người gửi đang ở quầy → thu được ngay.
     * Không áp dụng đơn lấy tận nơi chưa lấy / đơn khách tự tạo chưa nhập kho (thu khi nhập kho gửi).
     */
    boolean goodsAtSenderCounter(ShipmentOrder order) {
        if (!atSenderWarehouse(order)) {
            return false;
        }
        if (order.getPickedUpAt() == null && Boolean.TRUE.equals(order.getHomePickup())) {
            return false;
        }
        return (
            order.getId() == null ||
            orderEventRepository == null ||
            !orderEventRepository.existsByOrder_IdAndActionAndActorUsername(order.getId(), "CREATE", "customer")
        );
    }

    /** Đơn khách tự tạo phải được VP gửi xác nhận nhập kho trước khi xếp xe / quét lên xe. */
    public void assertSenderWarehouseReceived(ShipmentOrder order) {
        var issue = order.getIssue();
        if (
            issue != null &&
            issue.getIssueStatus() == IssueStatus.OPEN &&
            issue.getIssueType() == com.mycompany.myapp.domain.enumeration.IssueType.CANCEL_REQUEST
        ) {
            throw new BadRequestAlertException("Đơn " + order.getOrderCode() + " đang chờ admin duyệt huỷ", ENTITY, "cancelRequestPending");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.WAITING) {
            return;
        }
        if (order.getPickedUpAt() != null || order.getId() == null) {
            return;
        }
        if (orderEventRepository.existsByOrder_IdAndActionAndActorUsername(order.getId(), "CREATE", "customer")) {
            throw new BadRequestAlertException(
                "Đơn " + order.getOrderCode() + " do khách tạo chưa nhập kho gửi — xác nhận nhập kho trước khi xếp xe",
                ENTITY,
                "notWarehouseReceived"
            );
        }
    }

    /**
     * Đơn người gửi trả: VP gửi xác nhận nhập kho nghĩa là đã thu cước của người gửi → ghi khoản thu đầu gửi
     * (người thu = người nhập kho) cho phần cước còn thiếu, để shipper không thu lại người nhận.
     */
    public void collectSenderFareOnWarehouseIn(ShipmentOrder order) {
        if (orderPaymentRepository == null || order.getPaymentTerm() != PaymentTerm.GUI_TRA) {
            return;
        }
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.WAITING) {
            return;
        }
        BigDecimal due = OrderMoney.collectDue(order);
        if (due.signum() <= 0) {
            return;
        }
        try {
            dayClosureGuard.assertCollectionMutable(order);
        } catch (RuntimeException closed) {
            org.slf4j.LoggerFactory.getLogger(OrderFacadeService.class).warn(
                "Skip sender prepaid for {}: collection day closed",
                order.getOrderCode()
            );
            return;
        }
        String actor = currentActor();
        com.mycompany.myapp.domain.OrderPayment payment = new com.mycompany.myapp.domain.OrderPayment();
        payment.setPaymentAt(Instant.now());
        payment.setAmount(due);
        payment.setMethod(com.mycompany.myapp.domain.enumeration.PaymentMethod.TM);
        payment.setPaymentKind(com.mycompany.myapp.domain.enumeration.PaymentKind.TRUOC);
        payment.setNote(NOTE_SENDER_PREPAID);
        payment.setCollectorUsername(actor);
        payment.setOrder(order);
        orderPaymentRepository.save(payment);
        order.setPaidAmount(OrderMoney.nz(order.getPaidAmount()).add(due));
        shipmentOrderRepository.save(order);
        appendEvent(order, "PAYMENT", NOTE_SENDER_PREPAID + " · " + due.toPlainString() + "đ", actor);
    }

    /**
     * Đơn thiếu tuyến/lộ trình (FE không gửi) → suy theo mã lộ trình "{điểm VP gửi}-{điểm VP nhận}" (vd PT-HD).
     * VP kiêm nhiều điểm thì thử theo thứ tự ưu tiên, lấy lộ trình đang bật đầu tiên.
     * Không khớp / lộ trình tắt thì để trống như cũ.
     */
    void fillItineraryIfMissing(ShipmentOrder order) {
        if (itineraryRepository == null || (!isBlank(order.getRouteLabel()) && !isBlank(order.getItineraryLabel()))) {
            return;
        }
        Office from = order.getFromOffice();
        Office to = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
        resolveItinerary(from, to).ifPresent(it -> {
            if (isBlank(order.getItineraryLabel())) {
                order.setItineraryLabel(blankToNull(it.getName()));
            }
            if (isBlank(order.getRouteLabel()) && it.getBranch() != null) {
                order.setRouteLabel(blankToNull(it.getBranch().getName()));
            }
        });
    }

    /** VP gửi / VP nhận cuối quyết định lộ trình — đổi một trong hai thì lộ trình cũ không còn đúng. */
    private static String itineraryEndpointsKey(ShipmentOrder order) {
        Office from = order.getFromOffice();
        Office to = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
        return (from != null ? from.getId() : null) + "->" + (to != null ? to.getId() : null);
    }

    /** Đổi VP mà client không gửi lộ trình mới: suy lại; không có lộ trình đang bật thì để trống thay vì giữ lộ sai. */
    void refillItinerary(ShipmentOrder order) {
        if (itineraryRepository == null) {
            return;
        }
        order.setRouteLabel(null);
        order.setItineraryLabel(null);
        fillItineraryIfMissing(order);
    }

    private Optional<Itinerary> resolveItinerary(Office from, Office to) {
        for (String fromPt : OfficeItineraryPoints.pointsOf(from)) {
            for (String toPt : OfficeItineraryPoints.pointsOf(to)) {
                Optional<Itinerary> hit = itineraryRepository
                    .findOneByCode(fromPt + "-" + toPt)
                    .filter(it -> !Boolean.FALSE.equals(it.getActive()));
                if (hit.isPresent()) {
                    return hit;
                }
            }
        }
        return Optional.empty();
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
        order.setInvoiceRequested(false);
        return order;
    }

    private void applyInvoiceFields(
        ShipmentOrder order,
        Boolean requested,
        String taxCode,
        String companyName,
        String email,
        String address,
        String buyerName,
        String buyerIdNumber,
        String buyerPhone,
        boolean patch
    ) {
        if ("ISSUED".equals(order.getInvoiceStatus()) || "DUPLICATE".equals(order.getInvoiceStatus())) {
            return;
        }
        if (!patch) {
            boolean want = Boolean.TRUE.equals(requested);
            order.setInvoiceRequested(want);
            if (want) {
                order.setInvoiceTaxCode(requireValidBuyerTaxCode(taxCode));
                order.setInvoiceCompanyName(blankToNull(companyName));
                order.setInvoiceEmail(blankToNull(email));
                order.setInvoiceCompanyAddress(blankToNull(address));
                order.setInvoiceBuyerName(InvoicePolicy.upperBuyerName(buyerName));
                applyOptionalInvoiceBuyerIds(order, buyerIdNumber, buyerPhone, true);
            } else {
                order.setInvoiceTaxCode(null);
                order.setInvoiceCompanyName(null);
                order.setInvoiceEmail(null);
                order.setInvoiceCompanyAddress(null);
                order.setInvoiceBuyerName(null);
                order.setInvoiceBuyerIdNumber(null);
                order.setInvoiceBuyerPhone(null);
            }
            return;
        }
        if (requested != null) {
            order.setInvoiceRequested(requested);
            if (!requested) {
                order.setInvoiceTaxCode(null);
                order.setInvoiceCompanyName(null);
                order.setInvoiceEmail(null);
                order.setInvoiceCompanyAddress(null);
                order.setInvoiceBuyerName(null);
                order.setInvoiceBuyerIdNumber(null);
                order.setInvoiceBuyerPhone(null);
                return;
            }
        }
        if (taxCode != null) {
            String t = blankToNull(taxCode);
            if (t != null || Boolean.TRUE.equals(order.getInvoiceRequested()) || Boolean.TRUE.equals(requested)) {
                order.setInvoiceTaxCode(requireValidBuyerTaxCode(taxCode));
            } else {
                order.setInvoiceTaxCode(null);
            }
        }
        if (companyName != null) {
            order.setInvoiceCompanyName(blankToNull(companyName));
        }
        if (email != null) {
            order.setInvoiceEmail(blankToNull(email));
        }
        if (address != null) {
            order.setInvoiceCompanyAddress(blankToNull(address));
        }
        if (buyerName != null) {
            order.setInvoiceBuyerName(InvoicePolicy.upperBuyerName(buyerName));
        }
        applyOptionalInvoiceBuyerIds(order, buyerIdNumber, buyerPhone, false);
    }

    private static void applyOptionalInvoiceBuyerIds(ShipmentOrder order, String buyerIdNumber, String buyerPhone, boolean force) {
        try {
            if (force || buyerIdNumber != null) {
                order.setInvoiceBuyerIdNumber(buyerIdNumber == null ? null : InvoicePolicy.normalizeBuyerIdNumber(buyerIdNumber));
            }
            if (force || buyerPhone != null) {
                order.setInvoiceBuyerPhone(buyerPhone == null ? null : InvoicePolicy.normalizeBuyerPhone(buyerPhone));
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY, "invoiceBuyerIdsInvalid");
        }
    }

    /** MST người mua — bắt buộc đúng checksum VN khi xuất HĐ (điền bừa bị từ chối). */
    private static String requireValidBuyerTaxCode(String taxCode) {
        String compact = VietnamTaxCode.compact(taxCode);
        if (compact.isEmpty()) {
            throw new BadRequestAlertException("Mã số thuế người mua là bắt buộc", ENTITY, "invoiceTaxRequired");
        }
        if (!VietnamTaxCode.isValid(compact)) {
            throw new BadRequestAlertException(
                "Mã số thuế không hợp lệ (sai định dạng hoặc checksum). Không gửi được sang MISA.",
                ENTITY,
                "invoiceTaxInvalid"
            );
        }
        return VietnamTaxCode.normalize(compact);
    }

    /** Ghi 1 dòng lịch sử đơn không đổi trạng thái (vd cập nhật từ đối tác giao). */
    public void recordEvent(ShipmentOrder order, String action, String detail, String actor) {
        appendEvent(order, action, detail, actor);
    }

    private void appendEvent(ShipmentOrder order, String action, String detail, String actor) {
        OrderEvent event = new OrderEvent();
        event.setEventAt(Instant.now());
        event.setAction(action == null ? "EVENT" : (action.length() > 100 ? action.substring(0, 100) : action));
        if (detail != null && !detail.isBlank()) {
            String d = detail.trim();
            event.setDetail(d.length() > OrderEvent.DETAIL_MAX ? d.substring(0, OrderEvent.DETAIL_MAX) : d);
        }
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
        applyCreator(dto, o);
        return dto;
    }

    private OrderDetailDTO toDetail(ShipmentOrder o) {
        OrderDetailDTO dto = new OrderDetailDTO();
        fillSummary(dto, o);
        applyCreator(dto, o);
        dto.setCancelReason(o.getCancelReason());
        dto.setReceiverActualName(o.getReceiverActualName());
        dto.setReceiverActualPhone(o.getReceiverActualPhone());
        dto.setFailCount(o.getFailCount());
        dto.setEvents(withActorInfo(mapEvents(o.getId())));
        List<OrderPodPhoto> podRows = orderPodPhotoRepository.findByOrder_IdOrderBySequenceNoAsc(o.getId());
        dto.setPodPhotos(podRows.stream().map(p -> showMedia(p.getPhotoUrl())).toList());
        dto.setPodPhotoCaptions(podRows.stream().map(p -> p.getCaption() == null ? "" : p.getCaption()).toList());
        dto.setIssues(
            orderIssueRepository
                .findByOrder_IdOrderByOpenedAtAscIdAsc(o.getId())
                .stream()
                .map(issue -> {
                    OrderDetailDTO.OrderIssueViewDTO view = ExceptionFacadeService.toIssueView(issue);
                    if (view.getPhotos() != null) {
                        view.setPhotos(view.getPhotos().stream().map(this::showMedia).toList());
                    }
                    return view;
                })
                .toList()
        );
        dto.setCurrentIssueId(o.getIssue() != null ? o.getIssue().getId() : null);
        return dto;
    }

    private void fillSummary(OrderSummaryDTO dto, ShipmentOrder o) {
        fillSummary(dto, o, null);
        if (o.getId() != null) {
            applyStageTimes(dto, stageEventTimes(List.of(o.getId())).getOrDefault(o.getId(), java.util.Map.of()));
            dto.setHasGoodsPhoto(goodsPhotoRepository != null && goodsPhotoRepository.existsByOrderId(o.getId()));
        }
    }

    private void fillSummary(OrderSummaryDTO dto, ShipmentOrder o, List<OrderLeg> preloadedLegs) {
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
        dto.setDueAmount(OrderMoney.collectDue(o));
        dto.setOnCredit(Boolean.TRUE.equals(o.getOnCredit()));
        dto.setPickupFeeAmount(o.getPickupFeeAmount());
        dto.setDeliveryFeeAmount(o.getDeliveryFeeAmount());
        dto.setHomePickup(o.getHomePickup());
        dto.setHomeDelivery(o.getHomeDelivery());
        dto.setQrDropOff(o.getQrDropOff());
        dto.setCurrentTripCode(o.getCurrentTrip() != null ? o.getCurrentTrip().getTripCode() : null);
        dto.setShelfNumber(o.getShelfNumber());
        dto.setNote(o.getNote());
        dto.setPickupAddress(o.getPickupAddress());
        dto.setDeliveryAddress(o.getDeliveryAddress());
        dto.setPickingAt(o.getPickingAt());
        dto.setPickedUpAt(o.getPickedUpAt());
        dto.setPickupStaffUsername(o.getPickupStaffUsername());
        dto.setPartnerCode(o.getPartnerCode());
        dto.setPartnerFeeAmount(o.getPartnerFeeAmount());
        dto.setPartnerOrderId(o.getPartnerOrderId());
        dto.setPartnerStatus(o.getPartnerStatus());
        dto.setPartnerTrackingUrl(o.getPartnerTrackingUrl());
        dto.setPartnerDriverName(o.getPartnerDriverName());
        dto.setPartnerDriverPhone(o.getPartnerDriverPhone());
        dto.setPartnerPodUrl(o.getPartnerPodUrl());
        dto.setPartnerFailReason(o.getPartnerFailReason());
        dto.setPartnerUpdatedAt(o.getPartnerUpdatedAt());
        dto.setPartnerCodAmount(o.getPartnerCodAmount());
        dto.setPartnerCodCollectedAt(o.getPartnerCodCollectedAt());
        dto.setPartnerCodCollectedBy(o.getPartnerCodCollectedBy());
        dto.setPartnerShipConfirmedBy(o.getPartnerShipConfirmedBy());
        dto.setPartnerShipConfirmedAt(o.getPartnerShipConfirmedAt());
        if (o.getShipper() != null) {
            dto.setShipperId(o.getShipper().getId());
            dto.setShipperName(o.getShipper().getFullName());
            dto.setShipperPhone(o.getShipper().getPhone());
        }
        dto.setCodAmount(o.getCodAmount());
        dto.setCodFeeAmount(o.getCodFeeAmount());
        dto.setGoodsFareAmount(o.getGoodsFareAmount());
        dto.setDeclaredFeeAmount(o.getDeclaredFeeAmount());
        dto.setDiscountAmount(o.getDiscountAmount());
        dto.setBankName(o.getBankName());
        dto.setBankAccountNo(o.getBankAccountNo());
        dto.setBankAccountName(o.getBankAccountName());
        dto.setInvoiceRequested(Boolean.TRUE.equals(o.getInvoiceRequested()));
        dto.setInvoiceTaxCode(o.getInvoiceTaxCode());
        dto.setInvoiceCompanyName(o.getInvoiceCompanyName());
        dto.setInvoiceEmail(o.getInvoiceEmail());
        dto.setInvoiceCompanyAddress(o.getInvoiceCompanyAddress());
        dto.setInvoiceBuyerName(o.getInvoiceBuyerName());
        dto.setInvoiceBuyerIdNumber(o.getInvoiceBuyerIdNumber());
        dto.setInvoiceBuyerPhone(o.getInvoiceBuyerPhone());
        dto.setInvoiceRefId(o.getInvoiceRefId());
        dto.setInvoiceStatus(o.getInvoiceStatus());
        dto.setInvoiceType(com.mycompany.myapp.service.invoice.InvoicePolicy.typeOf(o));
        dto.setInvoiceTransactionId(o.getInvoiceTransactionId());
        dto.setInvoiceNo(o.getInvoiceNo());
        dto.setInvoiceSeries(o.getInvoiceSeries());
        dto.setInvoiceCode(o.getInvoiceCode());
        dto.setInvoiceGrossAmount(o.getInvoiceGrossAmount());
        dto.setInvoiceNetAmount(o.getInvoiceNetAmount());
        dto.setInvoiceVatAmount(o.getInvoiceVatAmount());
        dto.setInvoiceIssuedAt(o.getInvoiceIssuedAt());
        dto.setInvoiceError(o.getInvoiceError());
        dto.setRouteLabel(o.getRouteLabel());
        dto.setItineraryLabel(o.getItineraryLabel());
        dto.setCodExportedAt(o.getCodExportedAt());
        dto.setCodExportedBy(o.getCodExportedBy());
        if (o.getCurrentTrip() != null) {
            if (o.getCurrentTrip().getVehicle() != null) {
                dto.setVehiclePlate(o.getCurrentTrip().getVehicle().getPlateNumber());
            }
            if (o.getCurrentTrip().getDriver() != null) {
                dto.setDriverName(o.getCurrentTrip().getDriver().getFullName());
            }
            dto.setDepartAt(o.getCurrentTrip().getDepartAt());
        }
        java.util.List<OrderLeg> legs = preloadedLegs != null
            ? preloadedLegs
            : o.getId() == null ? java.util.List.of() : orderLegRepository.findByOrder_IdOrderByLegIndexAsc(o.getId());
        dto.setLegs(legs.stream().map(this::toLegView).toList());
        dto.setCurrentLegIndex(currentLegIndex(legs));
        if ((dto.getVehiclePlate() == null || dto.getDriverName() == null || dto.getDepartAt() == null) && !legs.isEmpty()) {
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
                if (dto.getDepartAt() == null && leg.getTrip().getDepartAt() != null) {
                    dto.setDepartAt(leg.getTrip().getDepartAt());
                }
                if (dto.getVehiclePlate() != null && dto.getDriverName() != null && dto.getDepartAt() != null) {
                    break;
                }
            }
        }
        if (o.getStatus() == OrderStatus.DELIVERED && o.getId() != null) {
            // Không nhét URL/base64 ảnh vào list — FE bấm "Xem POD" mới gọi getByCode.
            dto.setReceiverActualName(o.getReceiverActualName());
            dto.setReceiverActualPhone(o.getReceiverActualPhone());
        }
        if (o.getIssue() != null && o.getIssue().getIssueStatus() == IssueStatus.OPEN) {
            var issue = o.getIssue();
            dto.setIssueType(issue.getIssueType() != null ? issue.getIssueType().name() : null);
            dto.setIssueReason(issue.getReason());
            dto.setIssueOpenedAt(issue.getOpenedAt());
            dto.setIssueOpenedBy(issue.getOpenedByUsername());
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

    private java.util.Map<Long, String> creatorLogins(java.util.Collection<Long> orderIds) {
        java.util.Map<Long, String> out = new java.util.HashMap<>();
        if (orderEventRepository == null || orderIds == null || orderIds.isEmpty()) {
            return out;
        }
        for (Object[] row : orderEventRepository.creatorsByOrderIds(orderIds)) {
            if (row[0] != null && row[1] != null) {
                out.putIfAbsent((Long) row[0], (String) row[1]);
            }
        }
        return out;
    }

    private void applyCreator(OrderSummaryDTO dto, ShipmentOrder o) {
        if (o.getId() == null) {
            return;
        }
        applyCreator(dto, creatorLogins(List.of(o.getId())).get(o.getId()), new java.util.HashMap<>());
    }

    private void applyCreator(
        OrderSummaryDTO dto,
        String login,
        java.util.Map<String, java.util.Optional<com.mycompany.myapp.domain.StaffProfile>> profiles
    ) {
        if (login == null || login.isBlank()) {
            return;
        }
        dto.setCreatedBy(login.trim());
        if ("customer".equalsIgnoreCase(login.trim()) || staffProfileRepository == null) {
            return;
        }
        profiles
            .computeIfAbsent(login.trim().toLowerCase(), staffProfileRepository::findOneByUserLoginIgnoreCase)
            .ifPresent(sp -> {
                String name = sp.getDisplayName();
                dto.setCreatedByName(name != null && !name.isBlank() ? name.trim() : null);
                dto.setCreatedByRole(sp.getRoleCode() != null ? sp.getRoleCode().name() : null);
            });
    }

    /** login (chữ thường) → họ tên nhân viên; tài khoản không có hồ sơ / không có tên thì bỏ qua. */
    private java.util.Map<String, String> staffNamesByLogin(java.util.Collection<String> logins) {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        if (staffProfileRepository == null) {
            return out;
        }
        for (String raw : logins) {
            String login = raw == null ? "" : raw.trim().toLowerCase();
            if (login.isEmpty() || out.containsKey(login)) {
                continue;
            }
            String name = staffProfileRepository
                .findOneByUserLoginIgnoreCase(login)
                .map(com.mycompany.myapp.domain.StaffProfile::getDisplayName)
                .filter(n -> !n.isBlank())
                .map(String::trim)
                .orElse(null);
            out.put(login, name);
        }
        return out;
    }

    /** Gắn mã NV + họ tên người thao tác (mỗi tài khoản tra một lần). */
    private List<OrderDetailDTO.OrderEventViewDTO> withActorInfo(List<OrderDetailDTO.OrderEventViewDTO> events) {
        if (staffProfileRepository == null || events.isEmpty()) {
            return events;
        }
        java.util.Map<String, java.util.Optional<com.mycompany.myapp.domain.StaffProfile>> byLogin = new java.util.HashMap<>();
        for (OrderDetailDTO.OrderEventViewDTO v : events) {
            String login = v.getBy() == null ? "" : v.getBy().trim().toLowerCase();
            if (login.isEmpty()) {
                continue;
            }
            java.util.Optional<com.mycompany.myapp.domain.StaffProfile> p = byLogin.computeIfAbsent(
                login,
                staffProfileRepository::findOneByUserLoginIgnoreCase
            );
            p.ifPresent(sp -> {
                v.setByStaffCode(sp.getStaffCode());
                String name = sp.getDisplayName();
                v.setByName(name != null && !name.isBlank() && !name.equalsIgnoreCase(sp.getUserLogin()) ? name.trim() : null);
            });
        }
        return events;
    }

    /**
    /** KM Ahamove từ VP tới địa chỉ; thiếu địa chỉ, thiếu GPS VP hoặc Ahamove lỗi thì chặn lưu. */
    private BigDecimal requireDoorKm(Office office, String address, String label) {
        if (doorKmEstimator == null) {
            throw new BadRequestAlertException("Không tính được km " + label + " tận nơi", ENTITY, "doorKmEstimate");
        }
        return doorKmEstimator.km(office, null, null, address, label);
    }

    private static boolean sameText(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        return x.equals(y);
    }

    private static String doorKmNote(BigDecimal pickupKm, BigDecimal deliveryKm) {
        StringBuilder sb = new StringBuilder();
        if (pickupKm != null) {
            sb.append("km lấy ").append(pickupKm.stripTrailingZeros().toPlainString());
        }
        if (deliveryKm != null) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("km giao ").append(deliveryKm.stripTrailingZeros().toPlainString());
        }
        return sb.toString();
    }

    /** Cước hàng (hoặc giá bảng nếu đơn chưa lưu cước hàng) + phí tận nơi + phí COD + phí khai giá − giảm giá. */
    static BigDecimal recalcFareAfterDoorChange(ShipmentOrder order, SimpleFareCalculator.FareBreakdown doorFees) {
        BigDecimal goods = order.getGoodsFareAmount() != null
            ? order.getGoodsFareAmount()
            : OrderMoney.nz(doorFees.total()).subtract(OrderMoney.nz(doorFees.pickupFee())).subtract(OrderMoney.nz(doorFees.deliveryFee()));
        BigDecimal codFee = OrderMoney.nz(order.getCodAmount()).signum() > 0 ? OrderMoney.nz(order.getCodFeeAmount()) : BigDecimal.ZERO;
        return goods
            .add(OrderMoney.nz(order.getPickupFeeAmount()))
            .add(OrderMoney.nz(order.getDeliveryFeeAmount()))
            .add(codFee)
            .add(OrderMoney.nz(order.getDeclaredFeeAmount()))
            .subtract(OrderMoney.nz(order.getDiscountAmount()))
            .max(BigDecimal.ZERO);
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
