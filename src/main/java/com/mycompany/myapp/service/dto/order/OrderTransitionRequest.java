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

    /** DRAFT → CONFIRMED mới cấp mã thật: xác nhận khi VP đã vượt 1000 đơn/ngày. */
    private Boolean confirmDailyOverflow = false;

    public Boolean getConfirmDailyOverflow() {
        return confirmDailyOverflow;
    }

    public void setConfirmDailyOverflow(Boolean confirmDailyOverflow) {
        this.confirmDailyOverflow = confirmDailyOverflow;
    }

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
