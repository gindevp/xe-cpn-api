package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class AddPaymentRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private PaymentMethod method;

    @NotNull
    private PaymentKind paymentKind = PaymentKind.SAU;

    @Size(max = 255)
    private String note;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentKind getPaymentKind() {
        return paymentKind;
    }

    public void setPaymentKind(PaymentKind paymentKind) {
        this.paymentKind = paymentKind;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
