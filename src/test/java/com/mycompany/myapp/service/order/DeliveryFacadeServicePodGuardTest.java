package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
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
    void senderPaysOrderRejectsCollectingFromReceiver() {
        ShipmentOrder o = order(OrderStatus.AT_DEST, 1, 2, null);
        o.setPaymentTerm(PaymentTerm.GUI_TRA);
        assertThatThrownBy(() -> DeliveryFacadeService.assertReceiverCollectAllowed(o, new BigDecimal("60000")))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("senderPaysNoCollect");
        assertThatCode(() -> DeliveryFacadeService.assertReceiverCollectAllowed(o, BigDecimal.ZERO)).doesNotThrowAnyException();
    }

    @Test
    void receiverPaysOrderAllowsCollecting() {
        ShipmentOrder o = order(OrderStatus.AT_DEST, 1, 2, null);
        o.setPaymentTerm(PaymentTerm.NHAN_TRA);
        assertThatCode(() -> DeliveryFacadeService.assertReceiverCollectAllowed(o, new BigDecimal("60000"))).doesNotThrowAnyException();
    }

    private static ShipmentOrder ahamove(String partnerStatus) {
        ShipmentOrder o = order(OrderStatus.OUT_FOR_DELIVERY, 1, 2, null);
        o.setPartnerCode("AHAMOVE");
        o.setPartnerOrderId("AHA1");
        o.setPartnerStatus(partnerStatus);
        return o;
    }

    @Test
    void ahamoveDeliveringBlocksManualPodForStaff() {
        assertThatThrownBy(() -> DeliveryFacadeService.assertPartnerPodAllowed(ahamove("IN PROCESS"), false))
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("ahamoveDeliveringPod");
    }

    @Test
    void ahamoveDeliveringAllowsAdminOverride() {
        assertThatCode(() -> DeliveryFacadeService.assertPartnerPodAllowed(ahamove("ACCEPTED"), true)).doesNotThrowAnyException();
    }

    @Test
    void ahamoveCompletedOrInternalShipperAllowsManualPod() {
        assertThatCode(() -> DeliveryFacadeService.assertPartnerPodAllowed(ahamove("COMPLETED"), false)).doesNotThrowAnyException();
        assertThatCode(() -> DeliveryFacadeService.assertPartnerPodAllowed(ahamove("CANCELLED"), false)).doesNotThrowAnyException();
        assertThatCode(() -> DeliveryFacadeService.assertPartnerPodAllowed(order(OrderStatus.OUT_FOR_DELIVERY, 1, 2, null), false)
        ).doesNotThrowAnyException();
    }

    @Test
    void atDestOrOutForDeliveryIsAllowed() {
        assertThatCode(() -> DeliveryFacadeService.assertArrivedForPod(order(OrderStatus.AT_DEST, 1, 2, 2L))).doesNotThrowAnyException();
        assertThatCode(() -> DeliveryFacadeService.assertArrivedForPod(order(OrderStatus.OUT_FOR_DELIVERY, 1, 2, null))
        ).doesNotThrowAnyException();
    }
}
