package com.mycompany.myapp.service.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.TripOrderAssignment;
import com.mycompany.myapp.domain.enumeration.AssignmentStatus;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.TripStatus;
import com.mycompany.myapp.repository.*;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.audit.AuditRecorder;
import com.mycompany.myapp.service.dto.trip.AssignOrdersToTripRequest;
import com.mycompany.myapp.service.order.OrderFacadeService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TripReassignTest {

    private TripRepository tripRepository;
    private TripOrderAssignmentRepository assignmentRepository;
    private ShipmentOrderRepository shipmentOrderRepository;
    private OrderEventRepository orderEventRepository;
    private TripFacadeService service;

    private Trip tripA;
    private Trip tripB;
    private ShipmentOrder order;
    private TripOrderAssignment oldAssignment;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        assignmentRepository = mock(TripOrderAssignmentRepository.class);
        shipmentOrderRepository = mock(ShipmentOrderRepository.class);
        orderEventRepository = mock(OrderEventRepository.class);
        service = new TripFacadeService(
            tripRepository,
            assignmentRepository,
            shipmentOrderRepository,
            mock(OfficeRepository.class),
            mock(RouteRepository.class),
            mock(VehicleRepository.class),
            mock(DriverRepository.class),
            orderEventRepository,
            mock(TripCodeGenerator.class),
            mock(OrderFacadeService.class),
            mock(StaffAccessService.class),
            mock(AuditRecorder.class)
        );

        tripA = trip(1L, "TRIP-A");
        tripB = trip(2L, "TRIP-B");
        order = new ShipmentOrder();
        order.setId(10L);
        order.setOrderCode("ORD1");
        order.setStatus(OrderStatus.WAITING);
        order.setForwardStage(ForwardStage.TRANSFER_PENDING);
        order.setCurrentTrip(tripA);
        oldAssignment = new TripOrderAssignment();
        oldAssignment.setTrip(tripA);
        oldAssignment.setOrder(order);
        oldAssignment.setAssignmentStatus(AssignmentStatus.SCANNED);

        lenient().when(tripRepository.findOneByTripCode("TRIP-B")).thenReturn(Optional.of(tripB));
        lenient().when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("ORD1")).thenReturn(Optional.of(order));
        lenient()
            .when(assignmentRepository.findFirstByTrip_IdAndOrder_IdAndAssignmentStatusNot(1L, 10L, AssignmentStatus.REMOVED))
            .thenReturn(Optional.of(oldAssignment));
        lenient()
            .when(assignmentRepository.findFirstByTrip_IdAndOrder_IdAndAssignmentStatusNot(2L, 10L, AssignmentStatus.REMOVED))
            .thenReturn(Optional.empty());
        lenient().when(assignmentRepository.save(any(TripOrderAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Trip trip(Long id, String code) {
        Trip t = new Trip();
        t.setId(id);
        t.setTripCode(code);
        t.setStatus(TripStatus.CREATED);
        return t;
    }

    private AssignOrdersToTripRequest req() {
        AssignOrdersToTripRequest r = new AssignOrdersToTripRequest();
        r.setTripCode("TRIP-B");
        r.setOrderCodes(List.of("ORD1"));
        return r;
    }

    @Test
    void movesWaitingOrderAndRemovesOldAssignment() {
        service.assignOrders(req());

        assertThat(order.getCurrentTrip()).isSameAs(tripB);
        assertThat(order.getForwardStage()).isEqualTo(ForwardStage.TRANSFER_PENDING);
        assertThat(oldAssignment.getAssignmentStatus()).isEqualTo(AssignmentStatus.REMOVED);
        verify(tripRepository).save(tripA);
        ArgumentCaptor<OrderEvent> ev = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository).save(ev.capture());
        assertThat(ev.getValue().getAction()).isEqualTo("REASSIGN_TRIP");
        assertThat(ev.getValue().getDetail()).contains("TRIP-A").contains("TRIP-B");
    }

    @Test
    void rejectsOrderAlreadyLoadedOnTruck() {
        order.setStatus(OrderStatus.IN_TRANSIT);
        order.setForwardStage(ForwardStage.TRANSFERRING);

        assertThatThrownBy(() -> service.assignOrders(req())).isInstanceOf(BadRequestAlertException.class);
        assertThat(order.getCurrentTrip()).isSameAs(tripA);
        verify(tripRepository, never()).save(eq(tripA));
    }
}
