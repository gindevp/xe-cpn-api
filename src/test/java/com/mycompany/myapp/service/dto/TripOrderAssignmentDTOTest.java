package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TripOrderAssignmentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(TripOrderAssignmentDTO.class);
        TripOrderAssignmentDTO tripOrderAssignmentDTO1 = new TripOrderAssignmentDTO();
        tripOrderAssignmentDTO1.setId(1L);
        TripOrderAssignmentDTO tripOrderAssignmentDTO2 = new TripOrderAssignmentDTO();
        assertThat(tripOrderAssignmentDTO1).isNotEqualTo(tripOrderAssignmentDTO2);
        tripOrderAssignmentDTO2.setId(tripOrderAssignmentDTO1.getId());
        assertThat(tripOrderAssignmentDTO1).isEqualTo(tripOrderAssignmentDTO2);
        tripOrderAssignmentDTO2.setId(2L);
        assertThat(tripOrderAssignmentDTO1).isNotEqualTo(tripOrderAssignmentDTO2);
        tripOrderAssignmentDTO1.setId(null);
        assertThat(tripOrderAssignmentDTO1).isNotEqualTo(tripOrderAssignmentDTO2);
    }
}
