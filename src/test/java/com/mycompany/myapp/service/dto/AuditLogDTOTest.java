package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AuditLogDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AuditLogDTO.class);
        AuditLogDTO auditLogDTO1 = new AuditLogDTO();
        auditLogDTO1.setId(1L);
        AuditLogDTO auditLogDTO2 = new AuditLogDTO();
        assertThat(auditLogDTO1).isNotEqualTo(auditLogDTO2);
        auditLogDTO2.setId(auditLogDTO1.getId());
        assertThat(auditLogDTO1).isEqualTo(auditLogDTO2);
        auditLogDTO2.setId(2L);
        assertThat(auditLogDTO1).isNotEqualTo(auditLogDTO2);
        auditLogDTO1.setId(null);
        assertThat(auditLogDTO1).isNotEqualTo(auditLogDTO2);
    }
}
