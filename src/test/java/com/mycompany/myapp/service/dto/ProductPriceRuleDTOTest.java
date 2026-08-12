package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ProductPriceRuleDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ProductPriceRuleDTO.class);
        ProductPriceRuleDTO productPriceRuleDTO1 = new ProductPriceRuleDTO();
        productPriceRuleDTO1.setId(1L);
        ProductPriceRuleDTO productPriceRuleDTO2 = new ProductPriceRuleDTO();
        assertThat(productPriceRuleDTO1).isNotEqualTo(productPriceRuleDTO2);
        productPriceRuleDTO2.setId(productPriceRuleDTO1.getId());
        assertThat(productPriceRuleDTO1).isEqualTo(productPriceRuleDTO2);
        productPriceRuleDTO2.setId(2L);
        assertThat(productPriceRuleDTO1).isNotEqualTo(productPriceRuleDTO2);
        productPriceRuleDTO1.setId(null);
        assertThat(productPriceRuleDTO1).isNotEqualTo(productPriceRuleDTO2);
    }
}
