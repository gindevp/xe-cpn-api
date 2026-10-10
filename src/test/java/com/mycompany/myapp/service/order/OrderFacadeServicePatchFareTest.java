package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
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
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.PatchOrderRequest;
import com.mycompany.myapp.service.partner.DoorKmEstimator;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderFacadeServicePatchFareTest {

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
    private DoorKmEstimator doorKmEstimator;

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

        Office from = new Office();
        from.setId(1L);
        from.setCode("GP");

        order = new ShipmentOrder();
        order.setId(10L);
        order.setOrderCode("XE-H1-001");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSenderName("Sender");
        order.setSenderPhone("0901234567");
        order.setReceiverName("Receiver");
        order.setReceiverPhone("0912345678");
        order.setFromOffice(from);
        order.setFareAmount(new BigDecimal("40000"));
        order.setPaidAmount(new BigDecimal("30000"));
        order.setNote("keep-me");
    }

    @Test
    void assertFareNotBelowPaid_rejectsStrictlyLess() {
        assertThatThrownBy(() -> OrderFacadeService.assertFareNotBelowPaid(new BigDecimal("10000"), new BigDecimal("30000")))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("fareBelowPaid");
    }

    @Test
    void assertFareNotBelowPaid_allowsEqualAndGreater() {
        assertThatCode(() -> OrderFacadeService.assertFareNotBelowPaid(new BigDecimal("30000"), new BigDecimal("30000"))
        ).doesNotThrowAnyException();
        assertThatCode(() -> OrderFacadeService.assertFareNotBelowPaid(new BigDecimal("50000"), new BigDecimal("30000"))
        ).doesNotThrowAnyException();
    }

    @Test
    void assertFareNotBelowPaid_nullPaidTreatedAsZero() {
        assertThatCode(() -> OrderFacadeService.assertFareNotBelowPaid(BigDecimal.ZERO, null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> OrderFacadeService.assertFareNotBelowPaid(new BigDecimal("-1"), null))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("fareBelowPaid");
    }

    @Test
    void patch_fareGreaterThanPaid_ok() {
        stubLoadAndDetail();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("50000"));

        OrderDetailDTO dto = service.patch("XE-H1-001", req);

        assertThat(dto.getFareAmount()).isEqualByComparingTo("50000");
        assertThat(dto.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("50000");
        verify(shipmentOrderRepository).save(order);
    }

    @Test
    void patch_fareEqualPaid_ok() {
        stubLoadAndDetail();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("30000"));

        OrderDetailDTO dto = service.patch("XE-H1-001", req);

        assertThat(dto.getFareAmount()).isEqualByComparingTo("30000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("30000");
        verify(shipmentOrderRepository).save(order);
    }

    @Test
    void patch_fareLessThanPaid_throws400Key() {
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("XE-H1-001")).thenReturn(Optional.of(order));
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("10000"));

        assertThatThrownBy(() -> service.patch("XE-H1-001", req))
            .isInstanceOf(BadRequestAlertException.class)
            .satisfies(ex -> {
                BadRequestAlertException bad = (BadRequestAlertException) ex;
                assertThat(bad.getErrorKey()).isEqualTo("fareBelowPaid");
                assertThat(bad.getStatusCode().value()).isEqualTo(400);
            });

        assertThat(order.getFareAmount()).isEqualByComparingTo("40000");
        verify(shipmentOrderRepository, never()).save(any());
    }

    @Test
    void patch_partialNote_preservesFareAndPaidAndSender() {
        stubLoadAndDetail();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setNote("only-note");

        OrderDetailDTO dto = service.patch("XE-H1-001", req);

        assertThat(dto.getNote()).isEqualTo("only-note");
        assertThat(dto.getFareAmount()).isEqualByComparingTo("40000");
        assertThat(dto.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(dto.getSenderName()).isEqualTo("Sender");
        assertThat(order.getFareAmount()).isEqualByComparingTo("40000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(order.getSenderName()).isEqualTo("Sender");
    }

    @Test
    void patch_withoutFare_doesNotChangeFare() {
        stubLoadAndDetail();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setSenderName("Renamed");

        OrderDetailDTO dto = service.patch("XE-H1-001", req);

        assertThat(dto.getSenderName()).isEqualTo("Renamed");
        assertThat(dto.getFareAmount()).isEqualByComparingTo("40000");
        assertThat(dto.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(dto.getNote()).isEqualTo("keep-me");

        ArgumentCaptor<ShipmentOrder> captor = ArgumentCaptor.forClass(ShipmentOrder.class);
        verify(shipmentOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getFareAmount()).isEqualByComparingTo("40000");
    }

    @Test
    void patch_untickHomeDelivery_rebuildsFareIgnoringStaleClientFare() {
        stubLoadAndDetail();
        order.setGoodsFareAmount(new BigDecimal("30000"));
        order.setDeliveryFeeAmount(new BigDecimal("50000"));
        order.setHomeDelivery(true);
        order.setFareAmount(new BigDecimal("80000"));
        order.setPaidAmount(BigDecimal.ZERO);
        when(fareCalculator.estimate(any(), eq(false), eq(false), any(), any())).thenReturn(
            new SimpleFareCalculator.FareBreakdown(new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30000"))
        );
        PatchOrderRequest req = new PatchOrderRequest();
        req.setHomeDelivery(false);
        req.setFareAmount(new BigDecimal("80000"));

        service.patch("XE-H1-001", req);

        assertThat(order.getDeliveryFeeAmount()).isEqualByComparingTo("0");
        assertThat(order.getFareAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void patch_untickHomeDelivery_belowPaid_throws() {
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("XE-H1-001")).thenReturn(Optional.of(order));
        order.setGoodsFareAmount(new BigDecimal("30000"));
        order.setDeliveryFeeAmount(new BigDecimal("50000"));
        order.setHomeDelivery(true);
        order.setFareAmount(new BigDecimal("80000"));
        order.setPaidAmount(new BigDecimal("80000"));
        when(fareCalculator.estimate(any(), eq(false), eq(false), any(), any())).thenReturn(
            new SimpleFareCalculator.FareBreakdown(new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30000"))
        );
        PatchOrderRequest req = new PatchOrderRequest();
        req.setHomeDelivery(false);
        req.setFareAmount(new BigDecimal("80000"));

        assertThatThrownBy(() -> service.patch("XE-H1-001", req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("fareBelowPaid");
        verify(shipmentOrderRepository, never()).save(any());
    }

    @Test
    void patch_sameHomeDeliveryFlag_keepsDoorFees() {
        stubLoadAndDetail();
        order.setDeliveryFeeAmount(new BigDecimal("65000"));
        order.setHomeDelivery(true);
        PatchOrderRequest req = new PatchOrderRequest();
        req.setHomeDelivery(true);

        service.patch("XE-H1-001", req);

        assertThat(order.getDeliveryFeeAmount()).isEqualByComparingTo("65000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("40000");
        verify(fareCalculator, never()).estimate(any(), anyBoolean(), anyBoolean(), any(), any());
    }

    @Test
    void patch_tickHomeDelivery_usesAhamoveKmForDoorFee() {
        stubLoadAndDetail();
        service.setDoorKmEstimator(doorKmEstimator);
        Office dest = new Office();
        dest.setCode("VP_BC");
        order.setToOffice(dest);
        order.setGoodsFareAmount(new BigDecimal("45000"));
        order.setFareAmount(new BigDecimal("45000"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setHomeDelivery(false);
        order.setDeliveryAddress("2b ngách 1 ngõ 318");
        BigDecimal km = new BigDecimal("5.2");
        when(doorKmEstimator.km(eq(dest), isNull(), isNull(), eq("2b ngách 1 ngõ 318"), eq("giao"))).thenReturn(km);
        when(fareCalculator.estimate(any(), eq(false), eq(true), any(), any(), isNull(), eq(km))).thenReturn(
            new SimpleFareCalculator.FareBreakdown(
                new BigDecimal("45000"),
                BigDecimal.ZERO,
                new BigDecimal("80000"),
                new BigDecimal("125000")
            )
        );
        PatchOrderRequest req = new PatchOrderRequest();
        req.setHomeDelivery(true);

        service.patch("XE-H1-001", req);

        assertThat(order.getDeliveryFeeAmount()).isEqualByComparingTo("80000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("125000");
        ArgumentCaptor<OrderEvent> ev = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository).save(ev.capture());
        assertThat(ev.getValue().getDetail()).contains("km giao 5.2");
    }

    @Test
    void patch_changeDeliveryAddressWhileHome_reestimatesKm() {
        stubLoadAndDetail();
        service.setDoorKmEstimator(doorKmEstimator);
        Office dest = new Office();
        dest.setCode("VP_BC");
        order.setFinalToOffice(dest);
        order.setHomeDelivery(true);
        order.setDeliveryAddress("địa chỉ cũ");
        order.setGoodsFareAmount(new BigDecimal("45000"));
        order.setDeliveryFeeAmount(new BigDecimal("60000"));
        order.setFareAmount(new BigDecimal("105000"));
        order.setPaidAmount(BigDecimal.ZERO);
        BigDecimal km = new BigDecimal("6");
        when(doorKmEstimator.km(eq(dest), isNull(), isNull(), eq("địa chỉ mới"), eq("giao"))).thenReturn(km);
        when(fareCalculator.estimate(any(), eq(false), eq(true), any(), any(), isNull(), eq(km))).thenReturn(
            new SimpleFareCalculator.FareBreakdown(
                new BigDecimal("45000"),
                BigDecimal.ZERO,
                new BigDecimal("80000"),
                new BigDecimal("125000")
            )
        );
        PatchOrderRequest req = new PatchOrderRequest();
        req.setDeliveryAddress("địa chỉ mới");

        service.patch("XE-H1-001", req);

        assertThat(order.getDeliveryFeeAmount()).isEqualByComparingTo("80000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("125000");
    }

    @Test
    void patch_tickHomeDelivery_withoutAddress_rejected() {
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("XE-H1-001")).thenReturn(Optional.of(order));
        Office dest = new Office();
        dest.setLatitude(new BigDecimal("21.0"));
        dest.setLongitude(new BigDecimal("105.0"));
        order.setToOffice(dest);
        order.setHomeDelivery(false);
        order.setDeliveryAddress(null);
        service.setDoorKmEstimator(
            new DoorKmEstimator(org.mockito.Mockito.mock(com.mycompany.myapp.service.partner.AhamoveOrderClient.class))
        );
        PatchOrderRequest req = new PatchOrderRequest();
        req.setHomeDelivery(true);

        assertThatThrownBy(() -> service.patch("XE-H1-001", req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("doorKmAddressRequired");
        verify(shipmentOrderRepository, never()).save(any());
    }

    @Test
    void senderPrepaidAtWarehouse_fareUp_recordsAdjustmentAndPaidFollows() {
        stubLoadAndDetail();
        prepaidAtSenderWarehouse();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("55000"));

        service.patch("XE-H1-001", req);

        ArgumentCaptor<OrderPayment> pay = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(pay.capture());
        assertThat(pay.getValue().getAmount()).isEqualByComparingTo("15000");
        assertThat(pay.getValue().getPaymentKind()).isEqualTo(PaymentKind.TRUOC);
        assertThat(pay.getValue().getNote()).isEqualTo(OrderFacadeService.NOTE_SENDER_PREPAID_ADJUST);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("55000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("55000");
    }

    @Test
    void senderPrepaidAtWarehouse_fareDown_reversesDifferenceInsteadOfH1() {
        stubLoadAndDetail();
        prepaidAtSenderWarehouse();
        when(receiptOrderLineRepository.existsByOrder_Id(10L)).thenReturn(false);
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("25000"));

        service.patch("XE-H1-001", req);

        ArgumentCaptor<OrderPayment> pay = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(pay.capture());
        assertThat(pay.getValue().getAmount()).isEqualByComparingTo("-15000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("25000");
        verify(dayClosureGuard).assertCollectionMutable(order);
    }

    @Test
    void senderPrepaidAtWarehouse_fareDownAfterReceipt_rejected() {
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("XE-H1-001")).thenReturn(Optional.of(order));
        prepaidAtSenderWarehouse();
        when(receiptOrderLineRepository.existsByOrder_Id(10L)).thenReturn(true);
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("25000"));

        assertThatThrownBy(() -> service.patch("XE-H1-001", req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("fareBelowReceipted");
        verify(orderPaymentRepository, never()).save(any());
        verify(shipmentOrderRepository, never()).save(any());
    }

    @Test
    void senderPrepaidAtWarehouse_fareUp_allowedEvenIfReceipted() {
        stubLoadAndDetail();
        prepaidAtSenderWarehouse();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("50000"));

        service.patch("XE-H1-001", req);

        verify(receiptOrderLineRepository, never()).existsByOrder_Id(any());
        verify(orderPaymentRepository).save(any(OrderPayment.class));
        assertThat(order.getPaidAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void senderPrepaidOnTruck_fareDown_stillH1() {
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("XE-H1-001")).thenReturn(Optional.of(order));
        prepaidAtSenderWarehouse();
        order.setCurrentTrip(new Trip());
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("25000"));

        assertThatThrownBy(() -> service.patch("XE-H1-001", req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("fareBelowPaid");
        verify(orderPaymentRepository, never()).save(any());
    }

    @Test
    void receiverPays_fareUp_paidUnchanged() {
        stubLoadAndDetail();
        prepaidAtSenderWarehouse();
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("50000"));

        service.patch("XE-H1-001", req);

        verify(orderPaymentRepository, never()).save(any());
        assertThat(order.getPaidAmount()).isEqualByComparingTo("40000");
    }

    private void prepaidAtSenderWarehouse() {
        service.setOrderPaymentRepository(orderPaymentRepository);
        service.setReceiptOrderLineRepository(receiptOrderLineRepository);
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setForwardStage(ForwardStage.WH_IN);
        order.setFareAmount(new BigDecimal("40000"));
        order.setPaidAmount(new BigDecimal("40000"));
    }

    private void stubLoadAndDetail() {
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("XE-H1-001")).thenReturn(Optional.of(order));
        when(shipmentOrderRepository.save(any(ShipmentOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(10L)).thenReturn(List.of());
        when(orderPodPhotoRepository.findByOrder_IdOrderBySequenceNoAsc(10L)).thenReturn(List.of());
        when(orderIssueRepository.findByOrder_IdOrderByOpenedAtAscIdAsc(10L)).thenReturn(List.of());
        when(orderLegRepository.findByOrder_IdOrderByLegIndexAsc(10L)).thenReturn(List.of());
    }
}
