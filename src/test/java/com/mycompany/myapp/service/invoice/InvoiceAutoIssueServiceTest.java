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
import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
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

        InvoiceAutoIssueService.InvoiceRow row = InvoiceAutoIssueService.toRow(o, paid, paid);

        assertThat(row.payer()).isEqualTo("RECEIVER");
        assertThat(row.deadlineAt()).isEqualTo(paid.plus(Duration.ofHours(3)));
        assertThat(row.late()).isTrue();
        assertThat(row.invoiceType()).isEqualTo(InvoicePolicy.TYPE_PERSONAL);
        assertThat(row.invoiceAmount()).isEqualByComparingTo("110000");
    }

    @Test
    void row_notLateWithinGraceAfterDeadline() {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode("X2");
        o.setStatus(OrderStatus.DELIVERED);
        o.setPaymentTerm(PaymentTerm.NHAN_TRA);
        o.setInvoiceStatus(MeInvoiceIssueService.STATUS_ISSUED);
        Instant paid = Instant.parse("2026-10-02T01:00:00Z");

        o.setInvoiceIssuedAt(paid.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(15)));
        assertThat(InvoiceAutoIssueService.toRow(o, paid, paid).late()).isFalse();

        o.setInvoiceIssuedAt(paid.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(16)));
        assertThat(InvoiceAutoIssueService.toRow(o, paid, paid).late()).isTrue();
    }

    @Test
    void row_senderPaid_deadlineWaitsForDelivery() {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode("X3");
        o.setPaymentTerm(PaymentTerm.GUI_TRA);
        Instant pickedUp = Instant.parse("2026-10-02T01:00:00Z");
        o.setPickedUpAt(pickedUp);

        o.setStatus(OrderStatus.IN_TRANSIT);
        assertThat(InvoiceAutoIssueService.toRow(o, pickedUp, null).deadlineAt()).isNull();

        o.setStatus(OrderStatus.DELIVERED);
        Instant deliveredEarly = pickedUp.plus(Duration.ofHours(1));
        assertThat(InvoiceAutoIssueService.toRow(o, pickedUp, deliveredEarly).deadlineAt()).isEqualTo(pickedUp.plus(Duration.ofHours(3)));

        Instant deliveredLate = pickedUp.plus(Duration.ofDays(2));
        o.setInvoiceStatus(MeInvoiceIssueService.STATUS_ISSUED);
        o.setInvoiceIssuedAt(deliveredLate.plus(Duration.ofMinutes(5)));
        InvoiceAutoIssueService.InvoiceRow row = InvoiceAutoIssueService.toRow(o, pickedUp, deliveredLate);
        assertThat(row.deadlineAt()).isEqualTo(deliveredLate);
        assertThat(row.late()).isFalse();
    }

    private static ShipmentOrder invoicedBy(String code, String senderPhone, String receiverPhone, String tax, String company) {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode(code);
        o.setPaymentTerm(PaymentTerm.GUI_TRA);
        o.setSenderPhone(senderPhone);
        o.setReceiverPhone(receiverPhone);
        o.setInvoiceTaxCode(tax);
        o.setInvoiceCompanyName(company);
        return o;
    }

    @Test
    void buyerProfiles_distinctTaxCodes_newestFirst_senderOrReceiver_ignoresFormatting() {
        String phone = "0901234567";
        when(orderRepo.findInvoiceProfilesByPhone(any(), any())).thenReturn(
            List.of(
                invoicedBy("O5", "0901 234 567", "0911", "0101243150", "CTY A moi"),
                invoicedBy("O4", "0922", "+84 901 234 567", "0312345678", "Nguoi nhan"),
                invoicedBy("O3", phone, "0933", "0109876543", "CTY B"),
                invoicedBy("O2", phone, "0944", "0101243150", "CTY A cu"),
                invoicedBy("O1", "0999", "0888", "0100000000", "Khong lien quan")
            )
        );

        List<java.util.Map<String, String>> profiles = service.buyerProfiles("0901.234.567");

        assertThat(profiles).extracting(m -> m.get("taxCode")).containsExactly("0101243150", "0312345678", "0109876543");
        assertThat(profiles.get(0).get("companyName")).isEqualTo("CTY A moi");
        assertThat(service.buyerProfile(phone)).get().extracting(m -> m.get("fromOrderCode")).isEqualTo("O5");
    }

    private static ShipmentOrder senderPaysOrder(String code, OrderStatus status, String paid, String invoiceStatus) {
        ShipmentOrder o = new ShipmentOrder();
        o.setId((long) code.hashCode());
        o.setOrderCode(code);
        o.setStatus(status);
        o.setPaymentTerm(PaymentTerm.GUI_TRA);
        o.setFareAmount(new BigDecimal("395000"));
        o.setPaidAmount(paid == null ? null : new BigDecimal(paid));
        o.setInvoiceStatus(invoiceStatus);
        o.setPickedUpAt(Instant.parse("2026-10-02T02:00:00Z"));
        return o;
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_showsCancelledAndExceptionOrders_withOpenIssueType_excludesOnlyDrafts() {
        ShipmentOrder exception = senderPaysOrder("EXC", OrderStatus.RETURNING, "395000", null);
        OrderIssue open = new OrderIssue();
        open.setIssueType(IssueType.LOST);
        open.setIssueStatus(IssueStatus.OPEN);
        exception.setIssue(open);
        ShipmentOrder resolved = senderPaysOrder("RESOLVED", OrderStatus.CONFIRMED, "395000", null);
        OrderIssue done = new OrderIssue();
        done.setIssueType(IssueType.EXCEPTION);
        done.setIssueStatus(IssueStatus.RESOLVED);
        resolved.setIssue(done);
        when(orderRepo.findInvoiceWarehouseInBetween(any(), any(), any())).thenReturn(
            List.of(senderPaysOrder("CXL", OrderStatus.CANCELLED, "395000", null), exception, resolved)
        );
        when(eventRepo.findInvoiceDeliveredBetween(any(), any(), any())).thenReturn(List.of());

        List<InvoiceAutoIssueService.InvoiceRow> rows = service.list(
            java.time.LocalDate.parse("2026-10-02"),
            java.time.LocalDate.parse("2026-10-02"),
            java.util.Optional.empty()
        );

        assertThat(rows)
            .extracting(InvoiceAutoIssueService.InvoiceRow::orderCode, InvoiceAutoIssueService.InvoiceRow::openIssueType)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("CXL", null),
                org.assertj.core.groups.Tuple.tuple("EXC", "LOST"),
                org.assertj.core.groups.Tuple.tuple("RESOLVED", null)
            );
        ArgumentCaptor<java.util.Collection<OrderStatus>> excluded = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(orderRepo).findInvoiceWarehouseInBetween(any(), any(), excluded.capture());
        assertThat(excluded.getValue()).containsExactly(OrderStatus.DRAFT);
    }

    @Test
    void buyerProfile_matchesSenderEvenWhenReceiverPays() {
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
        when(orderRepo.findInvoiceProfilesByPhone(any(), any())).thenReturn(List.of(receiverPays, senderPays));

        assertThat(service.buyerProfile("0911111111")).hasValueSatisfying(m -> assertThat(m.get("companyName")).isEqualTo("Cty Nhận"));
        assertThat(service.buyerProfile("09")).isEmpty();
    }
}
