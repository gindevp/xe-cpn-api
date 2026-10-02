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
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
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
    private MeInvoiceIssueService service;
    private ShipmentOrder order;

    @BeforeEach
    void setUp() {
        orderRepo = mock(ShipmentOrderRepository.class);
        eventRepo = mock(OrderEventRepository.class);
        dayClosureGuard = mock(DayClosureGuard.class);
        client = mock(MisaMeInvoiceClient.class);
        service = new MeInvoiceIssueService(orderRepo, eventRepo, dayClosureGuard, client, new ObjectMapper(), true);

        order = new ShipmentOrder();
        order.setOrderCode("VT0001ABCD");
        order.setStatus(OrderStatus.DELIVERED);
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
        assertThat(inv.get("OriginalInvoiceDetail").get(0).get("UnitName").asText()).isEqualTo("Chuyến");

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

    @Test
    void issueManual_deliveredYesterday_rejects_noPublish() {
        deliveredAt(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1).toInstant());
        assertThatThrownBy(() -> service.issueManual("VT0001ABCD", validReq(), "u"))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("trong ngày giao");
        verify(client, never()).publish(any());
        verify(orderRepo, never()).save(any());
    }

    @Test
    void issueManual_deliveredToday_publishes() {
        deliveredAt(Instant.now());
        when(client.publish(any(ObjectNode.class))).thenReturn(
            new MisaMeInvoiceClient.PublishResult(true, false, "TX1", "0000123", "1C26TXE", "CODE1", "{}")
        );
        ShipmentOrder out = service.issueManual("VT0001ABCD", validReq(), "u");
        assertThat(out.getInvoiceStatus()).isEqualTo(MeInvoiceIssueService.STATUS_ISSUED);
    }

    @Test
    void issueManual_unknownOrder_404() {
        when(orderRepo.findOneByOrderCodeOrDraftCode("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.issueManual("NOPE", validReq(), "u")).isInstanceOfSatisfying(ResponseStatusException.class, e ->
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
        );
    }
}
