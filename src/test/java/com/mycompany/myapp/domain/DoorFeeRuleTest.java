package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.DoorFeeRuleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DoorFeeRuleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DoorFeeRule.class);
        DoorFeeRule doorFeeRule1 = getDoorFeeRuleSample1();
        DoorFeeRule doorFeeRule2 = new DoorFeeRule();
        assertThat(doorFeeRule1).isNotEqualTo(doorFeeRule2);

        doorFeeRule2.setId(doorFeeRule1.getId());
        assertThat(doorFeeRule1).isEqualTo(doorFeeRule2);

        doorFeeRule2 = getDoorFeeRuleSample2();
        assertThat(doorFeeRule1).isNotEqualTo(doorFeeRule2);
    }
}
