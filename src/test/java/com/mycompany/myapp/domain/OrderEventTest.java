package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OrderEventTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderEventTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderEvent.class);
        OrderEvent orderEvent1 = getOrderEventSample1();
        OrderEvent orderEvent2 = new OrderEvent();
        assertThat(orderEvent1).isNotEqualTo(orderEvent2);

        orderEvent2.setId(orderEvent1.getId());
        assertThat(orderEvent1).isEqualTo(orderEvent2);

        orderEvent2 = getOrderEventSample2();
        assertThat(orderEvent1).isNotEqualTo(orderEvent2);
    }

    @Test
    void orderTest() {
        OrderEvent orderEvent = getOrderEventRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderEvent.setOrder(shipmentOrderBack);
        assertThat(orderEvent.getOrder()).isEqualTo(shipmentOrderBack);

        orderEvent.order(null);
        assertThat(orderEvent.getOrder()).isNull();
    }
}
