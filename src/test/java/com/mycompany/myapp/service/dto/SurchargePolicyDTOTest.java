package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SurchargePolicyDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SurchargePolicyDTO.class);
        SurchargePolicyDTO surchargePolicyDTO1 = new SurchargePolicyDTO();
        surchargePolicyDTO1.setId(1L);
        SurchargePolicyDTO surchargePolicyDTO2 = new SurchargePolicyDTO();
        assertThat(surchargePolicyDTO1).isNotEqualTo(surchargePolicyDTO2);
        surchargePolicyDTO2.setId(surchargePolicyDTO1.getId());
        assertThat(surchargePolicyDTO1).isEqualTo(surchargePolicyDTO2);
        surchargePolicyDTO2.setId(2L);
        assertThat(surchargePolicyDTO1).isNotEqualTo(surchargePolicyDTO2);
        surchargePolicyDTO1.setId(null);
        assertThat(surchargePolicyDTO1).isNotEqualTo(surchargePolicyDTO2);
    }
}
