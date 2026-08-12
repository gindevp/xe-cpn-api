package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OrderPaymentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static OrderPayment getOrderPaymentSample1() {
        return new OrderPayment().id(1L).note("note1").collectorUsername("collectorUsername1");
    }

    public static OrderPayment getOrderPaymentSample2() {
        return new OrderPayment().id(2L).note("note2").collectorUsername("collectorUsername2");
    }

    public static OrderPayment getOrderPaymentRandomSampleGenerator() {
        return new OrderPayment()
            .id(longCount.incrementAndGet())
            .note(UUID.randomUUID().toString())
            .collectorUsername(UUID.randomUUID().toString());
    }
}
