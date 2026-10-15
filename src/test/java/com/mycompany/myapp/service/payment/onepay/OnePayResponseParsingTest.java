package com.mycompany.myapp.service.payment.onepay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.OnePayInvoice;
import org.junit.jupiter.api.Test;

class OnePayResponseParsingTest {

    private final ObjectMapper om = new ObjectMapper();
    private final OnePayClient client = new OnePayClient(om, mock(OnePayCallLogService.class));

    /** Cấu trúc phản hồi batch thật từ sandbox mtf.onepay.vn (status dạng chuỗi, accounts là object). */
    static final String BATCH_OK =
        "[{\"status\":\"200\",\"header\":\"\",\"body\":{\"id\":\"USRAC82EE59172E3C1B\",\"name\":\"CPN TEST\",\"state\":\"active\"," +
        "\"accounts\":{\"id\":\"ACC34EC19BFB1A9F7A9\",\"account_number\":\"8888880002120924\",\"bank_name\":\"BIDV\",\"swift_code\":\"BIDVVNVX\"," +
        "\"state\":\"active\",\"qr\":{\"data\":\"QR-ACCOUNT\",\"image\":\"iVBOR\"}}}}," +
        "{\"status\":\"201\",\"header\":\"\",\"body\":{\"id\":\"BILvyesZpKiy4S-2yCN\",\"amount\":\"10000\",\"state\":\"unpaid\"," +
        "\"reference\":\"CPNT1\",\"qr\":{\"data\":\"QR-INVOICE\",\"image\":\"iVBOR\"}}}]";

    @Test
    void batchSuccessParsed() throws Exception {
        OnePayClient.Result r = client.interpret(200, BATCH_OK);
        assertThat(r.ok()).isTrue();
        OnePayInvoice inv = new OnePayInvoice();
        OnePayAdminService.applyBatchResponse(inv, r.json());
        assertThat(inv.getOnepayUserId()).isEqualTo("USRAC82EE59172E3C1B");
        assertThat(inv.getAccountNumber()).isEqualTo("8888880002120924");
        assertThat(inv.getBankName()).isEqualTo("BIDV");
        assertThat(inv.getVaState()).isEqualTo("active");
        assertThat(inv.getOnepayInvoiceId()).isEqualTo("BILvyesZpKiy4S-2yCN");
        assertThat(inv.getQrData()).isEqualTo("QR-INVOICE");
    }

    @Test
    void accountsAsArrayAlsoParsed() throws Exception {
        OnePayInvoice inv = new OnePayInvoice();
        OnePayAdminService.applyBatchResponse(
            inv,
            om.readTree(
                "[{\"status\":\"200\",\"body\":{\"id\":\"U\",\"accounts\":[{\"account_number\":\"123\",\"qr\":{\"data\":\"Q\"}}]}}]"
            )
        );
        assertThat(inv.getAccountNumber()).isEqualTo("123");
        assertThat(inv.getQrData()).isEqualTo("Q");
    }

    @Test
    void errorCodeInHttp200IsFailure() {
        OnePayClient.Result r = client.interpret(200, "{\"errorCode\":5,\"message\":\"Partner doesn't have this user!\"}");
        assertThat(r.ok()).isFalse();
        assertThat(r.error()).contains("5").contains("Partner doesn't have this user");
    }

    @Test
    void failedBatchBlockIsFailure() {
        OnePayClient.Result r = client.interpret(
            200,
            "[{\"status\":\"500\",\"body\":{\"name\":\"INTERNAL_SERVER_ERROR\",\"errorCode\":4,\"message\":\"duplicate\"}}]"
        );
        assertThat(r.ok()).isFalse();
        assertThat(r.error()).contains("4");
        assertThat(client.interpret(502, "<html>").ok()).isFalse();
    }

    @Test
    void namesAndReferencesFitOnePayRules() {
        assertThat(OnePayAdminService.asciiName("Nguyễn Văn Đức (kế toán)"))
            .isEqualTo("Nguyen Van Duc ke toan")
            .matches("^[a-zA-Z0-9 ]{1,70}$");
        assertThat(OnePayAdminService.asciiName("x".repeat(90))).hasSize(70);
        String ref = OnePayAdminService.newReference();
        assertThat(ref).hasSizeLessThanOrEqualTo(20).matches("^[_a-zA-Z0-9.@]*$");
    }

    @Test
    void logStripsQrImages() {
        assertThat(OnePayCallLogService.stripImages("{\"qr\":{\"data\":\"D\",\"image\":\"iVBORw0KGgo\"}}")).isEqualTo(
            "{\"qr\":{\"data\":\"D\",\"image\":\"…\"}}"
        );
    }
}
