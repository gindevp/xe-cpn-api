package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.DriverTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DriverTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Driver.class);
        Driver driver1 = getDriverSample1();
        Driver driver2 = new Driver();
        assertThat(driver1).isNotEqualTo(driver2);

        driver2.setId(driver1.getId());
        assertThat(driver1).isEqualTo(driver2);

        driver2 = getDriverSample2();
        assertThat(driver1).isNotEqualTo(driver2);
    }
}
