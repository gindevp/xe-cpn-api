package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.ReceiptOrderLineTestSamples.*;
import static com.mycompany.myapp.domain.ReceiptTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReceiptOrderLineTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReceiptOrderLine.class);
        ReceiptOrderLine receiptOrderLine1 = getReceiptOrderLineSample1();
        ReceiptOrderLine receiptOrderLine2 = new ReceiptOrderLine();
        assertThat(receiptOrderLine1).isNotEqualTo(receiptOrderLine2);

        receiptOrderLine2.setId(receiptOrderLine1.getId());
        assertThat(receiptOrderLine1).isEqualTo(receiptOrderLine2);

        receiptOrderLine2 = getReceiptOrderLineSample2();
        assertThat(receiptOrderLine1).isNotEqualTo(receiptOrderLine2);
    }

    @Test
    void receiptTest() {
        ReceiptOrderLine receiptOrderLine = getReceiptOrderLineRandomSampleGenerator();
        Receipt receiptBack = getReceiptRandomSampleGenerator();

        receiptOrderLine.setReceipt(receiptBack);
        assertThat(receiptOrderLine.getReceipt()).isEqualTo(receiptBack);

        receiptOrderLine.receipt(null);
        assertThat(receiptOrderLine.getReceipt()).isNull();
    }

    @Test
    void orderTest() {
        ReceiptOrderLine receiptOrderLine = getReceiptOrderLineRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        receiptOrderLine.setOrder(shipmentOrderBack);
        assertThat(receiptOrderLine.getOrder()).isEqualTo(shipmentOrderBack);

        receiptOrderLine.order(null);
        assertThat(receiptOrderLine.getOrder()).isNull();
    }
}
