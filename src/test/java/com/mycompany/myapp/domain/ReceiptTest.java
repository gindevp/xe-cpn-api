package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OfficeTestSamples.*;
import static com.mycompany.myapp.domain.ReceiptTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReceiptTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Receipt.class);
        Receipt receipt1 = getReceiptSample1();
        Receipt receipt2 = new Receipt();
        assertThat(receipt1).isNotEqualTo(receipt2);

        receipt2.setId(receipt1.getId());
        assertThat(receipt1).isEqualTo(receipt2);

        receipt2 = getReceiptSample2();
        assertThat(receipt1).isNotEqualTo(receipt2);
    }

    @Test
    void officeTest() {
        Receipt receipt = getReceiptRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        receipt.setOffice(officeBack);
        assertThat(receipt.getOffice()).isEqualTo(officeBack);

        receipt.office(null);
        assertThat(receipt.getOffice()).isNull();
    }
}
