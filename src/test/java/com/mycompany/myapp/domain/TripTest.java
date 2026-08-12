package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.DriverTestSamples.*;
import static com.mycompany.myapp.domain.OfficeTestSamples.*;
import static com.mycompany.myapp.domain.RouteTestSamples.*;
import static com.mycompany.myapp.domain.TripTestSamples.*;
import static com.mycompany.myapp.domain.VehicleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TripTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Trip.class);
        Trip trip1 = getTripSample1();
        Trip trip2 = new Trip();
        assertThat(trip1).isNotEqualTo(trip2);

        trip2.setId(trip1.getId());
        assertThat(trip1).isEqualTo(trip2);

        trip2 = getTripSample2();
        assertThat(trip1).isNotEqualTo(trip2);
    }

    @Test
    void officeTest() {
        Trip trip = getTripRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        trip.setOffice(officeBack);
        assertThat(trip.getOffice()).isEqualTo(officeBack);

        trip.office(null);
        assertThat(trip.getOffice()).isNull();
    }

    @Test
    void routeTest() {
        Trip trip = getTripRandomSampleGenerator();
        Route routeBack = getRouteRandomSampleGenerator();

        trip.setRoute(routeBack);
        assertThat(trip.getRoute()).isEqualTo(routeBack);

        trip.route(null);
        assertThat(trip.getRoute()).isNull();
    }

    @Test
    void vehicleTest() {
        Trip trip = getTripRandomSampleGenerator();
        Vehicle vehicleBack = getVehicleRandomSampleGenerator();

        trip.setVehicle(vehicleBack);
        assertThat(trip.getVehicle()).isEqualTo(vehicleBack);

        trip.vehicle(null);
        assertThat(trip.getVehicle()).isNull();
    }

    @Test
    void driverTest() {
        Trip trip = getTripRandomSampleGenerator();
        Driver driverBack = getDriverRandomSampleGenerator();

        trip.setDriver(driverBack);
        assertThat(trip.getDriver()).isEqualTo(driverBack);

        trip.driver(null);
        assertThat(trip.getDriver()).isNull();
    }
}
