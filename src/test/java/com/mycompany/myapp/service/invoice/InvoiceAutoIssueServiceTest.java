package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.IntegrationConfig;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvoiceAutoIssueServiceTest {

    private MeInvoiceIssueService issueService;
    private ShipmentOrderRepository orderRepo;
    private OrderEventRepository eventRepo;
    private InvoiceAutoIssueService service;

    @BeforeEach
    void setUp() {
        issueService = mock(MeInvoiceIssueService.class);
        orderRepo = mock(ShipmentOrderRepository.class);
        eventRepo = mock(OrderEventRepository.class);
        service = new InvoiceAutoIssueService(issueService, orderRepo, eventRepo);
    }

    private IntegrationConfig enabledSince(Instant since) {
        IntegrationConfig cfg = new IntegrationConfig();
        cfg.setMisaAutoIssueEnabled(true);
        cfg.setMisaAutoIssueSince(since);
        when(issueService.autoIssueConfig()).thenReturn(cfg);
        when(issueService.autoIssueEnabled()).thenReturn(true);
        return cfg;
    }

    @Test
    void run_toggleOff_doesNothing() {
        when(issueService.autoIssueConfig()).thenReturn(null);
        assertThat(service.runAutoIssue(Instant.now())).isZero();
        verify(orderRepo, never()).findAutoInvoiceWarehouseInIds(any(), any(), any(), any());
    }

    @Test
    void run_enabledLessThan3hAgo_nothingDueYet() {
        Instant now = Instant.parse("2026-10-02T10:00:00Z");
        enabledSince(now.minus(Duration.ofHours(2)));
        assertThat(service.runAutoIssue(now)).isZero();
        verify(orderRepo, never()).findAutoInvoiceWarehouseInIds(any(), any(), any(), any());
    }

    @Test
    void run_windowIsSinceToNowMinus3h_issuesEachCandidateOnce() {
        Instant now = Instant.parse("2026-10-02T10:00:00Z");
        Instant since = now.minus(Duration.ofHours(5));
        enabledSince(since);
        when(orderRepo.findAutoInvoiceWarehouseInIds(any(), any(), any(), any())).thenReturn(List.of(1L, 2L));
        when(eventRepo.findAutoInvoiceDeliveredIds(any(), any(), any(), any())).thenReturn(List.of(2L, 3L));
        when(issueService.autoIssueOne(any())).thenReturn(MeInvoiceIssueService.STATUS_ISSUED);

        assertThat(service.runAutoIssue(now)).isEqualTo(3);

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(orderRepo).findAutoInvoiceWarehouseInIds(from.capture(), to.capture(), any(), any());
        assertThat(from.getValue()).isEqualTo(since);
        assertThat(to.getValue()).isEqualTo(now.minus(Duration.ofHours(3)));
        verify(issueService, times(3)).autoIssueOne(any());
    }

    @Test
    void backfill_empty_rejected() {
        when(issueService.isClientEnabled()).thenReturn(true);
        assertThatThrownBy(() -> service.startBackfill(List.of(" "), "k")).isInstanceOf(BadRequestAlertException.class);
        verify(issueService, never()).backfillOne(anyString(), eq("k"));
    }

    @Test
    void backfill_misaDisabled_rejected() {
        when(issueService.isClientEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.startBackfill(List.of("A"), "k")).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void row_lateWhenIssuedAfterDeadline_typeAndPayer() {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode("X1");
        o.setStatus(OrderStatus.DELIVERED);
        o.setPaymentTerm(PaymentTerm.NHAN_TRA);
        o.setGoodsFareAmount(new BigDecimal("110000"));
        o.setInvoiceStatus(MeInvoiceIssueService.STATUS_ISSUED);
        Instant paid = Instant.parse("2026-10-02T01:00:00Z");
        o.setInvoiceIssuedAt(paid.plus(Duration.ofHours(4)));

        InvoiceAutoIssueService.InvoiceRow row = InvoiceAutoIssueService.toRow(o, paid);

        assertThat(row.payer()).isEqualTo("RECEIVER");
        assertThat(row.deadlineAt()).isEqualTo(paid.plus(Duration.ofHours(3)));
        assertThat(row.late()).isTrue();
        assertThat(row.invoiceType()).isEqualTo(InvoicePolicy.TYPE_PERSONAL);
        assertThat(row.invoiceAmount()).isEqualByComparingTo("110000");
    }

    @Test
    void buyerProfile_onlyWhenPhoneIsPayer() {
        ShipmentOrder receiverPays = new ShipmentOrder();
        receiverPays.setPaymentTerm(PaymentTerm.NHAN_TRA);
        receiverPays.setSenderPhone("0911111111");
        receiverPays.setReceiverPhone("0922222222");
        receiverPays.setInvoiceTaxCode("0100233488");
        receiverPays.setInvoiceCompanyName("Cty Nhận");
        ShipmentOrder senderPays = new ShipmentOrder();
        senderPays.setPaymentTerm(PaymentTerm.GUI_TRA);
        senderPays.setSenderPhone("0911111111");
        senderPays.setInvoiceTaxCode("0103179782");
        senderPays.setInvoiceCompanyName("Cty Gửi");
        when(orderRepo.findInvoiceProfilesByPhone(eq("0911111111"), any())).thenReturn(List.of(receiverPays, senderPays));

        assertThat(service.buyerProfile("0911111111")).hasValueSatisfying(m -> assertThat(m.get("companyName")).isEqualTo("Cty Gửi"));
        assertThat(service.buyerProfile("09")).isEmpty();
    }
}
