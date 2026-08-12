package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PricingRuleDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PricingRuleDTO.class);
        PricingRuleDTO pricingRuleDTO1 = new PricingRuleDTO();
        pricingRuleDTO1.setId(1L);
        PricingRuleDTO pricingRuleDTO2 = new PricingRuleDTO();
        assertThat(pricingRuleDTO1).isNotEqualTo(pricingRuleDTO2);
        pricingRuleDTO2.setId(pricingRuleDTO1.getId());
        assertThat(pricingRuleDTO1).isEqualTo(pricingRuleDTO2);
        pricingRuleDTO2.setId(2L);
        assertThat(pricingRuleDTO1).isNotEqualTo(pricingRuleDTO2);
        pricingRuleDTO1.setId(null);
        assertThat(pricingRuleDTO1).isNotEqualTo(pricingRuleDTO2);
    }
}
