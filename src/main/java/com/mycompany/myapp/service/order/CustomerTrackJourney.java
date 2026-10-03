package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.service.dto.order.TrackOrderResponse.JourneyStep;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Hành trình đơn cho tra cứu công khai: chỉ các mốc trạng thái + giờ,
 * không lộ nội dung log (người thao tác, SĐT, số HĐ, xe/tài…).
 */
public final class CustomerTrackJourney {

    /** Action được phép trả về tra cứu công khai (chỉ action + giờ). */
    public static final Set<String> PUBLIC_EVENT_ACTIONS = Set.of(
        "CREATE",
        "WAREHOUSE_RECEIVE",
        "SCAN_OUT",
        "SCAN_IN",
        "DELIVERING",
        "POD",
        "POD_QUAY",
        "POD_HOME",
        "FAILED",
        "CANCEL",
        "RETURN_START"
    );

    private static final Set<String> POD_ACTIONS = Set.of("POD", "POD_QUAY", "POD_HOME");

    private CustomerTrackJourney() {}

    public static List<JourneyStep> build(ShipmentOrder order, List<OrderEvent> events) {
        Instant created = order.getCreatedAt();
        Instant whIn = null;
        Instant onTrip = null;
        Instant destWhIn = null;
        Instant delivered = null;
        Instant cancelled = null;
        Instant returnStart = null;
        boolean customerCreated = false;

        for (OrderEvent e : events) {
            String action = e.getAction();
            Instant at = e.getEventAt();
            if (action == null || at == null) continue;
            switch (action) {
                case "CREATE" -> {
                    if (created == null) created = at;
                    if ("customer".equalsIgnoreCase(e.getActorUsername())) customerCreated = true;
                }
                case "WAREHOUSE_RECEIVE" -> {
                    if (whIn == null) whIn = at;
                }
                case "SCAN_OUT" -> {
                    if (destWhIn == null) onTrip = at;
                }
                case "SCAN_REMOVE", "UNLOAD_BACK" -> {
                    if (destWhIn == null) onTrip = null;
                }
                case "SCAN_IN" -> {
                    if (destWhIn == null && onTrip != null) destWhIn = at;
                }
                case "CANCEL" -> cancelled = at;
                case "RETURN_START" -> {
                    if (returnStart == null) returnStart = at;
                }
                default -> {
                    if (POD_ACTIONS.contains(action) && delivered == null) delivered = at;
                }
            }
        }

        OrderStatus st = order.getStatus();
        boolean counterOrder =
            !customerCreated && !Boolean.TRUE.equals(order.getHomePickup()) && !Boolean.TRUE.equals(order.getQrDropOff());
        if (whIn == null && counterOrder && st != OrderStatus.DRAFT && st != OrderStatus.CANCELLED) {
            whIn = created;
        }
        if (st == OrderStatus.DELIVERED && delivered == null) {
            delivered = order.getUpdatedAt();
        }

        List<JourneyStep> steps = new ArrayList<>();
        steps.add(new JourneyStep("CREATED", "Tạo đơn", created));
        steps.add(new JourneyStep("WH_IN", "Nhập kho gửi", whIn));
        steps.add(new JourneyStep("ON_TRIP", "Lên xe", onTrip));
        steps.add(new JourneyStep("DEST_WH_IN", "Nhập kho giao", destWhIn));

        if (st == OrderStatus.CANCELLED) {
            steps.removeIf(s -> s.getAt() == null);
            steps.add(new JourneyStep("CANCELLED", "Đã hủy", cancelled != null ? cancelled : order.getUpdatedAt()));
            return steps;
        }
        if (st == OrderStatus.RETURNING || st == OrderStatus.RETURNED) {
            steps.removeIf(s -> s.getAt() == null);
            steps.add(new JourneyStep("RETURNING", "Chuyển hoàn về người gửi", returnStart));
            if (st == OrderStatus.RETURNED) {
                steps.add(new JourneyStep("RETURNED", "Đã hoàn", order.getUpdatedAt()));
            }
            return steps;
        }
        steps.add(new JourneyStep("DELIVERED", "Giao thành công", delivered));
        return steps;
    }
}
