package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.SurchargePolicyTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SurchargePolicyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SurchargePolicy.class);
        SurchargePolicy surchargePolicy1 = getSurchargePolicySample1();
        SurchargePolicy surchargePolicy2 = new SurchargePolicy();
        assertThat(surchargePolicy1).isNotEqualTo(surchargePolicy2);

        surchargePolicy2.setId(surchargePolicy1.getId());
        assertThat(surchargePolicy1).isEqualTo(surchargePolicy2);

        surchargePolicy2 = getSurchargePolicySample2();
        assertThat(surchargePolicy1).isNotEqualTo(surchargePolicy2);
    }
}
