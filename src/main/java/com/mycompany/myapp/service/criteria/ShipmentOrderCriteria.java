package com.mycompany.myapp.service.criteria;

import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import com.mycompany.myapp.domain.enumeration.ServiceType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.ShipmentOrder} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.ShipmentOrderResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /shipment-orders?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShipmentOrderCriteria implements Serializable, Criteria {

    /**
     * Class for filtering OrderStatus
     */
    public static class OrderStatusFilter extends Filter<OrderStatus> {

        public OrderStatusFilter() {}

        public OrderStatusFilter(OrderStatusFilter filter) {
            super(filter);
        }

        @Override
        public OrderStatusFilter copy() {
            return new OrderStatusFilter(this);
        }
    }

    /**
     * Class for filtering ForwardStage
     */
    public static class ForwardStageFilter extends Filter<ForwardStage> {

        public ForwardStageFilter() {}

        public ForwardStageFilter(ForwardStageFilter filter) {
            super(filter);
        }

        @Override
        public ForwardStageFilter copy() {
            return new ForwardStageFilter(this);
        }
    }

    /**
     * Class for filtering ReturnStage
     */
    public static class ReturnStageFilter extends Filter<ReturnStage> {

        public ReturnStageFilter() {}

        public ReturnStageFilter(ReturnStageFilter filter) {
            super(filter);
        }

        @Override
        public ReturnStageFilter copy() {
            return new ReturnStageFilter(this);
        }
    }

    /**
     * Class for filtering PaymentTerm
     */
    public static class PaymentTermFilter extends Filter<PaymentTerm> {

        public PaymentTermFilter() {}

        public PaymentTermFilter(PaymentTermFilter filter) {
            super(filter);
        }

        @Override
        public PaymentTermFilter copy() {
            return new PaymentTermFilter(this);
        }
    }

    /**
     * Class for filtering GoodsType
     */
    public static class GoodsTypeFilter extends Filter<GoodsType> {

        public GoodsTypeFilter() {}

        public GoodsTypeFilter(GoodsTypeFilter filter) {
            super(filter);
        }

        @Override
        public GoodsTypeFilter copy() {
            return new GoodsTypeFilter(this);
        }
    }

    /**
     * Class for filtering ServiceType
     */
    public static class ServiceTypeFilter extends Filter<ServiceType> {

        public ServiceTypeFilter() {}

        public ServiceTypeFilter(ServiceTypeFilter filter) {
            super(filter);
        }

        @Override
        public ServiceTypeFilter copy() {
            return new ServiceTypeFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter orderCode;

    private StringFilter draftCode;

    private OrderStatusFilter status;

    private ForwardStageFilter forwardStage;

    private ReturnStageFilter returnStage;

    private PaymentTermFilter paymentTerm;

    private GoodsTypeFilter goodsType;

    private ServiceTypeFilter serviceType;

    private StringFilter senderName;

    private StringFilter senderPhone;

    private StringFilter receiverName;

    private StringFilter receiverPhone;

    private StringFilter deliveryAddress;

    private StringFilter pickupAddress;

    private BooleanFilter homePickup;

    private BooleanFilter homeDelivery;

    private BooleanFilter qrDropOff;

    private StringFilter pickupStaffUsername;

    private InstantFilter pickingAt;

    private InstantFilter pickedUpAt;

    private StringFilter receiverActualName;

    private StringFilter receiverActualPhone;

    private BigDecimalFilter weightKg;

    private IntegerFilter quantity;

    private StringFilter dimensionsText;

    private BigDecimalFilter fareAmount;

    private BigDecimalFilter pickupFeeAmount;

    private BigDecimalFilter deliveryFeeAmount;

    private BigDecimalFilter partnerFeeAmount;

    private BigDecimalFilter paidAmount;

    private IntegerFilter shelfNumber;

    private StringFilter cancelReason;

    private InstantFilter labelPrintedAt;

    private IntegerFilter labelReprintCount;

    private IntegerFilter failCount;

    private StringFilter partnerCode;

    private BigDecimalFilter paymentPercent;

    private BooleanFilter publicTrackingAllowed;

    private LongFilter issueId;

    private LongFilter returnRequestId;

    private LongFilter fareAdjustmentRequestId;

    private LongFilter senderCustomerId;

    private LongFilter fromOfficeId;

    private LongFilter toOfficeId;

    private LongFilter hubOfficeId;

    private LongFilter finalToOfficeId;

    private LongFilter currentTripId;

    private Boolean distinct;

    public ShipmentOrderCriteria() {}

    public ShipmentOrderCriteria(ShipmentOrderCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.orderCode = other.optionalOrderCode().map(StringFilter::copy).orElse(null);
        this.draftCode = other.optionalDraftCode().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(OrderStatusFilter::copy).orElse(null);
        this.forwardStage = other.optionalForwardStage().map(ForwardStageFilter::copy).orElse(null);
        this.returnStage = other.optionalReturnStage().map(ReturnStageFilter::copy).orElse(null);
        this.paymentTerm = other.optionalPaymentTerm().map(PaymentTermFilter::copy).orElse(null);
        this.goodsType = other.optionalGoodsType().map(GoodsTypeFilter::copy).orElse(null);
        this.serviceType = other.optionalServiceType().map(ServiceTypeFilter::copy).orElse(null);
        this.senderName = other.optionalSenderName().map(StringFilter::copy).orElse(null);
        this.senderPhone = other.optionalSenderPhone().map(StringFilter::copy).orElse(null);
        this.receiverName = other.optionalReceiverName().map(StringFilter::copy).orElse(null);
        this.receiverPhone = other.optionalReceiverPhone().map(StringFilter::copy).orElse(null);
        this.deliveryAddress = other.optionalDeliveryAddress().map(StringFilter::copy).orElse(null);
        this.pickupAddress = other.optionalPickupAddress().map(StringFilter::copy).orElse(null);
        this.homePickup = other.optionalHomePickup().map(BooleanFilter::copy).orElse(null);
        this.homeDelivery = other.optionalHomeDelivery().map(BooleanFilter::copy).orElse(null);
        this.qrDropOff = other.optionalQrDropOff().map(BooleanFilter::copy).orElse(null);
        this.pickupStaffUsername = other.optionalPickupStaffUsername().map(StringFilter::copy).orElse(null);
        this.pickingAt = other.optionalPickingAt().map(InstantFilter::copy).orElse(null);
        this.pickedUpAt = other.optionalPickedUpAt().map(InstantFilter::copy).orElse(null);
        this.receiverActualName = other.optionalReceiverActualName().map(StringFilter::copy).orElse(null);
        this.receiverActualPhone = other.optionalReceiverActualPhone().map(StringFilter::copy).orElse(null);
        this.weightKg = other.optionalWeightKg().map(BigDecimalFilter::copy).orElse(null);
        this.quantity = other.optionalQuantity().map(IntegerFilter::copy).orElse(null);
        this.dimensionsText = other.optionalDimensionsText().map(StringFilter::copy).orElse(null);
        this.fareAmount = other.optionalFareAmount().map(BigDecimalFilter::copy).orElse(null);
        this.pickupFeeAmount = other.optionalPickupFeeAmount().map(BigDecimalFilter::copy).orElse(null);
        this.deliveryFeeAmount = other.optionalDeliveryFeeAmount().map(BigDecimalFilter::copy).orElse(null);
        this.partnerFeeAmount = other.optionalPartnerFeeAmount().map(BigDecimalFilter::copy).orElse(null);
        this.paidAmount = other.optionalPaidAmount().map(BigDecimalFilter::copy).orElse(null);
        this.shelfNumber = other.optionalShelfNumber().map(IntegerFilter::copy).orElse(null);
        this.cancelReason = other.optionalCancelReason().map(StringFilter::copy).orElse(null);
        this.labelPrintedAt = other.optionalLabelPrintedAt().map(InstantFilter::copy).orElse(null);
        this.labelReprintCount = other.optionalLabelReprintCount().map(IntegerFilter::copy).orElse(null);
        this.failCount = other.optionalFailCount().map(IntegerFilter::copy).orElse(null);
        this.partnerCode = other.optionalPartnerCode().map(StringFilter::copy).orElse(null);
        this.paymentPercent = other.optionalPaymentPercent().map(BigDecimalFilter::copy).orElse(null);
        this.publicTrackingAllowed = other.optionalPublicTrackingAllowed().map(BooleanFilter::copy).orElse(null);
        this.issueId = other.optionalIssueId().map(LongFilter::copy).orElse(null);
        this.returnRequestId = other.optionalReturnRequestId().map(LongFilter::copy).orElse(null);
        this.fareAdjustmentRequestId = other.optionalFareAdjustmentRequestId().map(LongFilter::copy).orElse(null);
        this.senderCustomerId = other.optionalSenderCustomerId().map(LongFilter::copy).orElse(null);
        this.fromOfficeId = other.optionalFromOfficeId().map(LongFilter::copy).orElse(null);
        this.toOfficeId = other.optionalToOfficeId().map(LongFilter::copy).orElse(null);
        this.hubOfficeId = other.optionalHubOfficeId().map(LongFilter::copy).orElse(null);
        this.finalToOfficeId = other.optionalFinalToOfficeId().map(LongFilter::copy).orElse(null);
        this.currentTripId = other.optionalCurrentTripId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ShipmentOrderCriteria copy() {
        return new ShipmentOrderCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getOrderCode() {
        return orderCode;
    }

    public Optional<StringFilter> optionalOrderCode() {
        return Optional.ofNullable(orderCode);
    }

    public StringFilter orderCode() {
        if (orderCode == null) {
            setOrderCode(new StringFilter());
        }
        return orderCode;
    }

    public void setOrderCode(StringFilter orderCode) {
        this.orderCode = orderCode;
    }

    public StringFilter getDraftCode() {
        return draftCode;
    }

    public Optional<StringFilter> optionalDraftCode() {
        return Optional.ofNullable(draftCode);
    }

    public StringFilter draftCode() {
        if (draftCode == null) {
            setDraftCode(new StringFilter());
        }
        return draftCode;
    }

    public void setDraftCode(StringFilter draftCode) {
        this.draftCode = draftCode;
    }

    public OrderStatusFilter getStatus() {
        return status;
    }

    public Optional<OrderStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public OrderStatusFilter status() {
        if (status == null) {
            setStatus(new OrderStatusFilter());
        }
        return status;
    }

    public void setStatus(OrderStatusFilter status) {
        this.status = status;
    }

    public ForwardStageFilter getForwardStage() {
        return forwardStage;
    }

    public Optional<ForwardStageFilter> optionalForwardStage() {
        return Optional.ofNullable(forwardStage);
    }

    public ForwardStageFilter forwardStage() {
        if (forwardStage == null) {
            setForwardStage(new ForwardStageFilter());
        }
        return forwardStage;
    }

    public void setForwardStage(ForwardStageFilter forwardStage) {
        this.forwardStage = forwardStage;
    }

    public ReturnStageFilter getReturnStage() {
        return returnStage;
    }

    public Optional<ReturnStageFilter> optionalReturnStage() {
        return Optional.ofNullable(returnStage);
    }

    public ReturnStageFilter returnStage() {
        if (returnStage == null) {
            setReturnStage(new ReturnStageFilter());
        }
        return returnStage;
    }

    public void setReturnStage(ReturnStageFilter returnStage) {
        this.returnStage = returnStage;
    }

    public PaymentTermFilter getPaymentTerm() {
        return paymentTerm;
    }

    public Optional<PaymentTermFilter> optionalPaymentTerm() {
        return Optional.ofNullable(paymentTerm);
    }

    public PaymentTermFilter paymentTerm() {
        if (paymentTerm == null) {
            setPaymentTerm(new PaymentTermFilter());
        }
        return paymentTerm;
    }

    public void setPaymentTerm(PaymentTermFilter paymentTerm) {
        this.paymentTerm = paymentTerm;
    }

    public GoodsTypeFilter getGoodsType() {
        return goodsType;
    }

    public Optional<GoodsTypeFilter> optionalGoodsType() {
        return Optional.ofNullable(goodsType);
    }

    public GoodsTypeFilter goodsType() {
        if (goodsType == null) {
            setGoodsType(new GoodsTypeFilter());
        }
        return goodsType;
    }

    public void setGoodsType(GoodsTypeFilter goodsType) {
        this.goodsType = goodsType;
    }

    public ServiceTypeFilter getServiceType() {
        return serviceType;
    }

    public Optional<ServiceTypeFilter> optionalServiceType() {
        return Optional.ofNullable(serviceType);
    }

    public ServiceTypeFilter serviceType() {
        if (serviceType == null) {
            setServiceType(new ServiceTypeFilter());
        }
        return serviceType;
    }

    public void setServiceType(ServiceTypeFilter serviceType) {
        this.serviceType = serviceType;
    }

    public StringFilter getSenderName() {
        return senderName;
    }

    public Optional<StringFilter> optionalSenderName() {
        return Optional.ofNullable(senderName);
    }

    public StringFilter senderName() {
        if (senderName == null) {
            setSenderName(new StringFilter());
        }
        return senderName;
    }

    public void setSenderName(StringFilter senderName) {
        this.senderName = senderName;
    }

    public StringFilter getSenderPhone() {
        return senderPhone;
    }

    public Optional<StringFilter> optionalSenderPhone() {
        return Optional.ofNullable(senderPhone);
    }

    public StringFilter senderPhone() {
        if (senderPhone == null) {
            setSenderPhone(new StringFilter());
        }
        return senderPhone;
    }

    public void setSenderPhone(StringFilter senderPhone) {
        this.senderPhone = senderPhone;
    }

    public StringFilter getReceiverName() {
        return receiverName;
    }

    public Optional<StringFilter> optionalReceiverName() {
        return Optional.ofNullable(receiverName);
    }

    public StringFilter receiverName() {
        if (receiverName == null) {
            setReceiverName(new StringFilter());
        }
        return receiverName;
    }

    public void setReceiverName(StringFilter receiverName) {
        this.receiverName = receiverName;
    }

    public StringFilter getReceiverPhone() {
        return receiverPhone;
    }

    public Optional<StringFilter> optionalReceiverPhone() {
        return Optional.ofNullable(receiverPhone);
    }

    public StringFilter receiverPhone() {
        if (receiverPhone == null) {
            setReceiverPhone(new StringFilter());
        }
        return receiverPhone;
    }

    public void setReceiverPhone(StringFilter receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public StringFilter getDeliveryAddress() {
        return deliveryAddress;
    }

    public Optional<StringFilter> optionalDeliveryAddress() {
        return Optional.ofNullable(deliveryAddress);
    }

    public StringFilter deliveryAddress() {
        if (deliveryAddress == null) {
            setDeliveryAddress(new StringFilter());
        }
        return deliveryAddress;
    }

    public void setDeliveryAddress(StringFilter deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public StringFilter getPickupAddress() {
        return pickupAddress;
    }

    public Optional<StringFilter> optionalPickupAddress() {
        return Optional.ofNullable(pickupAddress);
    }

    public StringFilter pickupAddress() {
        if (pickupAddress == null) {
            setPickupAddress(new StringFilter());
        }
        return pickupAddress;
    }

    public void setPickupAddress(StringFilter pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public BooleanFilter getHomePickup() {
        return homePickup;
    }

    public Optional<BooleanFilter> optionalHomePickup() {
        return Optional.ofNullable(homePickup);
    }

    public BooleanFilter homePickup() {
        if (homePickup == null) {
            setHomePickup(new BooleanFilter());
        }
        return homePickup;
    }

    public void setHomePickup(BooleanFilter homePickup) {
        this.homePickup = homePickup;
    }

    public BooleanFilter getHomeDelivery() {
        return homeDelivery;
    }

    public Optional<BooleanFilter> optionalHomeDelivery() {
        return Optional.ofNullable(homeDelivery);
    }

    public BooleanFilter homeDelivery() {
        if (homeDelivery == null) {
            setHomeDelivery(new BooleanFilter());
        }
        return homeDelivery;
    }

    public void setHomeDelivery(BooleanFilter homeDelivery) {
        this.homeDelivery = homeDelivery;
    }

    public BooleanFilter getQrDropOff() {
        return qrDropOff;
    }

    public Optional<BooleanFilter> optionalQrDropOff() {
        return Optional.ofNullable(qrDropOff);
    }

    public BooleanFilter qrDropOff() {
        if (qrDropOff == null) {
            setQrDropOff(new BooleanFilter());
        }
        return qrDropOff;
    }

    public void setQrDropOff(BooleanFilter qrDropOff) {
        this.qrDropOff = qrDropOff;
    }

    public StringFilter getPickupStaffUsername() {
        return pickupStaffUsername;
    }

    public Optional<StringFilter> optionalPickupStaffUsername() {
        return Optional.ofNullable(pickupStaffUsername);
    }

    public StringFilter pickupStaffUsername() {
        if (pickupStaffUsername == null) {
            setPickupStaffUsername(new StringFilter());
        }
        return pickupStaffUsername;
    }

    public void setPickupStaffUsername(StringFilter pickupStaffUsername) {
        this.pickupStaffUsername = pickupStaffUsername;
    }

    public InstantFilter getPickingAt() {
        return pickingAt;
    }

    public Optional<InstantFilter> optionalPickingAt() {
        return Optional.ofNullable(pickingAt);
    }

    public InstantFilter pickingAt() {
        if (pickingAt == null) {
            setPickingAt(new InstantFilter());
        }
        return pickingAt;
    }

    public void setPickingAt(InstantFilter pickingAt) {
        this.pickingAt = pickingAt;
    }

    public InstantFilter getPickedUpAt() {
        return pickedUpAt;
    }

    public Optional<InstantFilter> optionalPickedUpAt() {
        return Optional.ofNullable(pickedUpAt);
    }

    public InstantFilter pickedUpAt() {
        if (pickedUpAt == null) {
            setPickedUpAt(new InstantFilter());
        }
        return pickedUpAt;
    }

    public void setPickedUpAt(InstantFilter pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public StringFilter getReceiverActualName() {
        return receiverActualName;
    }

    public Optional<StringFilter> optionalReceiverActualName() {
        return Optional.ofNullable(receiverActualName);
    }

    public StringFilter receiverActualName() {
        if (receiverActualName == null) {
            setReceiverActualName(new StringFilter());
        }
        return receiverActualName;
    }

    public void setReceiverActualName(StringFilter receiverActualName) {
        this.receiverActualName = receiverActualName;
    }

    public StringFilter getReceiverActualPhone() {
        return receiverActualPhone;
    }

    public Optional<StringFilter> optionalReceiverActualPhone() {
        return Optional.ofNullable(receiverActualPhone);
    }

    public StringFilter receiverActualPhone() {
        if (receiverActualPhone == null) {
            setReceiverActualPhone(new StringFilter());
        }
        return receiverActualPhone;
    }

    public void setReceiverActualPhone(StringFilter receiverActualPhone) {
        this.receiverActualPhone = receiverActualPhone;
    }

    public BigDecimalFilter getWeightKg() {
        return weightKg;
    }

    public Optional<BigDecimalFilter> optionalWeightKg() {
        return Optional.ofNullable(weightKg);
    }

    public BigDecimalFilter weightKg() {
        if (weightKg == null) {
            setWeightKg(new BigDecimalFilter());
        }
        return weightKg;
    }

    public void setWeightKg(BigDecimalFilter weightKg) {
        this.weightKg = weightKg;
    }

    public IntegerFilter getQuantity() {
        return quantity;
    }

    public Optional<IntegerFilter> optionalQuantity() {
        return Optional.ofNullable(quantity);
    }

    public IntegerFilter quantity() {
        if (quantity == null) {
            setQuantity(new IntegerFilter());
        }
        return quantity;
    }

    public void setQuantity(IntegerFilter quantity) {
        this.quantity = quantity;
    }

    public StringFilter getDimensionsText() {
        return dimensionsText;
    }

    public Optional<StringFilter> optionalDimensionsText() {
        return Optional.ofNullable(dimensionsText);
    }

    public StringFilter dimensionsText() {
        if (dimensionsText == null) {
            setDimensionsText(new StringFilter());
        }
        return dimensionsText;
    }

    public void setDimensionsText(StringFilter dimensionsText) {
        this.dimensionsText = dimensionsText;
    }

    public BigDecimalFilter getFareAmount() {
        return fareAmount;
    }

    public Optional<BigDecimalFilter> optionalFareAmount() {
        return Optional.ofNullable(fareAmount);
    }

    public BigDecimalFilter fareAmount() {
        if (fareAmount == null) {
            setFareAmount(new BigDecimalFilter());
        }
        return fareAmount;
    }

    public void setFareAmount(BigDecimalFilter fareAmount) {
        this.fareAmount = fareAmount;
    }

    public BigDecimalFilter getPickupFeeAmount() {
        return pickupFeeAmount;
    }

    public Optional<BigDecimalFilter> optionalPickupFeeAmount() {
        return Optional.ofNullable(pickupFeeAmount);
    }

    public BigDecimalFilter pickupFeeAmount() {
        if (pickupFeeAmount == null) {
            setPickupFeeAmount(new BigDecimalFilter());
        }
        return pickupFeeAmount;
    }

    public void setPickupFeeAmount(BigDecimalFilter pickupFeeAmount) {
        this.pickupFeeAmount = pickupFeeAmount;
    }

    public BigDecimalFilter getDeliveryFeeAmount() {
        return deliveryFeeAmount;
    }

    public Optional<BigDecimalFilter> optionalDeliveryFeeAmount() {
        return Optional.ofNullable(deliveryFeeAmount);
    }

    public BigDecimalFilter deliveryFeeAmount() {
        if (deliveryFeeAmount == null) {
            setDeliveryFeeAmount(new BigDecimalFilter());
        }
        return deliveryFeeAmount;
    }

    public void setDeliveryFeeAmount(BigDecimalFilter deliveryFeeAmount) {
        this.deliveryFeeAmount = deliveryFeeAmount;
    }

    public BigDecimalFilter getPartnerFeeAmount() {
        return partnerFeeAmount;
    }

    public Optional<BigDecimalFilter> optionalPartnerFeeAmount() {
        return Optional.ofNullable(partnerFeeAmount);
    }

    public BigDecimalFilter partnerFeeAmount() {
        if (partnerFeeAmount == null) {
            setPartnerFeeAmount(new BigDecimalFilter());
        }
        return partnerFeeAmount;
    }

    public void setPartnerFeeAmount(BigDecimalFilter partnerFeeAmount) {
        this.partnerFeeAmount = partnerFeeAmount;
    }

    public BigDecimalFilter getPaidAmount() {
        return paidAmount;
    }

    public Optional<BigDecimalFilter> optionalPaidAmount() {
        return Optional.ofNullable(paidAmount);
    }

    public BigDecimalFilter paidAmount() {
        if (paidAmount == null) {
            setPaidAmount(new BigDecimalFilter());
        }
        return paidAmount;
    }

    public void setPaidAmount(BigDecimalFilter paidAmount) {
        this.paidAmount = paidAmount;
    }

    public IntegerFilter getShelfNumber() {
        return shelfNumber;
    }

    public Optional<IntegerFilter> optionalShelfNumber() {
        return Optional.ofNullable(shelfNumber);
    }

    public IntegerFilter shelfNumber() {
        if (shelfNumber == null) {
            setShelfNumber(new IntegerFilter());
        }
        return shelfNumber;
    }

    public void setShelfNumber(IntegerFilter shelfNumber) {
        this.shelfNumber = shelfNumber;
    }

    public StringFilter getCancelReason() {
        return cancelReason;
    }

    public Optional<StringFilter> optionalCancelReason() {
        return Optional.ofNullable(cancelReason);
    }

    public StringFilter cancelReason() {
        if (cancelReason == null) {
            setCancelReason(new StringFilter());
        }
        return cancelReason;
    }

    public void setCancelReason(StringFilter cancelReason) {
        this.cancelReason = cancelReason;
    }

    public InstantFilter getLabelPrintedAt() {
        return labelPrintedAt;
    }

    public Optional<InstantFilter> optionalLabelPrintedAt() {
        return Optional.ofNullable(labelPrintedAt);
    }

    public InstantFilter labelPrintedAt() {
        if (labelPrintedAt == null) {
            setLabelPrintedAt(new InstantFilter());
        }
        return labelPrintedAt;
    }

    public void setLabelPrintedAt(InstantFilter labelPrintedAt) {
        this.labelPrintedAt = labelPrintedAt;
    }

    public IntegerFilter getLabelReprintCount() {
        return labelReprintCount;
    }

    public Optional<IntegerFilter> optionalLabelReprintCount() {
        return Optional.ofNullable(labelReprintCount);
    }

    public IntegerFilter labelReprintCount() {
        if (labelReprintCount == null) {
            setLabelReprintCount(new IntegerFilter());
        }
        return labelReprintCount;
    }

    public void setLabelReprintCount(IntegerFilter labelReprintCount) {
        this.labelReprintCount = labelReprintCount;
    }

    public IntegerFilter getFailCount() {
        return failCount;
    }

    public Optional<IntegerFilter> optionalFailCount() {
        return Optional.ofNullable(failCount);
    }

    public IntegerFilter failCount() {
        if (failCount == null) {
            setFailCount(new IntegerFilter());
        }
        return failCount;
    }

    public void setFailCount(IntegerFilter failCount) {
        this.failCount = failCount;
    }

    public StringFilter getPartnerCode() {
        return partnerCode;
    }

    public Optional<StringFilter> optionalPartnerCode() {
        return Optional.ofNullable(partnerCode);
    }

    public StringFilter partnerCode() {
        if (partnerCode == null) {
            setPartnerCode(new StringFilter());
        }
        return partnerCode;
    }

    public void setPartnerCode(StringFilter partnerCode) {
        this.partnerCode = partnerCode;
    }

    public BigDecimalFilter getPaymentPercent() {
        return paymentPercent;
    }

    public Optional<BigDecimalFilter> optionalPaymentPercent() {
        return Optional.ofNullable(paymentPercent);
    }

    public BigDecimalFilter paymentPercent() {
        if (paymentPercent == null) {
            setPaymentPercent(new BigDecimalFilter());
        }
        return paymentPercent;
    }

    public void setPaymentPercent(BigDecimalFilter paymentPercent) {
        this.paymentPercent = paymentPercent;
    }

    public BooleanFilter getPublicTrackingAllowed() {
        return publicTrackingAllowed;
    }

    public Optional<BooleanFilter> optionalPublicTrackingAllowed() {
        return Optional.ofNullable(publicTrackingAllowed);
    }

    public BooleanFilter publicTrackingAllowed() {
        if (publicTrackingAllowed == null) {
            setPublicTrackingAllowed(new BooleanFilter());
        }
        return publicTrackingAllowed;
    }

    public void setPublicTrackingAllowed(BooleanFilter publicTrackingAllowed) {
        this.publicTrackingAllowed = publicTrackingAllowed;
    }

    public LongFilter getIssueId() {
        return issueId;
    }

    public Optional<LongFilter> optionalIssueId() {
        return Optional.ofNullable(issueId);
    }

    public LongFilter issueId() {
        if (issueId == null) {
            setIssueId(new LongFilter());
        }
        return issueId;
    }

    public void setIssueId(LongFilter issueId) {
        this.issueId = issueId;
    }

    public LongFilter getReturnRequestId() {
        return returnRequestId;
    }

    public Optional<LongFilter> optionalReturnRequestId() {
        return Optional.ofNullable(returnRequestId);
    }

    public LongFilter returnRequestId() {
        if (returnRequestId == null) {
            setReturnRequestId(new LongFilter());
        }
        return returnRequestId;
    }

    public void setReturnRequestId(LongFilter returnRequestId) {
        this.returnRequestId = returnRequestId;
    }

    public LongFilter getFareAdjustmentRequestId() {
        return fareAdjustmentRequestId;
    }

    public Optional<LongFilter> optionalFareAdjustmentRequestId() {
        return Optional.ofNullable(fareAdjustmentRequestId);
    }

    public LongFilter fareAdjustmentRequestId() {
        if (fareAdjustmentRequestId == null) {
            setFareAdjustmentRequestId(new LongFilter());
        }
        return fareAdjustmentRequestId;
    }

    public void setFareAdjustmentRequestId(LongFilter fareAdjustmentRequestId) {
        this.fareAdjustmentRequestId = fareAdjustmentRequestId;
    }

    public LongFilter getSenderCustomerId() {
        return senderCustomerId;
    }

    public Optional<LongFilter> optionalSenderCustomerId() {
        return Optional.ofNullable(senderCustomerId);
    }

    public LongFilter senderCustomerId() {
        if (senderCustomerId == null) {
            setSenderCustomerId(new LongFilter());
        }
        return senderCustomerId;
    }

    public void setSenderCustomerId(LongFilter senderCustomerId) {
        this.senderCustomerId = senderCustomerId;
    }

    public LongFilter getFromOfficeId() {
        return fromOfficeId;
    }

    public Optional<LongFilter> optionalFromOfficeId() {
        return Optional.ofNullable(fromOfficeId);
    }

    public LongFilter fromOfficeId() {
        if (fromOfficeId == null) {
            setFromOfficeId(new LongFilter());
        }
        return fromOfficeId;
    }

    public void setFromOfficeId(LongFilter fromOfficeId) {
        this.fromOfficeId = fromOfficeId;
    }

    public LongFilter getToOfficeId() {
        return toOfficeId;
    }

    public Optional<LongFilter> optionalToOfficeId() {
        return Optional.ofNullable(toOfficeId);
    }

    public LongFilter toOfficeId() {
        if (toOfficeId == null) {
            setToOfficeId(new LongFilter());
        }
        return toOfficeId;
    }

    public void setToOfficeId(LongFilter toOfficeId) {
        this.toOfficeId = toOfficeId;
    }

    public LongFilter getHubOfficeId() {
        return hubOfficeId;
    }

    public Optional<LongFilter> optionalHubOfficeId() {
        return Optional.ofNullable(hubOfficeId);
    }

    public LongFilter hubOfficeId() {
        if (hubOfficeId == null) {
            setHubOfficeId(new LongFilter());
        }
        return hubOfficeId;
    }

    public void setHubOfficeId(LongFilter hubOfficeId) {
        this.hubOfficeId = hubOfficeId;
    }

    public LongFilter getFinalToOfficeId() {
        return finalToOfficeId;
    }

    public Optional<LongFilter> optionalFinalToOfficeId() {
        return Optional.ofNullable(finalToOfficeId);
    }

    public LongFilter finalToOfficeId() {
        if (finalToOfficeId == null) {
            setFinalToOfficeId(new LongFilter());
        }
        return finalToOfficeId;
    }

    public void setFinalToOfficeId(LongFilter finalToOfficeId) {
        this.finalToOfficeId = finalToOfficeId;
    }

    public LongFilter getCurrentTripId() {
        return currentTripId;
    }

    public Optional<LongFilter> optionalCurrentTripId() {
        return Optional.ofNullable(currentTripId);
    }

    public LongFilter currentTripId() {
        if (currentTripId == null) {
            setCurrentTripId(new LongFilter());
        }
        return currentTripId;
    }

    public void setCurrentTripId(LongFilter currentTripId) {
        this.currentTripId = currentTripId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ShipmentOrderCriteria that = (ShipmentOrderCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(orderCode, that.orderCode) &&
            Objects.equals(draftCode, that.draftCode) &&
            Objects.equals(status, that.status) &&
            Objects.equals(forwardStage, that.forwardStage) &&
            Objects.equals(returnStage, that.returnStage) &&
            Objects.equals(paymentTerm, that.paymentTerm) &&
            Objects.equals(goodsType, that.goodsType) &&
            Objects.equals(serviceType, that.serviceType) &&
            Objects.equals(senderName, that.senderName) &&
            Objects.equals(senderPhone, that.senderPhone) &&
            Objects.equals(receiverName, that.receiverName) &&
            Objects.equals(receiverPhone, that.receiverPhone) &&
            Objects.equals(deliveryAddress, that.deliveryAddress) &&
            Objects.equals(pickupAddress, that.pickupAddress) &&
            Objects.equals(homePickup, that.homePickup) &&
            Objects.equals(homeDelivery, that.homeDelivery) &&
            Objects.equals(qrDropOff, that.qrDropOff) &&
            Objects.equals(pickupStaffUsername, that.pickupStaffUsername) &&
            Objects.equals(pickingAt, that.pickingAt) &&
            Objects.equals(pickedUpAt, that.pickedUpAt) &&
            Objects.equals(receiverActualName, that.receiverActualName) &&
            Objects.equals(receiverActualPhone, that.receiverActualPhone) &&
            Objects.equals(weightKg, that.weightKg) &&
            Objects.equals(quantity, that.quantity) &&
            Objects.equals(dimensionsText, that.dimensionsText) &&
            Objects.equals(fareAmount, that.fareAmount) &&
            Objects.equals(pickupFeeAmount, that.pickupFeeAmount) &&
            Objects.equals(deliveryFeeAmount, that.deliveryFeeAmount) &&
            Objects.equals(partnerFeeAmount, that.partnerFeeAmount) &&
            Objects.equals(paidAmount, that.paidAmount) &&
            Objects.equals(shelfNumber, that.shelfNumber) &&
            Objects.equals(cancelReason, that.cancelReason) &&
            Objects.equals(labelPrintedAt, that.labelPrintedAt) &&
            Objects.equals(labelReprintCount, that.labelReprintCount) &&
            Objects.equals(failCount, that.failCount) &&
            Objects.equals(partnerCode, that.partnerCode) &&
            Objects.equals(paymentPercent, that.paymentPercent) &&
            Objects.equals(publicTrackingAllowed, that.publicTrackingAllowed) &&
            Objects.equals(issueId, that.issueId) &&
            Objects.equals(returnRequestId, that.returnRequestId) &&
            Objects.equals(fareAdjustmentRequestId, that.fareAdjustmentRequestId) &&
            Objects.equals(senderCustomerId, that.senderCustomerId) &&
            Objects.equals(fromOfficeId, that.fromOfficeId) &&
            Objects.equals(toOfficeId, that.toOfficeId) &&
            Objects.equals(hubOfficeId, that.hubOfficeId) &&
            Objects.equals(finalToOfficeId, that.finalToOfficeId) &&
            Objects.equals(currentTripId, that.currentTripId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            orderCode,
            draftCode,
            status,
            forwardStage,
            returnStage,
            paymentTerm,
            goodsType,
            serviceType,
            senderName,
            senderPhone,
            receiverName,
            receiverPhone,
            deliveryAddress,
            pickupAddress,
            homePickup,
            homeDelivery,
            qrDropOff,
            pickupStaffUsername,
            pickingAt,
            pickedUpAt,
            receiverActualName,
            receiverActualPhone,
            weightKg,
            quantity,
            dimensionsText,
            fareAmount,
            pickupFeeAmount,
            deliveryFeeAmount,
            partnerFeeAmount,
            paidAmount,
            shelfNumber,
            cancelReason,
            labelPrintedAt,
            labelReprintCount,
            failCount,
            partnerCode,
            paymentPercent,
            publicTrackingAllowed,
            issueId,
            returnRequestId,
            fareAdjustmentRequestId,
            senderCustomerId,
            fromOfficeId,
            toOfficeId,
            hubOfficeId,
            finalToOfficeId,
            currentTripId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShipmentOrderCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalOrderCode().map(f -> "orderCode=" + f + ", ").orElse("") +
            optionalDraftCode().map(f -> "draftCode=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalForwardStage().map(f -> "forwardStage=" + f + ", ").orElse("") +
            optionalReturnStage().map(f -> "returnStage=" + f + ", ").orElse("") +
            optionalPaymentTerm().map(f -> "paymentTerm=" + f + ", ").orElse("") +
            optionalGoodsType().map(f -> "goodsType=" + f + ", ").orElse("") +
            optionalServiceType().map(f -> "serviceType=" + f + ", ").orElse("") +
            optionalSenderName().map(f -> "senderName=" + f + ", ").orElse("") +
            optionalSenderPhone().map(f -> "senderPhone=" + f + ", ").orElse("") +
            optionalReceiverName().map(f -> "receiverName=" + f + ", ").orElse("") +
            optionalReceiverPhone().map(f -> "receiverPhone=" + f + ", ").orElse("") +
            optionalDeliveryAddress().map(f -> "deliveryAddress=" + f + ", ").orElse("") +
            optionalPickupAddress().map(f -> "pickupAddress=" + f + ", ").orElse("") +
            optionalHomePickup().map(f -> "homePickup=" + f + ", ").orElse("") +
            optionalHomeDelivery().map(f -> "homeDelivery=" + f + ", ").orElse("") +
            optionalQrDropOff().map(f -> "qrDropOff=" + f + ", ").orElse("") +
            optionalPickupStaffUsername().map(f -> "pickupStaffUsername=" + f + ", ").orElse("") +
            optionalPickingAt().map(f -> "pickingAt=" + f + ", ").orElse("") +
            optionalPickedUpAt().map(f -> "pickedUpAt=" + f + ", ").orElse("") +
            optionalReceiverActualName().map(f -> "receiverActualName=" + f + ", ").orElse("") +
            optionalReceiverActualPhone().map(f -> "receiverActualPhone=" + f + ", ").orElse("") +
            optionalWeightKg().map(f -> "weightKg=" + f + ", ").orElse("") +
            optionalQuantity().map(f -> "quantity=" + f + ", ").orElse("") +
            optionalDimensionsText().map(f -> "dimensionsText=" + f + ", ").orElse("") +
            optionalFareAmount().map(f -> "fareAmount=" + f + ", ").orElse("") +
            optionalPickupFeeAmount().map(f -> "pickupFeeAmount=" + f + ", ").orElse("") +
            optionalDeliveryFeeAmount().map(f -> "deliveryFeeAmount=" + f + ", ").orElse("") +
            optionalPartnerFeeAmount().map(f -> "partnerFeeAmount=" + f + ", ").orElse("") +
            optionalPaidAmount().map(f -> "paidAmount=" + f + ", ").orElse("") +
            optionalShelfNumber().map(f -> "shelfNumber=" + f + ", ").orElse("") +
            optionalCancelReason().map(f -> "cancelReason=" + f + ", ").orElse("") +
            optionalLabelPrintedAt().map(f -> "labelPrintedAt=" + f + ", ").orElse("") +
            optionalLabelReprintCount().map(f -> "labelReprintCount=" + f + ", ").orElse("") +
            optionalFailCount().map(f -> "failCount=" + f + ", ").orElse("") +
            optionalPartnerCode().map(f -> "partnerCode=" + f + ", ").orElse("") +
            optionalPaymentPercent().map(f -> "paymentPercent=" + f + ", ").orElse("") +
            optionalPublicTrackingAllowed().map(f -> "publicTrackingAllowed=" + f + ", ").orElse("") +
            optionalIssueId().map(f -> "issueId=" + f + ", ").orElse("") +
            optionalReturnRequestId().map(f -> "returnRequestId=" + f + ", ").orElse("") +
            optionalFareAdjustmentRequestId().map(f -> "fareAdjustmentRequestId=" + f + ", ").orElse("") +
            optionalSenderCustomerId().map(f -> "senderCustomerId=" + f + ", ").orElse("") +
            optionalFromOfficeId().map(f -> "fromOfficeId=" + f + ", ").orElse("") +
            optionalToOfficeId().map(f -> "toOfficeId=" + f + ", ").orElse("") +
            optionalHubOfficeId().map(f -> "hubOfficeId=" + f + ", ").orElse("") +
            optionalFinalToOfficeId().map(f -> "finalToOfficeId=" + f + ", ").orElse("") +
            optionalCurrentTripId().map(f -> "currentTripId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
