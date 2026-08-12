package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OrderTransitionRequest {

    @NotNull
    private OrderStatus toStatus;

    @Size(max = 100)
    private String action;

    @Size(max = 255)
    private String detail;

    public OrderStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(OrderStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
