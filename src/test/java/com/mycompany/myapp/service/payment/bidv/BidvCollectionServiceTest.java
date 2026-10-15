package com.mycompany.myapp.service.payment.bidv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.BankTransaction;
import com.mycompany.myapp.domain.BidvConfig;
import com.mycompany.myapp.domain.BidvTestBill;
import com.mycompany.myapp.repository.BankTransactionRepository;
import com.mycompany.myapp.repository.BidvConfigRepository;
import com.mycompany.myapp.repository.BidvTestBillRepository;
import com.mycompany.myapp.service.payment.bidv.BidvCollectionService.CallContext;
import com.mycompany.myapp.service.payment.bidv.BidvCollectionService.Outcome;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BidvCollectionServiceTest {

    private static final String SECRET = "S3cret";
    private static final String KEY = "cpnbidv_key";
    private static final CallContext CTX = new CallContext("203.0.113.10", KEY);

    private final ObjectMapper om = new ObjectMapper();
    private BidvConfigRepository configRepo;
    private BidvTestBillRepository billRepo;
    private BankTransactionRepository txRepo;
    private BidvCollectionService service;
    private BidvConfig cfg;
    private BidvTestBill bill;

    @BeforeEach
    void setUp() {
        configRepo = mock(BidvConfigRepository.class);
        billRepo = mock(BidvTestBillRepository.class);
        txRepo = mock(BankTransactionRepository.class);
        service = new BidvCollectionService(configRepo, billRepo, txRepo);

        cfg = new BidvConfig();
        cfg.setId(1L);
        cfg.setEnabled(true);
        cfg.setServiceId("XECPN");
        cfg.setSecretCode(SECRET);
        cfg.setVaPrefix("V3XVN");
        cfg.setApiKey(KEY);
        when(configRepo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(cfg));

        bill = new BidvTestBill();
        bill.setId(7L);
        bill.setCustomerId("T0001");
        bill.setCustomerName("Nguyễn Văn A");
        bill.setAmount(new BigDecimal("10000"));
        when(billRepo.findOneByCustomerIdIgnoreCase("T0001")).thenReturn(Optional.of(bill));
        when(billRepo.findOneByCustomerIdIgnoreCase(anyString())).thenAnswer(inv ->
            "T0001".equalsIgnoreCase(inv.getArgument(0)) ? Optional.of(bill) : Optional.empty()
        );
        when(billRepo.findByIdForUpdate(7L)).thenReturn(Optional.of(bill));
        when(txRepo.findOneByTransId(anyString())).thenReturn(Optional.empty());
        when(txRepo.findFirstByTargetTypeAndTargetIdOrderByIdDesc(eq(BankTransaction.TARGET_TEST), anyLong())).thenReturn(Optional.empty());
    }

    private JsonNode getBillReq(String customerId) {
        String sum = BidvChecksum.hash(SECRET + "+XECPN+" + customerId);
        return om.valueToTree(Map.of("serviceId", "XECPN", "customerId", customerId, "checksum", sum));
    }

    private JsonNode payBillReq(String transId, String customerId, String billId, String amount) {
        String sum = BidvChecksum.hash(SECRET + "+|" + transId + "+" + amount);
        return om.valueToTree(
            Map.of(
                "transId",
                transId,
                "transDate",
                "20261014170000",
                "customerId",
                customerId,
                "billId",
                billId,
                "amount",
                amount,
                "checksum",
                sum
            )
        );
    }

    // ---------------------------------------------------------------- getbill

    @Test
    void getBillReturnsBillWithPrefixStrippedAndAsciiName() {
        Outcome o = service.getBill(getBillReq("V3XVNT0001"), CTX);
        assertThat(o.resultCode()).isEqualTo("000");
        assertThat(o.body()).containsEntry("customer_id", "V3XVNT0001");
        assertThat(o.body()).containsEntry("customer_name", "Nguyen Van A");
        assertThat(o.body()).containsEntry("bill_id", "T0001");
        assertThat(o.body()).containsEntry("total_amount", "10000");
        assertThat(o.body()).containsEntry("type", "1");
        assertThat(o.checksumVariant()).isEqualTo(BidvChecksum.GETBILL_PLUS);
    }

    @Test
    void getBillAcceptsCustomerIdWithoutPrefixAndSnakeCase() {
        String sum = BidvChecksum.hash(SECRET + "XECPN" + "T0001");
        JsonNode req = om.valueToTree(Map.of("service_id", "XECPN", "customer_id", "T0001", "checksum", sum));
        Outcome o = service.getBill(req, CTX);
        assertThat(o.resultCode()).isEqualTo("000");
        assertThat(o.checksumVariant()).isEqualTo(BidvChecksum.GETBILL_CONCAT);
    }

    @Test
    void getBillUnknownCustomer() {
        assertThat(service.getBill(getBillReq("V3XVNX9999"), CTX).resultCode()).isEqualTo("011");
    }

    @Test
    void getBillAlreadyPaid() {
        when(txRepo.findFirstByTargetTypeAndTargetIdOrderByIdDesc(BankTransaction.TARGET_TEST, 7L)).thenReturn(
            Optional.of(tx("TX1", "T0001", "10000"))
        );
        assertThat(service.getBill(getBillReq("V3XVNT0001"), CTX).resultCode()).isEqualTo("023");
    }

    @Test
    void getBillMissingCustomer() {
        JsonNode req = om.valueToTree(Map.of("serviceId", "XECPN"));
        assertThat(service.getBill(req, CTX).resultCode()).isEqualTo("001");
    }

    @Test
    void disabledReturnsSystemError() {
        cfg.setEnabled(false);
        assertThat(service.getBill(getBillReq("V3XVNT0001"), CTX).resultCode()).isEqualTo("031");
    }

    @Test
    void ipWhitelistBlocksOthersAndAcceptsCidr() {
        cfg.setAllowedIps("198.51.100.0/24\n203.0.113.99");
        assertThat(service.getBill(getBillReq("V3XVNT0001"), CTX).resultCode()).isEqualTo("002");
        cfg.setAllowedIps("203.0.113.0/24");
        assertThat(service.getBill(getBillReq("V3XVNT0001"), CTX).resultCode()).isEqualTo("000");
    }

    @Test
    void apiKeyRequiredAndWrongKeyRejected() {
        assertThat(service.getBill(getBillReq("V3XVNT0001"), new CallContext("203.0.113.10", null)).resultCode()).isEqualTo("003");
        assertThat(service.getBill(getBillReq("V3XVNT0001"), new CallContext("203.0.113.10", "wrong")).resultCode()).isEqualTo("003");
        cfg.setApiKeyRequired(false);
        assertThat(service.getBill(getBillReq("V3XVNT0001"), new CallContext("203.0.113.10", null)).resultCode()).isEqualTo("000");
        assertThat(service.getBill(getBillReq("V3XVNT0001"), new CallContext("203.0.113.10", "wrong")).resultCode()).isEqualTo("003");
    }

    @Test
    void wrongServiceId() {
        String sum = BidvChecksum.hash(SECRET + "+OTHER+V3XVNT0001");
        JsonNode req = om.valueToTree(Map.of("serviceId", "OTHER", "customerId", "V3XVNT0001", "checksum", sum));
        assertThat(service.getBill(req, CTX).resultCode()).isEqualTo("006");
    }

    @Test
    void checksumRules() {
        JsonNode wrong = om.valueToTree(Map.of("serviceId", "XECPN", "customerId", "V3XVNT0001", "checksum", "abc="));
        assertThat(service.getBill(wrong, CTX).resultCode()).isEqualTo("004");

        JsonNode missing = om.valueToTree(Map.of("serviceId", "XECPN", "customerId", "V3XVNT0001"));
        assertThat(service.getBill(missing, CTX).resultCode()).isEqualTo("004");

        cfg.setChecksumRequired(false);
        Outcome o = service.getBill(missing, CTX);
        assertThat(o.resultCode()).isEqualTo("000");
        assertThat(o.checksumVariant()).isEqualTo("NONE");
        assertThat(service.getBill(wrong, CTX).resultCode()).isEqualTo("004");
    }

    // ---------------------------------------------------------------- paybill

    @Test
    void payBillRecordsTransaction() {
        Outcome o = service.payBill(payBillReq("TX1", "V3XVNT0001", "T0001", "10000"), CTX);
        assertThat(o.resultCode()).isEqualTo("000");
        ArgumentCaptor<BankTransaction> saved = ArgumentCaptor.forClass(BankTransaction.class);
        verify(txRepo).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getTransId()).isEqualTo("TX1");
        assertThat(saved.getValue().getTargetType()).isEqualTo(BankTransaction.TARGET_TEST);
        assertThat(saved.getValue().getTargetId()).isEqualTo(7L);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("10000");
        assertThat(saved.getValue().getTransDate()).isEqualTo("20261014170000");
    }

    @Test
    void payBillSameTransIdIsIdempotent() {
        when(txRepo.findOneByTransId("TX1")).thenReturn(Optional.of(tx("TX1", "T0001", "10000")));
        assertThat(service.payBill(payBillReq("TX1", "V3XVNT0001", "T0001", "10000"), CTX).resultCode()).isEqualTo("000");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void payBillTransIdReusedForOtherBill() {
        when(txRepo.findOneByTransId("TX1")).thenReturn(Optional.of(tx("TX1", "T0002", "10000")));
        assertThat(service.payBill(payBillReq("TX1", "V3XVNT0001", "T0001", "10000"), CTX).resultCode()).isEqualTo("023");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void payBillAlreadyPaidByOtherTransaction() {
        when(txRepo.findFirstByTargetTypeAndTargetIdOrderByIdDesc(BankTransaction.TARGET_TEST, 7L)).thenReturn(
            Optional.of(tx("TX0", "T0001", "10000"))
        );
        assertThat(service.payBill(payBillReq("TX2", "V3XVNT0001", "T0001", "10000"), CTX).resultCode()).isEqualTo("023");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void payBillAmountMismatch() {
        assertThat(service.payBill(payBillReq("TX1", "V3XVNT0001", "T0001", "9000"), CTX).resultCode()).isEqualTo("022");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void payBillWrongBillId() {
        assertThat(service.payBill(payBillReq("TX1", "V3XVNT0001", "OTHER", "10000"), CTX).resultCode()).isEqualTo("021");
    }

    @Test
    void payBillUnknownCustomer() {
        assertThat(service.payBill(payBillReq("TX1", "V3XVNX9", "X9", "10000"), CTX).resultCode()).isEqualTo("011");
    }

    @Test
    void payBillMissingFieldsAndBadAmount() {
        JsonNode missing = om.valueToTree(Map.of("transId", "TX1", "customerId", "V3XVNT0001"));
        assertThat(service.payBill(missing, CTX).resultCode()).isEqualTo("001");
        assertThat(service.payBill(payBillReq("TX1", "V3XVNT0001", "T0001", "abc"), CTX).resultCode()).isEqualTo("001");
        assertThat(service.payBill(payBillReq("TX1", "V3XVNT0001", "T0001", "-5"), CTX).resultCode()).isEqualTo("001");
    }

    @Test
    void payBillNumericAmountAccepted() {
        String sum = BidvChecksum.hash(SECRET + "+|TX9+10000");
        JsonNode req = om.valueToTree(
            Map.of("transId", "TX9", "customerId", "V3XVNT0001", "billId", "T0001", "amount", 10000, "checksum", sum)
        );
        assertThat(service.payBill(req, CTX).resultCode()).isEqualTo("000");
    }

    @Test
    void payBillBadChecksumNotSaved() {
        JsonNode req = om.valueToTree(
            Map.of("transId", "TX1", "customerId", "V3XVNT0001", "billId", "T0001", "amount", "10000", "checksum", "bad")
        );
        assertThat(service.payBill(req, CTX).resultCode()).isEqualTo("004");
        verify(txRepo, never()).saveAndFlush(any());
    }

    @Test
    void helpers() {
        assertThat(BidvCollectionService.normalizeCustomerId("V3XVN", "v3xvnT0001")).isEqualTo("T0001");
        assertThat(BidvCollectionService.normalizeCustomerId("V3XVN", "V3XVN")).isEqualTo("V3XVN");
        assertThat(BidvCollectionService.normalizeCustomerId(null, " t1 ")).isEqualTo("T1");
        assertThat(BidvCollectionService.ipAllowed(null, "1.2.3.4")).isTrue();
        assertThat(BidvCollectionService.ipAllowed("1.2.3.4, 5.6.7.0/24", "5.6.7.8")).isTrue();
        assertThat(BidvCollectionService.ipAllowed("1.2.3.4", null)).isFalse();
        assertThat(BidvCollectionService.ascii("Vé xe Hà Nội - Đà Nẵng!")).isEqualTo("Ve xe Ha Noi - Da Nang");
        assertThat(List.of(BidvCollectionService.plainAmount(new BigDecimal("250000.00")))).containsExactly("250000");
    }

    private static BankTransaction tx(String transId, String billId, String amount) {
        BankTransaction t = new BankTransaction();
        t.setId(1L);
        t.setTransId(transId);
        t.setBillId(billId);
        t.setAmount(new BigDecimal(amount));
        t.setTargetType(BankTransaction.TARGET_TEST);
        t.setTargetId(7L);
        return t;
    }
}
