package com.mycompany.myapp.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.service.finance.ReceiptSettlement;
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
    void legacyOrderWithoutGoodsFareDerivesIt() {
        ShipmentOrder o = order();
        o.setGoodsFareAmount(null);
        assertThat(RevenueReportService.feesOf(o).goods()).isEqualByComparingTo("60000");
    }

    @Test
    void paymentSideFollowsReceiptSettlement() {
        assertThat(ReceiptSettlement.isDeliverySidePayment(PaymentKind.SAU, "POD QUAY")).isTrue();
        assertThat(ReceiptSettlement.isDeliverySidePayment(PaymentKind.SAU, "RECEIPT")).isTrue();
        assertThat(ReceiptSettlement.isDeliverySidePayment(PaymentKind.SAU, "RECEIPT_SENDER")).isFalse();
        assertThat(ReceiptSettlement.isDeliverySidePayment(PaymentKind.TRUOC, null)).isFalse();
    }
}
