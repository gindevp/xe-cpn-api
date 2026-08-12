package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.DoorFeeKind;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.DoorFeeRule} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DoorFeeRuleDTO implements Serializable {

    private Long id;

    @NotNull
    private DoorFeeKind kind;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal minKg;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal maxKg;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal minKm;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal maxKm;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal feeAmount;

    @NotNull
    private Boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DoorFeeKind getKind() {
        return kind;
    }

    public void setKind(DoorFeeKind kind) {
        this.kind = kind;
    }

    public BigDecimal getMinKg() {
        return minKg;
    }

    public void setMinKg(BigDecimal minKg) {
        this.minKg = minKg;
    }

    public BigDecimal getMaxKg() {
        return maxKg;
    }

    public void setMaxKg(BigDecimal maxKg) {
        this.maxKg = maxKg;
    }

    public BigDecimal getMinKm() {
        return minKm;
    }

    public void setMinKm(BigDecimal minKm) {
        this.minKm = minKm;
    }

    public BigDecimal getMaxKm() {
        return maxKm;
    }

    public void setMaxKm(BigDecimal maxKm) {
        this.maxKm = maxKm;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoorFeeRuleDTO)) {
            return false;
        }

        DoorFeeRuleDTO doorFeeRuleDTO = (DoorFeeRuleDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, doorFeeRuleDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DoorFeeRuleDTO{" +
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
