package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.LegStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderSummaryDTO {

    private Long id;
    private String orderCode;
    private String draftCode;
    private OrderStatus status;
    private ForwardStage forwardStage;
    private ReturnStage returnStage;
    private String senderName;
    private String senderPhone;
    private String receiverName;
    private String receiverPhone;
    private String fromOfficeCode;
    private String toOfficeCode;
    private String hubOfficeCode;
    private String finalToOfficeCode;
    private GoodsType goodsType;
    private PaymentTerm paymentTerm;
    private BigDecimal weightKg;
    private Integer quantity;
    private BigDecimal fareAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private BigDecimal pickupFeeAmount;
    private BigDecimal deliveryFeeAmount;
    private Boolean homePickup;
    private Boolean homeDelivery;
    private Boolean qrDropOff;
    private String currentTripCode;
    private Integer shelfNumber;
    private String note;
    private Instant pickingAt;
    private Instant pickedUpAt;
    private String pickupStaffUsername;
    private String partnerCode;
    private BigDecimal partnerFeeAmount;
    private Integer currentLegIndex;
    private List<OrderLegViewDTO> legs = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getDraftCode() {
        return draftCode;
    }

    public void setDraftCode(String draftCode) {
        this.draftCode = draftCode;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public ForwardStage getForwardStage() {
        return forwardStage;
    }

    public void setForwardStage(ForwardStage forwardStage) {
        this.forwardStage = forwardStage;
    }

    public ReturnStage getReturnStage() {
        return returnStage;
    }

    public void setReturnStage(ReturnStage returnStage) {
        this.returnStage = returnStage;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getFromOfficeCode() {
        return fromOfficeCode;
    }

    public void setFromOfficeCode(String fromOfficeCode) {
        this.fromOfficeCode = fromOfficeCode;
    }

    public String getToOfficeCode() {
        return toOfficeCode;
    }

    public void setToOfficeCode(String toOfficeCode) {
        this.toOfficeCode = toOfficeCode;
    }

    public String getHubOfficeCode() {
        return hubOfficeCode;
    }

    public void setHubOfficeCode(String hubOfficeCode) {
        this.hubOfficeCode = hubOfficeCode;
    }

    public String getFinalToOfficeCode() {
        return finalToOfficeCode;
    }

    public void setFinalToOfficeCode(String finalToOfficeCode) {
        this.finalToOfficeCode = finalToOfficeCode;
    }

    public GoodsType getGoodsType() {
        return goodsType;
    }

    public void setGoodsType(GoodsType goodsType) {
        this.goodsType = goodsType;
    }

    public PaymentTerm getPaymentTerm() {
        return paymentTerm;
    }

    public void setPaymentTerm(PaymentTerm paymentTerm) {
        this.paymentTerm = paymentTerm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(BigDecimal dueAmount) {
        this.dueAmount = dueAmount;
    }

    public BigDecimal getPickupFeeAmount() {
        return pickupFeeAmount;
    }

    public void setPickupFeeAmount(BigDecimal pickupFeeAmount) {
        this.pickupFeeAmount = pickupFeeAmount;
    }

    public BigDecimal getDeliveryFeeAmount() {
        return deliveryFeeAmount;
    }

    public void setDeliveryFeeAmount(BigDecimal deliveryFeeAmount) {
        this.deliveryFeeAmount = deliveryFeeAmount;
    }

    public Boolean getHomePickup() {
        return homePickup;
    }

    public void setHomePickup(Boolean homePickup) {
        this.homePickup = homePickup;
    }

    public Boolean getHomeDelivery() {
        return homeDelivery;
    }

    public void setHomeDelivery(Boolean homeDelivery) {
        this.homeDelivery = homeDelivery;
    }

    public Boolean getQrDropOff() {
        return qrDropOff;
    }

    public void setQrDropOff(Boolean qrDropOff) {
        this.qrDropOff = qrDropOff;
    }

    public String getCurrentTripCode() {
        return currentTripCode;
    }

    public void setCurrentTripCode(String currentTripCode) {
        this.currentTripCode = currentTripCode;
    }

    public Integer getShelfNumber() {
        return shelfNumber;
    }

    public void setShelfNumber(Integer shelfNumber) {
        this.shelfNumber = shelfNumber;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getPickingAt() {
        return pickingAt;
    }

    public void setPickingAt(Instant pickingAt) {
        this.pickingAt = pickingAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(Instant pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public String getPickupStaffUsername() {
        return pickupStaffUsername;
    }

    public void setPickupStaffUsername(String pickupStaffUsername) {
        this.pickupStaffUsername = pickupStaffUsername;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public BigDecimal getPartnerFeeAmount() {
        return partnerFeeAmount;
    }

    public void setPartnerFeeAmount(BigDecimal partnerFeeAmount) {
        this.partnerFeeAmount = partnerFeeAmount;
    }

    public Integer getCurrentLegIndex() {
        return currentLegIndex;
    }

    public void setCurrentLegIndex(Integer currentLegIndex) {
        this.currentLegIndex = currentLegIndex;
    }

    public List<OrderLegViewDTO> getLegs() {
        return legs;
    }

    public void setLegs(List<OrderLegViewDTO> legs) {
        this.legs = legs;
    }

    public static class OrderLegViewDTO {

        private Integer index;
        private String fromOfficeCode;
        private String toOfficeCode;
        private String tripCode;
        private LegStatus status;
        private Instant departedAt;
        private Instant arrivedAt;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getFromOfficeCode() {
            return fromOfficeCode;
        }

        public void setFromOfficeCode(String fromOfficeCode) {
            this.fromOfficeCode = fromOfficeCode;
        }

        public String getToOfficeCode() {
            return toOfficeCode;
        }

        public void setToOfficeCode(String toOfficeCode) {
            this.toOfficeCode = toOfficeCode;
        }

        public String getTripCode() {
            return tripCode;
        }

        public void setTripCode(String tripCode) {
            this.tripCode = tripCode;
        }

        public LegStatus getStatus() {
            return status;
        }

        public void setStatus(LegStatus status) {
            this.status = status;
        }

        public Instant getDepartedAt() {
            return departedAt;
        }

        public void setDepartedAt(Instant departedAt) {
            this.departedAt = departedAt;
        }

        public Instant getArrivedAt() {
            return arrivedAt;
        }

        public void setArrivedAt(Instant arrivedAt) {
            this.arrivedAt = arrivedAt;
        }
    }
}
