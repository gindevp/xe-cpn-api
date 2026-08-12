package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DoorFeeRuleDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DoorFeeRuleDTO.class);
        DoorFeeRuleDTO doorFeeRuleDTO1 = new DoorFeeRuleDTO();
        doorFeeRuleDTO1.setId(1L);
        DoorFeeRuleDTO doorFeeRuleDTO2 = new DoorFeeRuleDTO();
        assertThat(doorFeeRuleDTO1).isNotEqualTo(doorFeeRuleDTO2);
        doorFeeRuleDTO2.setId(doorFeeRuleDTO1.getId());
        assertThat(doorFeeRuleDTO1).isEqualTo(doorFeeRuleDTO2);
        doorFeeRuleDTO2.setId(2L);
        assertThat(doorFeeRuleDTO1).isNotEqualTo(doorFeeRuleDTO2);
        doorFeeRuleDTO1.setId(null);
        assertThat(doorFeeRuleDTO1).isNotEqualTo(doorFeeRuleDTO2);
    }
}
