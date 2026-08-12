package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;

public class FailDeliveryResponse {

    private boolean ok;
    private String orderCode;
    private OrderStatus status;
    private int failCount;
    private boolean returnedToBranch;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public boolean isReturnedToBranch() {
        return returnedToBranch;
    }

    public void setReturnedToBranch(boolean returnedToBranch) {
        this.returnedToBranch = returnedToBranch;
    }
}
