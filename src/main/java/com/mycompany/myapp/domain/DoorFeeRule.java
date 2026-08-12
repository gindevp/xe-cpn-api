package com.mycompany.myapp.domain;

import com.mycompany.myapp.domain.enumeration.DoorFeeKind;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A DoorFeeRule.
 */
@Entity
@Table(name = "door_fee_rule")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DoorFeeRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private DoorFeeKind kind;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "min_kg", precision = 21, scale = 2, nullable = false)
    private BigDecimal minKg;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "max_kg", precision = 21, scale = 2, nullable = false)
    private BigDecimal maxKg;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "min_km", precision = 21, scale = 2, nullable = false)
    private BigDecimal minKm;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "max_km", precision = 21, scale = 2, nullable = false)
    private BigDecimal maxKm;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "fee_amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal feeAmount;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public DoorFeeRule id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DoorFeeKind getKind() {
        return this.kind;
    }

    public DoorFeeRule kind(DoorFeeKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(DoorFeeKind kind) {
        this.kind = kind;
    }

    public BigDecimal getMinKg() {
        return this.minKg;
    }

    public DoorFeeRule minKg(BigDecimal minKg) {
        this.setMinKg(minKg);
        return this;
    }

    public void setMinKg(BigDecimal minKg) {
        this.minKg = minKg;
    }

    public BigDecimal getMaxKg() {
        return this.maxKg;
    }

    public DoorFeeRule maxKg(BigDecimal maxKg) {
        this.setMaxKg(maxKg);
        return this;
    }

    public void setMaxKg(BigDecimal maxKg) {
        this.maxKg = maxKg;
    }

    public BigDecimal getMinKm() {
        return this.minKm;
    }

    public DoorFeeRule minKm(BigDecimal minKm) {
        this.setMinKm(minKm);
        return this;
    }

    public void setMinKm(BigDecimal minKm) {
        this.minKm = minKm;
    }

    public BigDecimal getMaxKm() {
        return this.maxKm;
    }

    public DoorFeeRule maxKm(BigDecimal maxKm) {
        this.setMaxKm(maxKm);
        return this;
    }

    public void setMaxKm(BigDecimal maxKm) {
        this.maxKm = maxKm;
    }

    public BigDecimal getFeeAmount() {
        return this.feeAmount;
    }

    public DoorFeeRule feeAmount(BigDecimal feeAmount) {
        this.setFeeAmount(feeAmount);
        return this;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public Boolean getActive() {
        return this.active;
    }

    public DoorFeeRule active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoorFeeRule)) {
            return false;
        }
        return getId() != null && getId().equals(((DoorFeeRule) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DoorFeeRule{" +
            "id=" + getId() +
            ", kind='" + getKind() + "'" +
            ", minKg=" + getMinKg() +
            ", maxKg=" + getMaxKg() +
            ", minKm=" + getMinKm() +
            ", maxKm=" + getMaxKm() +
            ", feeAmount=" + getFeeAmount() +
            ", active='" + getActive() + "'" +
            "}";
    }
}
