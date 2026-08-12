package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DayClosureCriteriaTest {

    @Test
    void newDayClosureCriteriaHasAllFiltersNullTest() {
        var dayClosureCriteria = new DayClosureCriteria();
        assertThat(dayClosureCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void dayClosureCriteriaFluentMethodsCreatesFiltersTest() {
        var dayClosureCriteria = new DayClosureCriteria();

        setAllFilters(dayClosureCriteria);

        assertThat(dayClosureCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void dayClosureCriteriaCopyCreatesNullFilterTest() {
        var dayClosureCriteria = new DayClosureCriteria();
        var copy = dayClosureCriteria.copy();

        assertThat(dayClosureCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(dayClosureCriteria)
        );
    }

    @Test
    void dayClosureCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var dayClosureCriteria = new DayClosureCriteria();
        setAllFilters(dayClosureCriteria);

        var copy = dayClosureCriteria.copy();

        assertThat(dayClosureCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(dayClosureCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var dayClosureCriteria = new DayClosureCriteria();

        assertThat(dayClosureCriteria).hasToString("DayClosureCriteria{}");
    }

    private static void setAllFilters(DayClosureCriteria dayClosureCriteria) {
        dayClosureCriteria.id();
        dayClosureCriteria.businessDate();
        dayClosureCriteria.status();
        dayClosureCriteria.confirmedByUsername();
        dayClosureCriteria.confirmedAt();
        dayClosureCriteria.reopenedByUsername();
        dayClosureCriteria.reopenedAt();
        dayClosureCriteria.officeId();
        dayClosureCriteria.distinct();
    }

    private static Condition<DayClosureCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getBusinessDate()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getConfirmedByUsername()) &&
                condition.apply(criteria.getConfirmedAt()) &&
                condition.apply(criteria.getReopenedByUsername()) &&
                condition.apply(criteria.getReopenedAt()) &&
                condition.apply(criteria.getOfficeId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DayClosureCriteria> copyFiltersAre(DayClosureCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getBusinessDate(), copy.getBusinessDate()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getConfirmedByUsername(), copy.getConfirmedByUsername()) &&
                condition.apply(criteria.getConfirmedAt(), copy.getConfirmedAt()) &&
                condition.apply(criteria.getReopenedByUsername(), copy.getReopenedByUsername()) &&
                condition.apply(criteria.getReopenedAt(), copy.getReopenedAt()) &&
                condition.apply(criteria.getOfficeId(), copy.getOfficeId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
