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
    private Instant createdAt;
    private Instant updatedAt;
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
    private String pickupAddress;
    private String deliveryAddress;
    private Instant pickingAt;
    private Instant pickedUpAt;
    private String pickupStaffUsername;
    private String partnerCode;
    private BigDecimal partnerFeeAmount;
    private String partnerOrderId;
    private String partnerStatus;
    private String partnerTrackingUrl;
    private String partnerDriverName;
    private String partnerDriverPhone;
    private String partnerPodUrl;
    private String partnerFailReason;
    private Instant partnerUpdatedAt;
    private BigDecimal partnerCodAmount;
    private Instant partnerCodCollectedAt;
    private String partnerCodCollectedBy;
    private Long shipperId;
    private String shipperName;
    private String shipperPhone;
    private Integer currentLegIndex;
    private List<OrderLegViewDTO> legs = new ArrayList<>();
    /** Ảnh POD (data-URL / URL). */
    private List<String> podPhotos = new ArrayList<>();

    /** Cùng thứ tự podPhotos. Nhận / Giao, hoặc rỗng nếu không gắn nhãn. */
    private List<String> podPhotoCaptions = new ArrayList<>();
    /** Khách gửi ảnh đơn hàng khi tạo đơn — ảnh lấy qua GET /api/orders/{code}/goods-photo. */
    private boolean hasGoodsPhoto;
    /** Người thực nhận — gắn khi DELIVERED. */
    private String receiverActualName;
    private String receiverActualPhone;

    private BigDecimal codAmount;
    private BigDecimal codFeeAmount;
    /** Thành phần của fareAmount (đơn cũ = null) — fareAmount vẫn là tổng phải thu. */
    private BigDecimal goodsFareAmount;
    private BigDecimal declaredFeeAmount;
    private BigDecimal discountAmount;
    private String bankName;
    private String bankAccountNo;
    private String bankAccountName;
    private Boolean invoiceRequested;
    private Boolean onCredit;
    private String invoiceTaxCode;
    private String invoiceCompanyName;
    private String invoiceEmail;
    private String invoiceCompanyAddress;
    private String invoiceBuyerName;

    private String invoiceBuyerIdNumber;

    private String invoiceBuyerPhone;
    private String invoiceRefId;
    private String invoiceStatus;
    private String invoiceType;
    private String invoiceTransactionId;
    private String invoiceNo;
    private String invoiceSeries;
    private String invoiceCode;
    private BigDecimal invoiceGrossAmount;
    private BigDecimal invoiceNetAmount;
    private BigDecimal invoiceVatAmount;
    private Instant invoiceIssuedAt;
    private String invoiceError;
    private String routeLabel;
    private String itineraryLabel;
    private Instant codExportedAt;
    private String codExportedBy;
    private String codExportedByName;
    /** Tài khoản tạo đơn (sự kiện CREATE); "customer" = khách tự tạo. */
    private String createdBy;
    private String createdByName;
    /** Mã vai trò nhân viên tạo đơn (Q, DH, …); null với khách. */
    private String createdByRole;
    private String vehiclePlate;
    private String driverName;
    /** Giờ xuất phát chuyến hiện tại (trip.departAt) — FE tab Hàng trên xe. */
    private Instant departAt;
    /** Mốc thời gian theo tab nhập kho/luân chuyển (sự kiện mới nhất của từng bước). */
    private Instant warehouseInAt;
    private Instant tripAssignedAt;
    private Instant driverSignedAt;
    private Instant destWarehouseInAt;
    private Instant shipperAssignedAt;
    /** Vụ việc OPEN hiện tại (list) — FE /ngoai-le. */
    private String issueType;
    private String issueReason;
    private Instant issueOpenedAt;
    private String issueOpenedBy;

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

    public String getPartnerOrderId() {
        return partnerOrderId;
    }

    public void setPartnerOrderId(String partnerOrderId) {
        this.partnerOrderId = partnerOrderId;
    }

    public String getPartnerStatus() {
        return partnerStatus;
    }

    public void setPartnerStatus(String partnerStatus) {
        this.partnerStatus = partnerStatus;
    }

    public String getPartnerTrackingUrl() {
        return partnerTrackingUrl;
    }

    public void setPartnerTrackingUrl(String partnerTrackingUrl) {
        this.partnerTrackingUrl = partnerTrackingUrl;
    }

    public String getPartnerDriverName() {
        return partnerDriverName;
    }

    public void setPartnerDriverName(String partnerDriverName) {
        this.partnerDriverName = partnerDriverName;
    }

    public String getPartnerDriverPhone() {
        return partnerDriverPhone;
    }

    public void setPartnerDriverPhone(String partnerDriverPhone) {
        this.partnerDriverPhone = partnerDriverPhone;
    }

    public String getPartnerPodUrl() {
        return partnerPodUrl;
    }

    public void setPartnerPodUrl(String partnerPodUrl) {
        this.partnerPodUrl = partnerPodUrl;
    }

    public String getPartnerFailReason() {
        return partnerFailReason;
    }

    public void setPartnerFailReason(String partnerFailReason) {
        this.partnerFailReason = partnerFailReason;
    }

    public Instant getPartnerUpdatedAt() {
        return partnerUpdatedAt;
    }

    public void setPartnerUpdatedAt(Instant partnerUpdatedAt) {
        this.partnerUpdatedAt = partnerUpdatedAt;
    }

    public BigDecimal getPartnerCodAmount() {
        return partnerCodAmount;
    }

    public void setPartnerCodAmount(BigDecimal partnerCodAmount) {
        this.partnerCodAmount = partnerCodAmount;
    }

    public Instant getPartnerCodCollectedAt() {
        return partnerCodCollectedAt;
    }

    public void setPartnerCodCollectedAt(Instant partnerCodCollectedAt) {
        this.partnerCodCollectedAt = partnerCodCollectedAt;
    }

    public String getPartnerCodCollectedBy() {
        return partnerCodCollectedBy;
    }

    public void setPartnerCodCollectedBy(String partnerCodCollectedBy) {
        this.partnerCodCollectedBy = partnerCodCollectedBy;
    }

    public Long getShipperId() {
        return shipperId;
    }

    public void setShipperId(Long shipperId) {
        this.shipperId = shipperId;
    }

    public String getShipperName() {
        return shipperName;
    }

    public void setShipperName(String shipperName) {
        this.shipperName = shipperName;
    }

    public String getShipperPhone() {
        return shipperPhone;
    }

    public void setShipperPhone(String shipperPhone) {
        this.shipperPhone = shipperPhone;
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

    public boolean isHasGoodsPhoto() {
        return hasGoodsPhoto;
    }

    public void setHasGoodsPhoto(boolean hasGoodsPhoto) {
        this.hasGoodsPhoto = hasGoodsPhoto;
    }

    public List<String> getPodPhotos() {
        return podPhotos;
    }

    public void setPodPhotos(List<String> podPhotos) {
        this.podPhotos = podPhotos;
    }

    public List<String> getPodPhotoCaptions() {
        return podPhotoCaptions;
    }

    public void setPodPhotoCaptions(List<String> podPhotoCaptions) {
        this.podPhotoCaptions = podPhotoCaptions;
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

    public BigDecimal getGoodsFareAmount() {
        return goodsFareAmount;
    }

    public void setGoodsFareAmount(BigDecimal goodsFareAmount) {
        this.goodsFareAmount = goodsFareAmount;
    }

    public BigDecimal getDeclaredFeeAmount() {
        return declaredFeeAmount;
    }

    public void setDeclaredFeeAmount(BigDecimal declaredFeeAmount) {
        this.declaredFeeAmount = declaredFeeAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
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

    public Boolean getInvoiceRequested() {
        return invoiceRequested;
    }

    public Boolean getOnCredit() {
        return onCredit;
    }

    public void setOnCredit(Boolean onCredit) {
        this.onCredit = onCredit;
    }

    public void setInvoiceRequested(Boolean invoiceRequested) {
        this.invoiceRequested = invoiceRequested;
    }

    public String getInvoiceTaxCode() {
        return invoiceTaxCode;
    }

    public void setInvoiceTaxCode(String invoiceTaxCode) {
        this.invoiceTaxCode = invoiceTaxCode;
    }

    public String getInvoiceCompanyName() {
        return invoiceCompanyName;
    }

    public void setInvoiceCompanyName(String invoiceCompanyName) {
        this.invoiceCompanyName = invoiceCompanyName;
    }

    public String getInvoiceEmail() {
        return invoiceEmail;
    }

    public void setInvoiceEmail(String invoiceEmail) {
        this.invoiceEmail = invoiceEmail;
    }

    public String getInvoiceCompanyAddress() {
        return invoiceCompanyAddress;
    }

    public void setInvoiceCompanyAddress(String invoiceCompanyAddress) {
        this.invoiceCompanyAddress = invoiceCompanyAddress;
    }

    public String getInvoiceBuyerName() {
        return invoiceBuyerName;
    }

    public void setInvoiceBuyerName(String invoiceBuyerName) {
        this.invoiceBuyerName = invoiceBuyerName;
    }

    public String getInvoiceBuyerIdNumber() {
        return invoiceBuyerIdNumber;
    }

    public void setInvoiceBuyerIdNumber(String invoiceBuyerIdNumber) {
        this.invoiceBuyerIdNumber = invoiceBuyerIdNumber;
    }

    public String getInvoiceBuyerPhone() {
        return invoiceBuyerPhone;
    }

    public void setInvoiceBuyerPhone(String invoiceBuyerPhone) {
        this.invoiceBuyerPhone = invoiceBuyerPhone;
    }

    public String getInvoiceRefId() {
        return invoiceRefId;
    }

    public void setInvoiceRefId(String invoiceRefId) {
        this.invoiceRefId = invoiceRefId;
    }

    public String getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(String invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getInvoiceTransactionId() {
        return invoiceTransactionId;
    }

    public void setInvoiceTransactionId(String invoiceTransactionId) {
        this.invoiceTransactionId = invoiceTransactionId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getInvoiceSeries() {
        return invoiceSeries;
    }

    public void setInvoiceSeries(String invoiceSeries) {
        this.invoiceSeries = invoiceSeries;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public BigDecimal getInvoiceGrossAmount() {
        return invoiceGrossAmount;
    }

    public void setInvoiceGrossAmount(BigDecimal invoiceGrossAmount) {
        this.invoiceGrossAmount = invoiceGrossAmount;
    }

    public BigDecimal getInvoiceNetAmount() {
        return invoiceNetAmount;
    }

    public void setInvoiceNetAmount(BigDecimal invoiceNetAmount) {
        this.invoiceNetAmount = invoiceNetAmount;
    }

    public BigDecimal getInvoiceVatAmount() {
        return invoiceVatAmount;
    }

    public void setInvoiceVatAmount(BigDecimal invoiceVatAmount) {
        this.invoiceVatAmount = invoiceVatAmount;
    }

    public Instant getInvoiceIssuedAt() {
        return invoiceIssuedAt;
    }

    public void setInvoiceIssuedAt(Instant invoiceIssuedAt) {
        this.invoiceIssuedAt = invoiceIssuedAt;
    }

    public String getInvoiceError() {
        return invoiceError;
    }

    public void setInvoiceError(String invoiceError) {
        this.invoiceError = invoiceError;
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

    public Instant getCodExportedAt() {
        return codExportedAt;
    }

    public void setCodExportedAt(Instant codExportedAt) {
        this.codExportedAt = codExportedAt;
    }

    public String getCodExportedBy() {
        return codExportedBy;
    }

    public void setCodExportedBy(String codExportedBy) {
        this.codExportedBy = codExportedBy;
    }

    public String getCodExportedByName() {
        return codExportedByName;
    }

    public void setCodExportedByName(String codExportedByName) {
        this.codExportedByName = codExportedByName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByRole() {
        return createdByRole;
    }

    public void setCreatedByRole(String createdByRole) {
        this.createdByRole = createdByRole;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public Instant getDepartAt() {
        return departAt;
    }

    public void setDepartAt(Instant departAt) {
        this.departAt = departAt;
    }

    public Instant getWarehouseInAt() {
        return warehouseInAt;
    }

    public void setWarehouseInAt(Instant warehouseInAt) {
        this.warehouseInAt = warehouseInAt;
    }

    public Instant getTripAssignedAt() {
        return tripAssignedAt;
    }

    public void setTripAssignedAt(Instant tripAssignedAt) {
        this.tripAssignedAt = tripAssignedAt;
    }

    public Instant getDriverSignedAt() {
        return driverSignedAt;
    }

    public void setDriverSignedAt(Instant driverSignedAt) {
        this.driverSignedAt = driverSignedAt;
    }

    public Instant getDestWarehouseInAt() {
        return destWarehouseInAt;
    }

    public void setDestWarehouseInAt(Instant destWarehouseInAt) {
        this.destWarehouseInAt = destWarehouseInAt;
    }

    public Instant getShipperAssignedAt() {
        return shipperAssignedAt;
    }

    public void setShipperAssignedAt(Instant shipperAssignedAt) {
        this.shipperAssignedAt = shipperAssignedAt;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getIssueReason() {
        return issueReason;
    }

    public void setIssueReason(String issueReason) {
        this.issueReason = issueReason;
    }

    public Instant getIssueOpenedAt() {
        return issueOpenedAt;
    }

    public void setIssueOpenedAt(Instant issueOpenedAt) {
        this.issueOpenedAt = issueOpenedAt;
    }

    public String getIssueOpenedBy() {
        return issueOpenedBy;
    }

    public void setIssueOpenedBy(String issueOpenedBy) {
        this.issueOpenedBy = issueOpenedBy;
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
