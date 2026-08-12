package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;
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

    public List<OrderDetailDTO.OrderEventViewDTO> getEvents() {
        return events;
    }

    public void setEvents(List<OrderDetailDTO.OrderEventViewDTO> events) {
        this.events = events;
    }
}
