package com.mycompany.myapp.service.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LogOrderEventRequest {

    @NotBlank
    @Size(max = 100)
    private String action;

    @Size(max = 255)
    private String detail;

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
