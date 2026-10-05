package com.mycompany.myapp.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BusinessReportServiceTest {

    @Test
    void periodUsesVietnamDayBoundaries() {
        var p = BusinessReportService.Period.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        assertThat(p.start()).isEqualTo(Instant.parse("2026-09-30T17:00:00Z"));
        assertThat(p.end()).isEqualTo(Instant.parse("2026-10-05T17:00:00Z"));
    }

    @Test
    void previousPeriodShiftsOneMonth() {
        var prev = BusinessReportService.Period.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)).previousMonth();
        assertThat(prev.from()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(prev.to()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void totalsAddReceiptAndBacklog() {
        var t = BusinessReportService.Totals.of(new BigDecimal("1000"), new BigDecimal("250"), 7, 3);
        assertThat(t.totalRevenue()).isEqualByComparingTo("1250");
        assertThat(t.totalOrders()).isEqualTo(10);
        var empty = BusinessReportService.Totals.of(null, null, 0, 0);
        assertThat(empty.totalRevenue()).isEqualByComparingTo("0");
    }
}
