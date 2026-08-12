package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderMoneyTest {

    @Test
    void due_isMaxZeroOfFareMinusPaid() {
        assertThat(OrderMoney.due(new BigDecimal("40000"), new BigDecimal("10000"))).isEqualByComparingTo("30000");
        assertThat(OrderMoney.due(new BigDecimal("40000"), new BigDecimal("40000"))).isEqualByComparingTo("0");
        assertThat(OrderMoney.due(new BigDecimal("10000"), new BigDecimal("30000"))).isEqualByComparingTo("0");
        assertThat(OrderMoney.due(null, null)).isEqualByComparingTo("0");
        assertThat(OrderMoney.due(new BigDecimal("5000"), null)).isEqualByComparingTo("5000");
    }

    @Test
    void due_fromOrder_andHasResidue() {
        ShipmentOrder o = new ShipmentOrder();
        o.setFareAmount(new BigDecimal("40000"));
        o.setPaidAmount(new BigDecimal("10000"));
        assertThat(OrderMoney.due(o)).isEqualByComparingTo("30000");
        assertThat(OrderMoney.hasUnpaidResidue(o)).isTrue();
        o.setPaidAmount(new BigDecimal("40000"));
        assertThat(OrderMoney.hasUnpaidResidue(o)).isFalse();
    }
}
