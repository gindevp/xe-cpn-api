package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderPayment} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderPaymentDTO implements Serializable {

    private Long id;

    @NotNull
    private Instant paymentAt;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private PaymentMethod method;

    @NotNull
    private PaymentKind paymentKind;

    @Size(max = 255)
    private String note;

    @NotNull
    @Size(max = 50)
    private String collectorUsername;

    @NotNull
    private ShipmentOrderDTO order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getPaymentAt() {
        return paymentAt;
    }

    public void setPaymentAt(Instant paymentAt) {
        this.paymentAt = paymentAt;
    }

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

    public String getCollectorUsername() {
        return collectorUsername;
    }

    public void setCollectorUsername(String collectorUsername) {
        this.collectorUsername = collectorUsername;
    }

    public ShipmentOrderDTO getOrder() {
        return order;
    }

    public void setOrder(ShipmentOrderDTO order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderPaymentDTO)) {
            return false;
        }

        OrderPaymentDTO orderPaymentDTO = (OrderPaymentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderPaymentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderPaymentDTO{" +
            "id=" + getId() +
            ", paymentAt='" + getPaymentAt() + "'" +
            ", amount=" + getAmount() +
            ", method='" + getMethod() + "'" +
            ", paymentKind='" + getPaymentKind() + "'" +
            ", note='" + getNote() + "'" +
            ", collectorUsername='" + getCollectorUsername() + "'" +
            ", order=" + getOrder() +
            "}";
    }
}
