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
