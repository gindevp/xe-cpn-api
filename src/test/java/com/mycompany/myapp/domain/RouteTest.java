package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OfficeTestSamples.*;
import static com.mycompany.myapp.domain.RouteTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RouteTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Route.class);
        Route route1 = getRouteSample1();
        Route route2 = new Route();
        assertThat(route1).isNotEqualTo(route2);

        route2.setId(route1.getId());
        assertThat(route1).isEqualTo(route2);

        route2 = getRouteSample2();
        assertThat(route1).isNotEqualTo(route2);
    }

    @Test
    void fromOfficeTest() {
        Route route = getRouteRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        route.setFromOffice(officeBack);
        assertThat(route.getFromOffice()).isEqualTo(officeBack);

        route.fromOffice(null);
        assertThat(route.getFromOffice()).isNull();
    }

    @Test
    void toOfficeTest() {
        Route route = getRouteRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        route.setToOffice(officeBack);
        assertThat(route.getToOffice()).isEqualTo(officeBack);

        route.toOffice(null);
        assertThat(route.getToOffice()).isNull();
    }
}
