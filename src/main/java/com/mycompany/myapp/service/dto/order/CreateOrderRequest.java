package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CreateOrderRequest {

    /** When set, confirm existing DRAFT and assign real orderCode. */
    private String draftCode;

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

    @NotBlank
    private String fromOfficeCode;

    @NotBlank
    private String toOfficeCode;

    private String hubOfficeCode;

    private String finalToOfficeCode;

    private BigDecimal weightKg;

    @Min(1)
    private Integer quantity = 1;

    private Boolean homeDelivery = false;

    private Boolean homePickup = false;

    private Boolean qrDropOff = false;

    @Size(max = 500)
    private String deliveryAddress;

    @Size(max = 500)
    private String pickupAddress;

    @Size(max = 2000)
    private String note;

    /** Optional override; otherwise calculated. */
    private BigDecimal fareAmount;

    /** Tuyến (Branch) code or name — preferred fare table. */
    private String branchCode;

    private BigDecimal codAmount;
    private BigDecimal codFeeAmount;
    private String bankName;
    private String bankAccountNo;
    private String bankAccountName;
    private String routeLabel;
    private String itineraryLabel;

    public String getDraftCode() {
        return draftCode;
    }

    public void setDraftCode(String draftCode) {
        this.draftCode = draftCode;
    }

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

    public Boolean getHomeDelivery() {
        return homeDelivery;
    }

    public void setHomeDelivery(Boolean homeDelivery) {
        this.homeDelivery = homeDelivery;
    }

    public Boolean getHomePickup() {
        return homePickup;
    }

    public void setHomePickup(Boolean homePickup) {
        this.homePickup = homePickup;
    }

    public Boolean getQrDropOff() {
        return qrDropOff;
    }

    public void setQrDropOff(Boolean qrDropOff) {
        this.qrDropOff = qrDropOff;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public BigDecimal getCodAmount() {
        return codAmount;
    }

    public void setCodAmount(BigDecimal codAmount) {
        this.codAmount = codAmount;
    }

    public BigDecimal getCodFeeAmount() {
        return codFeeAmount;
    }

    public void setCodFeeAmount(BigDecimal codFeeAmount) {
        this.codFeeAmount = codFeeAmount;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccountNo() {
        return bankAccountNo;
    }

    public void setBankAccountNo(String bankAccountNo) {
        this.bankAccountNo = bankAccountNo;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    public void setRouteLabel(String routeLabel) {
        this.routeLabel = routeLabel;
    }

    public String getItineraryLabel() {
        return itineraryLabel;
    }

    public void setItineraryLabel(String itineraryLabel) {
        this.itineraryLabel = itineraryLabel;
    }
}
