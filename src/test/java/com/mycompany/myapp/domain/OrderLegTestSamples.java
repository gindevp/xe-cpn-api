package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OrderLegTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static OrderLeg getOrderLegSample1() {
        return new OrderLeg().id(1L).legIndex(1);
    }

    public static OrderLeg getOrderLegSample2() {
        return new OrderLeg().id(2L).legIndex(2);
    }

    public static OrderLeg getOrderLegRandomSampleGenerator() {
        return new OrderLeg().id(longCount.incrementAndGet()).legIndex(intCount.incrementAndGet());
    }
}
