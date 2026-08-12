package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static com.mycompany.myapp.domain.TripOrderAssignmentTestSamples.*;
import static com.mycompany.myapp.domain.TripTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TripOrderAssignmentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TripOrderAssignment.class);
        TripOrderAssignment tripOrderAssignment1 = getTripOrderAssignmentSample1();
        TripOrderAssignment tripOrderAssignment2 = new TripOrderAssignment();
        assertThat(tripOrderAssignment1).isNotEqualTo(tripOrderAssignment2);

        tripOrderAssignment2.setId(tripOrderAssignment1.getId());
        assertThat(tripOrderAssignment1).isEqualTo(tripOrderAssignment2);

        tripOrderAssignment2 = getTripOrderAssignmentSample2();
        assertThat(tripOrderAssignment1).isNotEqualTo(tripOrderAssignment2);
    }

    @Test
    void tripTest() {
        TripOrderAssignment tripOrderAssignment = getTripOrderAssignmentRandomSampleGenerator();
        Trip tripBack = getTripRandomSampleGenerator();

        tripOrderAssignment.setTrip(tripBack);
        assertThat(tripOrderAssignment.getTrip()).isEqualTo(tripBack);

        tripOrderAssignment.trip(null);
        assertThat(tripOrderAssignment.getTrip()).isNull();
    }

    @Test
    void orderTest() {
        TripOrderAssignment tripOrderAssignment = getTripOrderAssignmentRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        tripOrderAssignment.setOrder(shipmentOrderBack);
        assertThat(tripOrderAssignment.getOrder()).isEqualTo(shipmentOrderBack);

        tripOrderAssignment.order(null);
        assertThat(tripOrderAssignment.getOrder()).isNull();
    }
}
