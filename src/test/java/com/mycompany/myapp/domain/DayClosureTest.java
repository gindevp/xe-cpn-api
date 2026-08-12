package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.DayClosureTestSamples.*;
import static com.mycompany.myapp.domain.OfficeTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DayClosureTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DayClosure.class);
        DayClosure dayClosure1 = getDayClosureSample1();
        DayClosure dayClosure2 = new DayClosure();
        assertThat(dayClosure1).isNotEqualTo(dayClosure2);

        dayClosure2.setId(dayClosure1.getId());
        assertThat(dayClosure1).isEqualTo(dayClosure2);

        dayClosure2 = getDayClosureSample2();
        assertThat(dayClosure1).isNotEqualTo(dayClosure2);
    }

    @Test
    void officeTest() {
        DayClosure dayClosure = getDayClosureRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        dayClosure.setOffice(officeBack);
        assertThat(dayClosure.getOffice()).isEqualTo(officeBack);

        dayClosure.office(null);
        assertThat(dayClosure.getOffice()).isNull();
    }
}
