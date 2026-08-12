package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.ReceiptOrderLine} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReceiptOrderLineDTO implements Serializable {

    private Long id;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal amountCollected;

    @NotNull
    private ReceiptDTO receipt;

    @NotNull
    private ShipmentOrderDTO order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmountCollected() {
        return amountCollected;
    }

    public void setAmountCollected(BigDecimal amountCollected) {
        this.amountCollected = amountCollected;
    }

    public ReceiptDTO getReceipt() {
        return receipt;
    }

    public void setReceipt(ReceiptDTO receipt) {
        this.receipt = receipt;
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
        if (!(o instanceof ReceiptOrderLineDTO)) {
            return false;
        }

        ReceiptOrderLineDTO receiptOrderLineDTO = (ReceiptOrderLineDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, receiptOrderLineDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReceiptOrderLineDTO{" +
            "id=" + getId() +
            ", amountCollected=" + getAmountCollected() +
            ", receipt=" + getReceipt() +
            ", order=" + getOrder() +
            "}";
    }
}
