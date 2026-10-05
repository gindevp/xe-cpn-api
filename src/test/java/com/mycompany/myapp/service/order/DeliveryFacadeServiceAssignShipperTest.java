package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Shipper;
import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.ShipperRepository;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.AssignShipperRequest;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DeliveryFacadeServiceAssignShipperTest {

    private ShipmentOrderRepository orderRepo;
    private OrderDeliveryAttemptRepository attemptRepo;
    private OrderFacadeService orderFacade;
    private DayClosureGuard dayClosureGuard;
    private ShipperRepository shipperRepo;
    private DeliveryFacadeService service;
    private ShipmentOrder order;

    private static Office office(long id) {
        Office o = new Office();
        o.setId(id);
        return o;
    }

    private static Shipper shipper(long id, long officeId, boolean active) {
        Shipper s = new Shipper();
        s.setId(id);
        s.setFullName("Nguyen Van A");
        s.setPhone("0901000001");
        s.setOffice(office(officeId));
        s.setActive(active);
        return s;
    }

    @BeforeEach
    void setUp() {
        orderRepo = mock(ShipmentOrderRepository.class);
        attemptRepo = mock(OrderDeliveryAttemptRepository.class);
        orderFacade = mock(OrderFacadeService.class);
        dayClosureGuard = mock(DayClosureGuard.class);
        shipperRepo = mock(ShipperRepository.class);
        service = new DeliveryFacadeService(
            orderRepo,
            mock(OrderPodPhotoRepository.class),
            mock(OrderPaymentRepository.class),
            attemptRepo,
            orderFacade,
            dayClosureGuard,
            shipperRepo
        );
        order = new ShipmentOrder();
        order.setId(10L);
        order.setOrderCode("GP0001");
        order.setStatus(OrderStatus.AT_DEST);
        order.setFromOffice(office(1));
        order.setToOffice(office(2));
        when(orderRepo.findOneByOrderCodeOrDraftCode("GP0001")).thenReturn(Optional.of(order));
    }

    private AssignShipperRequest internal(Long shipperId) {
        AssignShipperRequest r = new AssignShipperRequest();
        r.setMode("INTERNAL");
        r.setShipperId(shipperId);
        r.setNote("Gọi trước 15p");
        return r;
    }

    @Test
    void internalShipperOfDestinationOfficeIsAssigned() {
        when(shipperRepo.findById(5L)).thenReturn(Optional.of(shipper(5, 2, true)));

        service.assignShipper("GP0001", internal(5L));

        assertThat(order.getShipper()).isNotNull();
        assertThat(order.getShipper().getId()).isEqualTo(5L);
        ArgumentCaptor<OrderTransitionRequest> tr = ArgumentCaptor.forClass(OrderTransitionRequest.class);
        verify(orderFacade).transition(eq("GP0001"), tr.capture());
        assertThat(tr.getValue().getToStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(tr.getValue().getAction()).isEqualTo("TAKE_JOB");
        assertThat(tr.getValue().getDetail()).contains("Nguyen Van A").contains("0901000001").contains("Gọi trước 15p");
        ArgumentCaptor<OrderDeliveryAttempt> at = ArgumentCaptor.forClass(OrderDeliveryAttempt.class);
        verify(attemptRepo).save(at.capture());
        assertThat(at.getValue().getReason()).contains("Nguyen Van A");
    }

    @Test
    void finalToOfficeIsTheReceivingOffice() {
        order.setFinalToOffice(office(3));
        when(shipperRepo.findById(5L)).thenReturn(Optional.of(shipper(5, 2, true)));

        assertThatThrownBy(() -> service.assignShipper("GP0001", internal(5L)))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("shipperOfficeMismatch");
        verify(orderFacade, never()).transition(any(), any());
    }

    @Test
    void shipperOfOtherOfficeIsRejected() {
        when(shipperRepo.findById(5L)).thenReturn(Optional.of(shipper(5, 9, true)));

        assertThatThrownBy(() -> service.assignShipper("GP0001", internal(5L)))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("shipperOfficeMismatch");
        assertThat(order.getShipper()).isNull();
    }

    @Test
    void inactiveShipperIsRejected() {
        when(shipperRepo.findById(5L)).thenReturn(Optional.of(shipper(5, 2, false)));

        assertThatThrownBy(() -> service.assignShipper("GP0001", internal(5L)))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("shipperInactive");
    }

    @Test
    void wrongStatusIsRejectedBeforeShipperLookup() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);

        assertThatThrownBy(() -> service.assignShipper("GP0001", internal(5L))).isInstanceOf(BadRequestAlertException.class);
        verify(shipperRepo, never()).findById(any());
    }

    @Test
    void assignBlockedUntilAhamoveAdvanceRefunded() {
        order.setStatus(OrderStatus.FAILED_DELIVERY);
        order.setPartnerCodAmount(new java.math.BigDecimal("30000"));
        order.setPartnerCodCollectedAt(java.time.Instant.now());

        assertThatThrownBy(() -> service.assignShipper("GP0001", internal(5L)))
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("partnerAdvanceRefundDue");
        verify(orderFacade, never()).transition(any(), any());
    }

    @Test
    void manualPaymentBlockedWhileAdvancePending() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setFareAmount(new java.math.BigDecimal("50000"));
        order.setPaidAmount(java.math.BigDecimal.ZERO);
        order.setPartnerCodAmount(new java.math.BigDecimal("50000"));
        com.mycompany.myapp.service.dto.order.AddPaymentRequest pay = new com.mycompany.myapp.service.dto.order.AddPaymentRequest();
        pay.setAmount(new java.math.BigDecimal("50000"));
        pay.setMethod(com.mycompany.myapp.domain.enumeration.PaymentMethod.TM);

        assertThatThrownBy(() -> service.addPayment("GP0001", pay))
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("partnerAdvancePending");
    }

    @Test
    void recordPartnerAdvanceMarksCollectedAndAddsPayment() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setFareAmount(new java.math.BigDecimal("50000"));
        order.setPaidAmount(java.math.BigDecimal.ZERO);
        order.setPartnerCodAmount(new java.math.BigDecimal("50000"));

        service.recordPartnerAdvance(order, new java.math.BigDecimal("50000"), "POD AHAMOVE ỨNG");

        assertThat(order.getPartnerCodCollectedAt()).isNotNull();
        assertThat(order.getPaidAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void internalWithoutShipperIdKeepsLegacyTakeJob() {
        service.assignShipper("GP0001", new AssignShipperRequest());

        assertThat(order.getShipper()).isNull();
        ArgumentCaptor<OrderTransitionRequest> tr = ArgumentCaptor.forClass(OrderTransitionRequest.class);
        verify(orderFacade).transition(eq("GP0001"), tr.capture());
        assertThat(tr.getValue().getDetail()).isEqualTo("Internal shipper");
    }

    @Test
    void partnerAssignClearsInternalShipper() {
        order.setShipper(shipper(5, 2, true));
        AssignShipperRequest r = new AssignShipperRequest();
        r.setMode("PARTNER");
        r.setPartner(DeliveryPartner.AHAMOVE);

        service.assignShipper("GP0001", r);

        assertThat(order.getShipper()).isNull();
    }
}
