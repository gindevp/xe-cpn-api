package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.IssueType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import com.mycompany.myapp.service.dto.order.AddPaymentRequest;
import com.mycompany.myapp.service.dto.order.AssignShipperRequest;
import com.mycompany.myapp.service.dto.order.CreateDraftOrderRequest;
import com.mycompany.myapp.service.dto.order.CreateDraftOrderResponse;
import com.mycompany.myapp.service.dto.order.CreateOrderRequest;
import com.mycompany.myapp.service.dto.order.FailDeliveryRequest;
import com.mycompany.myapp.service.dto.order.FailDeliveryResponse;
import com.mycompany.myapp.service.dto.order.LogOrderEventRequest;
import com.mycompany.myapp.service.dto.order.MarkCodExportedRequest;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.OrderSummaryDTO;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.order.OrderTransitionResponse;
import com.mycompany.myapp.service.dto.order.PatchOrderRequest;
import com.mycompany.myapp.service.dto.order.PodRequest;
import com.mycompany.myapp.service.dto.order.PodResponse;
import com.mycompany.myapp.service.dto.order.TrackOrderRequest;
import com.mycompany.myapp.service.dto.order.TrackOrderResponse;
import com.mycompany.myapp.service.dto.trip.AssignOrdersToTripRequest;
import com.mycompany.myapp.service.dto.trip.TripSummaryDTO;
import com.mycompany.myapp.service.order.DeliveryFacadeService;
import com.mycompany.myapp.service.order.ExceptionFacadeService;
import com.mycompany.myapp.service.order.OrderFacadeService;
import com.mycompany.myapp.service.trip.TripFacadeService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * FE-facing order facade at {@code /api/orders} (A-API-08).
 * Generated CRUD remains at {@code /api/shipment-orders}.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderFacadeResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderFacadeResource.class);

    private final OrderFacadeService orderFacadeService;
    private final TripFacadeService tripFacadeService;
    private final DeliveryFacadeService deliveryFacadeService;
    private final ExceptionFacadeService exceptionFacadeService;

    public OrderFacadeResource(
        OrderFacadeService orderFacadeService,
        TripFacadeService tripFacadeService,
        DeliveryFacadeService deliveryFacadeService,
        ExceptionFacadeService exceptionFacadeService
    ) {
        this.orderFacadeService = orderFacadeService;
        this.tripFacadeService = tripFacadeService;
        this.deliveryFacadeService = deliveryFacadeService;
        this.exceptionFacadeService = exceptionFacadeService;
    }

    @GetMapping("")
    public ResponseEntity<ListPage> getOrders(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) String fromOfficeCode,
        @RequestParam(required = false) String toOfficeCode,
        @RequestParam(required = false) String receiverOfficeCode,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) PaymentTerm paymentTerm,
        @RequestParam(required = false) String createdFrom,
        @RequestParam(required = false) String createdTo,
        @RequestParam(required = false) String routeLabel,
        @RequestParam(required = false) String itineraryLabel,
        Pageable pageable
    ) {
        LOG.debug("REST request to get orders facade list");
        Page<OrderSummaryDTO> page = orderFacadeService.list(
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
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .body(new ListPage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @PostMapping("/cod/mark-exported")
    public Map<String, Object> markCodExported(@Valid @RequestBody MarkCodExportedRequest request) {
        int updated = orderFacadeService.markCodExported(request);
        return Map.of("updated", updated);
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<OrderDetailDTO> getOrder(@PathVariable String orderCode) {
        return ResponseEntity.ok(orderFacadeService.getByCode(orderCode));
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateDraftOrderResponse createDraft(@Valid @RequestBody CreateDraftOrderRequest request) {
        LOG.debug("REST request to create public draft order");
        return orderFacadeService.createDraft(request);
    }

    @PostMapping("")
    public ResponseEntity<OrderSummaryDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) throws URISyntaxException {
        LOG.debug("REST request to create confirmed order");
        OrderSummaryDTO created = orderFacadeService.createConfirmed(request);
        return ResponseEntity.created(new URI("/api/orders/" + created.getOrderCode())).body(created);
    }

    @PatchMapping("/{orderCode}")
    public OrderDetailDTO patchOrder(@PathVariable String orderCode, @RequestBody(required = false) PatchOrderRequest request) {
        return orderFacadeService.patch(orderCode, request != null ? request : new PatchOrderRequest());
    }

    @PostMapping("/{orderCode}/events")
    public OrderDetailDTO logOrderEvent(@PathVariable String orderCode, @Valid @RequestBody LogOrderEventRequest request) {
        return orderFacadeService.logEvent(orderCode, request);
    }

    @PostMapping("/{orderCode}/pickup-start")
    public OrderDetailDTO pickupStart(@PathVariable String orderCode) {
        return orderFacadeService.pickupStart(orderCode);
    }

    @PostMapping("/{orderCode}/warehouse-receive")
    public OrderDetailDTO warehouseReceive(@PathVariable String orderCode) {
        return orderFacadeService.warehouseReceive(orderCode);
    }

    @PostMapping("/{orderCode}/advance-leg")
    public OrderDetailDTO advanceLeg(@PathVariable String orderCode) {
        return orderFacadeService.advanceLeg(orderCode);
    }

    @PostMapping("/{orderCode}/transition")
    public OrderTransitionResponse transition(@PathVariable String orderCode, @Valid @RequestBody OrderTransitionRequest request) {
        return orderFacadeService.transition(orderCode, request);
    }

    @PostMapping("/{orderCode}/restore")
    public OrderTransitionResponse restore(@PathVariable String orderCode) {
        return orderFacadeService.restore(orderCode);
    }

    @PostMapping("/track")
    public TrackOrderResponse track(@Valid @RequestBody TrackOrderRequest request) {
        return orderFacadeService.track(request);
    }

    /**
     * Assign one or many orders to a trip. Path orderCode is included in the batch if not already listed.
     * Note: {@code @Valid} is intentionally omitted so empty {@code orderCodes} can be filled from the path
     * before service validation (otherwise Bean Validation rejects {@code @NotEmpty} too early).
     */
    @PostMapping("/{orderCode}/assign-trip")
    public TripSummaryDTO assignTrip(@PathVariable String orderCode, @RequestBody AssignOrdersToTripRequest request) {
        if (request == null) {
            throw new BadRequestAlertException("Request body required", "order", "bodyRequired");
        }
        if (request.getTripCode() == null || request.getTripCode().isBlank()) {
            throw new BadRequestAlertException("tripCode is required", "order", "tripCodeRequired");
        }
        List<String> codes = new ArrayList<>(request.getOrderCodes() != null ? request.getOrderCodes() : List.of());
        if (codes.stream().noneMatch(c -> orderCode.equalsIgnoreCase(c))) {
            codes.add(orderCode);
        }
        request.setOrderCodes(codes);
        return tripFacadeService.assignOrders(request);
    }

    @PostMapping("/{orderCode}/pod")
    public PodResponse pod(@PathVariable String orderCode, @Valid @RequestBody PodRequest request) {
        return deliveryFacadeService.pod(orderCode, request);
    }

    @PostMapping("/{orderCode}/fail-delivery")
    public FailDeliveryResponse failDelivery(@PathVariable String orderCode, @Valid @RequestBody FailDeliveryRequest request) {
        return deliveryFacadeService.failDelivery(orderCode, request);
    }

    @PostMapping("/{orderCode}/assign-shipper")
    public OrderDetailDTO assignShipper(@PathVariable String orderCode, @RequestBody(required = false) AssignShipperRequest request) {
        return deliveryFacadeService.assignShipper(orderCode, request != null ? request : new AssignShipperRequest());
    }

    @PostMapping("/{orderCode}/payments")
    public OrderDetailDTO addPayment(@PathVariable String orderCode, @Valid @RequestBody AddPaymentRequest request) {
        return deliveryFacadeService.addPayment(orderCode, request);
    }

    @PostMapping("/{orderCode}/return-start")
    public OrderDetailDTO returnStart(@PathVariable String orderCode, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return exceptionFacadeService.startReturn(orderCode, reason);
    }

    @PostMapping("/{orderCode}/return-stage")
    public OrderDetailDTO returnStage(@PathVariable String orderCode, @RequestBody(required = false) Map<String, String> body) {
        ReturnStage stage = parseEnum(ReturnStage.class, body != null ? body.get("returnStage") : null, "returnStage");
        return exceptionFacadeService.setReturnStage(orderCode, stage);
    }

    @PostMapping("/{orderCode}/return-complete")
    public OrderDetailDTO returnComplete(@PathVariable String orderCode) {
        return exceptionFacadeService.completeReturn(orderCode);
    }

    @PostMapping("/{orderCode}/issues")
    public OrderDetailDTO openIssue(@PathVariable String orderCode, @RequestBody(required = false) Map<String, String> body) {
        String rawType = body != null ? body.get("issueType") : null;
        IssueType type = rawType == null || rawType.isBlank() ? IssueType.EXCEPTION : parseEnum(IssueType.class, rawType, "issueType");
        String reason = body != null ? body.get("reason") : null;
        return exceptionFacadeService.openIssue(orderCode, type, reason);
    }

    @PostMapping("/{orderCode}/issues/resolve")
    public OrderDetailDTO resolveIssue(@PathVariable String orderCode, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("resolutionNote") : null;
        return exceptionFacadeService.resolveIssue(orderCode, note);
    }

    @GetMapping("/{orderCode}/issues")
    public java.util.List<OrderDetailDTO.OrderIssueViewDTO> listIssues(@PathVariable String orderCode) {
        return exceptionFacadeService.listIssues(orderCode);
    }

    @GetMapping("/{orderCode}/returns")
    public java.util.List<OrderDetailDTO.OrderReturnViewDTO> listReturns(@PathVariable String orderCode) {
        return exceptionFacadeService.listReturns(orderCode);
    }

    @PostMapping("/{orderCode}/forward-stage")
    public OrderDetailDTO forwardStage(@PathVariable String orderCode, @RequestBody(required = false) Map<String, String> body) {
        ForwardStage stage = parseEnum(ForwardStage.class, body != null ? body.get("forwardStage") : null, "forwardStage");
        return exceptionFacadeService.setForwardStage(orderCode, stage);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestAlertException(field + " is required", "order", "invalid" + capitalize(field));
        }
        try {
            return Enum.valueOf(type, raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestAlertException("Invalid " + field + ": " + raw, "order", "invalid" + capitalize(field));
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public record ListPage(java.util.List<OrderSummaryDTO> content, int page, int size, long totalElements) {}
}
