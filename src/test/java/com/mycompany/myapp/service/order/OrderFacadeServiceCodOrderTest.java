package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderFacadeServiceCodOrderTest {

    private static ShipmentOrder order(PaymentTerm term, String cod) {
        ShipmentOrder o = new ShipmentOrder();
        o.setPaymentTerm(term);
        o.setCodAmount(cod == null ? null : new BigDecimal(cod));
        return o;
    }

    @Test
    void senderOrReceiverPays_withCodAmount_isCod() {
        assertThat(OrderFacadeService.isCodOrder(order(PaymentTerm.GUI_TRA, "2440000"))).isTrue();
        assertThat(OrderFacadeService.isCodOrder(order(PaymentTerm.NHAN_TRA, "500000"))).isTrue();
    }

    @Test
    void legacyCodTerm_isCod() {
        assertThat(OrderFacadeService.isCodOrder(order(PaymentTerm.COD, null))).isTrue();
    }

    @Test
    void noCodAmount_notCod() {
        assertThat(OrderFacadeService.isCodOrder(order(PaymentTerm.GUI_TRA, "0"))).isFalse();
        assertThat(OrderFacadeService.isCodOrder(order(PaymentTerm.NHAN_TRA, null))).isFalse();
    }
}
