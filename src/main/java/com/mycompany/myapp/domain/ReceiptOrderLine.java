package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A ReceiptOrderLine.
 */
@Entity
@Table(name = "receipt_order_line")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReceiptOrderLine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "amount_collected", precision = 21, scale = 2, nullable = false)
    private BigDecimal amountCollected;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "office" }, allowSetters = true)
    private Receipt receipt;

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

    public ReceiptOrderLine id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmountCollected() {
        return this.amountCollected;
    }

    public ReceiptOrderLine amountCollected(BigDecimal amountCollected) {
        this.setAmountCollected(amountCollected);
        return this;
    }

    public void setAmountCollected(BigDecimal amountCollected) {
        this.amountCollected = amountCollected;
    }

    public Receipt getReceipt() {
        return this.receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public ReceiptOrderLine receipt(Receipt receipt) {
        this.setReceipt(receipt);
        return this;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public ReceiptOrderLine order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReceiptOrderLine)) {
            return false;
        }
        return getId() != null && getId().equals(((ReceiptOrderLine) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReceiptOrderLine{" +
            "id=" + getId() +
            ", amountCollected=" + getAmountCollected() +
            "}";
    }
}
