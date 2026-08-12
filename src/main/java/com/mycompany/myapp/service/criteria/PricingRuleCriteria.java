package com.mycompany.myapp.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.PricingRule} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.PricingRuleResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /pricing-rules?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PricingRuleCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter ruleCode;

    private StringFilter tierLabel;

    private BigDecimalFilter minKg;

    private BigDecimalFilter maxKg;

    private BigDecimalFilter unitPrice;

    private BigDecimalFilter surchargeAmount;

    private IntegerFilter dimDivisor;

    private BigDecimalFilter kmMin;

    private BigDecimalFilter kmRate;

    private IntegerFilter stepGram;

    private BigDecimalFilter addFeeAmount;

    private InstantFilter effectiveFrom;

    private InstantFilter effectiveTo;

    private BooleanFilter active;

    private LongFilter routeId;

    private Boolean distinct;

    public PricingRuleCriteria() {}

    public PricingRuleCriteria(PricingRuleCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.ruleCode = other.optionalRuleCode().map(StringFilter::copy).orElse(null);
        this.tierLabel = other.optionalTierLabel().map(StringFilter::copy).orElse(null);
        this.minKg = other.optionalMinKg().map(BigDecimalFilter::copy).orElse(null);
        this.maxKg = other.optionalMaxKg().map(BigDecimalFilter::copy).orElse(null);
        this.unitPrice = other.optionalUnitPrice().map(BigDecimalFilter::copy).orElse(null);
        this.surchargeAmount = other.optionalSurchargeAmount().map(BigDecimalFilter::copy).orElse(null);
        this.dimDivisor = other.optionalDimDivisor().map(IntegerFilter::copy).orElse(null);
        this.kmMin = other.optionalKmMin().map(BigDecimalFilter::copy).orElse(null);
        this.kmRate = other.optionalKmRate().map(BigDecimalFilter::copy).orElse(null);
        this.stepGram = other.optionalStepGram().map(IntegerFilter::copy).orElse(null);
        this.addFeeAmount = other.optionalAddFeeAmount().map(BigDecimalFilter::copy).orElse(null);
        this.effectiveFrom = other.optionalEffectiveFrom().map(InstantFilter::copy).orElse(null);
        this.effectiveTo = other.optionalEffectiveTo().map(InstantFilter::copy).orElse(null);
        this.active = other.optionalActive().map(BooleanFilter::copy).orElse(null);
        this.routeId = other.optionalRouteId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public PricingRuleCriteria copy() {
        return new PricingRuleCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getRuleCode() {
        return ruleCode;
    }

    public Optional<StringFilter> optionalRuleCode() {
        return Optional.ofNullable(ruleCode);
    }

    public StringFilter ruleCode() {
        if (ruleCode == null) {
            setRuleCode(new StringFilter());
        }
        return ruleCode;
    }

    public void setRuleCode(StringFilter ruleCode) {
        this.ruleCode = ruleCode;
    }

    public StringFilter getTierLabel() {
        return tierLabel;
    }

    public Optional<StringFilter> optionalTierLabel() {
        return Optional.ofNullable(tierLabel);
    }

    public StringFilter tierLabel() {
        if (tierLabel == null) {
            setTierLabel(new StringFilter());
        }
        return tierLabel;
    }

    public void setTierLabel(StringFilter tierLabel) {
        this.tierLabel = tierLabel;
    }

    public BigDecimalFilter getMinKg() {
        return minKg;
    }

    public Optional<BigDecimalFilter> optionalMinKg() {
        return Optional.ofNullable(minKg);
    }

    public BigDecimalFilter minKg() {
        if (minKg == null) {
            setMinKg(new BigDecimalFilter());
        }
        return minKg;
    }

    public void setMinKg(BigDecimalFilter minKg) {
        this.minKg = minKg;
    }

    public BigDecimalFilter getMaxKg() {
        return maxKg;
    }

    public Optional<BigDecimalFilter> optionalMaxKg() {
        return Optional.ofNullable(maxKg);
    }

    public BigDecimalFilter maxKg() {
        if (maxKg == null) {
            setMaxKg(new BigDecimalFilter());
        }
        return maxKg;
    }

    public void setMaxKg(BigDecimalFilter maxKg) {
        this.maxKg = maxKg;
    }

    public BigDecimalFilter getUnitPrice() {
        return unitPrice;
    }

    public Optional<BigDecimalFilter> optionalUnitPrice() {
        return Optional.ofNullable(unitPrice);
    }

    public BigDecimalFilter unitPrice() {
        if (unitPrice == null) {
            setUnitPrice(new BigDecimalFilter());
        }
        return unitPrice;
    }

    public void setUnitPrice(BigDecimalFilter unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimalFilter getSurchargeAmount() {
        return surchargeAmount;
    }

    public Optional<BigDecimalFilter> optionalSurchargeAmount() {
        return Optional.ofNullable(surchargeAmount);
    }

    public BigDecimalFilter surchargeAmount() {
        if (surchargeAmount == null) {
            setSurchargeAmount(new BigDecimalFilter());
        }
        return surchargeAmount;
    }

    public void setSurchargeAmount(BigDecimalFilter surchargeAmount) {
        this.surchargeAmount = surchargeAmount;
    }

    public IntegerFilter getDimDivisor() {
        return dimDivisor;
    }

    public Optional<IntegerFilter> optionalDimDivisor() {
        return Optional.ofNullable(dimDivisor);
    }

    public IntegerFilter dimDivisor() {
        if (dimDivisor == null) {
            setDimDivisor(new IntegerFilter());
        }
        return dimDivisor;
    }

    public void setDimDivisor(IntegerFilter dimDivisor) {
        this.dimDivisor = dimDivisor;
    }

    public BigDecimalFilter getKmMin() {
        return kmMin;
    }

    public Optional<BigDecimalFilter> optionalKmMin() {
        return Optional.ofNullable(kmMin);
    }

    public BigDecimalFilter kmMin() {
        if (kmMin == null) {
            setKmMin(new BigDecimalFilter());
        }
        return kmMin;
    }

    public void setKmMin(BigDecimalFilter kmMin) {
        this.kmMin = kmMin;
    }

    public BigDecimalFilter getKmRate() {
        return kmRate;
    }

    public Optional<BigDecimalFilter> optionalKmRate() {
        return Optional.ofNullable(kmRate);
    }

    public BigDecimalFilter kmRate() {
        if (kmRate == null) {
            setKmRate(new BigDecimalFilter());
        }
        return kmRate;
    }

    public void setKmRate(BigDecimalFilter kmRate) {
        this.kmRate = kmRate;
    }

    public IntegerFilter getStepGram() {
        return stepGram;
    }

    public Optional<IntegerFilter> optionalStepGram() {
        return Optional.ofNullable(stepGram);
    }

    public IntegerFilter stepGram() {
        if (stepGram == null) {
            setStepGram(new IntegerFilter());
        }
        return stepGram;
    }

    public void setStepGram(IntegerFilter stepGram) {
        this.stepGram = stepGram;
    }

    public BigDecimalFilter getAddFeeAmount() {
        return addFeeAmount;
    }

    public Optional<BigDecimalFilter> optionalAddFeeAmount() {
        return Optional.ofNullable(addFeeAmount);
    }

    public BigDecimalFilter addFeeAmount() {
        if (addFeeAmount == null) {
            setAddFeeAmount(new BigDecimalFilter());
        }
        return addFeeAmount;
    }

    public void setAddFeeAmount(BigDecimalFilter addFeeAmount) {
        this.addFeeAmount = addFeeAmount;
    }

    public InstantFilter getEffectiveFrom() {
        return effectiveFrom;
    }

    public Optional<InstantFilter> optionalEffectiveFrom() {
        return Optional.ofNullable(effectiveFrom);
    }

    public InstantFilter effectiveFrom() {
        if (effectiveFrom == null) {
            setEffectiveFrom(new InstantFilter());
        }
        return effectiveFrom;
    }

    public void setEffectiveFrom(InstantFilter effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public InstantFilter getEffectiveTo() {
        return effectiveTo;
    }

    public Optional<InstantFilter> optionalEffectiveTo() {
        return Optional.ofNullable(effectiveTo);
    }

    public InstantFilter effectiveTo() {
        if (effectiveTo == null) {
            setEffectiveTo(new InstantFilter());
        }
        return effectiveTo;
    }

    public void setEffectiveTo(InstantFilter effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public BooleanFilter getActive() {
        return active;
    }

    public Optional<BooleanFilter> optionalActive() {
        return Optional.ofNullable(active);
    }

    public BooleanFilter active() {
        if (active == null) {
            setActive(new BooleanFilter());
        }
        return active;
    }

    public void setActive(BooleanFilter active) {
        this.active = active;
    }

    public LongFilter getRouteId() {
        return routeId;
    }

    public Optional<LongFilter> optionalRouteId() {
        return Optional.ofNullable(routeId);
    }

    public LongFilter routeId() {
        if (routeId == null) {
            setRouteId(new LongFilter());
        }
        return routeId;
    }

    public void setRouteId(LongFilter routeId) {
        this.routeId = routeId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PricingRuleCriteria that = (PricingRuleCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(ruleCode, that.ruleCode) &&
            Objects.equals(tierLabel, that.tierLabel) &&
            Objects.equals(minKg, that.minKg) &&
            Objects.equals(maxKg, that.maxKg) &&
            Objects.equals(unitPrice, that.unitPrice) &&
            Objects.equals(surchargeAmount, that.surchargeAmount) &&
            Objects.equals(dimDivisor, that.dimDivisor) &&
            Objects.equals(kmMin, that.kmMin) &&
            Objects.equals(kmRate, that.kmRate) &&
            Objects.equals(stepGram, that.stepGram) &&
            Objects.equals(addFeeAmount, that.addFeeAmount) &&
            Objects.equals(effectiveFrom, that.effectiveFrom) &&
            Objects.equals(effectiveTo, that.effectiveTo) &&
            Objects.equals(active, that.active) &&
            Objects.equals(routeId, that.routeId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            ruleCode,
            tierLabel,
            minKg,
            maxKg,
            unitPrice,
            surchargeAmount,
            dimDivisor,
            kmMin,
            kmRate,
            stepGram,
            addFeeAmount,
            effectiveFrom,
            effectiveTo,
            active,
            routeId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PricingRuleCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalRuleCode().map(f -> "ruleCode=" + f + ", ").orElse("") +
            optionalTierLabel().map(f -> "tierLabel=" + f + ", ").orElse("") +
            optionalMinKg().map(f -> "minKg=" + f + ", ").orElse("") +
            optionalMaxKg().map(f -> "maxKg=" + f + ", ").orElse("") +
            optionalUnitPrice().map(f -> "unitPrice=" + f + ", ").orElse("") +
            optionalSurchargeAmount().map(f -> "surchargeAmount=" + f + ", ").orElse("") +
            optionalDimDivisor().map(f -> "dimDivisor=" + f + ", ").orElse("") +
            optionalKmMin().map(f -> "kmMin=" + f + ", ").orElse("") +
            optionalKmRate().map(f -> "kmRate=" + f + ", ").orElse("") +
            optionalStepGram().map(f -> "stepGram=" + f + ", ").orElse("") +
            optionalAddFeeAmount().map(f -> "addFeeAmount=" + f + ", ").orElse("") +
            optionalEffectiveFrom().map(f -> "effectiveFrom=" + f + ", ").orElse("") +
            optionalEffectiveTo().map(f -> "effectiveTo=" + f + ", ").orElse("") +
            optionalActive().map(f -> "active=" + f + ", ").orElse("") +
            optionalRouteId().map(f -> "routeId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
