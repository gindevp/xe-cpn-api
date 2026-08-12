package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OrderFareAdjustmentRequestTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static OrderFareAdjustmentRequest getOrderFareAdjustmentRequestSample1() {
        return new OrderFareAdjustmentRequest()
            .id(1L)
            .reason("reason1")
            .requestedByUsername("requestedByUsername1")
            .approvedByUsername("approvedByUsername1")
            .rejectedByUsername("rejectedByUsername1");
    }

    public static OrderFareAdjustmentRequest getOrderFareAdjustmentRequestSample2() {
        return new OrderFareAdjustmentRequest()
            .id(2L)
            .reason("reason2")
            .requestedByUsername("requestedByUsername2")
            .approvedByUsername("approvedByUsername2")
            .rejectedByUsername("rejectedByUsername2");
    }

    public static OrderFareAdjustmentRequest getOrderFareAdjustmentRequestRandomSampleGenerator() {
        return new OrderFareAdjustmentRequest()
            .id(longCount.incrementAndGet())
            .reason(UUID.randomUUID().toString())
            .requestedByUsername(UUID.randomUUID().toString())
            .approvedByUsername(UUID.randomUUID().toString())
            .rejectedByUsername(UUID.randomUUID().toString());
    }
}
