package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import org.junit.jupiter.api.Test;

class ExceptionFacadeServiceLeftTripTest {

    private static ShipmentOrder order(OrderStatus status) {
        ShipmentOrder o = new ShipmentOrder();
        o.setStatus(status);
        return o;
    }

    @Test
    void destStagesLeaveTrip() {
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.AT_DEST), ForwardStage.DEST_WH_IN)).isTrue();
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.OUT_FOR_DELIVERY), ForwardStage.DELIVERING)).isTrue();
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.FAILED_DELIVERY), ForwardStage.FAILED)).isTrue();
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.FAILED_DELIVERY), ForwardStage.REDELIVER_WAIT)).isTrue();
    }

    @Test
    void onTruckKeepsTrip() {
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.IN_TRANSIT), ForwardStage.TRANSFERRING)).isFalse();
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.WAITING), ForwardStage.TRANSFER_PENDING)).isFalse();
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.IN_TRANSIT), null)).isFalse();
    }

    @Test
    void noStageFallsBackToStatus() {
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.AT_DEST), null)).isTrue();
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.DELIVERED), null)).isTrue();
        assertThat(ExceptionFacadeService.leftTrip(order(OrderStatus.RETURNED), null)).isTrue();
    }
}
