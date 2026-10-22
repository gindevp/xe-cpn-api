package com.mycompany.myapp.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RevenueLedgerTest {

    private static RevenueLedger.LineAmount line(long lineId, long receiptId, long amount) {
        return new RevenueLedger.LineAmount(lineId, receiptId, BigDecimal.valueOf(amount));
    }

    @Test
    void codAboveFareIsNotRevenue() {
        Map<Long, BigDecimal> s = RevenueLedger.allocateFare(BigDecimal.valueOf(50_000), List.of(line(1, 10, 1_050_000)));
        assertThat(s.get(1L)).isEqualByComparingTo("50000");
    }

    @Test
    void earlierReceiptTakesFareFirst() {
        // Phiếu VP gửi 40k cước trả trước, phiếu giao sau 60k cước + 1tr COD.
        Map<Long, BigDecimal> s = RevenueLedger.allocateFare(
            BigDecimal.valueOf(100_000),
            List.of(line(7, 20, 1_060_000), line(3, 10, 40_000))
        );
        assertThat(s.get(3L)).isEqualByComparingTo("40000");
        assertThat(s.get(7L)).isEqualByComparingTo("60000");
    }

    @Test
    void fareFullyReceiptedLeavesLaterLinesAsCod() {
        Map<Long, BigDecimal> s = RevenueLedger.allocateFare(
            BigDecimal.valueOf(30_000),
            List.of(line(1, 10, 30_000), line(2, 11, 500_000))
        );
        assertThat(s.get(1L)).isEqualByComparingTo("30000");
        assertThat(s.get(2L)).isEqualByComparingTo("0");
    }

    @Test
    void ahamoveFeeComesOffFareOnceAndCannotGoNegative() {
        assertThat(RevenueLedger.ahamoveCost("AHAMOVE", "IN PROCESS", BigDecimal.valueOf(35_000))).isEqualByComparingTo("35000");
        assertThat(RevenueLedger.ahamoveCost("ahamove", null, BigDecimal.valueOf(35_000))).isEqualByComparingTo("35000");
        assertThat(RevenueLedger.ahamoveCost(null, null, BigDecimal.valueOf(35_000))).isEqualByComparingTo("0");
        assertThat(RevenueLedger.ahamoveCost("AHAMOVE", "CANCELLED", BigDecimal.valueOf(35_000))).isEqualByComparingTo("0");
        assertThat(
            RevenueLedger.netFare(BigDecimal.valueOf(100_000), "AHAMOVE", "ASSIGNING", BigDecimal.valueOf(20_000))
        ).isEqualByComparingTo("80000");
        assertThat(
            RevenueLedger.netFare(BigDecimal.valueOf(10_000), "AHAMOVE", "COMPLETED", BigDecimal.valueOf(35_000))
        ).isEqualByComparingTo("0");
        assertThat(RevenueLedger.netFare(BigDecimal.valueOf(100_000), null, null, BigDecimal.valueOf(20_000))).isEqualByComparingTo(
            "100000"
        );
        assertThat(RevenueLedger.ahamoveCost("EXTERNAL", "ASSIGNED", BigDecimal.valueOf(15_000))).isEqualByComparingTo("15000");
        assertThat(
            RevenueLedger.netFare(BigDecimal.valueOf(55_000), "EXTERNAL", "ASSIGNED", BigDecimal.valueOf(40_000))
        ).isEqualByComparingTo("15000");
        assertThat(RevenueLedger.ahamoveCost("INTERNAL", "ASSIGNED", BigDecimal.valueOf(38_500))).isEqualByComparingTo("38500");
        assertThat(
            RevenueLedger.netFare(BigDecimal.valueOf(55_000), "INTERNAL", "ASSIGNED", BigDecimal.valueOf(38_500))
        ).isEqualByComparingTo("16500");
    }

    @Test
    void missingFareMeansNoRevenue() {
        Map<Long, BigDecimal> s = RevenueLedger.allocateFare(null, List.of(line(1, 10, 20_000)));
        assertThat(s.get(1L)).isEqualByComparingTo("0");
    }
}
