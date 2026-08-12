package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OrderPaymentTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderPaymentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderPayment.class);
        OrderPayment orderPayment1 = getOrderPaymentSample1();
        OrderPayment orderPayment2 = new OrderPayment();
        assertThat(orderPayment1).isNotEqualTo(orderPayment2);

        orderPayment2.setId(orderPayment1.getId());
        assertThat(orderPayment1).isEqualTo(orderPayment2);

        orderPayment2 = getOrderPaymentSample2();
        assertThat(orderPayment1).isNotEqualTo(orderPayment2);
    }

    @Test
    void orderTest() {
        OrderPayment orderPayment = getOrderPaymentRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderPayment.setOrder(shipmentOrderBack);
        assertThat(orderPayment.getOrder()).isEqualTo(shipmentOrderBack);

        orderPayment.order(null);
        assertThat(orderPayment.getOrder()).isNull();
    }
}
