package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import org.junit.jupiter.api.Test;

class DeliveryFacadeServicePodGuardTest {

    private static Office office(long id) {
        Office o = new Office();
        o.setId(id);
        return o;
    }

    private static ShipmentOrder order(OrderStatus status, long from, long to, Long finalTo) {
        ShipmentOrder o = new ShipmentOrder();
        o.setStatus(status);
        o.setFromOffice(office(from));
        o.setToOffice(office(to));
        if (finalTo != null) {
            o.setFinalToOffice(office(finalTo));
        }
        return o;
    }

    @Test
    void confirmedAtOriginDifferentDestinationIsRejected() {
        assertThatThrownBy(() -> DeliveryFacadeService.assertArrivedForPod(order(OrderStatus.CONFIRMED, 32, 2, 2L)))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("not arrived");
    }

    @Test
    void waitingAtOriginDifferentDestinationIsRejected() {
        assertThatThrownBy(() -> DeliveryFacadeService.assertArrivedForPod(order(OrderStatus.WAITING, 1, 2, null))).isInstanceOf(
            BadRequestAlertException.class
        );
    }

    @Test
    void confirmedSameOfficeIsAllowed() {
        assertThatCode(() -> DeliveryFacadeService.assertArrivedForPod(order(OrderStatus.CONFIRMED, 5, 5, 5L))).doesNotThrowAnyException();
    }

    @Test
    void atDestOrOutForDeliveryIsAllowed() {
        assertThatCode(() -> DeliveryFacadeService.assertArrivedForPod(order(OrderStatus.AT_DEST, 1, 2, 2L))).doesNotThrowAnyException();
        assertThatCode(() -> DeliveryFacadeService.assertArrivedForPod(order(OrderStatus.OUT_FOR_DELIVERY, 1, 2, null))
        ).doesNotThrowAnyException();
    }
}
