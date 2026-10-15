package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
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

    @Test
    void receiptCollectable_addsCodWithoutChangingFareDue() {
        ShipmentOrder o = new ShipmentOrder();
        o.setFareAmount(new BigDecimal("40000"));
        o.setPaidAmount(new BigDecimal("10000"));
        o.setCodAmount(new BigDecimal("50000"));
        assertThat(OrderMoney.due(o)).isEqualByComparingTo("30000");
        assertThat(OrderMoney.receiptCollectable(o)).isEqualByComparingTo("80000");

        o.setPaidAmount(new BigDecimal("40000"));
        assertThat(OrderMoney.due(o)).isEqualByComparingTo("0");
        assertThat(OrderMoney.receiptCollectable(o)).isEqualByComparingTo("50000");

        o.setCodAmount(null);
        assertThat(OrderMoney.receiptCollectable(o)).isEqualByComparingTo("0");
    }

    @Test
    void receiverDue_senderPays_onlyStorageFee() {
        ShipmentOrder o = new ShipmentOrder();
        o.setPaymentTerm(PaymentTerm.GUI_TRA);
        o.setFareAmount(new BigDecimal("39000"));
        o.setPaidAmount(new BigDecimal("20000"));
        o.setStorageFeeAmount(new BigDecimal("9000"));
        assertThat(OrderMoney.receiverDue(o)).isEqualByComparingTo("9000");

        o.setPaidAmount(new BigDecimal("36000"));
        assertThat(OrderMoney.receiverDue(o)).isEqualByComparingTo("3000");

        o.setOnCredit(true);
        assertThat(OrderMoney.receiverDue(o)).isEqualByComparingTo("0");

        o.setOnCredit(false);
        o.setPaymentTerm(PaymentTerm.NHAN_TRA);
        assertThat(OrderMoney.receiverDue(o)).isEqualByComparingTo("3000");
    }
}
