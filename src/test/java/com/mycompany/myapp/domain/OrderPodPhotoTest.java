package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OrderPodPhotoTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderPodPhotoTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderPodPhoto.class);
        OrderPodPhoto orderPodPhoto1 = getOrderPodPhotoSample1();
        OrderPodPhoto orderPodPhoto2 = new OrderPodPhoto();
        assertThat(orderPodPhoto1).isNotEqualTo(orderPodPhoto2);

        orderPodPhoto2.setId(orderPodPhoto1.getId());
        assertThat(orderPodPhoto1).isEqualTo(orderPodPhoto2);

        orderPodPhoto2 = getOrderPodPhotoSample2();
        assertThat(orderPodPhoto1).isNotEqualTo(orderPodPhoto2);
    }

    @Test
    void orderTest() {
        OrderPodPhoto orderPodPhoto = getOrderPodPhotoRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderPodPhoto.setOrder(shipmentOrderBack);
        assertThat(orderPodPhoto.getOrder()).isEqualTo(shipmentOrderBack);

        orderPodPhoto.order(null);
        assertThat(orderPodPhoto.getOrder()).isNull();
    }
}
