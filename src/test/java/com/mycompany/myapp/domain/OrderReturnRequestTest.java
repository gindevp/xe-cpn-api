package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OrderReturnRequestTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderReturnRequestTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderReturnRequest.class);
        OrderReturnRequest orderReturnRequest1 = getOrderReturnRequestSample1();
        OrderReturnRequest orderReturnRequest2 = new OrderReturnRequest();
        assertThat(orderReturnRequest1).isNotEqualTo(orderReturnRequest2);

        orderReturnRequest2.setId(orderReturnRequest1.getId());
        assertThat(orderReturnRequest1).isEqualTo(orderReturnRequest2);

        orderReturnRequest2 = getOrderReturnRequestSample2();
        assertThat(orderReturnRequest1).isNotEqualTo(orderReturnRequest2);
    }

    @Test
    void orderTest() {
        OrderReturnRequest orderReturnRequest = getOrderReturnRequestRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderReturnRequest.setOrder(shipmentOrderBack);
        assertThat(orderReturnRequest.getOrder()).isEqualTo(shipmentOrderBack);
        assertThat(shipmentOrderBack.getReturnRequest()).isEqualTo(orderReturnRequest);

        orderReturnRequest.order(null);
        assertThat(orderReturnRequest.getOrder()).isNull();
        assertThat(shipmentOrderBack.getReturnRequest()).isNull();
    }
}
