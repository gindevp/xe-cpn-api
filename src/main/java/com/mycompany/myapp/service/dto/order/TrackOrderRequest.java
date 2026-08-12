package com.mycompany.myapp.service.dto.order;

import jakarta.validation.constraints.NotBlank;

public class TrackOrderRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String phone;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
