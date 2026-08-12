package com.mycompany.myapp.service.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FailDeliveryRequest {

    @NotBlank
    @Size(max = 255)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
