package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class AuditLogCriteriaTest {

    @Test
    void newAuditLogCriteriaHasAllFiltersNullTest() {
        var auditLogCriteria = new AuditLogCriteria();
        assertThat(auditLogCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void auditLogCriteriaFluentMethodsCreatesFiltersTest() {
        var auditLogCriteria = new AuditLogCriteria();

        setAllFilters(auditLogCriteria);

        assertThat(auditLogCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void auditLogCriteriaCopyCreatesNullFilterTest() {
        var auditLogCriteria = new AuditLogCriteria();
        var copy = auditLogCriteria.copy();

        assertThat(auditLogCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(auditLogCriteria)
        );
    }

    @Test
    void auditLogCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var auditLogCriteria = new AuditLogCriteria();
        setAllFilters(auditLogCriteria);

        var copy = auditLogCriteria.copy();

        assertThat(auditLogCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(auditLogCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var auditLogCriteria = new AuditLogCriteria();

        assertThat(auditLogCriteria).hasToString("AuditLogCriteria{}");
    }

    private static void setAllFilters(AuditLogCriteria auditLogCriteria) {
        auditLogCriteria.id();
        auditLogCriteria.action();
        auditLogCriteria.entityType();
        auditLogCriteria.entityId();
        auditLogCriteria.detail();
        auditLogCriteria.actedAt();
        auditLogCriteria.actedByUsername();
        auditLogCriteria.distinct();
    }

    private static Condition<AuditLogCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getAction()) &&
                condition.apply(criteria.getEntityType()) &&
                condition.apply(criteria.getEntityId()) &&
                condition.apply(criteria.getDetail()) &&
                condition.apply(criteria.getActedAt()) &&
                condition.apply(criteria.getActedByUsername()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<AuditLogCriteria> copyFiltersAre(AuditLogCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getAction(), copy.getAction()) &&
                condition.apply(criteria.getEntityType(), copy.getEntityType()) &&
                condition.apply(criteria.getEntityId(), copy.getEntityId()) &&
                condition.apply(criteria.getDetail(), copy.getDetail()) &&
                condition.apply(criteria.getActedAt(), copy.getActedAt()) &&
                condition.apply(criteria.getActedByUsername(), copy.getActedByUsername()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
