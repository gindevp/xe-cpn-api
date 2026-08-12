package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OfficeDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OfficeDTO.class);
        OfficeDTO officeDTO1 = new OfficeDTO();
        officeDTO1.setId(1L);
        OfficeDTO officeDTO2 = new OfficeDTO();
        assertThat(officeDTO1).isNotEqualTo(officeDTO2);
        officeDTO2.setId(officeDTO1.getId());
        assertThat(officeDTO1).isEqualTo(officeDTO2);
        officeDTO2.setId(2L);
        assertThat(officeDTO1).isNotEqualTo(officeDTO2);
        officeDTO1.setId(null);
        assertThat(officeDTO1).isNotEqualTo(officeDTO2);
    }
}
