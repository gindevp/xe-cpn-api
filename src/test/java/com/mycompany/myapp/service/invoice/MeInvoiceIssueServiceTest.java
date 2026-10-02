package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.IssueInvoiceRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MeInvoiceIssueServiceTest {

    private ShipmentOrderRepository orderRepo;
    private OrderEventRepository eventRepo;
    private DayClosureGuard dayClosureGuard;
    private MisaMeInvoiceClient client;
    private IntegrationConfigRepository configRepo;
    private MeInvoiceIssueService service;
    private ShipmentOrder order;

    @BeforeEach
    void setUp() {
        orderRepo = mock(ShipmentOrderRepository.class);
        eventRepo = mock(OrderEventRepository.class);
        dayClosureGuard = mock(DayClosureGuard.class);
        client = mock(MisaMeInvoiceClient.class);
        configRepo = mock(IntegrationConfigRepository.class);
        service = new MeInvoiceIssueService(orderRepo, eventRepo, configRepo, dayClosureGuard, client, new ObjectMapper(), true);

        order = new ShipmentOrder();
        order.setOrderCode("VT0001ABCD");
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        order.setSenderName("Nguyễn Gửi");
        order.setSenderPhone("0911111111");
        order.setReceiverName("Trần Nhận");
        order.setReceiverPhone("0922222222");
        order.setGoodsFareAmount(new BigDecimal("110000"));
        when(orderRepo.findOneByOrderCodeOrDraftCode("VT0001ABCD")).thenReturn(Optional.of(order));
        when(client.isEnabled()).thenReturn(true);
        when(client.getInvSeries()).thenReturn("1C26TXE");
    }

    private static IssueInvoiceRequest req(String tax, String company, String address, String email) {
        IssueInvoiceRequest r = new IssueInvoiceRequest();
        r.setTaxCode(tax);
        r.setCompanyName(company);
        r.setAddress(address);
        r.setEmail(email);
        return r;
    }

    private static IssueInvoiceRequest validReq() {
        return req(" 0100233488 ", " Cty ABC ", "1 Ly Thuong Kiet, Ha Noi", "ketoan@abc.vn");
    }

    @Test
    void issueManual_valid_savesBuyerInfo_publishes_logsEvent() {
        when(client.publish(any(ObjectNode.class))).thenReturn(
            new MisaMeInvoiceClient.PublishResult(true, false, "TX1", "0000123", "1C26TXE", "CODE1", "{}")
        );

        ShipmentOrder out = service.issueManual("VT0001ABCD", validReq(), "dieuphoi1");

        assertThat(out.getInvoiceRequested()).isTrue();
        assertThat(out.getInvoiceTaxCode()).isEqualTo("0100233488");
        assertThat(out.getInvoiceCompanyName()).isEqualTo("Cty ABC");
        assertThat(out.getInvoiceEmail()).isEqualTo("ketoan@abc.vn");
        assertThat(out.getInvoiceStatus()).isEqualTo(MeInvoiceIssueService.STATUS_ISSUED);
        assertThat(out.getInvoiceNo()).isEqualTo("0000123");
        assertThat(out.getInvoiceGrossAmount()).isEqualByComparingTo("110000");
        verify(dayClosureGuard).assertOrderMutable(order);

        ArgumentCaptor<ObjectNode> body = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).publish(body.capture());
        ObjectNode inv = (ObjectNode) body.getValue().get("InvoiceData").get(0);
        assertThat(inv.get("BuyerTaxCode").asText()).isEqualTo("0100233488");
        assertThat(inv.get("BuyerEmail").asText()).isEqualTo("ketoan@abc.vn");
        assertThat(inv.get("IsSendEmail").asBoolean()).isTrue();
        assertThat(inv.get("OriginalInvoiceDetail").get(0).get("UnitName").asText()).isEqualTo("Vận đơn");
        assertThat(inv.get("BuyerLegalName").asText()).isEqualTo("Cty ABC");
        assertThat(inv.get("BuyerFullName").asText()).isEmpty();
        assertThat(inv.get("BuyerPhoneNumber").asText()).isEmpty();
        assertThat(inv.get("ReceiverName").asText()).isEqualTo("Cty ABC");
        assertThat(inv.get("PaymentMethodName").asText()).isEqualTo("TM/CK");
        assertThat(out.getInvoiceType()).isEqualTo(InvoicePolicy.TYPE_COMPANY);

        ArgumentCaptor<OrderEvent> ev = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventRepo).save(ev.capture());
        assertThat(ev.getValue().getAction()).isEqualTo("INVOICE_ISSUE");
        assertThat(ev.getValue().getActorUsername()).isEqualTo("dieuphoi1");
        assertThat(ev.getValue().getDetail()).contains("0000123");
    }

    @Test
    void issueManual_misaError_marksFailed_noThrow() {
        when(client.publish(any(ObjectNode.class))).thenThrow(new IllegalStateException("MISA 500"));

        ShipmentOrder out = service.issueManual("VT0001ABCD", validReq(), "dieuphoi1");

        assertThat(out.getInvoiceStatus()).isEqualTo(MeInvoiceIssueService.STATUS_FAILED);
        assertThat(out.getInvoiceError()).contains("MISA 500");
        ArgumentCaptor<OrderEvent> ev = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventRepo).save(ev.capture());
        assertThat(ev.getValue().getDetail()).startsWith("Xuất HĐĐT MISA lỗi");
    }

    @Test
    void issueManual_misaDisabled_rejects() {
        when(client.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", validReq(), "u"))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("MISA");
        verify(client, never()).publish(any());
    }

    @Test
    void issueManual_notDelivered_rejects() {
        order.setStatus(OrderStatus.IN_TRANSIT);
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", validReq(), "u")).isInstanceOf(BadRequestAlertException.class);
        verify(client, never()).publish(any());
    }

    @Test
    void issueManual_alreadyIssued_rejects_keepsFields() {
        order.setInvoiceStatus(MeInvoiceIssueService.STATUS_ISSUED);
        order.setInvoiceNo("0000009");
        order.setInvoiceTaxCode("0103179782");
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", validReq(), "u"))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("0000009");
        assertThat(order.getInvoiceTaxCode()).isEqualTo("0103179782");
        verify(client, never()).publish(any());
    }

    @Test
    void issueManual_invalidTaxCode_rejects() {
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", req("1234567890", "A", "B", "a@b.vn"), "u")).isInstanceOf(
            BadRequestAlertException.class
        );
        verify(client, never()).publish(any());
    }

    @Test
    void issueManual_missingCompanyAddressOrEmail_rejects() {
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", req("0100233488", " ", "B", "a@b.vn"), "u")).isInstanceOf(
            BadRequestAlertException.class
        );
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", req("0100233488", "A", null, "a@b.vn"), "u")).isInstanceOf(
            BadRequestAlertException.class
        );
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", req("0100233488", "A", "B", "not-an-email"), "u")).isInstanceOf(
            BadRequestAlertException.class
        );
        verify(client, never()).publish(any());
        verify(orderRepo, never()).save(any());
    }

    @Test
    void issueManual_dayClosed_rejects_beforePublish() {
        doThrow(new BadRequestAlertException("closed", "dayClosure", "dayClosed")).when(dayClosureGuard).assertOrderMutable(order);
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", validReq(), "u")).isInstanceOf(BadRequestAlertException.class);
        verify(client, never()).publish(any());
        verify(orderRepo, never()).save(any());
    }

    @Test
    void saveInfo_inTransitOrder_savesWithoutDayClosureOrPublish() {
        order.setStatus(OrderStatus.IN_TRANSIT);
        doThrow(new BadRequestAlertException("closed", "dayClosure", "dayClosed")).when(dayClosureGuard).assertOrderMutable(order);

        service.saveInfo(
            "VT0001ABCD",
            new MeInvoiceIssueService.InvoiceInfoRequest(true, " 0100233488 ", " Cty ABC ", "1 Ly Thuong Kiet", "ketoan@abc.vn"),
            "u"
        );

        assertThat(order.getInvoiceRequested()).isTrue();
        assertThat(order.getInvoiceTaxCode()).isEqualTo("0100233488");
        assertThat(order.getInvoiceCompanyName()).isEqualTo("Cty ABC");
        verify(client, never()).publish(any());
        ArgumentCaptor<OrderEvent> ev = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventRepo).save(ev.capture());
        assertThat(ev.getValue().getAction()).isEqualTo("INVOICE_INFO");
    }

    @Test
    void saveInfo_notRequested_clearsFields() {
        order.setInvoiceRequested(true);
        order.setInvoiceTaxCode("0100233488");

        service.saveInfo("VT0001ABCD", new MeInvoiceIssueService.InvoiceInfoRequest(false, null, null, null, null), "u");

        assertThat(order.getInvoiceRequested()).isFalse();
        assertThat(order.getInvoiceTaxCode()).isNull();
    }

    @Test
    void saveInfo_alreadyIssued_rejects() {
        order.setInvoiceStatus(MeInvoiceIssueService.STATUS_ISSUED);
        order.setInvoiceTaxCode("0103179782");

        assertThatThrownBy(() ->
            service.saveInfo("VT0001ABCD", new MeInvoiceIssueService.InvoiceInfoRequest(true, "0100233488", "A", "B", "a@b.vn"), "u")
        ).isInstanceOf(BadRequestAlertException.class);
        assertThat(order.getInvoiceTaxCode()).isEqualTo("0103179782");
        verify(orderRepo, never()).save(any());
    }

    private void deliveredAt(Instant at) {
        order.setId(42L);
        List<Object[]> rows = List.<Object[]>of(new Object[] { 42L, at });
        when(eventRepo.latestEventAtByOrderIds(any(), any())).thenReturn(rows);
    }

    private void publishOk() {
        when(client.publish(any(ObjectNode.class))).thenReturn(
            new MisaMeInvoiceClient.PublishResult(true, false, "TX1", "0000123", "1C26TXE", "CODE1", "{}")
        );
    }

    private ObjectNode publishedInvoice() {
        ArgumentCaptor<ObjectNode> body = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).publish(body.capture());
        return (ObjectNode) body.getValue().get("InvoiceData").get(0);
    }

    @Test
    void issueManual_deliveredDaysAgo_lateIssueStillAllowed() {
        deliveredAt(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(3).toInstant());
        publishOk();
        ShipmentOrder out = service.issueManual("VT0001ABCD", validReq(), "u");
        assertThat(out.getInvoiceStatus()).isEqualTo(MeInvoiceIssueService.STATUS_ISSUED);
    }

    @Test
    void issueManual_senderPaysWarehousedNotDelivered_allowed() {
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setStatus(OrderStatus.IN_TRANSIT);
        order.setPickedUpAt(Instant.now().minusSeconds(600));
        publishOk();
        ShipmentOrder out = service.issueManual("VT0001ABCD", validReq(), "u");
        assertThat(out.getInvoiceStatus()).isEqualTo(MeInvoiceIssueService.STATUS_ISSUED);
    }

    @Test
    void issueManual_senderPaysNotWarehoused_rejects() {
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setStatus(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", validReq(), "u"))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("nhập kho");
        verify(client, never()).publish(any());
    }

    @Test
    void backfill_receiverPays_personalInvoice_receiverNamePhone_cash_noTax() {
        order.setFareAmount(new BigDecimal("110000"));
        order.setPaidAmount(new BigDecimal("110000"));
        publishOk();

        String result = service.backfillOne("VT0001ABCD", "ketoan");

        assertThat(result).isEqualTo(MeInvoiceIssueService.STATUS_ISSUED);
        assertThat(order.getInvoiceType()).isEqualTo(InvoicePolicy.TYPE_PERSONAL);
        ObjectNode inv = publishedInvoice();
        assertThat(inv.get("BuyerLegalName").asText()).isEmpty();
        assertThat(inv.get("BuyerFullName").asText()).isEqualTo("Trần Nhận");
        assertThat(inv.get("ReceiverName").asText()).isEqualTo("Trần Nhận");
        assertThat(inv.get("BuyerPhoneNumber").asText()).isEqualTo("0922222222");
        assertThat(inv.get("BuyerTaxCode").asText()).isEmpty();
        assertThat(inv.get("IsSendEmail").asBoolean()).isFalse();
        assertThat(inv.get("PaymentMethodName").asText()).isEqualTo("TM");
        assertThat(inv.get("OriginalInvoiceDetail").get(0).get("UnitName").asText()).isEqualTo("Vận đơn");
    }

    @Test
    void backfill_senderPays_personalUsesSender() {
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setPickedUpAt(Instant.now().minusSeconds(4 * 3600));
        publishOk();
        service.backfillOne("VT0001ABCD", "ketoan");
        ObjectNode inv = publishedInvoice();
        assertThat(inv.get("BuyerLegalName").asText()).isEmpty();
        assertThat(inv.get("BuyerFullName").asText()).isEqualTo("Nguyễn Gửi");
        assertThat(inv.get("BuyerPhoneNumber").asText()).isEqualTo("0911111111");
    }

    @Test
    void backfill_companyRequested_issuesCompany() {
        order.setInvoiceRequested(true);
        order.setInvoiceTaxCode("0100233488");
        order.setInvoiceCompanyName("Cty ABC");
        order.setInvoiceCompanyAddress("1 Ly Thuong Kiet");
        order.setInvoiceEmail("a@abc.vn");
        publishOk();
        service.backfillOne("VT0001ABCD", "ketoan");
        assertThat(order.getInvoiceType()).isEqualTo(InvoicePolicy.TYPE_COMPANY);
        assertThat(publishedInvoice().get("BuyerTaxCode").asText()).isEqualTo("0100233488");
    }

    @Test
    void backfill_markedOrIssued_skipped_noPublish() {
        order.setInvoiceStatus(MeInvoiceIssueService.STATUS_MANUAL);
        assertThat(service.backfillOne("VT0001ABCD", "k")).isEqualTo("ALREADY");
        order.setInvoiceStatus(MeInvoiceIssueService.STATUS_ISSUED);
        assertThat(service.backfillOne("VT0001ABCD", "k")).isEqualTo("ALREADY");
        verify(client, never()).publish(any());
    }

    @Test
    void backfill_unpaidResidue_skipped() {
        order.setFareAmount(new BigDecimal("110000"));
        order.setPaidAmount(new BigDecimal("50000"));
        assertThat(service.backfillOne("VT0001ABCD", "k")).isEqualTo("UNPAID_RESIDUE");
        verify(client, never()).publish(any());
    }

    @Test
    void markPersonal_thenSaveInfoAndIssueBlocked_unmarkRestores() {
        service.markPersonalIssued("VT0001ABCD", true, "ketoan");
        assertThat(order.getInvoiceStatus()).isEqualTo(MeInvoiceIssueService.STATUS_MANUAL);
        assertThat(order.getInvoiceType()).isEqualTo(InvoicePolicy.TYPE_PERSONAL);

        assertThatThrownBy(() ->
            service.saveInfo("VT0001ABCD", new MeInvoiceIssueService.InvoiceInfoRequest(true, "0100233488", "A", "B", "a@b.vn"), "u")
        ).isInstanceOf(BadRequestAlertException.class);
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", validReq(), "u")).isInstanceOf(BadRequestAlertException.class);

        service.markPersonalIssued("VT0001ABCD", false, "ketoan");
        assertThat(order.getInvoiceStatus()).isNull();
        assertThat(order.getInvoiceType()).isNull();
        verify(client, never()).publish(any());
    }

    @Test
    void markPersonal_alreadyIssued_rejects() {
        order.setInvoiceStatus(MeInvoiceIssueService.STATUS_ISSUED);
        assertThatThrownBy(() -> service.markPersonalIssued("VT0001ABCD", true, "k")).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void autoIssue_onCredit_skipped() {
        order.setId(7L);
        order.setOnCredit(true);
        when(orderRepo.findById(7L)).thenReturn(Optional.of(order));
        assertThat(service.autoIssueOne(7L)).isEqualTo("ON_CREDIT");
        verify(client, never()).publish(any());
    }

    @Test
    void autoIssue_legacySkipped_issuesPersonal() {
        order.setId(7L);
        order.setInvoiceStatus(MeInvoiceIssueService.STATUS_SKIPPED);
        when(orderRepo.findById(7L)).thenReturn(Optional.of(order));
        publishOk();
        assertThat(service.autoIssueOne(7L)).isEqualTo(MeInvoiceIssueService.STATUS_ISSUED);
        assertThat(order.getInvoiceType()).isEqualTo(InvoicePolicy.TYPE_PERSONAL);
    }

    @Test
    void autoIssue_failedNotRetried() {
        order.setId(7L);
        order.setInvoiceStatus(MeInvoiceIssueService.STATUS_FAILED);
        when(orderRepo.findById(7L)).thenReturn(Optional.of(order));
        assertThat(service.autoIssueOne(7L)).isEqualTo("ALREADY");
        verify(client, never()).publish(any());
    }

    @Test
    void onDelivered_toggleOn_leavesItToScheduler() {
        IntegrationConfig cfg = new IntegrationConfig();
        cfg.setMisaAutoIssueEnabled(true);
        cfg.setMisaAutoIssueSince(Instant.now().minusSeconds(3600));
        when(configRepo.findAll()).thenReturn(List.of(cfg));
        order.setInvoiceRequested(true);
        service.onOrderDelivered(new OrderDeliveredEvent("VT0001ABCD"));
        verify(client, never()).publish(any());
        assertThat(order.getInvoiceStatus()).isNull();
    }

    @Test
    void onDelivered_toggleOff_legacySkipsNotRequested() {
        when(configRepo.findAll()).thenReturn(List.of());
        service.onOrderDelivered(new OrderDeliveredEvent("VT0001ABCD"));
        assertThat(order.getInvoiceStatus()).isEqualTo(MeInvoiceIssueService.STATUS_SKIPPED);
        verify(client, never()).publish(any());
    }

    @Test
    void issueManual_unknownOrder_404() {
        when(orderRepo.findOneByOrderCodeOrDraftCode("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.issueManual("NOPE", validReq(), "u")).isInstanceOfSatisfying(ResponseStatusException.class, e ->
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
        );
    }
}
