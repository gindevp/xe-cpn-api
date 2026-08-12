package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class AssignShipperRequest {

    /** INTERNAL (staff take-job) or partner enum name. */
    private String mode = "INTERNAL";

    private DeliveryPartner partner;

    private String partnerCode;

    @DecimalMin("0")
    private BigDecimal partnerFeeAmount;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public DeliveryPartner getPartner() {
        return partner;
    }

    public void setPartner(DeliveryPartner partner) {
        this.partner = partner;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public BigDecimal getPartnerFeeAmount() {
        return partnerFeeAmount;
    }

    public void setPartnerFeeAmount(BigDecimal partnerFeeAmount) {
        this.partnerFeeAmount = partnerFeeAmount;
    }
}
