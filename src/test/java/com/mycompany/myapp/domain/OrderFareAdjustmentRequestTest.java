package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OrderFareAdjustmentRequestTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderFareAdjustmentRequestTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderFareAdjustmentRequest.class);
        OrderFareAdjustmentRequest orderFareAdjustmentRequest1 = getOrderFareAdjustmentRequestSample1();
        OrderFareAdjustmentRequest orderFareAdjustmentRequest2 = new OrderFareAdjustmentRequest();
        assertThat(orderFareAdjustmentRequest1).isNotEqualTo(orderFareAdjustmentRequest2);

        orderFareAdjustmentRequest2.setId(orderFareAdjustmentRequest1.getId());
        assertThat(orderFareAdjustmentRequest1).isEqualTo(orderFareAdjustmentRequest2);

        orderFareAdjustmentRequest2 = getOrderFareAdjustmentRequestSample2();
        assertThat(orderFareAdjustmentRequest1).isNotEqualTo(orderFareAdjustmentRequest2);
    }

    @Test
    void orderTest() {
        OrderFareAdjustmentRequest orderFareAdjustmentRequest = getOrderFareAdjustmentRequestRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderFareAdjustmentRequest.setOrder(shipmentOrderBack);
        assertThat(orderFareAdjustmentRequest.getOrder()).isEqualTo(shipmentOrderBack);
        assertThat(shipmentOrderBack.getFareAdjustmentRequest()).isEqualTo(orderFareAdjustmentRequest);

        orderFareAdjustmentRequest.order(null);
        assertThat(orderFareAdjustmentRequest.getOrder()).isNull();
        assertThat(shipmentOrderBack.getFareAdjustmentRequest()).isNull();
    }
}
