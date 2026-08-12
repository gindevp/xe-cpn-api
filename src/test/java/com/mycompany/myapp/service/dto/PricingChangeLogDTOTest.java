package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PricingChangeLogDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PricingChangeLogDTO.class);
        PricingChangeLogDTO pricingChangeLogDTO1 = new PricingChangeLogDTO();
        pricingChangeLogDTO1.setId(1L);
        PricingChangeLogDTO pricingChangeLogDTO2 = new PricingChangeLogDTO();
        assertThat(pricingChangeLogDTO1).isNotEqualTo(pricingChangeLogDTO2);
        pricingChangeLogDTO2.setId(pricingChangeLogDTO1.getId());
        assertThat(pricingChangeLogDTO1).isEqualTo(pricingChangeLogDTO2);
        pricingChangeLogDTO2.setId(2L);
        assertThat(pricingChangeLogDTO1).isNotEqualTo(pricingChangeLogDTO2);
        pricingChangeLogDTO1.setId(null);
        assertThat(pricingChangeLogDTO1).isNotEqualTo(pricingChangeLogDTO2);
    }
}
