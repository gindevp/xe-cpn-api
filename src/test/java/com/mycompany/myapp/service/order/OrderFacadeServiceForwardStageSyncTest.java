package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import org.junit.jupiter.api.Test;

class OrderFacadeServiceForwardStageSyncTest {

    @Test
    void failedDelivery_movesOutOfDeliveringTab() {
        assertThat(OrderFacadeService.forwardStageFor(OrderStatus.FAILED_DELIVERY, ForwardStage.DELIVERING)).isEqualTo(ForwardStage.FAILED);
    }

    @Test
    void failedDelivery_keepsRedeliverWait() {
        assertThat(OrderFacadeService.forwardStageFor(OrderStatus.FAILED_DELIVERY, ForwardStage.REDELIVER_WAIT)).isNull();
    }

    @Test
    void outForDeliveryAndAtDest_followStatus() {
        assertThat(OrderFacadeService.forwardStageFor(OrderStatus.OUT_FOR_DELIVERY, ForwardStage.REDELIVER_WAIT)).isEqualTo(
            ForwardStage.DELIVERING
        );
        assertThat(OrderFacadeService.forwardStageFor(OrderStatus.AT_DEST, ForwardStage.TRANSFERRING)).isEqualTo(ForwardStage.DEST_WH_IN);
    }

    @Test
    void otherStatuses_keepStage() {
        assertThat(OrderFacadeService.forwardStageFor(OrderStatus.IN_TRANSIT, ForwardStage.TRANSFERRING)).isNull();
        assertThat(OrderFacadeService.forwardStageFor(OrderStatus.RETURNING, ForwardStage.WH_IN)).isNull();
    }
}
