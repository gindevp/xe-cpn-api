package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import com.mycompany.myapp.domain.enumeration.ServiceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A ShipmentOrder.
 */
@Entity
@Table(name = "shipment_order")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShipmentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @NotNull
    @Size(max = 40)
    @Column(name = "order_code", length = 40, nullable = false, unique = true)
    private String orderCode;

    @Size(max = 40)
    @Column(name = "draft_code", length = 40, unique = true)
    private String draftCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "forward_stage")
    private ForwardStage forwardStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_stage")
    private ReturnStage returnStage;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_term", nullable = false)
    private PaymentTerm paymentTerm;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "goods_type", nullable = false)
    private GoodsType goodsType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Size(max = 100)
    @Column(name = "sender_name", length = 100)
    private String senderName;

    @NotNull
    @Pattern(regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$")
    @Column(name = "sender_phone", nullable = false)
    private String senderPhone;

    @NotNull
    @Size(max = 100)
    @Column(name = "receiver_name", length = 100, nullable = false)
    private String receiverName;

    @NotNull
    @Pattern(regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$")
    @Column(name = "receiver_phone", nullable = false)
    private String receiverPhone;

    @Size(max = 500)
    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Size(max = 500)
    @Column(name = "pickup_address", length = 500)
    private String pickupAddress;

    @NotNull
    @Column(name = "home_pickup", nullable = false)
    private Boolean homePickup;

    @NotNull
    @Column(name = "home_delivery", nullable = false)
    private Boolean homeDelivery;

    @NotNull
    @Column(name = "qr_drop_off", nullable = false)
    private Boolean qrDropOff;

    @Size(max = 50)
    @Column(name = "pickup_staff_username", length = 50)
    private String pickupStaffUsername;

    @Column(name = "picking_at")
    private Instant pickingAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Size(max = 100)
    @Column(name = "receiver_actual_name", length = 100)
    private String receiverActualName;

    @Size(max = 20)
    @Column(name = "receiver_actual_phone", length = 20)
    private String receiverActualPhone;

    @DecimalMin(value = "0")
    @Column(name = "weight_kg", precision = 21, scale = 2)
    private BigDecimal weightKg;

    @NotNull
    @Min(value = 1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Size(max = 120)
    @Column(name = "dimensions_text", length = 120)
    private String dimensionsText;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "fare_amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal fareAmount;

    @DecimalMin(value = "0")
    @Column(name = "pickup_fee_amount", precision = 21, scale = 2)
    private BigDecimal pickupFeeAmount;

    @DecimalMin(value = "0")
    @Column(name = "delivery_fee_amount", precision = 21, scale = 2)
    private BigDecimal deliveryFeeAmount;

    @DecimalMin(value = "0")
    @Column(name = "partner_fee_amount", precision = 21, scale = 2)
    private BigDecimal partnerFeeAmount;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "paid_amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal paidAmount;

    @Min(value = 0)
    @Max(value = 9)
    @Column(name = "shelf_number")
    private Integer shelfNumber;

    @Lob
    @Column(name = "note")
    private String note;

    @Size(max = 255)
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "label_printed_at")
    private Instant labelPrintedAt;

    @Min(value = 0)
    @Column(name = "label_reprint_count")
    private Integer labelReprintCount;

    @NotNull
    @Min(value = 0)
    @Column(name = "fail_count", nullable = false)
    private Integer failCount;

    @Size(max = 100)
    @Column(name = "partner_code", length = 100)
    private String partnerCode;

    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    @Column(name = "payment_percent", precision = 21, scale = 2)
    private BigDecimal paymentPercent;

    @NotNull
    @Column(name = "public_tracking_allowed", nullable = false)
    private Boolean publicTrackingAllowed;

    @PrePersist
    protected void setCreationTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void setUpdateTimestamp() {
        updatedAt = Instant.now();
    }

    @JsonIgnoreProperties(value = { "order" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private OrderIssue issue;

    @JsonIgnoreProperties(value = { "order" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private OrderReturnRequest returnRequest;

    @JsonIgnoreProperties(value = { "order" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private OrderFareAdjustmentRequest fareAdjustmentRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer senderCustomer;

    @ManyToOne(optional = false)
    @NotNull
    private Office fromOffice;

    @ManyToOne(optional = false)
    @NotNull
    private Office toOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    private Office hubOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    private Office finalToOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "office", "route", "vehicle", "driver" }, allowSetters = true)
    private Trip currentTrip;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public ShipmentOrder id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getOrderCode() {
        return this.orderCode;
    }

    public ShipmentOrder orderCode(String orderCode) {
        this.setOrderCode(orderCode);
        return this;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getDraftCode() {
        return this.draftCode;
    }

    public ShipmentOrder draftCode(String draftCode) {
        this.setDraftCode(draftCode);
        return this;
    }

    public void setDraftCode(String draftCode) {
        this.draftCode = draftCode;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public ShipmentOrder status(OrderStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public ForwardStage getForwardStage() {
        return this.forwardStage;
    }

    public ShipmentOrder forwardStage(ForwardStage forwardStage) {
        this.setForwardStage(forwardStage);
        return this;
    }

    public void setForwardStage(ForwardStage forwardStage) {
        this.forwardStage = forwardStage;
    }

    public ReturnStage getReturnStage() {
        return this.returnStage;
    }

    public ShipmentOrder returnStage(ReturnStage returnStage) {
        this.setReturnStage(returnStage);
        return this;
    }

    public void setReturnStage(ReturnStage returnStage) {
        this.returnStage = returnStage;
    }

    public PaymentTerm getPaymentTerm() {
        return this.paymentTerm;
    }

    public ShipmentOrder paymentTerm(PaymentTerm paymentTerm) {
        this.setPaymentTerm(paymentTerm);
        return this;
    }

    public void setPaymentTerm(PaymentTerm paymentTerm) {
        this.paymentTerm = paymentTerm;
    }

    public GoodsType getGoodsType() {
        return this.goodsType;
    }

    public ShipmentOrder goodsType(GoodsType goodsType) {
        this.setGoodsType(goodsType);
        return this;
    }

    public void setGoodsType(GoodsType goodsType) {
        this.goodsType = goodsType;
    }

    public ServiceType getServiceType() {
        return this.serviceType;
    }

    public ShipmentOrder serviceType(ServiceType serviceType) {
        this.setServiceType(serviceType);
        return this;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getSenderName() {
        return this.senderName;
    }

    public ShipmentOrder senderName(String senderName) {
        this.setSenderName(senderName);
        return this;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return this.senderPhone;
    }

    public ShipmentOrder senderPhone(String senderPhone) {
        this.setSenderPhone(senderPhone);
        return this;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getReceiverName() {
        return this.receiverName;
    }

    public ShipmentOrder receiverName(String receiverName) {
        this.setReceiverName(receiverName);
        return this;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return this.receiverPhone;
    }

    public ShipmentOrder receiverPhone(String receiverPhone) {
        this.setReceiverPhone(receiverPhone);
        return this;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getDeliveryAddress() {
        return this.deliveryAddress;
    }

    public ShipmentOrder deliveryAddress(String deliveryAddress) {
        this.setDeliveryAddress(deliveryAddress);
        return this;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPickupAddress() {
        return this.pickupAddress;
    }

    public ShipmentOrder pickupAddress(String pickupAddress) {
        this.setPickupAddress(pickupAddress);
        return this;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Boolean getHomePickup() {
        return this.homePickup;
    }

    public ShipmentOrder homePickup(Boolean homePickup) {
        this.setHomePickup(homePickup);
        return this;
    }

    public void setHomePickup(Boolean homePickup) {
        this.homePickup = homePickup;
    }

    public Boolean getHomeDelivery() {
        return this.homeDelivery;
    }

    public ShipmentOrder homeDelivery(Boolean homeDelivery) {
        this.setHomeDelivery(homeDelivery);
        return this;
    }

    public void setHomeDelivery(Boolean homeDelivery) {
        this.homeDelivery = homeDelivery;
    }

    public Boolean getQrDropOff() {
        return this.qrDropOff;
    }

    public ShipmentOrder qrDropOff(Boolean qrDropOff) {
        this.setQrDropOff(qrDropOff);
        return this;
    }

    public void setQrDropOff(Boolean qrDropOff) {
        this.qrDropOff = qrDropOff;
    }

    public String getPickupStaffUsername() {
        return this.pickupStaffUsername;
    }

    public ShipmentOrder pickupStaffUsername(String pickupStaffUsername) {
        this.setPickupStaffUsername(pickupStaffUsername);
        return this;
    }

    public void setPickupStaffUsername(String pickupStaffUsername) {
        this.pickupStaffUsername = pickupStaffUsername;
    }

    public Instant getPickingAt() {
        return this.pickingAt;
    }

    public ShipmentOrder pickingAt(Instant pickingAt) {
        this.setPickingAt(pickingAt);
        return this;
    }

    public void setPickingAt(Instant pickingAt) {
        this.pickingAt = pickingAt;
    }

    public Instant getPickedUpAt() {
        return this.pickedUpAt;
    }

    public ShipmentOrder pickedUpAt(Instant pickedUpAt) {
        this.setPickedUpAt(pickedUpAt);
        return this;
    }

    public void setPickedUpAt(Instant pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public String getReceiverActualName() {
        return this.receiverActualName;
    }

    public ShipmentOrder receiverActualName(String receiverActualName) {
        this.setReceiverActualName(receiverActualName);
        return this;
    }

    public void setReceiverActualName(String receiverActualName) {
        this.receiverActualName = receiverActualName;
    }

    public String getReceiverActualPhone() {
        return this.receiverActualPhone;
    }

    public ShipmentOrder receiverActualPhone(String receiverActualPhone) {
        this.setReceiverActualPhone(receiverActualPhone);
        return this;
    }

    public void setReceiverActualPhone(String receiverActualPhone) {
        this.receiverActualPhone = receiverActualPhone;
    }

    public BigDecimal getWeightKg() {
        return this.weightKg;
    }

    public ShipmentOrder weightKg(BigDecimal weightKg) {
        this.setWeightKg(weightKg);
        return this;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public ShipmentOrder quantity(Integer quantity) {
        this.setQuantity(quantity);
        return this;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDimensionsText() {
        return this.dimensionsText;
    }

    public ShipmentOrder dimensionsText(String dimensionsText) {
        this.setDimensionsText(dimensionsText);
        return this;
    }

    public void setDimensionsText(String dimensionsText) {
        this.dimensionsText = dimensionsText;
    }

    public BigDecimal getFareAmount() {
        return this.fareAmount;
    }

    public ShipmentOrder fareAmount(BigDecimal fareAmount) {
        this.setFareAmount(fareAmount);
        return this;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }

    public BigDecimal getPickupFeeAmount() {
        return this.pickupFeeAmount;
    }

    public ShipmentOrder pickupFeeAmount(BigDecimal pickupFeeAmount) {
        this.setPickupFeeAmount(pickupFeeAmount);
        return this;
    }

    public void setPickupFeeAmount(BigDecimal pickupFeeAmount) {
        this.pickupFeeAmount = pickupFeeAmount;
    }

    public BigDecimal getDeliveryFeeAmount() {
        return this.deliveryFeeAmount;
    }

    public ShipmentOrder deliveryFeeAmount(BigDecimal deliveryFeeAmount) {
        this.setDeliveryFeeAmount(deliveryFeeAmount);
        return this;
    }

    public void setDeliveryFeeAmount(BigDecimal deliveryFeeAmount) {
        this.deliveryFeeAmount = deliveryFeeAmount;
    }

    public BigDecimal getPartnerFeeAmount() {
        return this.partnerFeeAmount;
    }

    public ShipmentOrder partnerFeeAmount(BigDecimal partnerFeeAmount) {
        this.setPartnerFeeAmount(partnerFeeAmount);
        return this;
    }

    public void setPartnerFeeAmount(BigDecimal partnerFeeAmount) {
        this.partnerFeeAmount = partnerFeeAmount;
    }

    public BigDecimal getPaidAmount() {
        return this.paidAmount;
    }

    public ShipmentOrder paidAmount(BigDecimal paidAmount) {
        this.setPaidAmount(paidAmount);
        return this;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Integer getShelfNumber() {
        return this.shelfNumber;
    }

    public ShipmentOrder shelfNumber(Integer shelfNumber) {
        this.setShelfNumber(shelfNumber);
        return this;
    }

    public void setShelfNumber(Integer shelfNumber) {
        this.shelfNumber = shelfNumber;
    }

    public String getNote() {
        return this.note;
    }

    public ShipmentOrder note(String note) {
        this.setNote(note);
        return this;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCancelReason() {
        return this.cancelReason;
    }

    public ShipmentOrder cancelReason(String cancelReason) {
        this.setCancelReason(cancelReason);
        return this;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Instant getLabelPrintedAt() {
        return this.labelPrintedAt;
    }

    public ShipmentOrder labelPrintedAt(Instant labelPrintedAt) {
        this.setLabelPrintedAt(labelPrintedAt);
        return this;
    }

    public void setLabelPrintedAt(Instant labelPrintedAt) {
        this.labelPrintedAt = labelPrintedAt;
    }

    public Integer getLabelReprintCount() {
        return this.labelReprintCount;
    }

    public ShipmentOrder labelReprintCount(Integer labelReprintCount) {
        this.setLabelReprintCount(labelReprintCount);
        return this;
    }

    public void setLabelReprintCount(Integer labelReprintCount) {
        this.labelReprintCount = labelReprintCount;
    }

    public Integer getFailCount() {
        return this.failCount;
    }

    public ShipmentOrder failCount(Integer failCount) {
        this.setFailCount(failCount);
        return this;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public String getPartnerCode() {
        return this.partnerCode;
    }

    public ShipmentOrder partnerCode(String partnerCode) {
        this.setPartnerCode(partnerCode);
        return this;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public BigDecimal getPaymentPercent() {
        return this.paymentPercent;
    }

    public ShipmentOrder paymentPercent(BigDecimal paymentPercent) {
        this.setPaymentPercent(paymentPercent);
        return this;
    }

    public void setPaymentPercent(BigDecimal paymentPercent) {
        this.paymentPercent = paymentPercent;
    }

    public Boolean getPublicTrackingAllowed() {
        return this.publicTrackingAllowed;
    }

    public ShipmentOrder publicTrackingAllowed(Boolean publicTrackingAllowed) {
        this.setPublicTrackingAllowed(publicTrackingAllowed);
        return this;
    }

    public void setPublicTrackingAllowed(Boolean publicTrackingAllowed) {
        this.publicTrackingAllowed = publicTrackingAllowed;
    }

    public OrderIssue getIssue() {
        return this.issue;
    }

    public void setIssue(OrderIssue orderIssue) {
        this.issue = orderIssue;
    }

    public ShipmentOrder issue(OrderIssue orderIssue) {
        this.setIssue(orderIssue);
        return this;
    }

    public OrderReturnRequest getReturnRequest() {
        return this.returnRequest;
    }

    public void setReturnRequest(OrderReturnRequest orderReturnRequest) {
        this.returnRequest = orderReturnRequest;
    }

    public ShipmentOrder returnRequest(OrderReturnRequest orderReturnRequest) {
        this.setReturnRequest(orderReturnRequest);
        return this;
    }

    public OrderFareAdjustmentRequest getFareAdjustmentRequest() {
        return this.fareAdjustmentRequest;
    }

    public void setFareAdjustmentRequest(OrderFareAdjustmentRequest orderFareAdjustmentRequest) {
        this.fareAdjustmentRequest = orderFareAdjustmentRequest;
    }

    public ShipmentOrder fareAdjustmentRequest(OrderFareAdjustmentRequest orderFareAdjustmentRequest) {
        this.setFareAdjustmentRequest(orderFareAdjustmentRequest);
        return this;
    }

    public Customer getSenderCustomer() {
        return this.senderCustomer;
    }

    public void setSenderCustomer(Customer customer) {
        this.senderCustomer = customer;
    }

    public ShipmentOrder senderCustomer(Customer customer) {
        this.setSenderCustomer(customer);
        return this;
    }

    public Office getFromOffice() {
        return this.fromOffice;
    }

    public void setFromOffice(Office office) {
        this.fromOffice = office;
    }

    public ShipmentOrder fromOffice(Office office) {
        this.setFromOffice(office);
        return this;
    }

    public Office getToOffice() {
        return this.toOffice;
    }

    public void setToOffice(Office office) {
        this.toOffice = office;
    }

    public ShipmentOrder toOffice(Office office) {
        this.setToOffice(office);
        return this;
    }

    public Office getHubOffice() {
        return this.hubOffice;
    }

    public void setHubOffice(Office office) {
        this.hubOffice = office;
    }

    public ShipmentOrder hubOffice(Office office) {
        this.setHubOffice(office);
        return this;
    }

    public Office getFinalToOffice() {
        return this.finalToOffice;
    }

    public void setFinalToOffice(Office office) {
        this.finalToOffice = office;
    }

    public ShipmentOrder finalToOffice(Office office) {
        this.setFinalToOffice(office);
        return this;
    }

    public Trip getCurrentTrip() {
        return this.currentTrip;
    }

    public void setCurrentTrip(Trip trip) {
        this.currentTrip = trip;
    }

    public ShipmentOrder currentTrip(Trip trip) {
        this.setCurrentTrip(trip);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShipmentOrder)) {
            return false;
        }
        return getId() != null && getId().equals(((ShipmentOrder) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShipmentOrder{" +
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
            "}";
    }
}
