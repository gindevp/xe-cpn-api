package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OfficeTestSamples.*;
import static com.mycompany.myapp.domain.OrderLegTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static com.mycompany.myapp.domain.TripTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderLegTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderLeg.class);
        OrderLeg orderLeg1 = getOrderLegSample1();
        OrderLeg orderLeg2 = new OrderLeg();
        assertThat(orderLeg1).isNotEqualTo(orderLeg2);

        orderLeg2.setId(orderLeg1.getId());
        assertThat(orderLeg1).isEqualTo(orderLeg2);

        orderLeg2 = getOrderLegSample2();
        assertThat(orderLeg1).isNotEqualTo(orderLeg2);
    }

    @Test
    void orderTest() {
        OrderLeg orderLeg = getOrderLegRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderLeg.setOrder(shipmentOrderBack);
        assertThat(orderLeg.getOrder()).isEqualTo(shipmentOrderBack);

        orderLeg.order(null);
        assertThat(orderLeg.getOrder()).isNull();
    }

    @Test
    void fromOfficeTest() {
        OrderLeg orderLeg = getOrderLegRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        orderLeg.setFromOffice(officeBack);
        assertThat(orderLeg.getFromOffice()).isEqualTo(officeBack);

        orderLeg.fromOffice(null);
        assertThat(orderLeg.getFromOffice()).isNull();
    }

    @Test
    void toOfficeTest() {
        OrderLeg orderLeg = getOrderLegRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        orderLeg.setToOffice(officeBack);
        assertThat(orderLeg.getToOffice()).isEqualTo(officeBack);

        orderLeg.toOffice(null);
        assertThat(orderLeg.getToOffice()).isNull();
    }

    @Test
    void tripTest() {
        OrderLeg orderLeg = getOrderLegRandomSampleGenerator();
        Trip tripBack = getTripRandomSampleGenerator();

        orderLeg.setTrip(tripBack);
        assertThat(orderLeg.getTrip()).isEqualTo(tripBack);

        orderLeg.trip(null);
        assertThat(orderLeg.getTrip()).isNull();
    }
}
