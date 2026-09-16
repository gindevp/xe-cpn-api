package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerTrackStatusTest {

    @Test
    void pendingPickupTabs() {
        ShipmentOrder o = base(OrderStatus.CONFIRMED);
        o.setHomePickup(true);
        assertThat(CustomerTrackStatus.labelOf(o)).isEqualTo("Chờ lấy hàng");

        o.setPickingAt(Instant.now());
        assertThat(CustomerTrackStatus.labelOf(o)).isEqualTo("Đang lấy hàng");
    }

    @Test
    void pendingReceiveTab() {
        // Legacy DRAFT drop-off
        ShipmentOrder draft = base(OrderStatus.DRAFT);
        draft.setHomePickup(false);
        assertThat(CustomerTrackStatus.labelOf(draft)).isEqualTo("Chờ nhận hàng");

        // Current: CONFIRMED + qrDropOff
        ShipmentOrder qr = base(OrderStatus.CONFIRMED);
        qr.setQrDropOff(true);
        assertThat(CustomerTrackStatus.labelOf(qr)).isEqualTo("Chờ nhận hàng");
    }

    @Test
    void pipelineTabsFromStageAndStatus() {
        ShipmentOrder o = base(OrderStatus.WAITING);
        o.setPickedUpAt(Instant.now());
        assertThat(CustomerTrackStatus.labelOf(o)).isEqualTo("Đợi trung chuyển giao");

        o.setStatus(OrderStatus.IN_TRANSIT);
        o.setForwardStage(ForwardStage.TRANSFERRING);
        assertThat(CustomerTrackStatus.labelOf(o)).isEqualTo("Hàng trên xe");

        o.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        o.setForwardStage(ForwardStage.DELIVERING);
        assertThat(CustomerTrackStatus.labelOf(o)).isEqualTo("Đang giao hàng");

        o.setStatus(OrderStatus.FAILED_DELIVERY);
        o.setForwardStage(ForwardStage.REDELIVER_WAIT);
        assertThat(CustomerTrackStatus.labelOf(o)).isEqualTo("Chờ giao lại");
    }

    @Test
    void delivered() {
        assertThat(CustomerTrackStatus.labelOf(base(OrderStatus.DELIVERED))).isEqualTo("Đã giao");
    }

    private static ShipmentOrder base(OrderStatus st) {
        ShipmentOrder o = new ShipmentOrder();
        o.setStatus(st);
        o.setHomePickup(false);
        o.setQrDropOff(false);
        return o;
    }
}
