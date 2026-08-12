package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A OrderItem.
 */
@Entity
@Table(name = "order_item")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Min(value = 1)
    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @NotNull
    @Size(max = 150)
    @Column(name = "item_name", length = 150, nullable = false)
    private String itemName;

    @NotNull
    @Min(value = 1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @DecimalMin(value = "0")
    @Column(name = "weight_kg", precision = 21, scale = 2)
    private BigDecimal weightKg;

    @DecimalMin(value = "0")
    @Column(name = "length_cm", precision = 21, scale = 2)
    private BigDecimal lengthCm;

    @DecimalMin(value = "0")
    @Column(name = "width_cm", precision = 21, scale = 2)
    private BigDecimal widthCm;

    @DecimalMin(value = "0")
    @Column(name = "height_cm", precision = 21, scale = 2)
    private BigDecimal heightCm;

    @DecimalMin(value = "0")
    @Column(name = "declared_value_amount", precision = 21, scale = 2)
    private BigDecimal declaredValueAmount;

    @DecimalMin(value = "0")
    @Column(name = "fare_amount", precision = 21, scale = 2)
    private BigDecimal fareAmount;

    @Size(max = 255)
    @Column(name = "note", length = 255)
    private String note;

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

    public OrderItem id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLineNo() {
        return this.lineNo;
    }

    public OrderItem lineNo(Integer lineNo) {
        this.setLineNo(lineNo);
        return this;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getItemName() {
        return this.itemName;
    }

    public OrderItem itemName(String itemName) {
        this.setItemName(itemName);
        return this;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public OrderItem quantity(Integer quantity) {
        this.setQuantity(quantity);
        return this;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getWeightKg() {
        return this.weightKg;
    }

    public OrderItem weightKg(BigDecimal weightKg) {
        this.setWeightKg(weightKg);
        return this;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public BigDecimal getLengthCm() {
        return this.lengthCm;
    }

    public OrderItem lengthCm(BigDecimal lengthCm) {
        this.setLengthCm(lengthCm);
        return this;
    }

    public void setLengthCm(BigDecimal lengthCm) {
        this.lengthCm = lengthCm;
    }

    public BigDecimal getWidthCm() {
        return this.widthCm;
    }

    public OrderItem widthCm(BigDecimal widthCm) {
        this.setWidthCm(widthCm);
        return this;
    }

    public void setWidthCm(BigDecimal widthCm) {
        this.widthCm = widthCm;
    }

    public BigDecimal getHeightCm() {
        return this.heightCm;
    }

    public OrderItem heightCm(BigDecimal heightCm) {
        this.setHeightCm(heightCm);
        return this;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getDeclaredValueAmount() {
        return this.declaredValueAmount;
    }

    public OrderItem declaredValueAmount(BigDecimal declaredValueAmount) {
        this.setDeclaredValueAmount(declaredValueAmount);
        return this;
    }

    public void setDeclaredValueAmount(BigDecimal declaredValueAmount) {
        this.declaredValueAmount = declaredValueAmount;
    }

    public BigDecimal getFareAmount() {
        return this.fareAmount;
    }

    public OrderItem fareAmount(BigDecimal fareAmount) {
        this.setFareAmount(fareAmount);
        return this;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }

    public String getNote() {
        return this.note;
    }

    public OrderItem note(String note) {
        this.setNote(note);
        return this;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public OrderItem order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderItem)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderItem) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderItem{" +
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
            "}";
    }
}
