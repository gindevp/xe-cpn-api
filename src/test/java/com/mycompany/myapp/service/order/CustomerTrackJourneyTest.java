package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.service.dto.order.TrackOrderResponse.JourneyStep;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerTrackJourneyTest {

    private static final Instant T0 = Instant.parse("2026-10-03T09:50:00Z");

    private static Instant t(int minutes) {
        return T0.plusSeconds(minutes * 60L);
    }

    private static OrderEvent ev(String action, int minutes, String actor) {
        return new OrderEvent().action(action).eventAt(t(minutes)).actorUsername(actor);
    }

    private static ShipmentOrder order(OrderStatus status) {
        ShipmentOrder o = new ShipmentOrder();
        o.setStatus(status);
        o.setCreatedAt(t(0));
        o.setUpdatedAt(t(500));
        return o;
    }

    private static Instant at(List<JourneyStep> steps, String key) {
        return steps.stream().filter(s -> s.getKey().equals(key)).findFirst().map(JourneyStep::getAt).orElse(null);
    }

    private static List<String> keys(List<JourneyStep> steps) {
        return steps.stream().map(JourneyStep::getKey).toList();
    }

    @Test
    void deliveredOrderHasAllMilestones() {
        List<JourneyStep> s = CustomerTrackJourney.build(
            order(OrderStatus.DELIVERED),
            List.of(
                ev("CREATE", 0, "nv1"),
                ev("WAREHOUSE_RECEIVE", 1, "nv1"),
                ev("PRINT", 2, "nv1"),
                ev("SCAN_OUT", 14, "nv1"),
                ev("KY_BAN_GIAO_TAI_XE", 15, "nv1"),
                ev("SCAN_IN", 160, "nv2"),
                ev("AUTO_CALL_REQUEST", 160, "system"),
                ev("POD_QUAY", 185, "nv2")
            )
        );
        assertThat(keys(s)).containsExactly("CREATED", "WH_IN", "ON_TRIP", "DEST_WH_IN", "DELIVERED");
        assertThat(at(s, "WH_IN")).isEqualTo(t(1));
        assertThat(at(s, "ON_TRIP")).isEqualTo(t(14));
        assertThat(at(s, "DEST_WH_IN")).isEqualTo(t(160));
        assertThat(at(s, "DELIVERED")).isEqualTo(t(185));
    }

    @Test
    void counterOrderIsWarehousedAtCreation() {
        List<JourneyStep> s = CustomerTrackJourney.build(order(OrderStatus.CONFIRMED), List.of(ev("CREATE", 0, "nv1")));
        assertThat(at(s, "WH_IN")).isEqualTo(t(0));
        assertThat(at(s, "ON_TRIP")).isNull();
        assertThat(at(s, "DELIVERED")).isNull();
    }

    @Test
    void customerCreatedOrderNotWarehousedUntilReceived() {
        List<JourneyStep> s = CustomerTrackJourney.build(order(OrderStatus.CONFIRMED), List.of(ev("CREATE", 0, "customer")));
        assertThat(at(s, "WH_IN")).isNull();
    }

    @Test
    void scanRemovedClearsOnTripUntilRescanned() {
        List<JourneyStep> removed = CustomerTrackJourney.build(
            order(OrderStatus.CONFIRMED),
            List.of(ev("CREATE", 0, "nv1"), ev("SCAN_OUT", 10, "nv1"), ev("SCAN_REMOVE", 12, "nv1"))
        );
        assertThat(at(removed, "ON_TRIP")).isNull();

        List<JourneyStep> rescanned = CustomerTrackJourney.build(
            order(OrderStatus.IN_TRANSIT),
            List.of(ev("CREATE", 0, "nv1"), ev("SCAN_OUT", 10, "nv1"), ev("SCAN_REMOVE", 12, "nv1"), ev("SCAN_OUT", 30, "nv1"))
        );
        assertThat(at(rescanned, "ON_TRIP")).isEqualTo(t(30));
    }

    @Test
    void cancelledOrderDropsPendingStepsAndEndsWithCancel() {
        List<JourneyStep> s = CustomerTrackJourney.build(
            order(OrderStatus.CANCELLED),
            List.of(ev("CREATE", 0, "customer"), ev("CANCEL", 40, "nv1"))
        );
        assertThat(keys(s)).containsExactly("CREATED", "CANCELLED");
        assertThat(at(s, "CANCELLED")).isEqualTo(t(40));
    }
}
