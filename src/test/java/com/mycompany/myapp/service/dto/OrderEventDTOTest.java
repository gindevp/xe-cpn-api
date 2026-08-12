package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderEventDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderEventDTO.class);
        OrderEventDTO orderEventDTO1 = new OrderEventDTO();
        orderEventDTO1.setId(1L);
        OrderEventDTO orderEventDTO2 = new OrderEventDTO();
        assertThat(orderEventDTO1).isNotEqualTo(orderEventDTO2);
        orderEventDTO2.setId(orderEventDTO1.getId());
        assertThat(orderEventDTO1).isEqualTo(orderEventDTO2);
        orderEventDTO2.setId(2L);
        assertThat(orderEventDTO1).isNotEqualTo(orderEventDTO2);
        orderEventDTO1.setId(null);
        assertThat(orderEventDTO1).isNotEqualTo(orderEventDTO2);
    }
}
