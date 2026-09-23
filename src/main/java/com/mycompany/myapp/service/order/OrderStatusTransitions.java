package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Order status transitions aligned with FE {@code ORDER_TRANSITIONS} in store.ts.
 */
public final class OrderStatusTransitions {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.DRAFT, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED.put(
            OrderStatus.CONFIRMED,
            Set.of(OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.RETURNING)
        );
        ALLOWED.put(OrderStatus.WAITING, Set.of(OrderStatus.IN_TRANSIT, OrderStatus.CANCELLED, OrderStatus.RETURNING));
        ALLOWED.put(OrderStatus.IN_TRANSIT, Set.of(OrderStatus.WAITING, OrderStatus.AT_DEST, OrderStatus.RETURNING));
        ALLOWED.put(OrderStatus.AT_DEST, Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.RETURNING));
        ALLOWED.put(OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED, OrderStatus.FAILED_DELIVERY, OrderStatus.RETURNING));
        ALLOWED.put(OrderStatus.FAILED_DELIVERY, Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.AT_DEST, OrderStatus.RETURNING));
        ALLOWED.put(OrderStatus.DELIVERED, Set.of(OrderStatus.RETURNING));
        ALLOWED.put(OrderStatus.CANCELLED, Set.of());
        ALLOWED.put(
            OrderStatus.RETURNING,
            Set.of(
                OrderStatus.RETURNED,
                // Huỷ hoàn (admin): về đúng trạng thái trước khi bấm hoàn
                OrderStatus.CONFIRMED,
                OrderStatus.WAITING,
                OrderStatus.IN_TRANSIT,
                OrderStatus.AT_DEST,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.FAILED_DELIVERY,
                OrderStatus.DELIVERED
            )
        );
        // Re-return after complete (history cycle; FE has no button yet — API-only)
        ALLOWED.put(OrderStatus.RETURNED, Set.of(OrderStatus.RETURNING));
    }

    private OrderStatusTransitions() {}

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static List<OrderStatus> allowedTargets(OrderStatus from) {
        return List.copyOf(ALLOWED.getOrDefault(from, Set.of()));
    }
}
