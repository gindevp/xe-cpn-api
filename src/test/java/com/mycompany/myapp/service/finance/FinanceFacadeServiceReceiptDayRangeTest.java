package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptListFilter;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FinanceFacadeServiceReceiptDayRangeTest {

    private static Instant[] range(String day, String from, String to) {
        return FinanceFacadeService.receiptDayRange(new ReceiptListFilter(null, null, null, day, null, from, to));
    }

    @Test
    void noFilter_open() {
        assertThat(range(null, null, null)).containsExactly(null, null);
        assertThat(FinanceFacadeService.receiptDayRange(ReceiptListFilter.NONE)).containsExactly(null, null);
    }

    @Test
    void legacySingleDay_vnBounds() {
        assertThat(range("2026-10-04", null, null)).containsExactly(
            Instant.parse("2026-10-03T17:00:00Z"),
            Instant.parse("2026-10-04T17:00:00Z")
        );
    }

    @Test
    void range_inclusiveBothEnds() {
        assertThat(range(null, "2026-10-01", "2026-10-04")).containsExactly(
            Instant.parse("2026-09-30T17:00:00Z"),
            Instant.parse("2026-10-04T17:00:00Z")
        );
    }

    @Test
    void range_overridesLegacyDay() {
        assertThat(range("2026-01-01", "2026-10-01", "2026-10-01")).containsExactly(
            Instant.parse("2026-09-30T17:00:00Z"),
            Instant.parse("2026-10-01T17:00:00Z")
        );
    }

    @Test
    void range_openEnds() {
        assertThat(range(null, "2026-10-01", null)).containsExactly(Instant.parse("2026-09-30T17:00:00Z"), null);
        assertThat(range(null, null, "2026-10-04")).containsExactly(null, Instant.parse("2026-10-04T17:00:00Z"));
    }

    @Test
    void range_reversed_swapped() {
        assertThat(range(null, "2026-10-04", "2026-10-01")).containsExactly(
            Instant.parse("2026-09-30T17:00:00Z"),
            Instant.parse("2026-10-04T17:00:00Z")
        );
    }

    @Test
    void invalidDate_badRequest() {
        assertThatThrownBy(() -> range(null, "04/10/2026", null)).isInstanceOf(BadRequestAlertException.class);
    }
}
