package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class TripCriteriaTest {

    @Test
    void newTripCriteriaHasAllFiltersNullTest() {
        var tripCriteria = new TripCriteria();
        assertThat(tripCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void tripCriteriaFluentMethodsCreatesFiltersTest() {
        var tripCriteria = new TripCriteria();

        setAllFilters(tripCriteria);

        assertThat(tripCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void tripCriteriaCopyCreatesNullFilterTest() {
        var tripCriteria = new TripCriteria();
        var copy = tripCriteria.copy();

        assertThat(tripCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(tripCriteria)
        );
    }

    @Test
    void tripCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var tripCriteria = new TripCriteria();
        setAllFilters(tripCriteria);

        var copy = tripCriteria.copy();

        assertThat(tripCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(tripCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var tripCriteria = new TripCriteria();

        assertThat(tripCriteria).hasToString("TripCriteria{}");
    }

    private static void setAllFilters(TripCriteria tripCriteria) {
        tripCriteria.id();
        tripCriteria.tripCode();
        tripCriteria.status();
        tripCriteria.departAt();
        tripCriteria.loadedCount();
        tripCriteria.scannedCount();
        tripCriteria.closedAt();
        tripCriteria.forceClosed();
        tripCriteria.forceCloseReason();
        tripCriteria.officeId();
        tripCriteria.routeId();
        tripCriteria.vehicleId();
        tripCriteria.driverId();
        tripCriteria.distinct();
    }

    private static Condition<TripCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTripCode()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getDepartAt()) &&
                condition.apply(criteria.getLoadedCount()) &&
                condition.apply(criteria.getScannedCount()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getForceClosed()) &&
                condition.apply(criteria.getForceCloseReason()) &&
                condition.apply(criteria.getOfficeId()) &&
                condition.apply(criteria.getRouteId()) &&
                condition.apply(criteria.getVehicleId()) &&
                condition.apply(criteria.getDriverId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<TripCriteria> copyFiltersAre(TripCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTripCode(), copy.getTripCode()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getDepartAt(), copy.getDepartAt()) &&
                condition.apply(criteria.getLoadedCount(), copy.getLoadedCount()) &&
                condition.apply(criteria.getScannedCount(), copy.getScannedCount()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getForceClosed(), copy.getForceClosed()) &&
                condition.apply(criteria.getForceCloseReason(), copy.getForceCloseReason()) &&
                condition.apply(criteria.getOfficeId(), copy.getOfficeId()) &&
                condition.apply(criteria.getRouteId(), copy.getRouteId()) &&
                condition.apply(criteria.getVehicleId(), copy.getVehicleId()) &&
                condition.apply(criteria.getDriverId(), copy.getDriverId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
