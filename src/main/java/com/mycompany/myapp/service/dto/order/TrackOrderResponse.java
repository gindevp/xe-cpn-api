package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TrackOrderResponse {

    private boolean found;
    private String orderCode;
    private String draftCode;
    private OrderStatus status;
    /** Nhãn trạng thái = tên tab vận hành (Chờ lấy hàng / Nhập kho gửi / …). */
    private String statusLabel;
    private String fromOfficeCode;
    private String toOfficeCode;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private String goodsType;
    private String note;
    private Boolean homeDelivery;
    private Boolean homePickup;
    private BigDecimal fareAmount;
    private BigDecimal goodsFareAmount;
    private BigDecimal deliveryFeeAmount;
    private BigDecimal pickupFeeAmount;
    /** Chỉ action + giờ của các mốc công khai (không detail / người thao tác). */
    private List<OrderDetailDTO.OrderEventViewDTO> events = new ArrayList<>();
    private String fromOfficeName;
    private String toOfficeName;
    private String routeLabel;
    private String itineraryLabel;
    private List<JourneyStep> journey = new ArrayList<>();
    /** NONE / REQUESTED (đã lưu, chờ tự xuất) / ISSUED / OFFICE (VP xử lý). */
    private String invoiceState;
    private String invoiceNo;

    public String getInvoiceState() {
        return invoiceState;
    }

    public void setInvoiceState(String invoiceState) {
        this.invoiceState = invoiceState;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public static class JourneyStep {

        private String key;
        private String label;
        private Instant at;

        public JourneyStep() {}

        public JourneyStep(String key, String label, Instant at) {
            this.key = key;
            this.label = label;
            this.at = at;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Instant getAt() {
            return at;
        }

        public void setAt(Instant at) {
            this.at = at;
        }
    }

    public String getFromOfficeName() {
        return fromOfficeName;
    }

    public void setFromOfficeName(String fromOfficeName) {
        this.fromOfficeName = fromOfficeName;
    }

    public String getToOfficeName() {
        return toOfficeName;
    }

    public void setToOfficeName(String toOfficeName) {
        this.toOfficeName = toOfficeName;
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

    public List<JourneyStep> getJourney() {
        return journey;
    }

    public void setJourney(List<JourneyStep> journey) {
        this.journey = journey;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
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

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
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

    public String getGoodsType() {
        return goodsType;
    }

    public void setGoodsType(String goodsType) {
        this.goodsType = goodsType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public BigDecimal getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }

    public BigDecimal getGoodsFareAmount() {
        return goodsFareAmount;
    }

    public void setGoodsFareAmount(BigDecimal goodsFareAmount) {
        this.goodsFareAmount = goodsFareAmount;
    }

    public BigDecimal getDeliveryFeeAmount() {
        return deliveryFeeAmount;
    }

    public void setDeliveryFeeAmount(BigDecimal deliveryFeeAmount) {
        this.deliveryFeeAmount = deliveryFeeAmount;
    }

    public BigDecimal getPickupFeeAmount() {
        return pickupFeeAmount;
    }

    public void setPickupFeeAmount(BigDecimal pickupFeeAmount) {
        this.pickupFeeAmount = pickupFeeAmount;
    }

    public List<OrderDetailDTO.OrderEventViewDTO> getEvents() {
        return events;
    }

    public void setEvents(List<OrderDetailDTO.OrderEventViewDTO> events) {
        this.events = events;
    }
}
