package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.PricingRuleTestSamples.*;
import static com.mycompany.myapp.domain.RouteTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PricingRuleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PricingRule.class);
        PricingRule pricingRule1 = getPricingRuleSample1();
        PricingRule pricingRule2 = new PricingRule();
        assertThat(pricingRule1).isNotEqualTo(pricingRule2);

        pricingRule2.setId(pricingRule1.getId());
        assertThat(pricingRule1).isEqualTo(pricingRule2);

        pricingRule2 = getPricingRuleSample2();
        assertThat(pricingRule1).isNotEqualTo(pricingRule2);
    }

    @Test
    void routeTest() {
        PricingRule pricingRule = getPricingRuleRandomSampleGenerator();
        Route routeBack = getRouteRandomSampleGenerator();

        pricingRule.setRoute(routeBack);
        assertThat(pricingRule.getRoute()).isEqualTo(routeBack);

        pricingRule.route(null);
        assertThat(pricingRule.getRoute()).isNull();
    }
}
