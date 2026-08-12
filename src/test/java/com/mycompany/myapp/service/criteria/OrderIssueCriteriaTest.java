package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class OrderIssueCriteriaTest {

    @Test
    void newOrderIssueCriteriaHasAllFiltersNullTest() {
        var orderIssueCriteria = new OrderIssueCriteria();
        assertThat(orderIssueCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void orderIssueCriteriaFluentMethodsCreatesFiltersTest() {
        var orderIssueCriteria = new OrderIssueCriteria();

        setAllFilters(orderIssueCriteria);

        assertThat(orderIssueCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void orderIssueCriteriaCopyCreatesNullFilterTest() {
        var orderIssueCriteria = new OrderIssueCriteria();
        var copy = orderIssueCriteria.copy();

        assertThat(orderIssueCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(orderIssueCriteria)
        );
    }

    @Test
    void orderIssueCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var orderIssueCriteria = new OrderIssueCriteria();
        setAllFilters(orderIssueCriteria);

        var copy = orderIssueCriteria.copy();

        assertThat(orderIssueCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(orderIssueCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var orderIssueCriteria = new OrderIssueCriteria();

        assertThat(orderIssueCriteria).hasToString("OrderIssueCriteria{}");
    }

    private static void setAllFilters(OrderIssueCriteria orderIssueCriteria) {
        orderIssueCriteria.id();
        orderIssueCriteria.issueType();
        orderIssueCriteria.issueStatus();
        orderIssueCriteria.reason();
        orderIssueCriteria.openedAt();
        orderIssueCriteria.openedByUsername();
        orderIssueCriteria.resolvedAt();
        orderIssueCriteria.resolvedByUsername();
        orderIssueCriteria.resolutionNote();
        orderIssueCriteria.orderId();
        orderIssueCriteria.distinct();
    }

    private static Condition<OrderIssueCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getIssueType()) &&
                condition.apply(criteria.getIssueStatus()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getOpenedAt()) &&
                condition.apply(criteria.getOpenedByUsername()) &&
                condition.apply(criteria.getResolvedAt()) &&
                condition.apply(criteria.getResolvedByUsername()) &&
                condition.apply(criteria.getResolutionNote()) &&
                condition.apply(criteria.getOrderId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<OrderIssueCriteria> copyFiltersAre(OrderIssueCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getIssueType(), copy.getIssueType()) &&
                condition.apply(criteria.getIssueStatus(), copy.getIssueStatus()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getOpenedAt(), copy.getOpenedAt()) &&
                condition.apply(criteria.getOpenedByUsername(), copy.getOpenedByUsername()) &&
                condition.apply(criteria.getResolvedAt(), copy.getResolvedAt()) &&
                condition.apply(criteria.getResolvedByUsername(), copy.getResolvedByUsername()) &&
                condition.apply(criteria.getResolutionNote(), copy.getResolutionNote()) &&
                condition.apply(criteria.getOrderId(), copy.getOrderId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
