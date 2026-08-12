package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderLegDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderLegDTO.class);
        OrderLegDTO orderLegDTO1 = new OrderLegDTO();
        orderLegDTO1.setId(1L);
        OrderLegDTO orderLegDTO2 = new OrderLegDTO();
        assertThat(orderLegDTO1).isNotEqualTo(orderLegDTO2);
        orderLegDTO2.setId(orderLegDTO1.getId());
        assertThat(orderLegDTO1).isEqualTo(orderLegDTO2);
        orderLegDTO2.setId(2L);
        assertThat(orderLegDTO1).isNotEqualTo(orderLegDTO2);
        orderLegDTO1.setId(null);
        assertThat(orderLegDTO1).isNotEqualTo(orderLegDTO2);
    }
}
