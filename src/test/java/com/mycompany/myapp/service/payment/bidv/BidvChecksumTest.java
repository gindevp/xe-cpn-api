package com.mycompany.myapp.service.payment.bidv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BidvChecksumTest {

    @Test
    void hashIsBase64OfRawSha256() {
        assertThat(BidvChecksum.hash("abc")).isEqualTo("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=");
    }

    @Test
    void getBillMatchesSpecFormatAndPlainConcat() {
        String plus = BidvChecksum.hash("SECRET+XECPN+V3XVN0000000012345");
        String concat = BidvChecksum.hash("SECRETXECPNV3XVN0000000012345");
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN0000000012345", plus)).contains(BidvChecksum.GETBILL_PLUS);
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN0000000012345", concat)).contains(BidvChecksum.GETBILL_CONCAT);
        assertThat(BidvChecksum.matchGetBill("OTHER", "XECPN", "V3XVN0000000012345", plus)).isEmpty();
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN0000000012345", null)).isEmpty();
    }

    @Test
    void hmacMatchesRfc4231Vector() {
        // RFC 4231 test case 2
        assertThat(java.util.HexFormat.of().formatHex(BidvChecksum.hmac("Jefe", "what do ya want for nothing?"))).isEqualTo(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"
        );
    }

    @Test
    void officialTestVectorsFromBidvNote() {
        // "Công thức checksum mới: HMAC-SHA256" 14/10/2026 — secret giả dummysecret1234
        assertThat(
            BidvChecksum.matchGetBill("dummysecret1234", "CB9B001", "V3XVN0000000817851", "QCPgicW3xZGjAYRZ8yjg8BRMIyPQFja7OaH3dTg9rZ0=")
        ).contains(BidvChecksum.CONFIRMED);
        assertThat(
            BidvChecksum.matchPayBill("dummysecret1234", "BIDV_TX_202610140001", "250000", "9YvmXlzQukz01Fvn8iPvIL3ZrXKghkau7+QRuOeyWLs=")
        ).contains(BidvChecksum.CONFIRMED);
        assertThat(
            BidvChecksum.matchPayBill(
                "dummysecret1234",
                "BIDV_TX_202610140001",
                "250000.00",
                "9YvmXlzQukz01Fvn8iPvIL3ZrXKghkau7+QRuOeyWLs="
            )
        ).contains("HMAC_CONCAT_INT_FULL_B64");
    }

    @Test
    void confirmedFormulaMatchesFirst() {
        String getBill = java.util.Base64.getEncoder().encodeToString(BidvChecksum.hmac("SECRET", "SECRETXECPNV3XVN01"));
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN01", getBill)).contains(BidvChecksum.CONFIRMED);
        assertThat(BidvChecksum.getBillChecksums("SECRET", "XECPN", "V3XVN01").keySet().iterator().next()).isEqualTo(
            BidvChecksum.CONFIRMED
        );

        String payBill = java.util.Base64.getEncoder().encodeToString(BidvChecksum.hmac("SECRET", "SECRETTX00110000"));
        assertThat(BidvChecksum.matchPayBill("SECRET", "TX001", "10000", payBill)).contains(BidvChecksum.CONFIRMED);
        assertThat(BidvChecksum.matchPayBill("SECRET", "TX001", "10000.00", payBill)).contains("HMAC_CONCAT_INT_FULL_B64");
    }

    @Test
    void getBillAcceptsHmacVariants() {
        byte[] mac = BidvChecksum.hmac("SECRET", "XECPN+V3XVN01");
        String b64 = java.util.Base64.getEncoder().encodeToString(mac);
        String hex = java.util.HexFormat.of().formatHex(mac);
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN01", b64)).contains("HMAC_PLUS_B64");
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN01", hex.toUpperCase())).contains("HMAC_PLUS_HEX");

        String concat = java.util.Base64.getEncoder().encodeToString(BidvChecksum.hmac("SECRET", "XECPNV3XVN01"));
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN01", concat)).contains("HMAC_CONCAT_B64");

        String pipe = java.util.HexFormat.of().formatHex(BidvChecksum.hmac("SECRET", "XECPN|V3XVN01"));
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN01", pipe)).contains("HMAC_PIPE_HEX");

        String full = java.util.Base64.getEncoder().encodeToString(BidvChecksum.hmac("SECRET", "SECRET+XECPN+V3XVN01"));
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN01", full)).contains("HMAC_PLUS_FULL_B64");

        String wrongKey = java.util.Base64.getEncoder().encodeToString(BidvChecksum.hmac("OTHER", "XECPN+V3XVN01"));
        assertThat(BidvChecksum.matchGetBill("SECRET", "XECPN", "V3XVN01", wrongKey)).isEmpty();
    }

    @Test
    void payBillAcceptsHmacVariants() {
        String spec = java.util.Base64.getEncoder().encodeToString(BidvChecksum.hmac("K", "TX1+250000"));
        assertThat(BidvChecksum.matchPayBill("K", "TX1", "250000", spec)).contains("HMAC_PLUS_PIPE_B64");
        assertThat(BidvChecksum.matchPayBill("K", "TX1", "250000.00", spec)).contains("HMAC_PLUS_PIPE_INT_B64");
        String concat = java.util.HexFormat.of().formatHex(BidvChecksum.hmac("K", "TX1250000"));
        assertThat(BidvChecksum.matchPayBill("K", "TX1", "250000", concat)).contains("HMAC_CONCAT_HEX");
        assertThat(BidvChecksum.matchPayBill("K", "TX1", "250001", spec)).isEmpty();
    }

    @Test
    void noSecretSkipsHmacWithoutError() {
        assertThat(BidvChecksum.matchGetBill(null, "XECPN", "V3XVN01", "abc")).isEmpty();
        assertThat(BidvChecksum.getBillChecksums("", "XECPN", "V3XVN01")).doesNotContainKey("HMAC_PLUS_B64");
    }

    @Test
    void payBillMatchesSpecFormatAndIntegerAmount() {
        String spec = BidvChecksum.hash("MySecretCode+|BIDV_TX_202609040001+250000");
        assertThat(BidvChecksum.matchPayBill("MySecretCode", "BIDV_TX_202609040001", "250000", spec)).contains(
            BidvChecksum.PAYBILL_PLUS_PIPE
        );
        // BIDV gửi "250000.00" nhưng băm trên "250000"
        assertThat(BidvChecksum.matchPayBill("MySecretCode", "BIDV_TX_202609040001", "250000.00", spec)).contains(
            BidvChecksum.PAYBILL_PLUS_PIPE + "_INT"
        );
        String pipe = BidvChecksum.hash("MySecretCode|BIDV_TX_202609040001|250000");
        assertThat(BidvChecksum.matchPayBill("MySecretCode", "BIDV_TX_202609040001", "250000", pipe)).contains(BidvChecksum.PAYBILL_PIPE);
        assertThat(BidvChecksum.matchPayBill("MySecretCode", "BIDV_TX_202609040001", "250001", spec)).isEmpty();
    }
}
