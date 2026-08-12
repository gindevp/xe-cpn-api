package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OfficeTestSamples.*;
import static com.mycompany.myapp.domain.StaffProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StaffProfileTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StaffProfile.class);
        StaffProfile staffProfile1 = getStaffProfileSample1();
        StaffProfile staffProfile2 = new StaffProfile();
        assertThat(staffProfile1).isNotEqualTo(staffProfile2);

        staffProfile2.setId(staffProfile1.getId());
        assertThat(staffProfile1).isEqualTo(staffProfile2);

        staffProfile2 = getStaffProfileSample2();
        assertThat(staffProfile1).isNotEqualTo(staffProfile2);
    }

    @Test
    void officeTest() {
        StaffProfile staffProfile = getStaffProfileRandomSampleGenerator();
        Office officeBack = getOfficeRandomSampleGenerator();

        staffProfile.setOffice(officeBack);
        assertThat(staffProfile.getOffice()).isEqualTo(officeBack);

        staffProfile.office(null);
        assertThat(staffProfile.getOffice()).isNull();
    }
}
