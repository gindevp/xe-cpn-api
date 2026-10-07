package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.PartnerFeeExpense;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.PartnerFeeExpenseRepository;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.FailDeliveryRequest;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.order.PodRequest;
import com.mycompany.myapp.service.order.DeliveryFacadeService;
import com.mycompany.myapp.service.order.OrderFacadeService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

class AhamoveDispatchServiceFlowTest {

    private final ObjectMapper om = new ObjectMapper();
    private ShipmentOrderRepository orderRepo;
    private IntegrationConfigRepository cfgRepo;
    private AhamoveOrderClient client;
    private DeliveryFacadeService delivery;
    private OrderFacadeService orders;
    private OrderPaymentRepository paymentRepo;
    private ReceiptOrderLineRepository receiptLineRepo;
    private OrderDeliveryAttemptRepository attemptRepo;
    private PartnerFeeExpenseRepository feeRepo;
    private AhamoveDispatchService service;
    private ShipmentOrder order;

    @BeforeEach
    void setUp() {
        orderRepo = mock(ShipmentOrderRepository.class);
        cfgRepo = mock(IntegrationConfigRepository.class);
        client = mock(AhamoveOrderClient.class);
        delivery = mock(DeliveryFacadeService.class);
        orders = mock(OrderFacadeService.class);
        paymentRepo = mock(OrderPaymentRepository.class);
        receiptLineRepo = mock(ReceiptOrderLineRepository.class);
        attemptRepo = mock(OrderDeliveryAttemptRepository.class);
        feeRepo = mock(PartnerFeeExpenseRepository.class);
        service = new AhamoveDispatchService(
            orderRepo,
            cfgRepo,
            attemptRepo,
            client,
            delivery,
            orders,
            mock(DayClosureGuard.class),
            paymentRepo,
            receiptLineRepo,
            feeRepo,
            mock(PlatformTransactionManager.class)
        );

        Office office = new Office();
        office.setId(2L);
        office.setName("VP Nhận");
        office.setAddress("1 Trần Duy Hưng");
        office.setLatitude(new BigDecimal("21.0100000"));
        office.setLongitude(new BigDecimal("105.8000000"));
        order = new ShipmentOrder();
        order.setId(10L);
        order.setOrderCode("GP-0001");
        order.setStatus(OrderStatus.AT_DEST);
        order.setHomeDelivery(true);
        order.setFareAmount(new BigDecimal("50000"));
        order.setPaidAmount(new BigDecimal("50000"));
        order.setToOffice(office);
        order.setReceiverName("Chị Lan");
        order.setReceiverPhone("0901234567");
        order.setDeliveryAddress("12 Láng Hạ");
        when(orderRepo.findOneByOrderCodeOrDraftCode("GP-0001")).thenReturn(Optional.of(order));

        IntegrationConfig cfg = new IntegrationConfig();
        cfg.setAhamoveMobile("0912345678");
        cfg.setAhamoveWebhookToken("tok123");
        when(cfgRepo.findAll()).thenReturn(List.of(cfg));
    }

    private AhamoveDispatchService.DispatchRequest pin() {
        AhamoveDispatchService.DispatchRequest r = new AhamoveDispatchService.DispatchRequest();
        r.setLat(21.02);
        r.setLng(105.81);
        return r;
    }

    @Test
    void dispatch_senderPaysUnpaid_driverAdvancesWholeDue() {
        order.setPaidAmount(new BigDecimal("20000"));
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        when(client.createOrder(any(), any(), any(), any())).thenReturn(
            new AhamoveOrderClient.CreatedOrder("AHA6", "ASSIGNING", null, new BigDecimal("25000"), null)
        );
        service.dispatch("GP-0001", pin());
        ArgumentCaptor<AhamoveOrderClient.Stop> drop = ArgumentCaptor.forClass(AhamoveOrderClient.Stop.class);
        verify(client).createOrder(any(), drop.capture(), any(), any());
        assertThat(drop.getValue().cod()).isEqualTo(30000L);
        assertThat(order.getPartnerCodAmount()).isEqualByComparingTo("30000");
        verify(delivery).recordPartnerAdvance(eq(order), eq(new BigDecimal("30000")), anyString(), eq("system"));
    }

    @Test
    void dispatch_onCreditUnpaid_rejected() {
        order.setPaidAmount(BigDecimal.ZERO);
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        order.setOnCredit(true);
        assertThatThrownBy(() -> service.dispatch("GP-0001", pin()))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamoveOnCredit");
        verify(client, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    void dispatch_receiverPaysUnpaid_sendsCodAndStoresAdvance() {
        order.setPaidAmount(new BigDecimal("20000"));
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        when(client.createOrder(any(), any(), any(), any())).thenReturn(
            new AhamoveOrderClient.CreatedOrder("AHA3", "ASSIGNING", null, new BigDecimal("25000"), null)
        );
        service.dispatch("GP-0001", pin());

        ArgumentCaptor<AhamoveOrderClient.Stop> pickup = ArgumentCaptor.forClass(AhamoveOrderClient.Stop.class);
        ArgumentCaptor<AhamoveOrderClient.Stop> drop = ArgumentCaptor.forClass(AhamoveOrderClient.Stop.class);
        verify(client).createOrder(pickup.capture(), drop.capture(), any(), any());
        assertThat(pickup.getValue().cod()).isZero();
        assertThat(drop.getValue().cod()).isEqualTo(30000L);
        assertThat(order.getPartnerCodAmount()).isEqualByComparingTo("30000");
        assertThat(order.getPartnerCodCollectedAt()).isNull();
        assertThat(order.getPaidAmount()).isEqualByComparingTo("20000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void dispatch_paidOrder_sendsZeroCod() {
        when(client.createOrder(any(), any(), any(), any())).thenReturn(
            new AhamoveOrderClient.CreatedOrder("AHA4", "ASSIGNING", null, null, null)
        );
        service.dispatch("GP-0001", pin());
        ArgumentCaptor<AhamoveOrderClient.Stop> drop = ArgumentCaptor.forClass(AhamoveOrderClient.Stop.class);
        verify(client).createOrder(any(), drop.capture(), any(), any());
        assertThat(drop.getValue().cod()).isZero();
        assertThat(order.getPartnerCodAmount()).isNull();
    }

    @Test
    void dispatch_bulkyPackage_sendsTier() {
        order.setNote("[PKGKG]35[/PKGKG]\n[PKGDIM]55x45x50[/PKGDIM]");
        when(client.createOrder(any(), any(), any(), any())).thenReturn(
            new AhamoveOrderClient.CreatedOrder("AHA6", "ASSIGNING", null, null, null)
        );
        service.dispatch("GP-0001", pin());
        ArgumentCaptor<AhamoveCargo> cargo = ArgumentCaptor.forClass(AhamoveCargo.class);
        verify(client).createOrder(any(), any(), any(), cargo.capture());
        assertThat(cargo.getValue().tier()).isEqualTo("TIER_2");
        assertThat(cargo.getValue().packages()).hasSize(1);
        assertThat(cargo.getValue().packages().get(0).lengthCm()).isEqualTo(55);
    }

    @Test
    void dispatch_oversizePackage_rejected() {
        order.setWeightKg(new BigDecimal("90"));
        assertThatThrownBy(() -> service.dispatch("GP-0001", pin()))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamoveBulkyOver");
        verify(client, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    void dispatch_addressOnly_sendsNoCoordinates() {
        when(client.createOrder(any(), any(), any(), any())).thenReturn(
            new AhamoveOrderClient.CreatedOrder("AHA5", "ASSIGNING", null, null, null)
        );
        AhamoveDispatchService.DispatchRequest r = new AhamoveDispatchService.DispatchRequest();
        r.setAddress("12 Ngõ 34 Láng Hạ, Đống Đa, Hà Nội");
        service.dispatch("GP-0001", r);
        ArgumentCaptor<AhamoveOrderClient.Stop> drop = ArgumentCaptor.forClass(AhamoveOrderClient.Stop.class);
        verify(client).createOrder(any(), drop.capture(), any(), any());
        assertThat(drop.getValue().lat()).isNull();
        assertThat(drop.getValue().lng()).isNull();
        assertThat(drop.getValue().address()).isEqualTo("12 Ngõ 34 Láng Hạ, Đống Đa, Hà Nội");
        assertThat(order.getDeliveryLat()).isNull();
    }

    @Test
    void dispatch_halfCoordinates_rejected() {
        AhamoveDispatchService.DispatchRequest r = new AhamoveDispatchService.DispatchRequest();
        r.setLat(21.02);
        assertThatThrownBy(() -> service.dispatch("GP-0001", r))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamovePinInvalid");
        verify(client, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    void dispatch_blockedWhileAdvanceNotRefunded() {
        order.setStatus(OrderStatus.FAILED_DELIVERY);
        order.setPartnerCodAmount(new BigDecimal("30000"));
        order.setPartnerCodCollectedAt(Instant.now());
        assertThatThrownBy(() -> service.dispatch("GP-0001", pin()))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("partnerAdvanceRefundDue");
        verify(client, never()).createOrder(any(), any(), any(), any());
    }

    private void advancePending() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setPartnerCode("AHAMOVE");
        order.setPartnerOrderId("AHA3");
        order.setPartnerDriverName("Anh Tú");
        order.setPaidAmount(new BigDecimal("20000"));
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        order.setPartnerCodAmount(new BigDecimal("30000"));
    }

    @Test
    void confirmAdvance_recordsCashPaymentOwedByDispatcher() {
        advancePending();
        OrderDeliveryAttempt first = new OrderDeliveryAttempt();
        first.setDeliveryPartner(DeliveryPartner.AHAMOVE);
        first.setReason("ASSIGN_PARTNER");
        first.setHandledByUsername("old_dispatcher");
        OrderDeliveryAttempt last = new OrderDeliveryAttempt();
        last.setDeliveryPartner(DeliveryPartner.AHAMOVE);
        last.setReason("ASSIGN_PARTNER");
        last.setHandledByUsername("dungtm");
        when(attemptRepo.findByOrder_IdOrderByAttemptAtAsc(10L)).thenReturn(List.of(first, last));

        service.confirmAdvance("GP-0001");

        ArgumentCaptor<String> note = ArgumentCaptor.forClass(String.class);
        verify(delivery).recordPartnerAdvance(eq(order), eq(new BigDecimal("30000")), note.capture(), eq("dungtm"));
        assertThat(note.getValue()).startsWith("POD AHAMOVE ỨNG").contains("Anh Tú");
        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(orders).recordEvent(eq(order), eq("AHAMOVE_ADVANCE_IN"), detail.capture(), anyString());
        assertThat(detail.getValue()).contains("người nhận nợ dungtm");
    }

    @Test
    void confirmAdvance_twiceRejected() {
        advancePending();
        order.setPartnerCodCollectedAt(Instant.now());
        assertThatThrownBy(() -> service.confirmAdvance("GP-0001"))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamoveAdvanceDone");
        verify(delivery, never()).recordPartnerAdvance(any(), any(), any(), any());
    }

    private OrderPayment advancePaid() {
        advancePending();
        order.setStatus(OrderStatus.FAILED_DELIVERY);
        order.setPaidAmount(new BigDecimal("50000"));
        order.setPartnerCodCollectedAt(Instant.now());
        OrderPayment p = new OrderPayment();
        p.setAmount(new BigDecimal("30000"));
        p.setNote("POD AHAMOVE ỨNG · tài xế Anh Tú");
        p.setPaymentAt(Instant.now());
        OrderPayment other = new OrderPayment();
        other.setAmount(new BigDecimal("20000"));
        other.setNote("POD HOME");
        other.setPaymentAt(Instant.now().minusSeconds(3600));
        when(paymentRepo.findByOrder_IdOrderByPaymentAtDesc(10L)).thenReturn(List.of(p, other));
        return p;
    }

    @Test
    void refundAdvance_deletesAdvancePaymentAndRestoresDue() {
        OrderPayment p = advancePaid();
        service.refundAdvance("GP-0001");
        verify(paymentRepo).delete(p);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("20000");
        assertThat(order.getPartnerCodAmount()).isNull();
        assertThat(order.getPartnerCodCollectedAt()).isNull();
        verify(orders).recordEvent(eq(order), eq("AHAMOVE_ADVANCE_REFUND"), anyString(), anyString());
    }

    @Test
    void refundAdvance_blockedWhenAlreadyInReceipt() {
        advancePaid();
        when(receiptLineRepo.existsByOrder_IdAndReceipt_CreatedAtGreaterThanEqual(eq(10L), any())).thenReturn(true);
        assertThatThrownBy(() -> service.refundAdvance("GP-0001"))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamoveAdvanceInReceipt");
        verify(paymentRepo, never()).delete(any());
        assertThat(order.getPaidAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void refundAdvance_notAllowedWhileOutForDelivery() {
        advancePaid();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        assertThatThrownBy(() -> service.refundAdvance("GP-0001"))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamoveNoRefund");
    }

    @Test
    void dispatch_rejectsCod() {
        order.setCodAmount(new BigDecimal("100000"));
        assertThatThrownBy(() -> service.dispatch("GP-0001", pin()))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamoveCod");
        verify(client, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    void dispatch_rejectsCounterPickupAndWrongStatus() {
        order.setHomeDelivery(false);
        assertThatThrownBy(() -> service.dispatch("GP-0001", pin()))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamoveNotHomeDelivery");
        order.setHomeDelivery(true);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        assertThatThrownBy(() -> service.dispatch("GP-0001", pin()))
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("ahamoveStatus");
        verify(client, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    void dispatch_ok_savesPartnerFeeAndPushesShip_withoutTouchingFare() {
        when(client.createOrder(any(), any(), eq("BALANCE"), any())).thenReturn(
            new AhamoveOrderClient.CreatedOrder("AHA1", "ASSIGNING", "https://aha/s/AHA1", new BigDecimal("28000"), "HAN-BIKE")
        );
        service.dispatch("GP-0001", pin());

        assertThat(order.getPartnerCode()).isEqualTo("AHAMOVE");
        assertThat(order.getPartnerOrderId()).isEqualTo("AHA1");
        assertThat(order.getPartnerFeeAmount()).isEqualByComparingTo("28000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("50000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("50000");
        ArgumentCaptor<OrderTransitionRequest> tr = ArgumentCaptor.forClass(OrderTransitionRequest.class);
        verify(orders).transition(eq("GP-0001"), tr.capture());
        assertThat(tr.getValue().getToStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(tr.getValue().getAction()).isEqualTo("PUSH_SHIP");
    }

    @Test
    void dispatch_dbFailure_cancelsAhamoveOrder() {
        when(client.createOrder(any(), any(), any(), any())).thenReturn(
            new AhamoveOrderClient.CreatedOrder("AHA2", "ASSIGNING", null, null, null)
        );
        when(orders.transition(anyString(), any())).thenThrow(new BadRequestAlertException("Day closed", "order", "dayClosed"));
        assertThatThrownBy(() -> service.dispatch("GP-0001", pin())).isInstanceOf(BadRequestAlertException.class);
        verify(client).cancelOrder(eq("AHA2"), anyString());
    }

    private void outForDelivery() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setPartnerCode("AHAMOVE");
        order.setPartnerOrderId("AHA1");
        when(orderRepo.findFirstByPartnerOrderId("AHA1")).thenReturn(Optional.of(order));
    }

    @Test
    void webhook_completedWithPhoto_autoPodsWithoutCollecting() throws Exception {
        outForDelivery();
        service.applyWebhook(
            om.readTree(
                "{\"_id\":\"AHA1\",\"status\":\"COMPLETED\",\"path\":[{},{\"status\":\"COMPLETED\",\"pod_info\":[{\"image_url\":\"https://img/p.jpg\"}]}]}"
            )
        );
        ArgumentCaptor<PodRequest> pod = ArgumentCaptor.forClass(PodRequest.class);
        verify(delivery).pod(eq("GP-0001"), pod.capture());
        assertThat(pod.getValue().getChannel()).isEqualTo("HOME");
        assertThat(pod.getValue().getPhotos()).containsExactly("https://img/p.jpg");
        assertThat(pod.getValue().getCollectedAmount()).isEqualByComparingTo("0");
        assertThat(order.getPartnerPodUrl()).isEqualTo("https://img/p.jpg");
    }

    @Test
    void webhook_completedWithoutPhoto_leavesManualPod() throws Exception {
        outForDelivery();
        service.applyWebhook(om.readTree("{\"_id\":\"AHA1\",\"status\":\"COMPLETED\",\"path\":[{},{\"status\":\"COMPLETED\"}]}"));
        verify(delivery, never()).pod(anyString(), any());
        assertThat(order.getPartnerStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void webhook_failedStop_recordsFailDelivery() throws Exception {
        outForDelivery();
        service.applyWebhook(
            om.readTree(
                "{\"_id\":\"AHA1-1\",\"status\":\"IN PROCESS\",\"path\":[{},{\"status\":\"FAILED\",\"fail_comment\":\"Không nghe máy\"}]}"
            )
        );
        ArgumentCaptor<FailDeliveryRequest> fail = ArgumentCaptor.forClass(FailDeliveryRequest.class);
        verify(delivery).failDelivery(eq("GP-0001"), fail.capture());
        assertThat(fail.getValue().getReason()).contains("Không nghe máy");
    }

    @Test
    void webhook_afterDelivered_onlyUpdatesPartnerInfo() throws Exception {
        outForDelivery();
        order.setStatus(OrderStatus.DELIVERED);
        service.applyWebhook(
            om.readTree(
                "{\"_id\":\"AHA1\",\"status\":\"COMPLETED\",\"path\":[{},{\"status\":\"COMPLETED\",\"pod_info\":\"https://img/x.jpg\"}]}"
            )
        );
        verify(delivery, never()).pod(anyString(), any());
        verify(delivery, never()).failDelivery(anyString(), any());
        verify(orders, never()).transition(anyString(), any());
    }

    private void cashConfig() {
        IntegrationConfig cfg = new IntegrationConfig();
        cfg.setAhamovePaymentMethod("CASH");
        when(cfgRepo.findAll()).thenReturn(List.of(cfg));
        OrderDeliveryAttempt assign = new OrderDeliveryAttempt();
        assign.setDeliveryPartner(DeliveryPartner.AHAMOVE);
        assign.setReason("ASSIGN_PARTNER");
        assign.setHandledByUsername("dungtm");
        when(attemptRepo.findByOrder_IdOrderByAttemptAtAsc(10L)).thenReturn(List.of(assign));
    }

    @Test
    void webhook_cashPickedUp_recordsFeeOwedToDispatcher() throws Exception {
        outForDelivery();
        cashConfig();
        order.setPartnerFeeAmount(new BigDecimal("32000"));
        service.applyWebhook(om.readTree("{\"_id\":\"AHA1\",\"status\":\"IN PROCESS\"}"));
        ArgumentCaptor<PartnerFeeExpense> cap = ArgumentCaptor.forClass(PartnerFeeExpense.class);
        verify(feeRepo).save(cap.capture());
        assertThat(cap.getValue().getAmount()).isEqualByComparingTo("32000");
        assertThat(cap.getValue().getPayerUsername()).isEqualTo("dungtm");
        assertThat(cap.getValue().getPartnerOrderId()).isEqualTo("AHA1");
        assertThat(cap.getValue().getReceipt()).isNull();
        verify(orders).recordEvent(eq(order), eq("AHAMOVE_FEE"), org.mockito.ArgumentMatchers.contains("32000"), eq("ahamove"));
    }

    @Test
    void webhook_cashAlreadyRecorded_noDuplicate() throws Exception {
        outForDelivery();
        cashConfig();
        order.setPartnerFeeAmount(new BigDecimal("32000"));
        when(feeRepo.existsByPartnerOrderId("AHA1")).thenReturn(true);
        service.applyWebhook(om.readTree("{\"_id\":\"AHA1\",\"status\":\"COMPLETED\",\"path\":[{},{\"status\":\"COMPLETED\"}]}"));
        verify(feeRepo, never()).save(any());
    }

    @Test
    void webhook_cashBeforePickup_noFee() throws Exception {
        outForDelivery();
        cashConfig();
        order.setPartnerFeeAmount(new BigDecimal("32000"));
        service.applyWebhook(om.readTree("{\"_id\":\"AHA1\",\"status\":\"ACCEPTED\"}"));
        service.applyWebhook(om.readTree("{\"_id\":\"AHA1\",\"status\":\"CANCELLED\"}"));
        verify(feeRepo, never()).save(any());
    }

    @Test
    void webhook_balancePayment_noFee() throws Exception {
        outForDelivery();
        order.setPartnerFeeAmount(new BigDecimal("32000"));
        service.applyWebhook(om.readTree("{\"_id\":\"AHA1\",\"status\":\"IN PROCESS\"}"));
        verify(feeRepo, never()).save(any());
    }

    @Test
    void webhook_unknownOrder_isIgnored() throws Exception {
        when(orderRepo.findFirstByPartnerOrderId("NOPE")).thenReturn(Optional.empty());
        assertThat(service.applyWebhook(om.readTree("{\"_id\":\"NOPE\",\"status\":\"ACCEPTED\"}"))).isEmpty();
    }

    @Test
    void webhookToken_queryOrHeader() {
        assertThat(service.webhookTokenValid("tok123", null)).isTrue();
        assertThat(service.webhookTokenValid(null, "tok123")).isTrue();
        assertThat(service.webhookTokenValid("bad", null)).isFalse();
        assertThat(service.webhookTokenValid(null, null)).isFalse();
    }
}
