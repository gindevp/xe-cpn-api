package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.OrderIssueTestSamples.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OrderIssueTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderIssue.class);
        OrderIssue orderIssue1 = getOrderIssueSample1();
        OrderIssue orderIssue2 = new OrderIssue();
        assertThat(orderIssue1).isNotEqualTo(orderIssue2);

        orderIssue2.setId(orderIssue1.getId());
        assertThat(orderIssue1).isEqualTo(orderIssue2);

        orderIssue2 = getOrderIssueSample2();
        assertThat(orderIssue1).isNotEqualTo(orderIssue2);
    }

    @Test
    void orderHistoryLinkDoesNotMutateCurrentPointer() {
        OrderIssue orderIssue = getOrderIssueRandomSampleGenerator();
        ShipmentOrder shipmentOrderBack = getShipmentOrderRandomSampleGenerator();

        orderIssue.setOrder(shipmentOrderBack);
        assertThat(orderIssue.getOrder()).isEqualTo(shipmentOrderBack);
        // history ManyToOne must NOT auto-set current OneToOne pointer
        assertThat(shipmentOrderBack.getIssue()).isNull();

        orderIssue.order(null);
        assertThat(orderIssue.getOrder()).isNull();
        assertThat(shipmentOrderBack.getIssue()).isNull();
    }
}
