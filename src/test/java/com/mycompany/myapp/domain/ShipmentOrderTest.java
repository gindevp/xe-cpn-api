package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.CustomerTestSamples.*;
import static com.mycompany.myapp.domain.OfficeTestSamples.*;
import static com.mycompany.myapp.domain.OrderFareAdjustmentRequestTestSamples.*;
import static com.mycompany.myapp.domain.OrderIssueTestSamples.*;
import static com.mycompany.myapp.domain.OrderReturnRequestTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static com.mycompany.myapp.domain.TripTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ShipmentOrderTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShipmentOrder.class);
        ShipmentOrder shipmentOrder1 = getShipmentOrderSample1();
        ShipmentOrder shipmentOrder2 = new ShipmentOrder();
        assertThat(shipmentOrder1).isNotEqualTo(shipmentOrder2);

        shipmentOrder2.setId(shipmentOrder1.getId());
        assertThat(shipmentOrder1).isEqualTo(shipmentOrder2);

        shipmentOrder2 = getShipmentOrderSample2();
        assertThat(shipmentOrder1).isNotEqualTo(shipmentOrder2);
    }

    @Test
    void issueTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        OrderIssue orderIssueBack = getOrderIssueRandomSampleGenerator();

        shipmentOrder.setIssue(orderIssueBack);
        assertThat(shipmentOrder.getIssue()).isEqualTo(orderIssueBack);

        shipmentOrder.issue(null);
        assertThat(shipmentOrder.getIssue()).isNull();
    }

    @Test
    void returnRequestTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        OrderReturnRequest orderReturnRequestBack = getOrderReturnRequestRandomSampleGenerator();

        shipmentOrder.setReturnRequest(orderReturnRequestBack);
        assertThat(shipmentOrder.getReturnRequest()).isEqualTo(orderReturnRequestBack);

        shipmentOrder.returnRequest(null);
        assertThat(shipmentOrder.getReturnRequest()).isNull();
    }

    @Test
    void fareAdjustmentRequestTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        OrderFareAdjustmentRequest orderFareAdjustmentRequestBack = getOrderFareAdjustmentRequestRandomSampleGenerator();

        shipmentOrder.setFareAdjustmentRequest(orderFareAdjustmentRequestBack);
        assertThat(shipmentOrder.getFareAdjustmentRequest()).isEqualTo(orderFareAdjustmentRequestBack);

        shipmentOrder.fareAdjustmentRequest(null);
        assertThat(shipmentOrder.getFareAdjustmentRequest()).isNull();
    }

    @Test
    void senderCustomerTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        Customer customerBack = getCustomerRandomSampleGenerator();

        shipmentOrder.setSenderCustomer(customerBack);
        assertThat(shipmentOrder.getSenderCustomer()).isEqualTo(customerBack);

        shipmentOrder.senderCustomer(null);
        assertThat(shipmentOrder.getSenderCustomer()).isNull();
    }

    @Test
    void fromOfficeTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        shipmentOrder.setFromOffice(officeBack);
        assertThat(shipmentOrder.getFromOffice()).isEqualTo(officeBack);

        shipmentOrder.fromOffice(null);
        assertThat(shipmentOrder.getFromOffice()).isNull();
    }

    @Test
    void toOfficeTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        shipmentOrder.setToOffice(officeBack);
        assertThat(shipmentOrder.getToOffice()).isEqualTo(officeBack);

        shipmentOrder.toOffice(null);
        assertThat(shipmentOrder.getToOffice()).isNull();
    }

    @Test
    void hubOfficeTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        shipmentOrder.setHubOffice(officeBack);
        assertThat(shipmentOrder.getHubOffice()).isEqualTo(officeBack);

        shipmentOrder.hubOffice(null);
        assertThat(shipmentOrder.getHubOffice()).isNull();
    }

    @Test
    void finalToOfficeTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        shipmentOrder.setFinalToOffice(officeBack);
        assertThat(shipmentOrder.getFinalToOffice()).isEqualTo(officeBack);

        shipmentOrder.finalToOffice(null);
        assertThat(shipmentOrder.getFinalToOffice()).isNull();
    }

    @Test
    void currentTripTest() {
        ShipmentOrder shipmentOrder = getShipmentOrderRandomSampleGenerator();
        Trip tripBack = getTripRandomSampleGenerator();

        shipmentOrder.setCurrentTrip(tripBack);
        assertThat(shipmentOrder.getCurrentTrip()).isEqualTo(tripBack);

        shipmentOrder.currentTrip(null);
        assertThat(shipmentOrder.getCurrentTrip()).isNull();
    }
}
