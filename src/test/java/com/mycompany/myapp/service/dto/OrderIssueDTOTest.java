package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderIssueDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderIssueDTO.class);
        OrderIssueDTO orderIssueDTO1 = new OrderIssueDTO();
        orderIssueDTO1.setId(1L);
        OrderIssueDTO orderIssueDTO2 = new OrderIssueDTO();
        assertThat(orderIssueDTO1).isNotEqualTo(orderIssueDTO2);
        orderIssueDTO2.setId(orderIssueDTO1.getId());
        assertThat(orderIssueDTO1).isEqualTo(orderIssueDTO2);
        orderIssueDTO2.setId(2L);
        assertThat(orderIssueDTO1).isNotEqualTo(orderIssueDTO2);
        orderIssueDTO1.setId(null);
        assertThat(orderIssueDTO1).isNotEqualTo(orderIssueDTO2);
    }
}
