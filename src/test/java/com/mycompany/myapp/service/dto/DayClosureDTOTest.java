package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DayClosureDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DayClosureDTO.class);
        DayClosureDTO dayClosureDTO1 = new DayClosureDTO();
        dayClosureDTO1.setId(1L);
        DayClosureDTO dayClosureDTO2 = new DayClosureDTO();
        assertThat(dayClosureDTO1).isNotEqualTo(dayClosureDTO2);
        dayClosureDTO2.setId(dayClosureDTO1.getId());
        assertThat(dayClosureDTO1).isEqualTo(dayClosureDTO2);
        dayClosureDTO2.setId(2L);
        assertThat(dayClosureDTO1).isNotEqualTo(dayClosureDTO2);
        dayClosureDTO1.setId(null);
        assertThat(dayClosureDTO1).isNotEqualTo(dayClosureDTO2);
    }
}
