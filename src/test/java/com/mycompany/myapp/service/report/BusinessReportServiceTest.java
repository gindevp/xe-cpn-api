package com.mycompany.myapp.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    @Test
    void hourBuckets_groupByVietnamTwoHourSlots_ofWarehouseIn() {
        var hits = List.of(
            new BusinessReportService.WhInHit(Instant.parse("2026-10-01T01:30:00Z"), "GP", BigDecimal.TEN), // 08:30 VN gửi
            new BusinessReportService.WhInHit(Instant.parse("2026-10-01T02:15:00Z"), "GP", BigDecimal.TEN), // 09:15 VN gửi
            new BusinessReportService.WhInHit(Instant.parse("2026-10-01T02:45:00Z"), "HD", BigDecimal.TEN), // 09:45 VN nhận
            new BusinessReportService.WhInHit(Instant.parse("2026-10-01T17:30:00Z"), "HD", BigDecimal.TEN) // 00:30 VN
        );
        var hours = BusinessReportService.hourBuckets(hits);
        assertThat(hours).hasSize(12);
        assertThat(hours.get(4).label()).isEqualTo("08-10h");
        assertThat(hours.get(4).count()).isEqualTo(3);
        assertThat(hours.get(4).perOffice()).containsEntry("GP", 2L);
        assertThat(hours.get(4).perOffice()).containsEntry("HD", 1L);
        assertThat(hours.get(0).count()).isEqualTo(1);
        assertThat(hours.get(0).perOffice()).containsEntry("HD", 1L);
        assertThat(hours.stream().mapToLong(BusinessReportService.HourBucket::count).sum()).isEqualTo(4);
    }
}
