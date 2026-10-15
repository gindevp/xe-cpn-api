package com.mycompany.myapp.service.payment.onepay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.BankTransaction;
import com.mycompany.myapp.domain.OnePayConfig;
import com.mycompany.myapp.domain.OnePayInvoice;
import com.mycompany.myapp.repository.BankTransactionRepository;
import com.mycompany.myapp.repository.OnePayConfigRepository;
import com.mycompany.myapp.repository.OnePayInvoiceRepository;
import com.mycompany.myapp.service.payment.onepay.OnePayIpnService.IpnRequest;
import com.mycompany.myapp.service.payment.onepay.OnePayIpnService.Outcome;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OnePayIpnServiceTest {

    static final String KEY = OnePaySignerTest.KEY;
    static final String IPN_BASE = "https://api.cpn.example/api/public/onepay/ipn";
    static final String PROXY_URL = "http://internal:8080/api/public/onepay/ipn";

    private OnePayConfigRepository configRepo;
    private OnePayInvoiceRepository invoiceRepo;
    private BankTransactionRepository txRepo;
    private OnePayIpnService service;
    private OnePayConfig cfg;
    private OnePayInvoice inv;

    @BeforeEach
    void setUp() {
        configRepo = mock(OnePayConfigRepository.class);
        invoiceRepo = mock(OnePayInvoiceRepository.class);
        txRepo = mock(BankTransactionRepository.class);
        service = new OnePayIpnService(configRepo, invoiceRepo, txRepo, new ObjectMapper());

        cfg = new OnePayConfig();
        cfg.setId(1L);
        cfg.setEnabled(true);
        cfg.setPartnerId("TESTPC1");
        cfg.setPartnerKey(KEY);
        cfg.setIpnUrl(IPN_BASE);
        when(configRepo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(cfg));

        inv = new OnePayInvoice();
        inv.setId(9L);
        inv.setUserReference("CPN26101512000012345");
        inv.setInvoiceReference("CPN26101512000012345");
        inv.setAmount(new BigDecimal("150000"));
        inv.setState(OnePayInvoice.STATE_UNPAID);
        inv.setAccountNumber("8888880000000403");
        when(invoiceRepo.findOneByUserReference(anyString())).thenReturn(Optional.empty());
        when(invoiceRepo.findOneByUserReference("CPN26101512000012345")).thenReturn(Optional.of(inv));
        when(invoiceRepo.findOneByOnepayInvoiceId(anyString())).thenReturn(Optional.empty());
        when(invoiceRepo.findOneByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(invoiceRepo.findByIdForUpdate(9L)).thenReturn(Optional.of(inv));
        when(txRepo.findOneByTransId(anyString())).thenReturn(Optional.empty());
    }

    static String body(String reference, String amount, String state, String merchant) {
        return (
            "{\"bank_txn_ref\":\"BIL-1\",\"create_time\":\"2026-10-15 12:00:00\",\"state\":\"" +
            state +
            "\",\"amount\":\"" +
            amount +
            "\",\"remark\":\"chuyen tien\",\"currency\":\"VND\",\"user\":{\"id\":\"USR1\",\"reference\":\"" +
            reference +
            "\",\"name\":\"CPN\"},\"account\":{\"id\":\"ACC1\",\"account_number\":\"8888880000000403\",\"bank_name\":\"BIDV\",\"swift_code\":\"BIDVVNVX\"},\"merchant_id\":\"" +
            merchant +
            "\"}"
        );
    }

    /** Ký như OnePay: uri = URL IPN đầy đủ kèm ?id=. */
    static IpnRequest signed(String signUrlBase, String requestUrl, String id, String body, String key) {
        String ts = OnePaySigner.timestamp(Instant.now());
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        String auth = OnePaySigner.authorization(
            "TESTPC1",
            key,
            "PUT",
            signUrlBase + "?id=" + id,
            Map.of(),
            Map.of("X-OP-Date", ts, "X-OP-Expires", "60000"),
            b,
            ts
        );
        Map<String, String> h = new HashMap<>();
        h.put("x-op-authorization", auth);
        h.put("x-op-date", ts);
        h.put("x-op-expires", "60000");
        return new IpnRequest("PUT", requestUrl, "id=" + id, id, h, b);
    }

    static IpnRequest ok(String id, String body) {
        return signed(IPN_BASE, PROXY_URL, id, body, KEY);
    }

    @Test
    void exactAmountMarksPaidAndRecordsTransaction() {
        Outcome o = service.handle(ok("TCL1", body("CPN26101512000012345", "150000", "Successful", "TESTPC1")));
        assertThat(o.httpStatus()).isEqualTo(200);
        assertThat(o.body()).containsEntry("error_code", "0");
        assertThat(o.result()).isEqualTo("PAID");
        assertThat(inv.getState()).isEqualTo(OnePayInvoice.STATE_PAID);
        assertThat(inv.getPaidTransId()).isEqualTo("TCL1");
        ArgumentCaptor<BankTransaction> tx = ArgumentCaptor.forClass(BankTransaction.class);
        verify(txRepo).saveAndFlush(tx.capture());
        assertThat(tx.getValue().getProvider()).isEqualTo("ONEPAY");
        assertThat(tx.getValue().getTargetType()).isEqualTo(BankTransaction.TARGET_ONEPAY_TEST);
        assertThat(tx.getValue().getTargetId()).isEqualTo(9L);
        assertThat(tx.getValue().getAmount()).isEqualByComparingTo("150000");
    }

    @Test
    void signatureOverRequestUrlAlsoAccepted() {
        cfg.setIpnUrl(null);
        IpnRequest r = signed(PROXY_URL, PROXY_URL, "TCL2", body("CPN26101512000012345", "150000", "Successful", "TESTPC1"), KEY);
        assertThat(service.handle(r).result()).isEqualTo("PAID");
    }

    @Test
    void wrongKeyRejectedWithoutTouchingMoney() {
        IpnRequest r = signed(IPN_BASE, PROXY_URL, "TCL3", body("CPN26101512000012345", "150000", "Successful", "TESTPC1"), "OTHERKEY");
        Outcome o = service.handle(r);
        assertThat(o.httpStatus()).isEqualTo(401);
        assertThat(o.body()).containsEntry("error_code", "1");
        assertThat(inv.getState()).isEqualTo(OnePayInvoice.STATE_UNPAID);
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void tamperedBodyRejected() {
        IpnRequest good = ok("TCL4", body("CPN26101512000012345", "150000", "Successful", "TESTPC1"));
        IpnRequest bad = new IpnRequest(
            good.method(),
            good.requestUrl(),
            good.queryString(),
            good.id(),
            good.headers(),
            body("CPN26101512000012345", "1500000", "Successful", "TESTPC1").getBytes(StandardCharsets.UTF_8)
        );
        assertThat(service.handle(bad).result()).isEqualTo("BAD_SIGNATURE");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void signatureCheckOffAcceptsButNotes() {
        cfg.setSignatureRequired(false);
        IpnRequest r = signed(IPN_BASE, PROXY_URL, "TCL5", body("CPN26101512000012345", "150000", "Successful", "TESTPC1"), "OTHERKEY");
        Outcome o = service.handle(r);
        assertThat(o.result()).isEqualTo("PAID");
        assertThat(o.message()).contains("bỏ qua chữ ký");
    }

    @Test
    void disabledRejected() {
        cfg.setEnabled(false);
        Outcome o = service.handle(ok("TCL6", body("CPN26101512000012345", "150000", "Successful", "TESTPC1")));
        assertThat(o.httpStatus()).isEqualTo(503);
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void duplicateIpnAcknowledgedOnce() {
        when(txRepo.findOneByTransId("TCL7")).thenReturn(Optional.of(new BankTransaction()));
        Outcome o = service.handle(ok("TCL7", body("CPN26101512000012345", "150000", "Successful", "TESTPC1")));
        assertThat(o.httpStatus()).isEqualTo(200);
        assertThat(o.result()).isEqualTo("DUPLICATE");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void differentAmountFlaggedMismatch() {
        Outcome o = service.handle(ok("TCL8", body("CPN26101512000012345", "100000", "Successful", "TESTPC1")));
        assertThat(o.result()).isEqualTo("AMOUNT_MISMATCH");
        assertThat(inv.getState()).isEqualTo(OnePayInvoice.STATE_AMOUNT_MISMATCH);
        assertThat(inv.getPaidAmount()).isEqualByComparingTo("100000");
        verify(txRepo).saveAndFlush(any());
    }

    @Test
    void secondTransferToPaidInvoiceIsExtraPayment() {
        inv.setState(OnePayInvoice.STATE_PAID);
        inv.setPaidTransId("TCL1");
        Outcome o = service.handle(ok("TCL9", body("CPN26101512000012345", "150000", "Successful", "TESTPC1")));
        assertThat(o.result()).isEqualTo("EXTRA_PAYMENT");
        assertThat(inv.getPaidTransId()).isEqualTo("TCL1");
        verify(txRepo).saveAndFlush(any());
    }

    @Test
    void unknownReferenceAcknowledgedAsUnmatched() {
        Outcome o = service.handle(ok("TCL10", body("SOMEONE_ELSE", "150000", "Successful", "TESTPC1")));
        when(invoiceRepo.findOneByAccountNumber("8888880000000403")).thenReturn(Optional.empty());
        assertThat(o.httpStatus()).isEqualTo(200);
        assertThat(o.result()).isEqualTo("UNMATCHED");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void matchesByAccountNumberWhenReferenceMissing() {
        when(invoiceRepo.findOneByAccountNumber("8888880000000403")).thenReturn(Optional.of(inv));
        Outcome o = service.handle(ok("TCL11", body("", "150000", "Successful", "TESTPC1")));
        assertThat(o.result()).isEqualTo("PAID");
    }

    @Test
    void nonSuccessfulStateIgnored() {
        Outcome o = service.handle(ok("TCL12", body("CPN26101512000012345", "150000", "Failed", "TESTPC1")));
        assertThat(o.httpStatus()).isEqualTo(200);
        assertThat(o.result()).isEqualTo("IGNORED_STATE");
        assertThat(inv.getState()).isEqualTo(OnePayInvoice.STATE_UNPAID);
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void otherMerchantRejected() {
        Outcome o = service.handle(ok("TCL13", body("CPN26101512000012345", "150000", "Successful", "TESTPC9")));
        assertThat(o.result()).isEqualTo("WRONG_MERCHANT");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void badJsonRejected() {
        IpnRequest r = new IpnRequest("PUT", PROXY_URL, "id=X", "X", new HashMap<>(), "not json".getBytes(StandardCharsets.UTF_8));
        assertThat(service.handle(r).httpStatus()).isEqualTo(400);
    }
}
