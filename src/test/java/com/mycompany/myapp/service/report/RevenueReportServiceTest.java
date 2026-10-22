package com.mycompany.myapp.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RevenueReportServiceTest {

    private static BigDecimal d(long v) {
        return BigDecimal.valueOf(v);
    }

    /** fare 100k = hàng 60k + tận nơi 20k + lấy 10k + phí COD 8k + KBGT 7k − giảm 5k. */
    private static ShipmentOrder order() {
        ShipmentOrder o = new ShipmentOrder();
        o.setFareAmount(d(100_000));
        o.setGoodsFareAmount(d(60_000));
        o.setDeliveryFeeAmount(d(20_000));
        o.setPickupFeeAmount(d(10_000));
        o.setCodFeeAmount(d(8_000));
        o.setDeclaredFeeAmount(d(7_000));
        o.setDiscountAmount(d(5_000));
        return o;
    }

    @Test
    void fullCollectionKeepsOrderFees() {
        RevenueReportService.Share s = RevenueReportService.share(RevenueReportService.feesOf(order()), d(100_000));
        assertThat(s.goods()).isEqualByComparingTo("60000");
        assertThat(s.delivery()).isEqualByComparingTo("20000");
        assertThat(s.pickup()).isEqualByComparingTo("10000");
        assertThat(s.codFee()).isEqualByComparingTo("8000");
        assertThat(s.declared()).isEqualByComparingTo("7000");
        assertThat(s.discount()).isEqualByComparingTo("5000");
        assertThat(s.total()).isEqualByComparingTo("100000");
    }

    @Test
    void partialCollectionScalesFeesAndColumnsAddUpToCollected() {
        RevenueReportService.Fees fees = RevenueReportService.feesOf(order());
        RevenueReportService.Share sender = RevenueReportService.share(fees, d(33_333));
        RevenueReportService.Share delivery = RevenueReportService.share(fees, d(66_667));
        for (RevenueReportService.Share s : new RevenueReportService.Share[] { sender, delivery }) {
            BigDecimal sum = s.goods().add(s.delivery()).add(s.pickup()).add(s.codFee()).add(s.declared()).subtract(s.discount());
            assertThat(sum).isEqualByComparingTo(s.total());
        }
        assertThat(sender.delivery()).isEqualByComparingTo("6667");
        assertThat(sender.total().add(delivery.total())).isEqualByComparingTo("100000");
    }

    @Test
    void nothingCollectedHasNoRevenue() {
        assertThat(RevenueReportService.share(RevenueReportService.feesOf(order()), BigDecimal.ZERO)).isNull();
    }

    @Test
    void inconsistentLegacyOrderNeverGoesNegative() {
        ShipmentOrder o = new ShipmentOrder();
        o.setFareAmount(d(40_000));
        o.setDeliveryFeeAmount(d(50_000));
        RevenueReportService.Share s = RevenueReportService.share(RevenueReportService.feesOf(o), d(40_000));
        assertThat(s.goods()).isEqualByComparingTo("0");
        assertThat(s.delivery()).isEqualByComparingTo("40000");
        assertThat(s.total()).isEqualByComparingTo("40000");
    }

    @Test
    void shippingFee_oneOfPartnerExternalOrInternal() {
        ShipmentOrder aha = order();
        aha.setPartnerCode("AHAMOVE");
        aha.setPartnerFeeAmount(d(35_000));
        assertThat(RevenueReportService.shippingFee(aha)).isEqualByComparingTo("35000");

        ShipmentOrder external = order();
        external.setPartnerCode("EXTERNAL");
        external.setPartnerFeeAmount(d(15_000));
        assertThat(RevenueReportService.shippingFee(external)).isEqualByComparingTo("15000");

        ShipmentOrder internal = order();
        internal.setPartnerCode("INTERNAL");
        internal.setPartnerFeeAmount(d(14_000));
        assertThat(RevenueReportService.shippingFee(internal)).isEqualByComparingTo("14000");

        ShipmentOrder none = order();
        assertThat(RevenueReportService.shippingFee(none)).isEqualByComparingTo("0");
    }

    @Test
    void collectedNeverExceedsRevenue() {
        assertThat(RevenueLedger.collectedOf(d(65_000), d(100_000))).isEqualByComparingTo("65000");
        assertThat(RevenueLedger.collectedOf(d(100_000), d(40_000))).isEqualByComparingTo("40000");
        assertThat(RevenueLedger.collectedOf(d(100_000), null)).isEqualByComparingTo("0");
    }

    @Test
    void storageAndReturnFeesHaveOwnColumnsAndTotalUnchanged() {
        ShipmentOrder o = order();
        o.setFareAmount(d(112_000));
        o.setStorageFeeAmount(d(9_000));
        o.setReturnFeeAmount(d(3_000));
        RevenueReportService.Share s = RevenueReportService.share(RevenueReportService.feesOf(o), d(112_000));
        assertThat(s.goods()).isEqualByComparingTo("60000");
        assertThat(s.storage()).isEqualByComparingTo("9000");
        assertThat(s.returnFee()).isEqualByComparingTo("3000");
        assertThat(s.total()).isEqualByComparingTo("112000");

        RevenueReportService.Share half = RevenueReportService.share(RevenueReportService.feesOf(o), d(56_000));
        BigDecimal sum = half
            .goods()
            .add(half.delivery())
            .add(half.pickup())
            .add(half.codFee())
            .add(half.declared())
            .add(half.storage())
            .add(half.returnFee())
            .subtract(half.discount());
        assertThat(sum).isEqualByComparingTo("56000");
        assertThat(half.storage()).isEqualByComparingTo("4500");
    }

    @Test
    void legacyOrderWithoutGoodsFareDerivesIt() {
        ShipmentOrder o = order();
        o.setGoodsFareAmount(null);
        assertThat(RevenueReportService.feesOf(o).goods()).isEqualByComparingTo("60000");
    }
}
