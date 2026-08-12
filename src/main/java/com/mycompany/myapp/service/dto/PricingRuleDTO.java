package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.PricingRule} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PricingRuleDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 40)
    private String ruleCode;

    @NotNull
    @Size(max = 50)
    private String tierLabel;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal minKg;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal maxKg;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal surchargeAmount;

    @Min(value = 1)
    private Integer dimDivisor;

    @DecimalMin(value = "0")
    private BigDecimal kmMin;

    @DecimalMin(value = "0")
    private BigDecimal kmRate;

    @Min(value = 0)
    private Integer stepGram;

    @DecimalMin(value = "0")
    private BigDecimal addFeeAmount;

    @NotNull
    private Instant effectiveFrom;

    private Instant effectiveTo;

    @NotNull
    private Boolean active;

    @NotNull
    private RouteDTO route;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getTierLabel() {
        return tierLabel;
    }

    public void setTierLabel(String tierLabel) {
        this.tierLabel = tierLabel;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSurchargeAmount() {
        return surchargeAmount;
    }

    public void setSurchargeAmount(BigDecimal surchargeAmount) {
        this.surchargeAmount = surchargeAmount;
    }

    public Integer getDimDivisor() {
        return dimDivisor;
    }

    public void setDimDivisor(Integer dimDivisor) {
        this.dimDivisor = dimDivisor;
    }

    public BigDecimal getKmMin() {
        return kmMin;
    }

    public void setKmMin(BigDecimal kmMin) {
        this.kmMin = kmMin;
    }

    public BigDecimal getKmRate() {
        return kmRate;
    }

    public void setKmRate(BigDecimal kmRate) {
        this.kmRate = kmRate;
    }

    public Integer getStepGram() {
        return stepGram;
    }

    public void setStepGram(Integer stepGram) {
        this.stepGram = stepGram;
    }

    public BigDecimal getAddFeeAmount() {
        return addFeeAmount;
    }

    public void setAddFeeAmount(BigDecimal addFeeAmount) {
        this.addFeeAmount = addFeeAmount;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public RouteDTO getRoute() {
        return route;
    }

    public void setRoute(RouteDTO route) {
        this.route = route;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PricingRuleDTO)) {
            return false;
        }

        PricingRuleDTO pricingRuleDTO = (PricingRuleDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, pricingRuleDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PricingRuleDTO{" +
            "id=" + getId() +
            ", ruleCode='" + getRuleCode() + "'" +
            ", tierLabel='" + getTierLabel() + "'" +
            ", minKg=" + getMinKg() +
            ", maxKg=" + getMaxKg() +
            ", unitPrice=" + getUnitPrice() +
            ", surchargeAmount=" + getSurchargeAmount() +
            ", dimDivisor=" + getDimDivisor() +
            ", kmMin=" + getKmMin() +
            ", kmRate=" + getKmRate() +
            ", stepGram=" + getStepGram() +
            ", addFeeAmount=" + getAddFeeAmount() +
            ", effectiveFrom='" + getEffectiveFrom() + "'" +
            ", effectiveTo='" + getEffectiveTo() + "'" +
            ", active='" + getActive() + "'" +
            ", route=" + getRoute() +
            "}";
    }
}
