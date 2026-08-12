package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.PricingChangeLogTestSamples.*;
import static com.mycompany.myapp.domain.PricingRuleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PricingChangeLogTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PricingChangeLog.class);
        PricingChangeLog pricingChangeLog1 = getPricingChangeLogSample1();
        PricingChangeLog pricingChangeLog2 = new PricingChangeLog();
        assertThat(pricingChangeLog1).isNotEqualTo(pricingChangeLog2);

        pricingChangeLog2.setId(pricingChangeLog1.getId());
        assertThat(pricingChangeLog1).isEqualTo(pricingChangeLog2);

        pricingChangeLog2 = getPricingChangeLogSample2();
        assertThat(pricingChangeLog1).isNotEqualTo(pricingChangeLog2);
    }

    @Test
    void pricingRuleTest() {
        PricingChangeLog pricingChangeLog = getPricingChangeLogRandomSampleGenerator();
        PricingRule pricingRuleBack = getPricingRuleRandomSampleGenerator();

        pricingChangeLog.setPricingRule(pricingRuleBack);
        assertThat(pricingChangeLog.getPricingRule()).isEqualTo(pricingRuleBack);

        pricingChangeLog.pricingRule(null);
        assertThat(pricingChangeLog.getPricingRule()).isNull();
    }
}
