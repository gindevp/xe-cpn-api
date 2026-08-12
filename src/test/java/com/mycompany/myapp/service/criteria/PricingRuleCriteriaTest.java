package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PricingRuleCriteriaTest {

    @Test
    void newPricingRuleCriteriaHasAllFiltersNullTest() {
        var pricingRuleCriteria = new PricingRuleCriteria();
        assertThat(pricingRuleCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void pricingRuleCriteriaFluentMethodsCreatesFiltersTest() {
        var pricingRuleCriteria = new PricingRuleCriteria();

        setAllFilters(pricingRuleCriteria);

        assertThat(pricingRuleCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void pricingRuleCriteriaCopyCreatesNullFilterTest() {
        var pricingRuleCriteria = new PricingRuleCriteria();
        var copy = pricingRuleCriteria.copy();

        assertThat(pricingRuleCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(pricingRuleCriteria)
        );
    }

    @Test
    void pricingRuleCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var pricingRuleCriteria = new PricingRuleCriteria();
        setAllFilters(pricingRuleCriteria);

        var copy = pricingRuleCriteria.copy();

        assertThat(pricingRuleCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(pricingRuleCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var pricingRuleCriteria = new PricingRuleCriteria();

        assertThat(pricingRuleCriteria).hasToString("PricingRuleCriteria{}");
    }

    private static void setAllFilters(PricingRuleCriteria pricingRuleCriteria) {
        pricingRuleCriteria.id();
        pricingRuleCriteria.ruleCode();
        pricingRuleCriteria.tierLabel();
        pricingRuleCriteria.minKg();
        pricingRuleCriteria.maxKg();
        pricingRuleCriteria.unitPrice();
        pricingRuleCriteria.surchargeAmount();
        pricingRuleCriteria.dimDivisor();
        pricingRuleCriteria.kmMin();
        pricingRuleCriteria.kmRate();
        pricingRuleCriteria.stepGram();
        pricingRuleCriteria.addFeeAmount();
        pricingRuleCriteria.effectiveFrom();
        pricingRuleCriteria.effectiveTo();
        pricingRuleCriteria.active();
        pricingRuleCriteria.routeId();
        pricingRuleCriteria.distinct();
    }

    private static Condition<PricingRuleCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getRuleCode()) &&
                condition.apply(criteria.getTierLabel()) &&
                condition.apply(criteria.getMinKg()) &&
                condition.apply(criteria.getMaxKg()) &&
                condition.apply(criteria.getUnitPrice()) &&
                condition.apply(criteria.getSurchargeAmount()) &&
                condition.apply(criteria.getDimDivisor()) &&
                condition.apply(criteria.getKmMin()) &&
                condition.apply(criteria.getKmRate()) &&
                condition.apply(criteria.getStepGram()) &&
                condition.apply(criteria.getAddFeeAmount()) &&
                condition.apply(criteria.getEffectiveFrom()) &&
                condition.apply(criteria.getEffectiveTo()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getRouteId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PricingRuleCriteria> copyFiltersAre(PricingRuleCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getRuleCode(), copy.getRuleCode()) &&
                condition.apply(criteria.getTierLabel(), copy.getTierLabel()) &&
                condition.apply(criteria.getMinKg(), copy.getMinKg()) &&
                condition.apply(criteria.getMaxKg(), copy.getMaxKg()) &&
                condition.apply(criteria.getUnitPrice(), copy.getUnitPrice()) &&
                condition.apply(criteria.getSurchargeAmount(), copy.getSurchargeAmount()) &&
                condition.apply(criteria.getDimDivisor(), copy.getDimDivisor()) &&
                condition.apply(criteria.getKmMin(), copy.getKmMin()) &&
                condition.apply(criteria.getKmRate(), copy.getKmRate()) &&
                condition.apply(criteria.getStepGram(), copy.getStepGram()) &&
                condition.apply(criteria.getAddFeeAmount(), copy.getAddFeeAmount()) &&
                condition.apply(criteria.getEffectiveFrom(), copy.getEffectiveFrom()) &&
                condition.apply(criteria.getEffectiveTo(), copy.getEffectiveTo()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getRouteId(), copy.getRouteId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
