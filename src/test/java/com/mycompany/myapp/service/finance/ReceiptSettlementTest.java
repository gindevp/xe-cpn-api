package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.service.finance.ReceiptSettlement.Split;
import com.mycompany.myapp.service.finance.ReceiptSettlement.Totals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReceiptSettlementTest {

    @Test
    void guiTra_prepaidBeforeWarehouse_senderHoldsCash() {
        ShipmentOrder o = order(PaymentTerm.GUI_TRA, OrderStatus.CONFIRMED, null, "30000", "30000", "0");
        Split s = ReceiptSettlement.split(o, Totals.ZERO);
        assertThat(s.senderOut()).isEqualByComparingTo("30000");
        assertThat(s.senderHeldOut()).isEqualByComparingTo("30000");
        assertThat(s.deliveryOut()).isEqualByComparingTo("0");
    }

    @Test
    void guiTra_unpaidAfterWarehouse_senderOwesFare() {
        ShipmentOrder o = order(PaymentTerm.GUI_TRA, OrderStatus.CONFIRMED, ForwardStage.WH_IN, "30000", "0", "0");
        Split s = ReceiptSettlement.split(o, Totals.ZERO);
        assertThat(s.senderOut()).isEqualByComparingTo("30000");
        assertThat(s.senderFareDue()).isEqualByComparingTo("30000");
    }

    @Test
    void guiTra_unpaidBeforeWarehouse_nothing() {
        ShipmentOrder o = order(PaymentTerm.GUI_TRA, OrderStatus.CONFIRMED, null, "30000", "0", "0");
        assertThat(ReceiptSettlement.split(o, Totals.ZERO).totalOut()).isEqualByComparingTo("0");
    }

    @Test
    void guiTraWithCod_codOnlyAfterDelivery_evenIfSenderAlreadyReceipted() {
        ShipmentOrder o = order(PaymentTerm.GUI_TRA, OrderStatus.IN_TRANSIT, ForwardStage.TRANSFERRING, "30000", "30000", "50000");
        Split before = ReceiptSettlement.split(o, Totals.ZERO);
        assertThat(before.senderOut()).isEqualByComparingTo("30000");
        assertThat(before.deliveryOut()).isEqualByComparingTo("0");

        o.setStatus(OrderStatus.DELIVERED);
        Split after = ReceiptSettlement.split(o, totals("0", "0", "30000"));
        assertThat(after.senderOut()).isEqualByComparingTo("0");
        assertThat(after.deliveryOut()).isEqualByComparingTo("50000");
        assertThat(after.codDue()).isEqualByComparingTo("50000");
    }

    @Test
    void partialPrepay_delivered_splitsSenderAndDelivery() {
        ShipmentOrder o = order(PaymentTerm.P30_70, OrderStatus.DELIVERED, ForwardStage.DELIVERING, "100000", "30000", "0");
        Split s = ReceiptSettlement.split(o, Totals.ZERO);
        assertThat(s.senderOut()).isEqualByComparingTo("30000");
        assertThat(s.deliveryOut()).isEqualByComparingTo("70000");
        assertThat(s.deliveryFareDue()).isEqualByComparingTo("70000");
    }

    @Test
    void nhanTra_inTransit_nothing() {
        ShipmentOrder o = order(PaymentTerm.NHAN_TRA, OrderStatus.IN_TRANSIT, ForwardStage.TRANSFERRING, "30000", "0", "0");
        assertThat(ReceiptSettlement.split(o, Totals.ZERO).totalOut()).isEqualByComparingTo("0");
    }

    @Test
    void nhanTra_collectedAtPod_deliveryHoldsCash() {
        ShipmentOrder o = order(PaymentTerm.NHAN_TRA, OrderStatus.DELIVERED, ForwardStage.DELIVERING, "30000", "30000", "0");
        Split s = ReceiptSettlement.split(o, totals("30000", "0", "0"));
        assertThat(s.senderOut()).isEqualByComparingTo("0");
        assertThat(s.deliveryOut()).isEqualByComparingTo("30000");
        assertThat(s.deliveryHeldOut()).isEqualByComparingTo("30000");
        assertThat(s.deliveryFareDue()).isEqualByComparingTo("0");
    }

    @Test
    void returning_prepaid_senderStillOwes() {
        ShipmentOrder o = order(PaymentTerm.GUI_TRA, OrderStatus.RETURNING, ForwardStage.FAILED, "30000", "30000", "0");
        Split s = ReceiptSettlement.split(o, Totals.ZERO);
        assertThat(s.senderOut()).isEqualByComparingTo("30000");
        assertThat(s.deliveryOut()).isEqualByComparingTo("0");
    }

    @Test
    void fullyReceipted_nothing() {
        ShipmentOrder o = order(PaymentTerm.GUI_TRA, OrderStatus.DELIVERED, ForwardStage.DELIVERING, "30000", "30000", "0");
        assertThat(ReceiptSettlement.split(o, totals("0", "0", "30000")).totalOut()).isEqualByComparingTo("0");
    }

    @Test
    void cancelled_nothing() {
        ShipmentOrder o = order(PaymentTerm.GUI_TRA, OrderStatus.CANCELLED, null, "30000", "30000", "0");
        assertThat(ReceiptSettlement.split(o, Totals.ZERO).totalOut()).isEqualByComparingTo("0");
    }

    @Test
    void deliverySidePayment_detectsPodAndReceiptNotes() {
        assertThat(ReceiptSettlement.isDeliverySidePayment(PaymentKind.SAU, "POD QUAY")).isTrue();
        assertThat(ReceiptSettlement.isDeliverySidePayment(PaymentKind.SAU, "RECEIPT")).isTrue();
        assertThat(ReceiptSettlement.isDeliverySidePayment(PaymentKind.SAU, "RECEIPT_SENDER")).isFalse();
        assertThat(ReceiptSettlement.isDeliverySidePayment(PaymentKind.TRUOC, "POD")).isFalse();
    }

    private static Totals totals(String deliverySidePaid, String codPaid, String receipted) {
        return new Totals(new BigDecimal(deliverySidePaid), new BigDecimal(codPaid), new BigDecimal(receipted));
    }

    private static ShipmentOrder order(PaymentTerm term, OrderStatus status, ForwardStage stage, String fare, String paid, String cod) {
        ShipmentOrder o = new ShipmentOrder();
        o.setPaymentTerm(term);
        o.setStatus(status);
        o.setForwardStage(stage);
        o.setFareAmount(new BigDecimal(fare));
        o.setPaidAmount(new BigDecimal(paid));
        o.setCodAmount(new BigDecimal(cod));
        return o;
    }
}
