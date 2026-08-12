package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ReceiptCriteriaTest {

    @Test
    void newReceiptCriteriaHasAllFiltersNullTest() {
        var receiptCriteria = new ReceiptCriteria();
        assertThat(receiptCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void receiptCriteriaFluentMethodsCreatesFiltersTest() {
        var receiptCriteria = new ReceiptCriteria();

        setAllFilters(receiptCriteria);

        assertThat(receiptCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void receiptCriteriaCopyCreatesNullFilterTest() {
        var receiptCriteria = new ReceiptCriteria();
        var copy = receiptCriteria.copy();

        assertThat(receiptCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(receiptCriteria)
        );
    }

    @Test
    void receiptCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var receiptCriteria = new ReceiptCriteria();
        setAllFilters(receiptCriteria);

        var copy = receiptCriteria.copy();

        assertThat(receiptCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(receiptCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var receiptCriteria = new ReceiptCriteria();

        assertThat(receiptCriteria).hasToString("ReceiptCriteria{}");
    }

    private static void setAllFilters(ReceiptCriteria receiptCriteria) {
        receiptCriteria.id();
        receiptCriteria.receiptCode();
        receiptCriteria.payerName();
        receiptCriteria.payerCode();
        receiptCriteria.totalAmount();
        receiptCriteria.createdAt();
        receiptCriteria.createdByUsername();
        receiptCriteria.officeId();
        receiptCriteria.distinct();
    }

    private static Condition<ReceiptCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getReceiptCode()) &&
                condition.apply(criteria.getPayerName()) &&
                condition.apply(criteria.getPayerCode()) &&
                condition.apply(criteria.getTotalAmount()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getCreatedByUsername()) &&
                condition.apply(criteria.getOfficeId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ReceiptCriteria> copyFiltersAre(ReceiptCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getReceiptCode(), copy.getReceiptCode()) &&
                condition.apply(criteria.getPayerName(), copy.getPayerName()) &&
                condition.apply(criteria.getPayerCode(), copy.getPayerCode()) &&
                condition.apply(criteria.getTotalAmount(), copy.getTotalAmount()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getCreatedByUsername(), copy.getCreatedByUsername()) &&
                condition.apply(criteria.getOfficeId(), copy.getOfficeId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
