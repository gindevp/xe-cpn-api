package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.IntegrationConfigTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class IntegrationConfigTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(IntegrationConfig.class);
        IntegrationConfig integrationConfig1 = getIntegrationConfigSample1();
        IntegrationConfig integrationConfig2 = new IntegrationConfig();
        assertThat(integrationConfig1).isNotEqualTo(integrationConfig2);

        integrationConfig2.setId(integrationConfig1.getId());
        assertThat(integrationConfig1).isEqualTo(integrationConfig2);

        integrationConfig2 = getIntegrationConfigSample2();
        assertThat(integrationConfig1).isNotEqualTo(integrationConfig2);
    }
}
