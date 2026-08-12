package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderReturnRequestDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderReturnRequestDTO.class);
        OrderReturnRequestDTO orderReturnRequestDTO1 = new OrderReturnRequestDTO();
        orderReturnRequestDTO1.setId(1L);
        OrderReturnRequestDTO orderReturnRequestDTO2 = new OrderReturnRequestDTO();
        assertThat(orderReturnRequestDTO1).isNotEqualTo(orderReturnRequestDTO2);
        orderReturnRequestDTO2.setId(orderReturnRequestDTO1.getId());
        assertThat(orderReturnRequestDTO1).isEqualTo(orderReturnRequestDTO2);
        orderReturnRequestDTO2.setId(2L);
        assertThat(orderReturnRequestDTO1).isNotEqualTo(orderReturnRequestDTO2);
        orderReturnRequestDTO1.setId(null);
        assertThat(orderReturnRequestDTO1).isNotEqualTo(orderReturnRequestDTO2);
    }
}
