package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.ProductPriceRuleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ProductPriceRuleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ProductPriceRule.class);
        ProductPriceRule productPriceRule1 = getProductPriceRuleSample1();
        ProductPriceRule productPriceRule2 = new ProductPriceRule();
        assertThat(productPriceRule1).isNotEqualTo(productPriceRule2);

        productPriceRule2.setId(productPriceRule1.getId());
        assertThat(productPriceRule1).isEqualTo(productPriceRule2);

        productPriceRule2 = getProductPriceRuleSample2();
        assertThat(productPriceRule1).isNotEqualTo(productPriceRule2);
    }
}
