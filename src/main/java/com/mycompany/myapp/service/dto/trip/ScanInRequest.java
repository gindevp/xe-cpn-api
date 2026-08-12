package com.mycompany.myapp.service.dto.trip;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ScanInRequest {

    @NotBlank
    private String orderCode;

    private String officeCode;

    private boolean overrideWrongOffice;

    @Min(0)
    @Max(9)
    private Integer shelfNumber;

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getOfficeCode() {
        return officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public boolean isOverrideWrongOffice() {
        return overrideWrongOffice;
    }

    public void setOverrideWrongOffice(boolean overrideWrongOffice) {
        this.overrideWrongOffice = overrideWrongOffice;
    }

    public Integer getShelfNumber() {
        return shelfNumber;
    }

    public void setShelfNumber(Integer shelfNumber) {
        this.shelfNumber = shelfNumber;
    }
}
