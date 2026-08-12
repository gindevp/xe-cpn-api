package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PodResponse {

    private boolean ok;
    private String orderCode;
    private OrderStatus status;
    private BigDecimal fareAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private List<String> photoUrls = new ArrayList<>();

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

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }
}
