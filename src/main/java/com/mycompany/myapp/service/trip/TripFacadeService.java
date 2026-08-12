package com.mycompany.myapp.service.trip;

import com.mycompany.myapp.domain.Driver;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.TripOrderAssignment;
import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.domain.enumeration.AssignmentStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.TripStatus;
import com.mycompany.myapp.repository.DriverRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.RouteRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.TripOrderAssignmentRepository;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.repository.VehicleRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.trip.AssignOrdersToTripRequest;
import com.mycompany.myapp.service.dto.trip.CloseTripRequest;
import com.mycompany.myapp.service.dto.trip.CreateTripRequest;
import com.mycompany.myapp.service.dto.trip.HandoverRequest;
import com.mycompany.myapp.service.dto.trip.ScanInRequest;
import com.mycompany.myapp.service.dto.trip.ScanOutRequest;
import com.mycompany.myapp.service.dto.trip.TripSummaryDTO;
import com.mycompany.myapp.service.dto.trip.TripTransitionRequest;
import com.mycompany.myapp.service.dto.trip.TripTransitionResponse;
import com.mycompany.myapp.service.order.OrderFacadeService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TripFacadeService {

    private static final String ENTITY = "trip";

    private final TripRepository tripRepository;
    private final TripOrderAssignmentRepository assignmentRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OfficeRepository officeRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final OrderEventRepository orderEventRepository;
    private final TripCodeGenerator tripCodeGenerator;
    private final OrderFacadeService orderFacadeService;
    private final StaffAccessService staffAccessService;

    public TripFacadeService(
        TripRepository tripRepository,
        TripOrderAssignmentRepository assignmentRepository,
        ShipmentOrderRepository shipmentOrderRepository,
        OfficeRepository officeRepository,
        RouteRepository routeRepository,
        VehicleRepository vehicleRepository,
        DriverRepository driverRepository,
        OrderEventRepository orderEventRepository,
        TripCodeGenerator tripCodeGenerator,
        OrderFacadeService orderFacadeService,
        StaffAccessService staffAccessService
    ) {
        this.tripRepository = tripRepository;
        this.assignmentRepository = assignmentRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.officeRepository = officeRepository;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.orderEventRepository = orderEventRepository;
        this.tripCodeGenerator = tripCodeGenerator;
        this.orderFacadeService = orderFacadeService;
        this.staffAccessService = staffAccessService;
    }

    @Transactional(readOnly = true)
    public Page<TripSummaryDTO> list(TripStatus status, String officeCode, String routeCode, String keyword, Pageable pageable) {
        Specification<Trip> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (officeCode != null && !officeCode.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("office").get("code"), officeCode.trim().toUpperCase()));
        }
        if (routeCode != null && !routeCode.isBlank()) {
            String rc = routeCode.trim();
            spec = spec.and((root, q, cb) ->
                cb.or(cb.equal(root.get("route").get("code"), rc.toUpperCase()), cb.equal(root.get("route").get("name"), rc))
            );
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("tripCode")), like),
                    cb.like(cb.lower(root.get("vehicle").get("plateNumber")), like),
                    cb.like(cb.lower(root.get("driver").get("fullName")), like)
                )
            );
        }
        if (officeCode == null || officeCode.isBlank()) {
            String scoped = staffAccessService.scopedOfficeCode().orElse(null);
            if (scoped != null) {
                spec = spec.and((root, q, cb) -> cb.equal(root.get("office").get("code"), scoped));
            }
        }
        return tripRepository.findAll(spec, pageable).map(t -> toSummary(t, false));
    }

    @Transactional(readOnly = true)
    public TripSummaryDTO getByCode(String tripCode) {
        return toSummary(requireTrip(tripCode), true);
    }

    public TripSummaryDTO create(CreateTripRequest req) {
        Office office = requireOffice(req.getOfficeCode());
        Route route = resolveRoute(req.getRouteCode());
        Vehicle vehicle = resolveVehicle(req);
        Driver driver = resolveDriver(req);

        Trip trip = new Trip();
        trip.setTripCode(tripCodeGenerator.nextTripCode(office.getCode()));
        trip.setStatus(TripStatus.CREATED);
        trip.setDepartAt(req.getDepartAt());
        trip.setLoadedCount(0);
        trip.setScannedCount(0);
        trip.setForceClosed(false);
        trip.setOffice(office);
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip = tripRepository.save(trip);
        return toSummary(trip, false);
    }

    public TripTransitionResponse transition(String tripCode, TripTransitionRequest req) {
        Trip trip = requireTrip(tripCode);
        if (!TripStatusTransitions.canTransition(trip.getStatus(), req.getToStatus())) {
            throw new BadRequestAlertException(
                "Invalid trip transition " + trip.getStatus() + " -> " + req.getToStatus(),
                ENTITY,
                "invalidtriptransition"
            );
        }
        trip.setStatus(req.getToStatus());
        if (req.getToStatus() == TripStatus.CLOSED) {
            trip.setClosedAt(Instant.now());
        }
        tripRepository.save(trip);
        return new TripTransitionResponse(true, trip.getStatus(), trip.getTripCode());
    }

    public TripSummaryDTO assignOrders(AssignOrdersToTripRequest req) {
        Trip trip = requireTrip(req.getTripCode());
        if (trip.getStatus() == TripStatus.CLOSED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new BadRequestAlertException("Cannot assign to closed/cancelled trip", ENTITY, "tripclosed");
        }
        for (String code : distinct(req.getOrderCodes())) {
            ShipmentOrder order = requireOrder(code);
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
                throw new BadRequestAlertException("Order not assignable: " + code, ENTITY, "orderNotAssignable");
            }
            ensureActiveAssignment(trip, order);
            order.setCurrentTrip(trip);
            shipmentOrderRepository.save(order);
            appendOrderEvent(order, "ASSIGN_TRIP", "Trip " + trip.getTripCode(), currentActor());
        }
        refreshCounts(trip);
        return toSummary(trip, true);
    }

    public TripSummaryDTO scanOut(String tripCode, ScanOutRequest req) {
        String mode = req.getMode() == null ? "ADD" : req.getMode().trim().toUpperCase();
        if ("REMOVE".equals(mode)) {
            return removeScanOut(tripCode, req.getOrderCode());
        }
        Trip trip = requireTrip(tripCode);
        if (trip.getStatus() != TripStatus.CREATED && trip.getStatus() != TripStatus.LOADING) {
            throw new BadRequestAlertException("Scan-out only in CREATED/LOADING", ENTITY, "scanOutStatus");
        }
        if (trip.getStatus() == TripStatus.CREATED) {
            trip.setStatus(TripStatus.LOADING);
        }
        ShipmentOrder order = requireOrder(req.getOrderCode());
        TripOrderAssignment assignment = ensureActiveAssignment(trip, order);
        Instant now = Instant.now();
        assignment.setScannedAt(now);
        assignment.setLoadedAt(now);
        assignment.setAssignmentStatus(AssignmentStatus.LOADED);
        assignment.setRemovedAt(null);
        assignmentRepository.save(assignment);

        order.setCurrentTrip(trip);
        shipmentOrderRepository.save(order);

        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.WAITING) {
            OrderTransitionRequest tr = new OrderTransitionRequest();
            tr.setToStatus(OrderStatus.IN_TRANSIT);
            tr.setAction("SCAN_OUT");
            tr.setDetail("Trip " + trip.getTripCode());
            orderFacadeService.transition(order.getOrderCode(), tr);
        } else if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            appendOrderEvent(order, "SCAN_OUT", "Trip " + trip.getTripCode(), currentActor());
        }

        refreshCounts(trip);
        tripRepository.save(trip);
        return toSummary(trip, true);
    }

    public TripSummaryDTO removeScanOut(String tripCode, String orderCode) {
        Trip trip = requireTrip(tripCode);
        ShipmentOrder order = requireOrder(orderCode);
        TripOrderAssignment assignment = assignmentRepository
            .findFirstByTrip_IdAndOrder_IdAndAssignmentStatusNot(trip.getId(), order.getId(), AssignmentStatus.REMOVED)
            .orElseThrow(() -> new BadRequestAlertException("Assignment not found", ENTITY, "assignmentNotFound"));
        assignment.setAssignmentStatus(AssignmentStatus.REMOVED);
        assignment.setRemovedAt(Instant.now());
        assignmentRepository.save(assignment);

        if (order.getCurrentTrip() != null && order.getCurrentTrip().getId().equals(trip.getId())) {
            order.setCurrentTrip(null);
            shipmentOrderRepository.save(order);
        }
        if (order.getStatus() == OrderStatus.IN_TRANSIT) {
            OrderTransitionRequest tr = new OrderTransitionRequest();
            tr.setToStatus(OrderStatus.WAITING);
            tr.setAction("SCAN_REMOVE");
            tr.setDetail("Trip " + trip.getTripCode());
            orderFacadeService.transition(order.getOrderCode(), tr);
        } else {
            appendOrderEvent(order, "SCAN_REMOVE", "Trip " + trip.getTripCode(), currentActor());
        }
        refreshCounts(trip);
        tripRepository.save(trip);
        return toSummary(trip, true);
    }

    public TripSummaryDTO handover(String tripCode, HandoverRequest req) {
        Trip trip = requireTrip(tripCode);
        Instant now = Instant.now();
        for (String code : distinct(req.getOrderCodes())) {
            ShipmentOrder order = requireOrder(code);
            TripOrderAssignment assignment = ensureActiveAssignment(trip, order);
            assignment.setAssignmentStatus(AssignmentStatus.LOADED);
            assignment.setLoadedAt(now);
            if (assignment.getScannedAt() == null) {
                assignment.setScannedAt(now);
            }
            assignmentRepository.save(assignment);
            order.setCurrentTrip(trip);
            shipmentOrderRepository.save(order);
            appendOrderEvent(order, "HANDOVER", "Trip " + trip.getTripCode(), currentActor());
        }
        refreshCounts(trip);
        tripRepository.save(trip);
        return toSummary(trip, true);
    }

    public TripSummaryDTO scanIn(String tripCodeOrNull, ScanInRequest req) {
        ShipmentOrder order = requireOrder(req.getOrderCode());
        Trip trip = null;
        if (tripCodeOrNull != null && !tripCodeOrNull.isBlank() && !"me".equalsIgnoreCase(tripCodeOrNull)) {
            trip = requireTrip(tripCodeOrNull);
        } else if (order.getCurrentTrip() != null) {
            trip = order.getCurrentTrip();
        }

        String officeCode = req.getOfficeCode() != null ? req.getOfficeCode().trim().toUpperCase() : null;
        if (officeCode != null) {
            String dest = order.getToOffice() != null ? order.getToOffice().getCode() : null;
            String hub = order.getHubOffice() != null ? order.getHubOffice().getCode() : null;
            boolean okOffice = officeCode.equals(dest) || officeCode.equals(hub);
            if (!okOffice && !req.isOverrideWrongOffice()) {
                throw new BadRequestAlertException("Wrong office for order (E-VP-001)", ENTITY, "wrongOffice");
            }
        }

        if (trip != null && trip.getStatus() == TripStatus.DEPARTED) {
            trip.setStatus(TripStatus.UNLOADING);
            tripRepository.save(trip);
        }

        boolean hubIn =
            order.getHubOffice() != null &&
            officeCode != null &&
            officeCode.equals(order.getHubOffice().getCode()) &&
            order.getToOffice() != null &&
            !officeCode.equals(order.getToOffice().getCode());

        if (hubIn) {
            appendOrderEvent(order, "HUB_IN", "Hub " + officeCode, currentActor());
        } else if (order.getStatus() == OrderStatus.IN_TRANSIT || order.getStatus() == OrderStatus.WAITING) {
            OrderTransitionRequest tr = new OrderTransitionRequest();
            tr.setToStatus(OrderStatus.AT_DEST);
            tr.setAction("SCAN_IN");
            tr.setDetail(officeCode != null ? "VP " + officeCode : "SCAN_IN");
            orderFacadeService.transition(order.getOrderCode(), tr);
        }

        if (req.getShelfNumber() != null) {
            // reload after possible transition
            order = requireOrder(req.getOrderCode());
            order.setShelfNumber(req.getShelfNumber());
            shipmentOrderRepository.save(order);
        }

        return trip != null ? toSummary(requireTrip(trip.getTripCode()), true) : toSummaryPlaceholder(order);
    }

    public TripSummaryDTO close(String tripCode, CloseTripRequest req) {
        Trip trip = requireTrip(tripCode);
        boolean force = req != null && req.isForce();
        if (force) {
            staffAccessService.requireForceCloseRole();
        }
        if (trip.getStatus() == TripStatus.UNLOADING) {
            trip.setStatus(TripStatus.CLOSED);
        } else if (
            force &&
            (trip.getStatus() == TripStatus.LOADING || trip.getStatus() == TripStatus.DEPARTED || trip.getStatus() == TripStatus.CREATED)
        ) {
            trip.setForceClosed(true);
            trip.setForceCloseReason(req.getReason());
            trip.setStatus(TripStatus.CLOSED);
        } else {
            throw new BadRequestAlertException("Close requires UNLOADING (or force from CREATED/LOADING/DEPARTED)", ENTITY, "closeInvalid");
        }
        trip.setClosedAt(Instant.now());
        tripRepository.save(trip);
        return toSummary(trip, true);
    }

    private TripSummaryDTO toSummaryPlaceholder(ShipmentOrder order) {
        TripSummaryDTO dto = new TripSummaryDTO();
        dto.setTripCode(order.getCurrentTrip() != null ? order.getCurrentTrip().getTripCode() : null);
        dto.setStatus(order.getCurrentTrip() != null ? order.getCurrentTrip().getStatus() : null);
        return dto;
    }

    private TripOrderAssignment ensureActiveAssignment(Trip trip, ShipmentOrder order) {
        return assignmentRepository
            .findFirstByTrip_IdAndOrder_IdAndAssignmentStatusNot(trip.getId(), order.getId(), AssignmentStatus.REMOVED)
            .orElseGet(() -> {
                TripOrderAssignment a = new TripOrderAssignment();
                a.setTrip(trip);
                a.setOrder(order);
                a.setAssignmentStatus(AssignmentStatus.SCANNED);
                return assignmentRepository.save(a);
            });
    }

    private void refreshCounts(Trip trip) {
        long loaded = assignmentRepository.countByTrip_IdAndAssignmentStatus(trip.getId(), AssignmentStatus.LOADED);
        long scanned = assignmentRepository.countByTrip_IdAndScannedAtIsNotNullAndAssignmentStatusNot(
            trip.getId(),
            AssignmentStatus.REMOVED
        );
        trip.setLoadedCount((int) loaded);
        trip.setScannedCount((int) scanned);
    }

    private TripSummaryDTO toSummary(Trip trip, boolean withAssignments) {
        TripSummaryDTO dto = new TripSummaryDTO();
        dto.setId(trip.getId());
        dto.setTripCode(trip.getTripCode());
        dto.setStatus(trip.getStatus());
        dto.setDepartAt(trip.getDepartAt());
        dto.setClosedAt(trip.getClosedAt());
        dto.setForceClosed(trip.getForceClosed());
        dto.setForceCloseReason(trip.getForceCloseReason());
        dto.setLoadedCount(trip.getLoadedCount());
        dto.setScannedCount(trip.getScannedCount());
        dto.setOfficeCode(trip.getOffice() != null ? trip.getOffice().getCode() : null);
        if (trip.getRoute() != null) {
            dto.setRouteCode(trip.getRoute().getCode());
            dto.setRouteName(trip.getRoute().getName());
        }
        if (trip.getVehicle() != null) {
            dto.setVehiclePlate(trip.getVehicle().getPlateNumber());
        }
        if (trip.getDriver() != null) {
            dto.setDriverName(trip.getDriver().getFullName());
            dto.setDriverCode(trip.getDriver().getDriverCode());
        }
        if (withAssignments) {
            List<TripSummaryDTO.AssignmentView> views = new ArrayList<>();
            for (TripOrderAssignment a : assignmentRepository.findActiveByTripId(trip.getId())) {
                TripSummaryDTO.AssignmentView v = new TripSummaryDTO.AssignmentView();
                v.setOrderCode(a.getOrder() != null ? a.getOrder().getOrderCode() : null);
                v.setAssignmentStatus(a.getAssignmentStatus());
                v.setScannedAt(a.getScannedAt());
                v.setLoadedAt(a.getLoadedAt());
                views.add(v);
            }
            dto.setAssignments(views);
        }
        return dto;
    }

    private Trip requireTrip(String tripCode) {
        return tripRepository
            .findOneByTripCode(tripCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripCode));
    }

    private ShipmentOrder requireOrder(String code) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(code.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + code));
    }

    private Office requireOffice(String code) {
        return officeRepository
            .findOneByCode(code.trim().toUpperCase())
            .orElseThrow(() -> new BadRequestAlertException("Unknown office: " + code, ENTITY, "officeNotFound"));
    }

    private Route resolveRoute(String routeCodeOrName) {
        String raw = routeCodeOrName.trim();
        return routeRepository
            .findOneByCode(raw.toUpperCase())
            .or(() -> routeRepository.findFirstByNameIgnoreCase(raw))
            .or(() -> routeRepository.findOneByCode(raw.replace(" → ", "-").replace("->", "-").replace(" ", "").toUpperCase()))
            .orElseThrow(() -> new BadRequestAlertException("Unknown route: " + routeCodeOrName, ENTITY, "routeNotFound"));
    }

    private Vehicle resolveVehicle(CreateTripRequest req) {
        if (req.getVehicleId() != null) {
            return vehicleRepository
                .findById(req.getVehicleId())
                .orElseThrow(() -> new BadRequestAlertException("Vehicle not found", ENTITY, "vehicleNotFound"));
        }
        if (req.getVehiclePlate() != null && !req.getVehiclePlate().isBlank()) {
            return vehicleRepository
                .findOneByPlateNumber(req.getVehiclePlate().trim())
                .orElseThrow(() -> new BadRequestAlertException("Vehicle plate not found", ENTITY, "vehicleNotFound"));
        }
        throw new BadRequestAlertException("vehicleId or vehiclePlate required", ENTITY, "vehicleRequired");
    }

    private Driver resolveDriver(CreateTripRequest req) {
        if (req.getDriverId() != null) {
            return driverRepository
                .findById(req.getDriverId())
                .orElseThrow(() -> new BadRequestAlertException("Driver not found", ENTITY, "driverNotFound"));
        }
        if (req.getDriverCode() != null && !req.getDriverCode().isBlank()) {
            return driverRepository
                .findOneByDriverCode(req.getDriverCode().trim())
                .orElseThrow(() -> new BadRequestAlertException("Driver code not found", ENTITY, "driverNotFound"));
        }
        if (req.getDriverName() != null && !req.getDriverName().isBlank()) {
            return driverRepository
                .findFirstByFullNameIgnoreCase(req.getDriverName().trim())
                .orElseThrow(() -> new BadRequestAlertException("Driver name not found", ENTITY, "driverNotFound"));
        }
        throw new BadRequestAlertException("driverId, driverCode or driverName required", ENTITY, "driverRequired");
    }

    private void appendOrderEvent(ShipmentOrder order, String action, String detail, String actor) {
        OrderEvent event = new OrderEvent();
        event.setEventAt(Instant.now());
        event.setAction(action);
        event.setDetail(detail);
        event.setActorUsername(actor == null ? "system" : actor);
        event.setOrder(order);
        orderEventRepository.save(event);
    }

    private static List<String> distinct(List<String> codes) {
        Set<String> set = new LinkedHashSet<>();
        for (String c : codes) {
            if (c != null && !c.isBlank()) {
                set.add(c.trim());
            }
        }
        return List.copyOf(set);
    }

    private static String currentActor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }
}
