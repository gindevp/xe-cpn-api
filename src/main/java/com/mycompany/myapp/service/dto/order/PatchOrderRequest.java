package com.mycompany.myapp.service.dto.order;

import java.math.BigDecimal;

public class PatchOrderRequest {

    private String senderName;
    private String senderPhone;
    private String receiverName;
    private String receiverPhone;
    private String note;
    private String pickupAddress;
    private String deliveryAddress;
    private BigDecimal weightKg;
    private Integer quantity;
    private BigDecimal fareAmount;
    private Boolean homePickup;
    private Boolean homeDelivery;
    private String pickingAt;
    private String pickedUpAt;
    private String pickupStaffUsername;
    private String partnerCode;
    private BigDecimal partnerFeeAmount;

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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
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

    public String getPickingAt() {
        return pickingAt;
    }

    public void setPickingAt(String pickingAt) {
        this.pickingAt = pickingAt;
    }

    public String getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(String pickedUpAt) {
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
}
