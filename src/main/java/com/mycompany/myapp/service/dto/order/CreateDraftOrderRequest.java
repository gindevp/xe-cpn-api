package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CreateDraftOrderRequest {

    @NotBlank
    @Pattern(regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$")
    private String senderPhone;

    @Size(max = 100)
    private String senderName;

    @NotBlank
    @Size(max = 100)
    private String receiverName;

    @NotBlank
    @Pattern(regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$")
    private String receiverPhone;

    @NotNull
    private GoodsType goodsType;

    @NotNull
    private PaymentTerm paymentTerm;

    /** Mid-point kg from FE weight band (optional). */
    private BigDecimal estimatedWeightKg;

    private Boolean homeDelivery = false;

    @Size(max = 500)
    private String deliveryAddress;

    private Boolean homePickup = false;

    @Size(max = 500)
    private String pickupAddress;

    /** Destination office when not home delivery. */
    private String toOfficeCode;

    /** Hub office when home delivery (FE hubOffice). */
    private String hubOfficeCode;

    /** Public draft origin; defaults to GP. */
    private String fromOfficeCode;

    @Size(max = 2000)
    private String note;

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
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

    public BigDecimal getEstimatedWeightKg() {
        return estimatedWeightKg;
    }

    public void setEstimatedWeightKg(BigDecimal estimatedWeightKg) {
        this.estimatedWeightKg = estimatedWeightKg;
    }

    public Boolean getHomeDelivery() {
        return homeDelivery;
    }

    public void setHomeDelivery(Boolean homeDelivery) {
        this.homeDelivery = homeDelivery;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Boolean getHomePickup() {
        return homePickup;
    }

    public void setHomePickup(Boolean homePickup) {
        this.homePickup = homePickup;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
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

    public String getFromOfficeCode() {
        return fromOfficeCode;
    }

    public void setFromOfficeCode(String fromOfficeCode) {
        this.fromOfficeCode = fromOfficeCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
