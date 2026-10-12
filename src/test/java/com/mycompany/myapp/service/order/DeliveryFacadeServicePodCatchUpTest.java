package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.ShipperRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.order.PodRequest;
import com.mycompany.myapp.service.partner.AhamoveDispatchService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class DeliveryFacadeServicePodCatchUpTest {

    private OrderPaymentRepository paymentRepo;
    private OrderFacadeService orderFacade;
    private StaffProfileRepository staffRepo;
    private AhamoveDispatchService ahamove;
    private DeliveryFacadeService service;
    private ShipmentOrder order;

    private static Office office(long id) {
        Office o = new Office();
        o.setId(id);
        return o;
    }

    private static void login(String user, String... roles) {
        List<SimpleGrantedAuthority> auth = java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "x", auth));
    }

    @BeforeEach
    void setUp() {
        ShipmentOrderRepository orderRepo = mock(ShipmentOrderRepository.class);
        orderFacade = mock(OrderFacadeService.class);
        staffRepo = mock(StaffProfileRepository.class);
        ahamove = mock(AhamoveDispatchService.class);
        service = new DeliveryFacadeService(
            orderRepo,
            mock(OrderPodPhotoRepository.class),
            paymentRepo = mock(OrderPaymentRepository.class),
            mock(OrderDeliveryAttemptRepository.class),
            orderFacade,
            mock(DayClosureGuard.class),
            mock(ShipperRepository.class)
        );
        service.setStaffProfileRepository(staffRepo);
        service.setAhamoveDispatchService(ahamove);
        order = new ShipmentOrder();
        order.setId(10L);
        order.setOrderCode("GP0001");
        order.setStatus(OrderStatus.AT_DEST);
        order.setFromOffice(office(1));
        order.setToOffice(office(2));
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        order.setFareAmount(new BigDecimal("50000"));
        order.setPaidAmount(BigDecimal.ZERO);
        when(orderRepo.findOneByOrderCodeOrDraftCode("GP0001")).thenReturn(Optional.of(order));
        StaffProfile shipA = new StaffProfile();
        shipA.setUserLogin("shipa");
        shipA.setActive(true);
        when(staffRepo.findOneByUserLoginIgnoreCase("SHIPA")).thenReturn(Optional.of(shipA));
        login("admin", AuthoritiesConstants.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static PodRequest catchUp(String owner) {
        PodRequest r = new PodRequest();
        r.setChannel("COUNTER");
        r.setActualRecipientName("Khach A");
        r.setCatchUpReason("Quên quét giao khách");
        r.setDebtOwnerUsername(owner);
        r.setCollectedAmount(BigDecimal.ZERO);
        return r;
    }

    @Test
    void catchUpWithoutPhoto_chargesChosenStaffForFullDue() {
        service.pod("GP0001", catchUp("SHIPA"));

        ArgumentCaptor<OrderPayment> pay = ArgumentCaptor.forClass(OrderPayment.class);
        verify(paymentRepo).save(pay.capture());
        assertThat(pay.getValue().getCollectorUsername()).isEqualTo("shipa");
        assertThat(pay.getValue().getAmount()).isEqualByComparingTo("50000");
        assertThat(pay.getValue().getNote()).isEqualTo("POD COUNTER");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("50000");
        ArgumentCaptor<OrderTransitionRequest> tr = ArgumentCaptor.forClass(OrderTransitionRequest.class);
        verify(orderFacade).transition(eq("GP0001"), tr.capture());
        assertThat(tr.getValue().getToStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(tr.getValue().getAction()).isEqualTo("POD_QUAY");
        assertThat(tr.getValue().getDetail()).contains("Giao bù: Quên quét giao khách");
        verify(orderFacade).recordEvent(eq(order), eq(DeliveryFacadeService.POD_OWNER_ACTION), contains("shipa"), eq("shipa"));
    }

    @Test
    void adminPodWithDueAndNoOwner_rejected() {
        PodRequest r = catchUp(null);

        assertThatThrownBy(() -> service.pod("GP0001", r))
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("podDebtOwnerRequired");
        verify(orderFacade, never()).transition(any(), any());
    }

    @Test
    void codOnlyOrder_requiresOwnerEvenWhenFarePaid() {
        order.setPaidAmount(new BigDecimal("50000"));
        order.setCodAmount(new BigDecimal("200000"));

        assertThatThrownBy(() -> service.pod("GP0001", catchUp(null)))
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("podDebtOwnerRequired");
    }

    @Test
    void senderPaysWithCod_ownerEventOnlyNoReceiverPayment() {
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setCodAmount(new BigDecimal("200000"));

        service.pod("GP0001", catchUp("SHIPA"));

        verify(paymentRepo, never()).save(any());
        verify(orderFacade).recordEvent(eq(order), eq(DeliveryFacadeService.POD_OWNER_ACTION), anyString(), eq("shipa"));
    }

    @Test
    void fullyPaid_adminPodWithoutOwnerAllowed() {
        order.setPaidAmount(new BigDecimal("50000"));

        service.pod("GP0001", catchUp(null));

        verify(paymentRepo, never()).save(any());
        verify(orderFacade, never()).recordEvent(any(), eq(DeliveryFacadeService.POD_OWNER_ACTION), any(), any());
        verify(orderFacade).transition(eq("GP0001"), any());
    }

    @Test
    void unknownOwner_rejected() {
        assertThatThrownBy(() -> service.pod("GP0001", catchUp("ghost")))
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("podDebtOwnerInvalid");
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void nonAdminCatchUp_rejected() {
        login("quay1", "ROLE_USER");

        assertThatThrownBy(() -> service.pod("GP0001", catchUp("SHIPA")))
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("podCatchUpAdminOnly");
    }

    @Test
    void noPhotoWithoutCatchUpReason_rejected() {
        PodRequest r = catchUp("SHIPA");
        r.setCatchUpReason(" ");

        assertThatThrownBy(() -> service.pod("GP0001", r))
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("podPhotoRequired");
    }

    @Test
    void activeAhamove_recordsCashFeeForHandover() {
        order.setPaidAmount(new BigDecimal("50000"));
        order.setPartnerCode("AHAMOVE");
        order.setPartnerOrderId("AHA1");
        order.setPartnerStatus("IN PROCESS");
        PodRequest r = catchUp(null);
        r.setChannel("HOME");

        service.pod("GP0001", r);

        verify(ahamove).recordCashFeeIfPickedUp(order, "COMPLETED");
    }

    @Test
    void cancelledAhamove_noCashFee() {
        order.setPaidAmount(new BigDecimal("50000"));
        order.setPartnerCode("AHAMOVE");
        order.setPartnerOrderId("AHA1");
        order.setPartnerStatus("CANCELLED");

        service.pod("GP0001", catchUp(null));

        verify(ahamove, never()).recordCashFeeIfPickedUp(any(), any());
    }
}
