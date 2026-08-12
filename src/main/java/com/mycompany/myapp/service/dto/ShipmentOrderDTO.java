package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import com.mycompany.myapp.domain.enumeration.ServiceType;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.ShipmentOrder} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShipmentOrderDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 40)
    private String orderCode;

    @Size(max = 40)
    private String draftCode;

    @NotNull
    private OrderStatus status;

    private ForwardStage forwardStage;

    private ReturnStage returnStage;

    @NotNull
    private PaymentTerm paymentTerm;

    @NotNull
    private GoodsType goodsType;

    @NotNull
    private ServiceType serviceType;

    @Size(max = 100)
    private String senderName;

    @NotNull
    @Pattern(regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$")
    private String senderPhone;

    @NotNull
    @Size(max = 100)
    private String receiverName;

    @NotNull
    @Pattern(regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$")
    private String receiverPhone;

    @Size(max = 500)
    private String deliveryAddress;

    @Size(max = 500)
    private String pickupAddress;

    @NotNull
    private Boolean homePickup;

    @NotNull
    private Boolean homeDelivery;

    @NotNull
    private Boolean qrDropOff;

    @Size(max = 50)
    private String pickupStaffUsername;

    private Instant pickingAt;

    private Instant pickedUpAt;

    @Size(max = 100)
    private String receiverActualName;

    @Size(max = 20)
    private String receiverActualPhone;

    @DecimalMin(value = "0")
    private BigDecimal weightKg;

    @NotNull
    @Min(value = 1)
    private Integer quantity;

    @Size(max = 120)
    private String dimensionsText;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal fareAmount;

    @DecimalMin(value = "0")
    private BigDecimal pickupFeeAmount;

    @DecimalMin(value = "0")
    private BigDecimal deliveryFeeAmount;

    @DecimalMin(value = "0")
    private BigDecimal partnerFeeAmount;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal paidAmount;

    @Min(value = 0)
    @Max(value = 9)
    private Integer shelfNumber;

    @Lob
    private String note;

    @Size(max = 255)
    private String cancelReason;

    private Instant labelPrintedAt;

    @Min(value = 0)
    private Integer labelReprintCount;

    @NotNull
    @Min(value = 0)
    private Integer failCount;

    @Size(max = 100)
    private String partnerCode;

    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    private BigDecimal paymentPercent;

    @NotNull
    private Boolean publicTrackingAllowed;

    private OrderIssueDTO issue;

    private OrderReturnRequestDTO returnRequest;

    private OrderFareAdjustmentRequestDTO fareAdjustmentRequest;

    private CustomerDTO senderCustomer;

    @NotNull
    private OfficeDTO fromOffice;

    @NotNull
    private OfficeDTO toOffice;

    private OfficeDTO hubOffice;

    private OfficeDTO finalToOffice;

    private TripDTO currentTrip;

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

    public PaymentTerm getPaymentTerm() {
        return paymentTerm;
    }

    public void setPaymentTerm(PaymentTerm paymentTerm) {
        this.paymentTerm = paymentTerm;
    }

    public GoodsType getGoodsType() {
        return goodsType;
    }

    public void setGoodsType(GoodsType goodsType) {
        this.goodsType = goodsType;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
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

    public String getPickupStaffUsername() {
        return pickupStaffUsername;
    }

    public void setPickupStaffUsername(String pickupStaffUsername) {
        this.pickupStaffUsername = pickupStaffUsername;
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

    public String getReceiverActualName() {
        return receiverActualName;
    }

    public void setReceiverActualName(String receiverActualName) {
        this.receiverActualName = receiverActualName;
    }

    public String getReceiverActualPhone() {
        return receiverActualPhone;
    }

    public void setReceiverActualPhone(String receiverActualPhone) {
        this.receiverActualPhone = receiverActualPhone;
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

    public String getDimensionsText() {
        return dimensionsText;
    }

    public void setDimensionsText(String dimensionsText) {
        this.dimensionsText = dimensionsText;
    }

    public BigDecimal getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
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

    public BigDecimal getPartnerFeeAmount() {
        return partnerFeeAmount;
    }

    public void setPartnerFeeAmount(BigDecimal partnerFeeAmount) {
        this.partnerFeeAmount = partnerFeeAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
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

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Instant getLabelPrintedAt() {
        return labelPrintedAt;
    }

    public void setLabelPrintedAt(Instant labelPrintedAt) {
        this.labelPrintedAt = labelPrintedAt;
    }

    public Integer getLabelReprintCount() {
        return labelReprintCount;
    }

    public void setLabelReprintCount(Integer labelReprintCount) {
        this.labelReprintCount = labelReprintCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public BigDecimal getPaymentPercent() {
        return paymentPercent;
    }

    public void setPaymentPercent(BigDecimal paymentPercent) {
        this.paymentPercent = paymentPercent;
    }

    public Boolean getPublicTrackingAllowed() {
        return publicTrackingAllowed;
    }

    public void setPublicTrackingAllowed(Boolean publicTrackingAllowed) {
        this.publicTrackingAllowed = publicTrackingAllowed;
    }

    public OrderIssueDTO getIssue() {
        return issue;
    }

    public void setIssue(OrderIssueDTO issue) {
        this.issue = issue;
    }

    public OrderReturnRequestDTO getReturnRequest() {
        return returnRequest;
    }

    public void setReturnRequest(OrderReturnRequestDTO returnRequest) {
        this.returnRequest = returnRequest;
    }

    public OrderFareAdjustmentRequestDTO getFareAdjustmentRequest() {
        return fareAdjustmentRequest;
    }

    public void setFareAdjustmentRequest(OrderFareAdjustmentRequestDTO fareAdjustmentRequest) {
        this.fareAdjustmentRequest = fareAdjustmentRequest;
    }

    public CustomerDTO getSenderCustomer() {
        return senderCustomer;
    }

    public void setSenderCustomer(CustomerDTO senderCustomer) {
        this.senderCustomer = senderCustomer;
    }

    public OfficeDTO getFromOffice() {
        return fromOffice;
    }

    public void setFromOffice(OfficeDTO fromOffice) {
        this.fromOffice = fromOffice;
    }

    public OfficeDTO getToOffice() {
        return toOffice;
    }

    public void setToOffice(OfficeDTO toOffice) {
        this.toOffice = toOffice;
    }

    public OfficeDTO getHubOffice() {
        return hubOffice;
    }

    public void setHubOffice(OfficeDTO hubOffice) {
        this.hubOffice = hubOffice;
    }

    public OfficeDTO getFinalToOffice() {
        return finalToOffice;
    }

    public void setFinalToOffice(OfficeDTO finalToOffice) {
        this.finalToOffice = finalToOffice;
    }

    public TripDTO getCurrentTrip() {
        return currentTrip;
    }

    public void setCurrentTrip(TripDTO currentTrip) {
        this.currentTrip = currentTrip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShipmentOrderDTO)) {
            return false;
        }

        ShipmentOrderDTO shipmentOrderDTO = (ShipmentOrderDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, shipmentOrderDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShipmentOrderDTO{" +
            "id=" + getId() +
            ", orderCode='" + getOrderCode() + "'" +
            ", draftCode='" + getDraftCode() + "'" +
            ", status='" + getStatus() + "'" +
            ", forwardStage='" + getForwardStage() + "'" +
            ", returnStage='" + getReturnStage() + "'" +
            ", paymentTerm='" + getPaymentTerm() + "'" +
            ", goodsType='" + getGoodsType() + "'" +
            ", serviceType='" + getServiceType() + "'" +
            ", senderName='" + getSenderName() + "'" +
            ", senderPhone='" + getSenderPhone() + "'" +
            ", receiverName='" + getReceiverName() + "'" +
            ", receiverPhone='" + getReceiverPhone() + "'" +
            ", deliveryAddress='" + getDeliveryAddress() + "'" +
            ", pickupAddress='" + getPickupAddress() + "'" +
            ", homePickup='" + getHomePickup() + "'" +
            ", homeDelivery='" + getHomeDelivery() + "'" +
            ", qrDropOff='" + getQrDropOff() + "'" +
            ", pickupStaffUsername='" + getPickupStaffUsername() + "'" +
            ", pickingAt='" + getPickingAt() + "'" +
            ", pickedUpAt='" + getPickedUpAt() + "'" +
            ", receiverActualName='" + getReceiverActualName() + "'" +
            ", receiverActualPhone='" + getReceiverActualPhone() + "'" +
            ", weightKg=" + getWeightKg() +
            ", quantity=" + getQuantity() +
            ", dimensionsText='" + getDimensionsText() + "'" +
            ", fareAmount=" + getFareAmount() +
            ", pickupFeeAmount=" + getPickupFeeAmount() +
            ", deliveryFeeAmount=" + getDeliveryFeeAmount() +
            ", partnerFeeAmount=" + getPartnerFeeAmount() +
            ", paidAmount=" + getPaidAmount() +
            ", shelfNumber=" + getShelfNumber() +
            ", note='" + getNote() + "'" +
            ", cancelReason='" + getCancelReason() + "'" +
            ", labelPrintedAt='" + getLabelPrintedAt() + "'" +
            ", labelReprintCount=" + getLabelReprintCount() +
            ", failCount=" + getFailCount() +
            ", partnerCode='" + getPartnerCode() + "'" +
            ", paymentPercent=" + getPaymentPercent() +
            ", publicTrackingAllowed='" + getPublicTrackingAllowed() + "'" +
            ", issue=" + getIssue() +
            ", returnRequest=" + getReturnRequest() +
            ", fareAdjustmentRequest=" + getFareAdjustmentRequest() +
            ", senderCustomer=" + getSenderCustomer() +
            ", fromOffice=" + getFromOffice() +
            ", toOffice=" + getToOffice() +
            ", hubOffice=" + getHubOffice() +
            ", finalToOffice=" + getFinalToOffice() +
            ", currentTrip=" + getCurrentTrip() +
            "}";
    }
}
