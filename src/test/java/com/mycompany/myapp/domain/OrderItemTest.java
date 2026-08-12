package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OrderItemTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderItem.class);
        OrderItem orderItem1 = getOrderItemSample1();
        OrderItem orderItem2 = new OrderItem();
        assertThat(orderItem1).isNotEqualTo(orderItem2);

        orderItem2.setId(orderItem1.getId());
        assertThat(orderItem1).isEqualTo(orderItem2);

        orderItem2 = getOrderItemSample2();
        assertThat(orderItem1).isNotEqualTo(orderItem2);
    }

    @Test
    void orderTest() {
        OrderItem orderItem = getOrderItemRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderItem.setOrder(shipmentOrderBack);
        assertThat(orderItem.getOrder()).isEqualTo(shipmentOrderBack);

        orderItem.order(null);
        assertThat(orderItem.getOrder()).isNull();
    }
}
