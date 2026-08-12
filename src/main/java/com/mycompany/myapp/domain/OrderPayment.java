package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A OrderPayment.
 */
@Entity
@Table(name = "order_payment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "payment_at", nullable = false)
    private Instant paymentAt;

    @NotNull
    @Column(name = "amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private PaymentMethod method;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_kind", nullable = false)
    private PaymentKind paymentKind;

    @Size(max = 255)
    @Column(name = "note", length = 255)
    private String note;

    @NotNull
    @Size(max = 50)
    @Column(name = "collector_username", length = 50, nullable = false)
    private String collectorUsername;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = {
            "issue",
            "returnRequest",
            "fareAdjustmentRequest",
            "senderCustomer",
            "fromOffice",
            "toOffice",
            "hubOffice",
            "finalToOffice",
            "currentTrip",
        },
        allowSetters = true
    )
    private ShipmentOrder order;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public OrderPayment id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getPaymentAt() {
        return this.paymentAt;
    }

    public OrderPayment paymentAt(Instant paymentAt) {
        this.setPaymentAt(paymentAt);
        return this;
    }

    public void setPaymentAt(Instant paymentAt) {
        this.paymentAt = paymentAt;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public OrderPayment amount(BigDecimal amount) {
        this.setAmount(amount);
        return this;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return this.method;
    }

    public OrderPayment method(PaymentMethod method) {
        this.setMethod(method);
        return this;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentKind getPaymentKind() {
        return this.paymentKind;
    }

    public OrderPayment paymentKind(PaymentKind paymentKind) {
        this.setPaymentKind(paymentKind);
        return this;
    }

    public void setPaymentKind(PaymentKind paymentKind) {
        this.paymentKind = paymentKind;
    }

    public String getNote() {
        return this.note;
    }

    public OrderPayment note(String note) {
        this.setNote(note);
        return this;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCollectorUsername() {
        return this.collectorUsername;
    }

    public OrderPayment collectorUsername(String collectorUsername) {
        this.setCollectorUsername(collectorUsername);
        return this;
    }

    public void setCollectorUsername(String collectorUsername) {
        this.collectorUsername = collectorUsername;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public OrderPayment order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderPayment)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderPayment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderPayment{" +
            "id=" + getId() +
            ", paymentAt='" + getPaymentAt() + "'" +
            ", amount=" + getAmount() +
            ", method='" + getMethod() + "'" +
            ", paymentKind='" + getPaymentKind() + "'" +
            ", note='" + getNote() + "'" +
            ", collectorUsername='" + getCollectorUsername() + "'" +
            "}";
    }
}
