package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderDeliveryAttemptDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderDeliveryAttemptDTO.class);
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO1 = new OrderDeliveryAttemptDTO();
        orderDeliveryAttemptDTO1.setId(1L);
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO2 = new OrderDeliveryAttemptDTO();
        assertThat(orderDeliveryAttemptDTO1).isNotEqualTo(orderDeliveryAttemptDTO2);
        orderDeliveryAttemptDTO2.setId(orderDeliveryAttemptDTO1.getId());
        assertThat(orderDeliveryAttemptDTO1).isEqualTo(orderDeliveryAttemptDTO2);
        orderDeliveryAttemptDTO2.setId(2L);
        assertThat(orderDeliveryAttemptDTO1).isNotEqualTo(orderDeliveryAttemptDTO2);
        orderDeliveryAttemptDTO1.setId(null);
        assertThat(orderDeliveryAttemptDTO1).isNotEqualTo(orderDeliveryAttemptDTO2);
    }
}
