package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;

/**
 * Shared money helpers for fare / paid / due.
 * H1: fare must not go below paid. M1: due remains visible after underpay POD.
 */
public final class OrderMoney {

    private OrderMoney() {}

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** due = max(0, fare - paid). */
    public static BigDecimal due(BigDecimal fareAmount, BigDecimal paidAmount) {
        BigDecimal due = nz(fareAmount).subtract(nz(paidAmount));
        return due.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : due;
    }

    public static BigDecimal due(ShipmentOrder order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        return due(order.getFareAmount(), order.getPaidAmount());
    }

    public static boolean hasUnpaidResidue(ShipmentOrder order) {
        return due(order).compareTo(BigDecimal.ZERO) > 0;
    }
}
