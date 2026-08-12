package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderFareAdjustmentRequestDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderFareAdjustmentRequestDTO.class);
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO1 = new OrderFareAdjustmentRequestDTO();
        orderFareAdjustmentRequestDTO1.setId(1L);
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO2 = new OrderFareAdjustmentRequestDTO();
        assertThat(orderFareAdjustmentRequestDTO1).isNotEqualTo(orderFareAdjustmentRequestDTO2);
        orderFareAdjustmentRequestDTO2.setId(orderFareAdjustmentRequestDTO1.getId());
        assertThat(orderFareAdjustmentRequestDTO1).isEqualTo(orderFareAdjustmentRequestDTO2);
        orderFareAdjustmentRequestDTO2.setId(2L);
        assertThat(orderFareAdjustmentRequestDTO1).isNotEqualTo(orderFareAdjustmentRequestDTO2);
        orderFareAdjustmentRequestDTO1.setId(null);
        assertThat(orderFareAdjustmentRequestDTO1).isNotEqualTo(orderFareAdjustmentRequestDTO2);
    }
}
