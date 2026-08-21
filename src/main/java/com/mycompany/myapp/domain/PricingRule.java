package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A PricingRule.
 */
@Entity
@Table(name = "pricing_rule")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PricingRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 40)
    @Column(name = "rule_code", length = 40, nullable = false, unique = true)
    private String ruleCode;

    @NotNull
    @Size(max = 50)
    @Column(name = "tier_label", length = 50, nullable = false)
    private String tierLabel;

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
    @Column(name = "unit_price", precision = 21, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "surcharge_amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal surchargeAmount;

    @Min(value = 1)
    @Column(name = "dim_divisor")
    private Integer dimDivisor;

    @DecimalMin(value = "0")
    @Column(name = "km_min", precision = 21, scale = 2)
    private BigDecimal kmMin;

    @DecimalMin(value = "0")
    @Column(name = "km_rate", precision = 21, scale = 2)
    private BigDecimal kmRate;

    @Min(value = 0)
    @Column(name = "step_gram")
    private Integer stepGram;

    @DecimalMin(value = "0")
    @Column(name = "add_fee_amount", precision = 21, scale = 2)
    private BigDecimal addFeeAmount;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @ManyToOne
    @JsonIgnoreProperties(value = { "fromOffice", "toOffice" }, allowSetters = true)
    private Route route;

    /** Master Tuyến — preferred match for create-order fare. */
    @ManyToOne
    @JsonIgnoreProperties(allowSetters = true)
    private Branch branch;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public PricingRule id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return this.ruleCode;
    }

    public PricingRule ruleCode(String ruleCode) {
        this.setRuleCode(ruleCode);
        return this;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getTierLabel() {
        return this.tierLabel;
    }

    public PricingRule tierLabel(String tierLabel) {
        this.setTierLabel(tierLabel);
        return this;
    }

    public void setTierLabel(String tierLabel) {
        this.tierLabel = tierLabel;
    }

    public BigDecimal getMinKg() {
        return this.minKg;
    }

    public PricingRule minKg(BigDecimal minKg) {
        this.setMinKg(minKg);
        return this;
    }

    public void setMinKg(BigDecimal minKg) {
        this.minKg = minKg;
    }

    public BigDecimal getMaxKg() {
        return this.maxKg;
    }

    public PricingRule maxKg(BigDecimal maxKg) {
        this.setMaxKg(maxKg);
        return this;
    }

    public void setMaxKg(BigDecimal maxKg) {
        this.maxKg = maxKg;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public PricingRule unitPrice(BigDecimal unitPrice) {
        this.setUnitPrice(unitPrice);
        return this;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSurchargeAmount() {
        return this.surchargeAmount;
    }

    public PricingRule surchargeAmount(BigDecimal surchargeAmount) {
        this.setSurchargeAmount(surchargeAmount);
        return this;
    }

    public void setSurchargeAmount(BigDecimal surchargeAmount) {
        this.surchargeAmount = surchargeAmount;
    }

    public Integer getDimDivisor() {
        return this.dimDivisor;
    }

    public PricingRule dimDivisor(Integer dimDivisor) {
        this.setDimDivisor(dimDivisor);
        return this;
    }

    public void setDimDivisor(Integer dimDivisor) {
        this.dimDivisor = dimDivisor;
    }

    public BigDecimal getKmMin() {
        return this.kmMin;
    }

    public PricingRule kmMin(BigDecimal kmMin) {
        this.setKmMin(kmMin);
        return this;
    }

    public void setKmMin(BigDecimal kmMin) {
        this.kmMin = kmMin;
    }

    public BigDecimal getKmRate() {
        return this.kmRate;
    }

    public PricingRule kmRate(BigDecimal kmRate) {
        this.setKmRate(kmRate);
        return this;
    }

    public void setKmRate(BigDecimal kmRate) {
        this.kmRate = kmRate;
    }

    public Integer getStepGram() {
        return this.stepGram;
    }

    public PricingRule stepGram(Integer stepGram) {
        this.setStepGram(stepGram);
        return this;
    }

    public void setStepGram(Integer stepGram) {
        this.stepGram = stepGram;
    }

    public BigDecimal getAddFeeAmount() {
        return this.addFeeAmount;
    }

    public PricingRule addFeeAmount(BigDecimal addFeeAmount) {
        this.setAddFeeAmount(addFeeAmount);
        return this;
    }

    public void setAddFeeAmount(BigDecimal addFeeAmount) {
        this.addFeeAmount = addFeeAmount;
    }

    public Instant getEffectiveFrom() {
        return this.effectiveFrom;
    }

    public PricingRule effectiveFrom(Instant effectiveFrom) {
        this.setEffectiveFrom(effectiveFrom);
        return this;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return this.effectiveTo;
    }

    public PricingRule effectiveTo(Instant effectiveTo) {
        this.setEffectiveTo(effectiveTo);
        return this;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Boolean getActive() {
        return this.active;
    }

    public PricingRule active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Route getRoute() {
        return this.route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public PricingRule route(Route route) {
        this.setRoute(route);
        return this;
    }

    public Branch getBranch() {
        return this.branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public PricingRule branch(Branch branch) {
        this.setBranch(branch);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PricingRule)) {
            return false;
        }
        return getId() != null && getId().equals(((PricingRule) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PricingRule{" +
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
            "}";
    }
}
