package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PartnerAdvanceTest {

    private static ShipmentOrder order(OrderStatus st, String cod, boolean collected) {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode("GP1");
        o.setStatus(st);
        o.setPartnerCodAmount(cod == null ? null : new BigDecimal(cod));
        o.setPartnerCodCollectedAt(collected ? Instant.now() : null);
        return o;
    }

    @Test
    void pendingWhileOutOrDeliveredAndNotCollected() {
        assertThat(PartnerAdvance.pending(order(OrderStatus.OUT_FOR_DELIVERY, "30000", false))).isTrue();
        assertThat(PartnerAdvance.pending(order(OrderStatus.DELIVERED, "30000", false))).isTrue();
        assertThat(PartnerAdvance.pending(order(OrderStatus.OUT_FOR_DELIVERY, "30000", true))).isFalse();
        assertThat(PartnerAdvance.pending(order(OrderStatus.OUT_FOR_DELIVERY, null, false))).isFalse();
        assertThat(PartnerAdvance.pending(order(OrderStatus.FAILED_DELIVERY, "30000", false))).isFalse();
    }

    @Test
    void pendingBlocksMoneyChanges() {
        assertThatThrownBy(() -> PartnerAdvance.assertNotPending(order(OrderStatus.OUT_FOR_DELIVERY, "30000", false), "order"))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("partnerAdvancePending");
        assertThatCode(() -> PartnerAdvance.assertNotPending(order(OrderStatus.OUT_FOR_DELIVERY, "30000", true), "order")
        ).doesNotThrowAnyException();
    }

    @Test
    void refundDueOnlyAfterCollectedAndNotDelivering() {
        assertThat(PartnerAdvance.needsRefund(order(OrderStatus.FAILED_DELIVERY, "30000", true))).isTrue();
        assertThat(PartnerAdvance.needsRefund(order(OrderStatus.AT_DEST, "30000", true))).isTrue();
        assertThat(PartnerAdvance.needsRefund(order(OrderStatus.FAILED_DELIVERY, "30000", false))).isFalse();
        assertThat(PartnerAdvance.needsRefund(order(OrderStatus.DELIVERED, "30000", true))).isFalse();
    }
}
