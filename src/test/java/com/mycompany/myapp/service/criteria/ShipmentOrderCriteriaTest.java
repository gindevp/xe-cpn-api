package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ShipmentOrderCriteriaTest {

    @Test
    void newShipmentOrderCriteriaHasAllFiltersNullTest() {
        var shipmentOrderCriteria = new ShipmentOrderCriteria();
        assertThat(shipmentOrderCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void shipmentOrderCriteriaFluentMethodsCreatesFiltersTest() {
        var shipmentOrderCriteria = new ShipmentOrderCriteria();

        setAllFilters(shipmentOrderCriteria);

        assertThat(shipmentOrderCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void shipmentOrderCriteriaCopyCreatesNullFilterTest() {
        var shipmentOrderCriteria = new ShipmentOrderCriteria();
        var copy = shipmentOrderCriteria.copy();

        assertThat(shipmentOrderCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(shipmentOrderCriteria)
        );
    }

    @Test
    void shipmentOrderCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var shipmentOrderCriteria = new ShipmentOrderCriteria();
        setAllFilters(shipmentOrderCriteria);

        var copy = shipmentOrderCriteria.copy();

        assertThat(shipmentOrderCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(shipmentOrderCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var shipmentOrderCriteria = new ShipmentOrderCriteria();

        assertThat(shipmentOrderCriteria).hasToString("ShipmentOrderCriteria{}");
    }

    private static void setAllFilters(ShipmentOrderCriteria shipmentOrderCriteria) {
        shipmentOrderCriteria.id();
        shipmentOrderCriteria.orderCode();
        shipmentOrderCriteria.draftCode();
        shipmentOrderCriteria.status();
        shipmentOrderCriteria.forwardStage();
        shipmentOrderCriteria.returnStage();
        shipmentOrderCriteria.paymentTerm();
        shipmentOrderCriteria.goodsType();
        shipmentOrderCriteria.serviceType();
        shipmentOrderCriteria.senderName();
        shipmentOrderCriteria.senderPhone();
        shipmentOrderCriteria.receiverName();
        shipmentOrderCriteria.receiverPhone();
        shipmentOrderCriteria.deliveryAddress();
        shipmentOrderCriteria.pickupAddress();
        shipmentOrderCriteria.homePickup();
        shipmentOrderCriteria.homeDelivery();
        shipmentOrderCriteria.qrDropOff();
        shipmentOrderCriteria.pickupStaffUsername();
        shipmentOrderCriteria.pickingAt();
        shipmentOrderCriteria.pickedUpAt();
        shipmentOrderCriteria.receiverActualName();
        shipmentOrderCriteria.receiverActualPhone();
        shipmentOrderCriteria.weightKg();
        shipmentOrderCriteria.quantity();
        shipmentOrderCriteria.dimensionsText();
        shipmentOrderCriteria.fareAmount();
        shipmentOrderCriteria.pickupFeeAmount();
        shipmentOrderCriteria.deliveryFeeAmount();
        shipmentOrderCriteria.partnerFeeAmount();
        shipmentOrderCriteria.paidAmount();
        shipmentOrderCriteria.shelfNumber();
        shipmentOrderCriteria.cancelReason();
        shipmentOrderCriteria.labelPrintedAt();
        shipmentOrderCriteria.labelReprintCount();
        shipmentOrderCriteria.failCount();
        shipmentOrderCriteria.partnerCode();
        shipmentOrderCriteria.paymentPercent();
        shipmentOrderCriteria.publicTrackingAllowed();
        shipmentOrderCriteria.issueId();
        shipmentOrderCriteria.returnRequestId();
        shipmentOrderCriteria.fareAdjustmentRequestId();
        shipmentOrderCriteria.senderCustomerId();
        shipmentOrderCriteria.fromOfficeId();
        shipmentOrderCriteria.toOfficeId();
        shipmentOrderCriteria.hubOfficeId();
        shipmentOrderCriteria.finalToOfficeId();
        shipmentOrderCriteria.currentTripId();
        shipmentOrderCriteria.distinct();
    }

    private static Condition<ShipmentOrderCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getOrderCode()) &&
                condition.apply(criteria.getDraftCode()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getForwardStage()) &&
                condition.apply(criteria.getReturnStage()) &&
                condition.apply(criteria.getPaymentTerm()) &&
                condition.apply(criteria.getGoodsType()) &&
                condition.apply(criteria.getServiceType()) &&
                condition.apply(criteria.getSenderName()) &&
                condition.apply(criteria.getSenderPhone()) &&
                condition.apply(criteria.getReceiverName()) &&
                condition.apply(criteria.getReceiverPhone()) &&
                condition.apply(criteria.getDeliveryAddress()) &&
                condition.apply(criteria.getPickupAddress()) &&
                condition.apply(criteria.getHomePickup()) &&
                condition.apply(criteria.getHomeDelivery()) &&
                condition.apply(criteria.getQrDropOff()) &&
                condition.apply(criteria.getPickupStaffUsername()) &&
                condition.apply(criteria.getPickingAt()) &&
                condition.apply(criteria.getPickedUpAt()) &&
                condition.apply(criteria.getReceiverActualName()) &&
                condition.apply(criteria.getReceiverActualPhone()) &&
                condition.apply(criteria.getWeightKg()) &&
                condition.apply(criteria.getQuantity()) &&
                condition.apply(criteria.getDimensionsText()) &&
                condition.apply(criteria.getFareAmount()) &&
                condition.apply(criteria.getPickupFeeAmount()) &&
                condition.apply(criteria.getDeliveryFeeAmount()) &&
                condition.apply(criteria.getPartnerFeeAmount()) &&
                condition.apply(criteria.getPaidAmount()) &&
                condition.apply(criteria.getShelfNumber()) &&
                condition.apply(criteria.getCancelReason()) &&
                condition.apply(criteria.getLabelPrintedAt()) &&
                condition.apply(criteria.getLabelReprintCount()) &&
                condition.apply(criteria.getFailCount()) &&
                condition.apply(criteria.getPartnerCode()) &&
                condition.apply(criteria.getPaymentPercent()) &&
                condition.apply(criteria.getPublicTrackingAllowed()) &&
                condition.apply(criteria.getIssueId()) &&
                condition.apply(criteria.getReturnRequestId()) &&
                condition.apply(criteria.getFareAdjustmentRequestId()) &&
                condition.apply(criteria.getSenderCustomerId()) &&
                condition.apply(criteria.getFromOfficeId()) &&
                condition.apply(criteria.getToOfficeId()) &&
                condition.apply(criteria.getHubOfficeId()) &&
                condition.apply(criteria.getFinalToOfficeId()) &&
                condition.apply(criteria.getCurrentTripId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ShipmentOrderCriteria> copyFiltersAre(
        ShipmentOrderCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getOrderCode(), copy.getOrderCode()) &&
                condition.apply(criteria.getDraftCode(), copy.getDraftCode()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getForwardStage(), copy.getForwardStage()) &&
                condition.apply(criteria.getReturnStage(), copy.getReturnStage()) &&
                condition.apply(criteria.getPaymentTerm(), copy.getPaymentTerm()) &&
                condition.apply(criteria.getGoodsType(), copy.getGoodsType()) &&
                condition.apply(criteria.getServiceType(), copy.getServiceType()) &&
                condition.apply(criteria.getSenderName(), copy.getSenderName()) &&
                condition.apply(criteria.getSenderPhone(), copy.getSenderPhone()) &&
                condition.apply(criteria.getReceiverName(), copy.getReceiverName()) &&
                condition.apply(criteria.getReceiverPhone(), copy.getReceiverPhone()) &&
                condition.apply(criteria.getDeliveryAddress(), copy.getDeliveryAddress()) &&
                condition.apply(criteria.getPickupAddress(), copy.getPickupAddress()) &&
                condition.apply(criteria.getHomePickup(), copy.getHomePickup()) &&
                condition.apply(criteria.getHomeDelivery(), copy.getHomeDelivery()) &&
                condition.apply(criteria.getQrDropOff(), copy.getQrDropOff()) &&
                condition.apply(criteria.getPickupStaffUsername(), copy.getPickupStaffUsername()) &&
                condition.apply(criteria.getPickingAt(), copy.getPickingAt()) &&
                condition.apply(criteria.getPickedUpAt(), copy.getPickedUpAt()) &&
                condition.apply(criteria.getReceiverActualName(), copy.getReceiverActualName()) &&
                condition.apply(criteria.getReceiverActualPhone(), copy.getReceiverActualPhone()) &&
                condition.apply(criteria.getWeightKg(), copy.getWeightKg()) &&
                condition.apply(criteria.getQuantity(), copy.getQuantity()) &&
                condition.apply(criteria.getDimensionsText(), copy.getDimensionsText()) &&
                condition.apply(criteria.getFareAmount(), copy.getFareAmount()) &&
                condition.apply(criteria.getPickupFeeAmount(), copy.getPickupFeeAmount()) &&
                condition.apply(criteria.getDeliveryFeeAmount(), copy.getDeliveryFeeAmount()) &&
                condition.apply(criteria.getPartnerFeeAmount(), copy.getPartnerFeeAmount()) &&
                condition.apply(criteria.getPaidAmount(), copy.getPaidAmount()) &&
                condition.apply(criteria.getShelfNumber(), copy.getShelfNumber()) &&
                condition.apply(criteria.getCancelReason(), copy.getCancelReason()) &&
                condition.apply(criteria.getLabelPrintedAt(), copy.getLabelPrintedAt()) &&
                condition.apply(criteria.getLabelReprintCount(), copy.getLabelReprintCount()) &&
                condition.apply(criteria.getFailCount(), copy.getFailCount()) &&
                condition.apply(criteria.getPartnerCode(), copy.getPartnerCode()) &&
                condition.apply(criteria.getPaymentPercent(), copy.getPaymentPercent()) &&
                condition.apply(criteria.getPublicTrackingAllowed(), copy.getPublicTrackingAllowed()) &&
                condition.apply(criteria.getIssueId(), copy.getIssueId()) &&
                condition.apply(criteria.getReturnRequestId(), copy.getReturnRequestId()) &&
                condition.apply(criteria.getFareAdjustmentRequestId(), copy.getFareAdjustmentRequestId()) &&
                condition.apply(criteria.getSenderCustomerId(), copy.getSenderCustomerId()) &&
                condition.apply(criteria.getFromOfficeId(), copy.getFromOfficeId()) &&
                condition.apply(criteria.getToOfficeId(), copy.getToOfficeId()) &&
                condition.apply(criteria.getHubOfficeId(), copy.getHubOfficeId()) &&
                condition.apply(criteria.getFinalToOfficeId(), copy.getFinalToOfficeId()) &&
                condition.apply(criteria.getCurrentTripId(), copy.getCurrentTripId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
