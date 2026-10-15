package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SurchargeFeeServiceTest {

    private SurchargePolicyRepository policies;
    private OrderEventRepository events;
    private SurchargeFeeService service;

    @BeforeEach
    void setUp() {
        policies = mock(SurchargePolicyRepository.class);
        events = mock(OrderEventRepository.class);
        service = new SurchargeFeeService(mock(ShipmentOrderRepository.class), events, policies);
    }

    @Test
    void storageChargesCalendarDaysAfterFreePeriod() {
        // 15:00 giờ VN ngày 01/10
        Instant arrived = Instant.parse("2026-10-01T08:00:00Z");
        assertThat(SurchargeFeeService.storageAmount(arrived, arrived.plus(Duration.ofHours(1)), 0, d("10000"))).isEqualByComparingTo(
            "10000"
        );
        // 23:59 VN ngày 03/10: vẫn trong 3 ngày miễn phí
        assertThat(SurchargeFeeService.storageAmount(arrived, Instant.parse("2026-10-03T16:59:00Z"), 3, d("10000"))).isEqualByComparingTo(
            "0"
        );
        // 00:01 VN ngày 04/10: sang ngày thứ 4
        assertThat(SurchargeFeeService.storageAmount(arrived, Instant.parse("2026-10-03T17:01:00Z"), 3, d("10000"))).isEqualByComparingTo(
            "10000"
        );
    }

    @Test
    void storageFeeDoesNotStepMidDay() {
        Instant arrived = Instant.parse("2026-10-01T08:00:00Z");
        // 00:10 và 16:00 VN cùng ngày 03/10 → cùng số tiền
        BigDecimal morning = SurchargeFeeService.storageAmount(arrived, Instant.parse("2026-10-02T17:10:00Z"), 1, d("3000"));
        BigDecimal afternoon = SurchargeFeeService.storageAmount(arrived, Instant.parse("2026-10-03T09:00:00Z"), 1, d("3000"));
        assertThat(morning).isEqualByComparingTo("6000");
        assertThat(afternoon).isEqualByComparingTo("6000");
    }

    @Test
    void returnFeeIsPercentOfGoodsAndDoorFees() {
        ShipmentOrder order = sittingOrder();
        order.setGoodsFareAmount(d("100000"));
        order.setPickupFeeAmount(d("20000"));
        order.setDeliveryFeeAmount(d("0"));
        assertThat(SurchargeFeeService.returnAmount(SurchargeFeeService.transportBase(order), d("10"))).isEqualByComparingTo("12000");
    }

    @Test
    void refreshAddsStorageOntoFare() {
        ShipmentOrder order = sittingOrder();
        order.setFareAmount(d("100000"));
        stubPolicy(true, 1, "10000", false, "0");
        OrderEvent arrived = new OrderEvent();
        arrived.setAction("SCAN_IN");
        // 01:00 giờ VN cách đây 2 ngày → hôm nay là ngày lịch thứ 3
        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
        arrived.setEventAt(LocalDate.now(vn).minusDays(2).atStartOfDay(vn).plusHours(1).toInstant());
        when(events.findByOrder_IdOrderByEventAtAsc(1L)).thenReturn(List.of(arrived));

        assertThat(service.refreshStorageFee(order)).isTrue();
        assertThat(order.getStorageFeeAmount()).isEqualByComparingTo("20000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("120000");
        assertThat(service.refreshStorageFee(order)).isFalse();
    }

    @Test
    void disabledStorageDoesNotDropFeeBelowAmountAlreadyPaid() {
        ShipmentOrder order = sittingOrder();
        order.setStorageFeeAmount(d("5000"));
        order.setFareAmount(d("10000"));
        order.setPaidAmount(d("10000"));
        stubPolicy(false, 0, "0", false, "0");

        assertThat(service.refreshStorageFee(order)).isFalse();
        assertThat(order.getStorageFeeAmount()).isEqualByComparingTo("5000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void outForDeliveryDoesNotAccrue() {
        ShipmentOrder order = sittingOrder();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setForwardStage(ForwardStage.DELIVERING);
        stubPolicy(true, 0, "10000", false, "0");

        assertThat(service.refreshStorageFee(order)).isFalse();
        assertThat(order.getStorageFeeAmount()).isNull();
    }

    @Test
    void returnFeeAppliesOnceAndCancelRemovesIt() {
        ShipmentOrder order = sittingOrder();
        order.setFareAmount(d("100000"));
        order.setGoodsFareAmount(d("100000"));
        stubPolicy(false, 0, "0", true, "10");

        assertThat(service.applyReturnFee(order)).isTrue();
        assertThat(order.getReturnFeeAmount()).isEqualByComparingTo("10000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("110000");
        assertThat(service.applyReturnFee(order)).isFalse();

        assertThat(service.revertReturnFee(order)).isTrue();
        assertThat(order.getReturnFeeAmount()).isEqualByComparingTo("0");
        assertThat(order.getFareAmount()).isEqualByComparingTo("100000");
    }

    private void stubPolicy(boolean storage, int freeDays, String perDay, boolean refund, String percent) {
        SurchargePolicy policy = new SurchargePolicy();
        policy.setStorageEnabled(storage);
        policy.setStorageFreeDays(freeDays);
        policy.setStorageFeePerDay(d(perDay));
        policy.setRefundEnabled(refund);
        policy.setRefundPercent(d(percent));
        when(policies.findAll()).thenReturn(List.of(policy));
    }

    private static ShipmentOrder sittingOrder() {
        ShipmentOrder order = new ShipmentOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.AT_DEST);
        order.setForwardStage(ForwardStage.DEST_WH_IN);
        return order;
    }

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }
}
