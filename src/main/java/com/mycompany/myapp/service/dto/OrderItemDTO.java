package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderItem} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderItemDTO implements Serializable {

    private Long id;

    @NotNull
    @Min(value = 1)
    private Integer lineNo;

    @NotNull
    @Size(max = 150)
    private String itemName;

    @NotNull
    @Min(value = 1)
    private Integer quantity;

    @DecimalMin(value = "0")
    private BigDecimal weightKg;

    @DecimalMin(value = "0")
    private BigDecimal lengthCm;

    @DecimalMin(value = "0")
    private BigDecimal widthCm;

    @DecimalMin(value = "0")
    private BigDecimal heightCm;

    @DecimalMin(value = "0")
    private BigDecimal declaredValueAmount;

    @DecimalMin(value = "0")
    private BigDecimal fareAmount;

    @Size(max = 255)
    private String note;

    @NotNull
    private ShipmentOrderDTO order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public BigDecimal getLengthCm() {
        return lengthCm;
    }

    public void setLengthCm(BigDecimal lengthCm) {
        this.lengthCm = lengthCm;
    }

    public BigDecimal getWidthCm() {
        return widthCm;
    }

    public void setWidthCm(BigDecimal widthCm) {
        this.widthCm = widthCm;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getDeclaredValueAmount() {
        return declaredValueAmount;
    }

    public void setDeclaredValueAmount(BigDecimal declaredValueAmount) {
        this.declaredValueAmount = declaredValueAmount;
    }

    public BigDecimal getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
        if (!(o instanceof OrderItemDTO)) {
            return false;
        }

        OrderItemDTO orderItemDTO = (OrderItemDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderItemDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderItemDTO{" +
            "id=" + getId() +
            ", lineNo=" + getLineNo() +
            ", itemName='" + getItemName() + "'" +
            ", quantity=" + getQuantity() +
            ", weightKg=" + getWeightKg() +
            ", lengthCm=" + getLengthCm() +
            ", widthCm=" + getWidthCm() +
            ", heightCm=" + getHeightCm() +
            ", declaredValueAmount=" + getDeclaredValueAmount() +
            ", fareAmount=" + getFareAmount() +
            ", note='" + getNote() + "'" +
            ", order=" + getOrder() +
            "}";
    }
}
