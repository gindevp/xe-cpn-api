package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MeInvoiceAmountsTest {

    @Test
    void splitGross_example33000() {
        MeInvoiceAmounts.Split s = MeInvoiceAmounts.splitGross(new BigDecimal("33000"));
        assertThat(s.gross()).isEqualByComparingTo("33000");
        assertThat(s.net()).isEqualByComparingTo("30000");
        assertThat(s.vat()).isEqualByComparingTo("3000");
        assertThat(s.net().add(s.vat())).isEqualByComparingTo(s.gross());
    }

    @Test
    void fromOrder_sumsShippingCodFeeDeclared() {
        ShipmentOrder o = new ShipmentOrder();
        o.setGoodsFareAmount(new BigDecimal("20000"));
        o.setPickupFeeAmount(BigDecimal.ZERO);
        o.setDeliveryFeeAmount(BigDecimal.ZERO);
        o.setCodFeeAmount(new BigDecimal("8000"));
        o.setDeclaredFeeAmount(new BigDecimal("5000"));
        o.setCodAmount(new BigDecimal("500000")); // không cộng vào HĐ

        MeInvoiceAmounts.Breakdown b = MeInvoiceAmounts.fromOrder(o);
        assertThat(b.gross()).isEqualByComparingTo("33000");
        assertThat(b.net()).isEqualByComparingTo("30000");
        assertThat(b.vat()).isEqualByComparingTo("3000");
        assertThat(b.shipping()).isEqualByComparingTo("20000");
    }

    @Test
    void fromOrder_includesPickupDeliveryInShipping() {
        ShipmentOrder o = new ShipmentOrder();
        o.setGoodsFareAmount(new BigDecimal("10000"));
        o.setPickupFeeAmount(new BigDecimal("3000"));
        o.setDeliveryFeeAmount(new BigDecimal("2000"));
        o.setCodFeeAmount(BigDecimal.ZERO);
        o.setDeclaredFeeAmount(BigDecimal.ZERO);

        MeInvoiceAmounts.Breakdown b = MeInvoiceAmounts.fromOrder(o);
        assertThat(b.shipping()).isEqualByComparingTo("15000");
        assertThat(b.gross()).isEqualByComparingTo("15000");
        assertThat(b.net().add(b.vat())).isEqualByComparingTo(b.gross());
    }

    @Test
    void refId_prefixed() {
        assertThat(MeInvoiceAmounts.refIdFor("GP260911001")).isEqualTo("XE-GP260911001");
    }
}
