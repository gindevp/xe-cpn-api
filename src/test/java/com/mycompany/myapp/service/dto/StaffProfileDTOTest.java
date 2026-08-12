package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StaffProfileDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(StaffProfileDTO.class);
        StaffProfileDTO staffProfileDTO1 = new StaffProfileDTO();
        staffProfileDTO1.setId(1L);
        StaffProfileDTO staffProfileDTO2 = new StaffProfileDTO();
        assertThat(staffProfileDTO1).isNotEqualTo(staffProfileDTO2);
        staffProfileDTO2.setId(staffProfileDTO1.getId());
        assertThat(staffProfileDTO1).isEqualTo(staffProfileDTO2);
        staffProfileDTO2.setId(2L);
        assertThat(staffProfileDTO1).isNotEqualTo(staffProfileDTO2);
        staffProfileDTO1.setId(null);
        assertThat(staffProfileDTO1).isNotEqualTo(staffProfileDTO2);
    }
}
