package com.mycompany.myapp.service.dto.trip;

import jakarta.validation.constraints.NotBlank;

public class ScanOutRequest {

    @NotBlank
    private String orderCode;

    /** ADD (default) or REMOVE */
    private String mode = "ADD";

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
