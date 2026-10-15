package com.mycompany.myapp.service.payment.onepay;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OnePaySignerTest {

    /** Partner sandbox công khai trong tài liệu OnePay. */
    static final String PARTNER = "TESTPC1";
    static final String KEY = "PC7525892B2E41A8845E210BF0114BA1";

    /** IPN mẫu trong code mẫu PaycollectTest.verifyOWSIpn của OnePay. */
    static final String IPN_URL = "https://dev-apigw.vinhomes.vn/web/api/v1/payment/onepay/NotifyFundTransfer?id=TCL7FB820851549C021";
    static final String IPN_BODY =
        "{\"bank_txn_ref\":\"BIL-MS4hA9p7QH-5up\",\"create_time\":\"2024-04-15 08:40:52\",\"state\":\"Successful\",\"amount\":\"150000\",\"remark\":\"chuyen tien 15\",\"currency\":\"VND\",\"user\":{\"id\":\"USR0D00D76D5DF3A5E8\",\"reference\":\"1713145213906\",\"name\":\"TRAN MON\"},\"account\":{\"id\":\"ACC40D69176842E7051\",\"account_number\":\"8888880000000403\",\"bank_name\":\"BIDV\",\"swift_code\":\"BIDVVNVX\"},\"merchant_id\":\"TESTPC1\"}";
    static final String IPN_AUTH =
        "OWS1-HMAC-SHA256 Credential=TESTPC1/20240415/onepay/paycollect/ows1_request,SignedHeaders=x-op-date;x-op-expires,Signature=a7cf76950862dbfca1b2e7f67f0dc1d7b7008606a4bdc4935d4007e43f27bd9a";

    static Map<String, String> ipnHeaders(String auth) {
        Map<String, String> h = new HashMap<>();
        h.put("x-op-authorization", auth);
        h.put("x-op-date", "20240415T014044Z");
        h.put("x-op-expires", "60000");
        h.put("content-type", "application/json");
        return h;
    }

    @Test
    void verifiesOnePaySampleIpn() {
        assertThat(OnePaySigner.verifyIpn(KEY, "PUT", IPN_URL, ipnHeaders(IPN_AUTH), IPN_BODY.getBytes(StandardCharsets.UTF_8))).isTrue();
    }

    @Test
    void rejectsTamperedAmountWrongKeyWrongUrlOrMissingHeader() {
        byte[] body = IPN_BODY.getBytes(StandardCharsets.UTF_8);
        byte[] tampered = IPN_BODY.replace("150000", "950000").getBytes(StandardCharsets.UTF_8);
        assertThat(OnePaySigner.verifyIpn(KEY, "PUT", IPN_URL, ipnHeaders(IPN_AUTH), tampered)).isFalse();
        assertThat(OnePaySigner.verifyIpn("PC7525892B2E41A8845E210BF0114BA5", "PUT", IPN_URL, ipnHeaders(IPN_AUTH), body)).isFalse();
        assertThat(OnePaySigner.verifyIpn(KEY, "PUT", IPN_URL.replace("TCL7", "TCL8"), ipnHeaders(IPN_AUTH), body)).isFalse();
        assertThat(OnePaySigner.verifyIpn(KEY, "POST", IPN_URL, ipnHeaders(IPN_AUTH), body)).isFalse();
        Map<String, String> noExpires = ipnHeaders(IPN_AUTH);
        noExpires.remove("x-op-expires");
        assertThat(OnePaySigner.verifyIpn(KEY, "PUT", IPN_URL, noExpires, body)).isFalse();
        assertThat(OnePaySigner.verifyIpn(KEY, "PUT", IPN_URL, ipnHeaders("garbage"), body)).isFalse();
        assertThat(OnePaySigner.verifyIpn("", "PUT", IPN_URL, ipnHeaders(IPN_AUTH), body)).isFalse();
    }

    @Test
    void signatureHexCaseInsensitive() {
        String upper = IPN_AUTH.replace(
            "a7cf76950862dbfca1b2e7f67f0dc1d7b7008606a4bdc4935d4007e43f27bd9a",
            "A7CF76950862DBFCA1B2E7F67F0DC1D7B7008606A4BDC4935D4007E43F27BD9A"
        );
        assertThat(OnePaySigner.verifyIpn(KEY, "PUT", IPN_URL, ipnHeaders(upper), IPN_BODY.getBytes(StandardCharsets.UTF_8))).isTrue();
    }

    @Test
    void outgoingAuthorizationRoundTripsThroughVerifier() {
        String ts = "20261015T050000Z";
        String url = "https://cpn.example/api/public/onepay/ipn?id=TCL1";
        byte[] body = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        String auth = OnePaySigner.authorization(
            PARTNER,
            KEY,
            "PUT",
            url,
            Map.of(),
            Map.of("X-OP-Expires", "3600", "X-OP-Date", ts),
            body,
            ts
        );
        assertThat(auth).startsWith(
            "OWS1-HMAC-SHA256 Credential=TESTPC1/20261015/onepay/paycollect/ows1_request,SignedHeaders=x-op-date;x-op-expires,Signature="
        );
        Map<String, String> h = Map.of("x-op-authorization", auth, "x-op-date", ts, "x-op-expires", "3600");
        assertThat(OnePaySigner.verifyIpn(KEY, "PUT", url, h, body)).isTrue();
    }

    @Test
    void uriEncodeMatchesOnePaySample() {
        assertThat(OnePaySigner.uriEncode("https://webhook.site/abc?id=TCL1", false)).isEqualTo("https%3A//webhook.site/abc%3Fid%3DTCL1");
        assertThat(OnePaySigner.uriEncode("/paycollect/api/v1/batchs", false)).isEqualTo("/paycollect/api/v1/batchs");
        assertThat(OnePaySigner.uriEncode("a b/c", true)).isEqualTo("a%20b%2Fc");
    }

    @Test
    void emptyAndUnsignedPayloadHashes() {
        String ts = "20261015T050000Z";
        Map<String, String> h = Map.of("x-op-date", ts, "x-op-expires", "3600");
        String scope = "20261015/onepay/paycollect/ows1_request";
        String empty = OnePaySigner.signature(KEY, "GET", "/p", Map.of(), new java.util.TreeMap<>(h), new byte[0], ts, scope);
        String unsigned = OnePaySigner.signature(KEY, "GET", "/p", Map.of(), new java.util.TreeMap<>(h), null, ts, scope);
        assertThat(empty).hasSize(64).isNotEqualTo(unsigned);
        assertThat(OnePaySigner.hex(OnePaySigner.sha256(new byte[0]))).isEqualTo(OnePaySigner.EMPTY_BODY_SHA256);
    }
}
