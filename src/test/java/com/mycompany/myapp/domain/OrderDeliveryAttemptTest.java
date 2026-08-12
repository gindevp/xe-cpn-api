package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OrderDeliveryAttemptTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderDeliveryAttemptTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderDeliveryAttempt.class);
        OrderDeliveryAttempt orderDeliveryAttempt1 = getOrderDeliveryAttemptSample1();
        OrderDeliveryAttempt orderDeliveryAttempt2 = new OrderDeliveryAttempt();
        assertThat(orderDeliveryAttempt1).isNotEqualTo(orderDeliveryAttempt2);

        orderDeliveryAttempt2.setId(orderDeliveryAttempt1.getId());
        assertThat(orderDeliveryAttempt1).isEqualTo(orderDeliveryAttempt2);

        orderDeliveryAttempt2 = getOrderDeliveryAttemptSample2();
        assertThat(orderDeliveryAttempt1).isNotEqualTo(orderDeliveryAttempt2);
    }

    @Test
    void orderTest() {
        OrderDeliveryAttempt orderDeliveryAttempt = getOrderDeliveryAttemptRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderDeliveryAttempt.setOrder(shipmentOrderBack);
        assertThat(orderDeliveryAttempt.getOrder()).isEqualTo(shipmentOrderBack);

        orderDeliveryAttempt.order(null);
        assertThat(orderDeliveryAttempt.getOrder()).isNull();
    }
}
