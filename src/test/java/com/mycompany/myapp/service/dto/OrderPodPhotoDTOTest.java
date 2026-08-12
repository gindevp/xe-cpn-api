package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderPodPhotoDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderPodPhotoDTO.class);
        OrderPodPhotoDTO orderPodPhotoDTO1 = new OrderPodPhotoDTO();
        orderPodPhotoDTO1.setId(1L);
        OrderPodPhotoDTO orderPodPhotoDTO2 = new OrderPodPhotoDTO();
        assertThat(orderPodPhotoDTO1).isNotEqualTo(orderPodPhotoDTO2);
        orderPodPhotoDTO2.setId(orderPodPhotoDTO1.getId());
        assertThat(orderPodPhotoDTO1).isEqualTo(orderPodPhotoDTO2);
        orderPodPhotoDTO2.setId(2L);
        assertThat(orderPodPhotoDTO1).isNotEqualTo(orderPodPhotoDTO2);
        orderPodPhotoDTO1.setId(null);
        assertThat(orderPodPhotoDTO1).isNotEqualTo(orderPodPhotoDTO2);
    }
}
