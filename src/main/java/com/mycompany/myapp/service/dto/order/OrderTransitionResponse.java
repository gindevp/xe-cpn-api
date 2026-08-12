package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;

public class OrderTransitionResponse {

    private boolean ok;
    private OrderStatus status;
    private String orderCode;

    public OrderTransitionResponse() {}

    public OrderTransitionResponse(boolean ok, OrderStatus status, String orderCode) {
        this.ok = ok;
        this.status = status;
        this.orderCode = orderCode;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }
}
