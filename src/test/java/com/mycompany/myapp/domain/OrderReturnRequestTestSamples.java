package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OrderReturnRequestTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static OrderReturnRequest getOrderReturnRequestSample1() {
        return new OrderReturnRequest()
            .id(1L)
            .reason("reason1")
            .requestedByUsername("requestedByUsername1")
            .decidedByUsername("decidedByUsername1")
            .decisionNote("decisionNote1");
    }

    public static OrderReturnRequest getOrderReturnRequestSample2() {
        return new OrderReturnRequest()
            .id(2L)
            .reason("reason2")
            .requestedByUsername("requestedByUsername2")
            .decidedByUsername("decidedByUsername2")
            .decisionNote("decisionNote2");
    }

    public static OrderReturnRequest getOrderReturnRequestRandomSampleGenerator() {
        return new OrderReturnRequest()
            .id(longCount.incrementAndGet())
            .reason(UUID.randomUUID().toString())
            .requestedByUsername(UUID.randomUUID().toString())
            .decidedByUsername(UUID.randomUUID().toString())
            .decisionNote(UUID.randomUUID().toString());
    }
}
