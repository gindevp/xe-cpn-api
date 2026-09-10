package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TrackOrderResponse {

    private boolean found;
    private String orderCode;
    private String draftCode;
    private OrderStatus status;
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
    private List<OrderDetailDTO.OrderEventViewDTO> events = new ArrayList<>();

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
