package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class CreateDraftOrderResponse {

    private String draftCode;
    private String orderCode;
    private OrderStatus status;
    private BigDecimal fareAmount;
    private Instant expiresAt;

    public CreateDraftOrderResponse() {}

    public CreateDraftOrderResponse(String draftCode, String orderCode, OrderStatus status, BigDecimal fareAmount, Instant expiresAt) {
        this.draftCode = draftCode;
        this.orderCode = orderCode;
        this.status = status;
        this.fareAmount = fareAmount;
        this.expiresAt = expiresAt;
    }

    public String getDraftCode() {
        return draftCode;
    }

    public void setDraftCode(String draftCode) {
        this.draftCode = draftCode;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
