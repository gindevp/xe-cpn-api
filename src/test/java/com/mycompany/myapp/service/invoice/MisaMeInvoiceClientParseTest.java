package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MisaMeInvoiceClientParseTest {

    private MisaMeInvoiceClient client;

    @BeforeEach
    void setUp() {
        client = new MisaMeInvoiceClient(
            new ObjectMapper(),
            false,
            "https://api.meinvoice.vn/api/integration",
            "app",
            "0103179782",
            "user",
            "pass",
            "1C26MYY",
            2,
            "SN"
        );
    }

    @Test
    void parseDuplicated_extractsInvNoAndTxFromNestedBody() {
        String raw =
            """
            {"ErrorCode":"InvoiceDuplicated","publishInvoiceResult":{"TransactionID":"Q_ABC","InvNo":"00097121","InvSeries":"1C26MYY","InvCode":"M1"}}
            """;
        MisaMeInvoiceClient.PublishResult r = client.parsePublishResponse(raw);
        assertThat(r.duplicated()).isTrue();
        assertThat(r.transactionId()).isEqualTo("Q_ABC");
        assertThat(r.invNo()).isEqualTo("00097121");
        assertThat(r.invSeries()).isEqualTo("1C26MYY");
        assertThat(r.invCode()).isEqualTo("M1");
    }

    @Test
    void parseDuplicated_withoutIds_stillDuplicated() {
        MisaMeInvoiceClient.PublishResult r = client.parsePublishResponse("{\"ErrorCode\":\"InvoiceDuplicated\",\"Message\":\"dup\"}");
        assertThat(r.duplicated()).isTrue();
        assertThat(r.transactionId()).isNull();
        assertThat(r.invNo()).isNull();
    }

    @Test
    void parseDuplicated_findsInvNoDeepInArray() {
        String raw =
            """
            {"errorCode":"InvoiceDuplicated","data":[{"InvNo":"0000123","TransactionID":"TX9"}]}
            """;
        MisaMeInvoiceClient.PublishResult r = client.parseDuplicated(raw);
        assertThat(r.invNo()).isEqualTo("0000123");
        assertThat(r.transactionId()).isEqualTo("TX9");
    }
}
