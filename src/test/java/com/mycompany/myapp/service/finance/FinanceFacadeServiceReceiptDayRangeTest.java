package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptListFilter;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FinanceFacadeServiceReceiptDayRangeTest {

    private static ReceiptListFilter filter(String day, String from, String to) {
        return new ReceiptListFilter(null, null, null, day, null, from, to);
    }

    private static Instant[] paid(String from, String to) {
        return FinanceFacadeService.receiptPaidRange(filter(null, from, to));
    }

    @Test
    void noFilter_open() {
        assertThat(FinanceFacadeService.receiptCreatedRange(ReceiptListFilter.NONE)).containsExactly(null, null);
        assertThat(FinanceFacadeService.receiptPaidRange(ReceiptListFilter.NONE)).containsExactly(null, null);
    }

    @Test
    void legacyDay_isCreatedDate_vnBounds() {
        ReceiptListFilter f = filter("2026-10-04", null, null);
        assertThat(FinanceFacadeService.receiptCreatedRange(f)).containsExactly(
            Instant.parse("2026-10-03T17:00:00Z"),
            Instant.parse("2026-10-04T17:00:00Z")
        );
        assertThat(FinanceFacadeService.receiptPaidRange(f)).containsExactly(null, null);
    }

    @Test
    void paidRange_inclusiveBothEnds() {
        assertThat(paid("2026-10-01", "2026-10-04")).containsExactly(
            Instant.parse("2026-09-30T17:00:00Z"),
            Instant.parse("2026-10-04T17:00:00Z")
        );
    }

    @Test
    void paidRange_doesNotTouchCreatedRange() {
        assertThat(FinanceFacadeService.receiptCreatedRange(filter(null, "2026-10-01", "2026-10-04"))).containsExactly(null, null);
    }

    @Test
    void paidRange_openEnds() {
        assertThat(paid("2026-10-01", null)).containsExactly(Instant.parse("2026-09-30T17:00:00Z"), null);
        assertThat(paid(null, "2026-10-04")).containsExactly(null, Instant.parse("2026-10-04T17:00:00Z"));
    }

    @Test
    void paidRange_reversed_swapped() {
        assertThat(paid("2026-10-04", "2026-10-01")).containsExactly(
            Instant.parse("2026-09-30T17:00:00Z"),
            Instant.parse("2026-10-04T17:00:00Z")
        );
    }

    @Test
    void invalidDate_badRequest() {
        assertThatThrownBy(() -> paid("04/10/2026", null)).isInstanceOf(BadRequestAlertException.class);
    }
}
