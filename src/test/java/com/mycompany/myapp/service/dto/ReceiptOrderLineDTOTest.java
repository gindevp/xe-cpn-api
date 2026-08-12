package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReceiptOrderLineDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReceiptOrderLineDTO.class);
        ReceiptOrderLineDTO receiptOrderLineDTO1 = new ReceiptOrderLineDTO();
        receiptOrderLineDTO1.setId(1L);
        ReceiptOrderLineDTO receiptOrderLineDTO2 = new ReceiptOrderLineDTO();
        assertThat(receiptOrderLineDTO1).isNotEqualTo(receiptOrderLineDTO2);
        receiptOrderLineDTO2.setId(receiptOrderLineDTO1.getId());
        assertThat(receiptOrderLineDTO1).isEqualTo(receiptOrderLineDTO2);
        receiptOrderLineDTO2.setId(2L);
        assertThat(receiptOrderLineDTO1).isNotEqualTo(receiptOrderLineDTO2);
        receiptOrderLineDTO1.setId(null);
        assertThat(receiptOrderLineDTO1).isNotEqualTo(receiptOrderLineDTO2);
    }
}
