package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderPaymentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderPaymentDTO.class);
        OrderPaymentDTO orderPaymentDTO1 = new OrderPaymentDTO();
        orderPaymentDTO1.setId(1L);
        OrderPaymentDTO orderPaymentDTO2 = new OrderPaymentDTO();
        assertThat(orderPaymentDTO1).isNotEqualTo(orderPaymentDTO2);
        orderPaymentDTO2.setId(orderPaymentDTO1.getId());
        assertThat(orderPaymentDTO1).isEqualTo(orderPaymentDTO2);
        orderPaymentDTO2.setId(2L);
        assertThat(orderPaymentDTO1).isNotEqualTo(orderPaymentDTO2);
        orderPaymentDTO1.setId(null);
        assertThat(orderPaymentDTO1).isNotEqualTo(orderPaymentDTO2);
    }
}
