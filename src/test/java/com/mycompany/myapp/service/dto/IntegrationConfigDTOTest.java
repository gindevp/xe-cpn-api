package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class IntegrationConfigDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(IntegrationConfigDTO.class);
        IntegrationConfigDTO integrationConfigDTO1 = new IntegrationConfigDTO();
        integrationConfigDTO1.setId(1L);
        IntegrationConfigDTO integrationConfigDTO2 = new IntegrationConfigDTO();
        assertThat(integrationConfigDTO1).isNotEqualTo(integrationConfigDTO2);
        integrationConfigDTO2.setId(integrationConfigDTO1.getId());
        assertThat(integrationConfigDTO1).isEqualTo(integrationConfigDTO2);
        integrationConfigDTO2.setId(2L);
        assertThat(integrationConfigDTO1).isNotEqualTo(integrationConfigDTO2);
        integrationConfigDTO1.setId(null);
        assertThat(integrationConfigDTO1).isNotEqualTo(integrationConfigDTO2);
    }
}
