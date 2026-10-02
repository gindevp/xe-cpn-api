package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.ChangePaymentTermRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderFacadeServicePaymentTermTest {

    @Mock
    private ShipmentOrderRepository shipmentOrderRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private OrderPodPhotoRepository orderPodPhotoRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private OrderCodeGenerator orderCodeGenerator;

    @Mock
    private SimpleFareCalculator fareCalculator;

    @Mock
    private StaffAccessService staffAccessService;

    @Mock
    private OrderLegRepository orderLegRepository;

    @Mock
    private DayClosureGuard dayClosureGuard;

    @Mock
    private OrderIssueRepository orderIssueRepository;

    @Mock
    private DraftExpiryService draftExpiryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    @Mock
    private ReceiptOrderLineRepository receiptOrderLineRepository;

    private OrderFacadeService service;
    private ShipmentOrder order;

    @BeforeEach
    void setUp() {
        service = new OrderFacadeService(
            shipmentOrderRepository,
            orderEventRepository,
            orderPodPhotoRepository,
            officeRepository,
            orderCodeGenerator,
            fareCalculator,
            staffAccessService,
            orderLegRepository,
            dayClosureGuard,
            orderIssueRepository,
            draftExpiryService,
            eventPublisher
        );
        service.setOrderPaymentRepository(orderPaymentRepository);
        service.setReceiptOrderLineRepository(receiptOrderLineRepository);
        order = new ShipmentOrder();
        order.setId(1L);
        order.setOrderCode("PT3009TERM");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setFareAmount(new BigDecimal("30000"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setFromOffice(office("VP_PT"));
        order.setToOffice(office("VP_HD"));
        lenient().when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("PT3009TERM")).thenReturn(Optional.of(order));
    }

    private static Office office(String code) {
        Office o = new Office();
        o.setCode(code);
        return o;
    }

    private void asDispatcher(String officeCode) {
        StaffProfile p = new StaffProfile();
        p.setRoleCode(RoleCode.DH);
        when(staffAccessService.isSystemAdmin()).thenReturn(false);
        when(staffAccessService.current()).thenReturn(Optional.of(p));
        lenient().when(staffAccessService.scopedOfficeCode()).thenReturn(Optional.of(officeCode));
    }

    private void asCounter(String officeCode) {
        StaffProfile p = new StaffProfile();
        p.setRoleCode(RoleCode.Q);
        when(staffAccessService.isSystemAdmin()).thenReturn(false);
        when(staffAccessService.current()).thenReturn(Optional.of(p));
        lenient().when(staffAccessService.scopedOfficeCode()).thenReturn(Optional.of(officeCode));
    }

    @Test
    void counter_waitingPickup_switches() {
        asCounter("VP_PT");
        order.setStatus(OrderStatus.DRAFT);
        order.setHomePickup(true);

        service.changePaymentTerm("PT3009TERM", req("NHAN_TRA"));

        assertThat(order.getPaymentTerm()).isEqualTo(PaymentTerm.NHAN_TRA);
    }

    @Test
    void counter_shipperPicking_isBlocked() {
        asCounter("VP_PT");
        order.setStatus(OrderStatus.DRAFT);
        order.setHomePickup(true);
        order.setPickupStaffUsername("ship1");

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermCounterStage");
    }

    @Test
    void counter_senderWarehouse_afterCollection_reverses() {
        asCounter("VP_PT");
        order.setPickedUpAt(Instant.now());
        order.setForwardStage(ForwardStage.WH_IN);
        order.setPaidAmount(new BigDecimal("30000"));

        service.changePaymentTerm("PT3009TERM", req("CONG_NO"));

        ArgumentCaptor<OrderPayment> pay = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(pay.capture());
        assertThat(pay.getValue().getAmount()).isEqualByComparingTo("-30000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(order.getOnCredit()).isTrue();
    }

    @Test
    void counter_assignedToVehicle_isBlocked() {
        asCounter("VP_PT");
        order.setPickedUpAt(Instant.now());
        order.setForwardStage(ForwardStage.WH_IN);
        order.setCurrentTrip(new Trip());

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermCounterStage");
    }

    @Test
    void counter_receiverOffice_isForbidden() {
        asCounter("VP_HD");
        order.setForwardStage(ForwardStage.WH_IN);

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA"))).isInstanceOf(ResponseStatusException.class);
    }

    private void asAdmin() {
        when(staffAccessService.isSystemAdmin()).thenReturn(true);
    }

    private static ChangePaymentTermRequest req(String method) {
        return new ChangePaymentTermRequest(method, "Khách đổi ý");
    }

    private static String errorKey(Throwable ex) {
        return ((BadRequestAlertException) ex).getErrorKey();
    }

    @Test
    void dispatcher_beforeAnyMoney_switchesAndLogsEvent() {
        asDispatcher("VP_PT");

        service.changePaymentTerm("PT3009TERM", req("NHAN_TRA"));

        assertThat(order.getPaymentTerm()).isEqualTo(PaymentTerm.NHAN_TRA);
        assertThat(order.getOnCredit()).isFalse();
        verify(orderPaymentRepository, never()).save(any());
        ArgumentCaptor<OrderEvent> ev = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository).save(ev.capture());
        assertThat(ev.getValue().getAction()).isEqualTo("PAYMENT_TERM_CHANGE");
        assertThat(ev.getValue().getDetail()).contains("Người gửi trả → Người nhận trả").contains("Khách đổi ý");
    }

    @Test
    void dispatcher_afterCollection_assignedToVehicle_isBlocked() {
        asDispatcher("VP_PT");
        order.setForwardStage(ForwardStage.TRANSFER_PENDING);
        order.setPaidAmount(new BigDecimal("30000"));

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA")))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermCollected");
    }

    @Test
    void senderDispatcher_atSenderWarehouse_afterCollection_reverses() {
        asDispatcher("VP_PT");
        order.setForwardStage(ForwardStage.WH_IN);
        order.setPaidAmount(new BigDecimal("30000"));

        service.changePaymentTerm("PT3009TERM", req("NHAN_TRA"));

        ArgumentCaptor<OrderPayment> pay = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(pay.capture());
        assertThat(pay.getValue().getAmount()).isEqualByComparingTo("-30000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(order.getPaymentTerm()).isEqualTo(PaymentTerm.NHAN_TRA);
    }

    @Test
    void receiverDispatcher_atSenderWarehouse_afterCollection_isBlocked() {
        asDispatcher("VP_HD");
        order.setForwardStage(ForwardStage.WH_IN);
        order.setPaidAmount(new BigDecimal("30000"));

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermCollected");
    }

    @Test
    void dispatcher_onTrip_isNotAtSenderWarehouse() {
        asDispatcher("VP_PT");
        order.setForwardStage(ForwardStage.WH_IN);
        order.setCurrentTrip(new Trip());
        order.setPaidAmount(new BigDecimal("30000"));

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermCollected");
    }

    @Test
    void toSenderPays_atSenderWarehouse_switchesAndCollectsSenderFare() {
        asDispatcher("VP_PT");
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        order.setForwardStage(ForwardStage.WH_IN);

        service.changePaymentTerm("PT3009TERM", req("GUI_TRA"));

        assertThat(order.getPaymentTerm()).isEqualTo(PaymentTerm.GUI_TRA);
        ArgumentCaptor<OrderPayment> pay = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(pay.capture());
        assertThat(pay.getValue().getAmount()).isEqualByComparingTo("30000");
        assertThat(pay.getValue().getNote()).isEqualTo(OrderFacadeService.NOTE_SENDER_PREPAID);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void dispatcher_otherOffice_isForbidden() {
        asDispatcher("VP_ND");

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA"))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void admin_afterCollection_reversesPaidAmount() {
        asAdmin();
        order.setPickedUpAt(Instant.now());
        order.setPaidAmount(new BigDecimal("30000"));

        service.changePaymentTerm("PT3009TERM", req("CONG_NO"));

        ArgumentCaptor<OrderPayment> pay = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(pay.capture());
        assertThat(pay.getValue().getAmount()).isEqualByComparingTo("-30000");
        assertThat(pay.getValue().getNote()).isEqualTo(OrderFacadeService.NOTE_PAYMENT_TERM_REVERSAL);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(order.getPaymentTerm()).isEqualTo(PaymentTerm.GUI_TRA);
        assertThat(order.getOnCredit()).isTrue();
    }

    @Test
    void receiptedOrder_isBlockedEvenForAdmin() {
        asAdmin();
        when(receiptOrderLineRepository.existsByOrder_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermReceipted");
    }

    @Test
    void toSenderPays_afterAssignedToVehicle_isBlocked() {
        asAdmin();
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        order.setPickedUpAt(Instant.now());
        order.setForwardStage(ForwardStage.TRANSFER_PENDING);

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("GUI_TRA")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermSenderGone");
    }

    @Test
    void admin_atReceiverWarehouse_isBlocked() {
        asAdmin();
        order.setStatus(OrderStatus.AT_DEST);
        order.setForwardStage(ForwardStage.DEST_WH_IN);

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermReceiverSide");
    }

    @Test
    void admin_failedDelivery_isBlocked() {
        asAdmin();
        order.setStatus(OrderStatus.FAILED_DELIVERY);
        order.setForwardStage(ForwardStage.REDELIVER_WAIT);

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("CONG_NO")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermReceiverSide");
    }

    @Test
    void deliveredOrder_isLocked() {
        asAdmin();
        order.setStatus(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", req("NHAN_TRA")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermLocked");
    }

    @Test
    void missingReason_isRejected() {
        assertThatThrownBy(() -> service.changePaymentTerm("PT3009TERM", new ChangePaymentTermRequest("NHAN_TRA", " ")))
            .extracting(OrderFacadeServicePaymentTermTest::errorKey)
            .isEqualTo("paymentTermReasonRequired");
    }
}
