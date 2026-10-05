package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicInvoiceServiceTest {

    private ShipmentOrderRepository orders;
    private OrderEventRepository events;
    private MeInvoiceIssueService issue;
    private TaxCodeLookupService tax;
    private PublicInvoiceService service;
    private ShipmentOrder order;

    @BeforeEach
    void setUp() {
        orders = mock(ShipmentOrderRepository.class);
        events = mock(OrderEventRepository.class);
        issue = mock(MeInvoiceIssueService.class);
        tax = mock(TaxCodeLookupService.class);
        service = new PublicInvoiceService(orders, events, issue, tax);

        order = new ShipmentOrder();
        order.setId(1L);
        order.setOrderCode("BC0310AAAA");
        order.setPublicTrackingAllowed(true);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setSenderPhone("0912345678");
        order.setReceiverPhone("0987654321");
        order.setFareAmount(new BigDecimal("50000"));
        order.setPaidAmount(new BigDecimal("50000"));
        when(orders.findOneByOrderCodeOrDraftCode("BC0310AAAA")).thenReturn(Optional.of(order));
        when(events.findByOrder_IdOrderByEventAtAsc(1L)).thenReturn(List.of());
        when(tax.lookup(anyString())).thenReturn(
            Map.of("ok", true, "taxCode", "0101243150", "companyName", "CONG TY A", "address", "Ha Noi")
        );
    }

    private PublicInvoiceService.SubmitRequest req(String tail) {
        return new PublicInvoiceService.SubmitRequest("BC0310AAAA", tail, "0101243150", "kt@a.vn");
    }

    @Test
    void receiverTailCannotIssueWhenSenderPays() {
        assertThatThrownBy(() -> service.submit(req("4321"), "ip1")).isInstanceOf(BadRequestAlertException.class);
        verify(issue, never()).issueManual(anyString(), any(), anyString());
        verify(issue, never()).saveInfo(anyString(), any(), anyString());
    }

    @Test
    void beforePaymentMilestoneSavesInfoForAutoIssue() {
        when(issue.paymentReached(order)).thenReturn(false);
        PublicInvoiceService.SubmitResult r = service.submit(req("5678"), "ip1");
        assertThat(r.action()).isEqualTo("SAVED");
        verify(issue).saveInfo(
            eq("BC0310AAAA"),
            eq(new MeInvoiceIssueService.InvoiceInfoRequest(true, "0101243150", "CONG TY A", "Ha Noi", "kt@a.vn")),
            eq(PublicInvoiceService.ACTOR)
        );
        verify(issue, never()).issueManual(anyString(), any(), anyString());
    }

    @Test
    void withinThreeHoursIssuesWithLookedUpCompany() {
        order.setPickedUpAt(Instant.now().minus(Duration.ofHours(1)));
        when(issue.paymentReached(order)).thenReturn(true);
        ShipmentOrder done = new ShipmentOrder();
        done.setInvoiceStatus("ISSUED");
        done.setInvoiceNo("00000123");
        when(issue.issueManual(eq("BC0310AAAA"), any(), eq(PublicInvoiceService.ACTOR))).thenReturn(done);
        PublicInvoiceService.SubmitResult r = service.submit(req("5678"), "ip1");
        assertThat(r.action()).isEqualTo("ISSUED");
        assertThat(r.invoiceNo()).isEqualTo("00000123");
    }

    @Test
    void senderPaidOverThreeHoursButNotDeliveredStillIssues() {
        order.setPickedUpAt(Instant.now().minus(Duration.ofHours(4)));
        when(issue.paymentReached(order)).thenReturn(true);
        ShipmentOrder done = new ShipmentOrder();
        done.setInvoiceStatus("ISSUED");
        when(issue.issueManual(eq("BC0310AAAA"), any(), eq(PublicInvoiceService.ACTOR))).thenReturn(done);
        assertThat(service.submit(req("5678"), "ip1").action()).isEqualTo("ISSUED");
    }

    @Test
    void deliveredAndOverThreeHoursIsBlocked() {
        order.setPickedUpAt(Instant.now().minus(Duration.ofHours(6)));
        order.setStatus(OrderStatus.DELIVERED);
        OrderEvent pod = new OrderEvent();
        pod.setAction("POD");
        pod.setEventAt(Instant.now().minus(Duration.ofHours(4)));
        when(events.findByOrder_IdOrderByEventAtAsc(1L)).thenReturn(List.of(pod));
        when(issue.paymentReached(order)).thenReturn(true);
        assertThatThrownBy(() -> service.submit(req("5678"), "ip1")).hasMessageContaining("quá 3 tiếng");
        verify(issue, never()).issueManual(anyString(), any(), anyString());
    }

    @Test
    void unpaidResidueIsBlocked() {
        order.setPickedUpAt(Instant.now().minus(Duration.ofMinutes(30)));
        order.setPaidAmount(new BigDecimal("20000"));
        when(issue.paymentReached(order)).thenReturn(true);
        assertThatThrownBy(() -> service.submit(req("5678"), "ip1")).hasMessageContaining("chưa thanh toán");
        verify(issue, never()).issueManual(anyString(), any(), anyString());
    }

    @Test
    void alreadyIssuedIsBlocked() {
        order.setInvoiceStatus("ISSUED");
        assertThatThrownBy(() -> service.submit(req("5678"), "ip1")).hasMessageContaining("đã xuất");
    }

    @Test
    void tooManyWrongTailsLocksOrder() {
        for (int i = 0; i < 10; i++) {
            try {
                service.submit(req("0000"), "ip" + i);
            } catch (Exception ignored) {
                // expected
            }
        }
        assertThatThrownBy(() -> service.submit(req("5678"), "ipX")).hasMessageContaining("quá nhiều lần");
    }
}
