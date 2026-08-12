package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OrderEventTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static OrderEvent getOrderEventSample1() {
        return new OrderEvent().id(1L).action("action1").detail("detail1").actorUsername("actorUsername1");
    }

    public static OrderEvent getOrderEventSample2() {
        return new OrderEvent().id(2L).action("action2").detail("detail2").actorUsername("actorUsername2");
    }

    public static OrderEvent getOrderEventRandomSampleGenerator() {
        return new OrderEvent()
            .id(longCount.incrementAndGet())
            .action(UUID.randomUUID().toString())
            .detail(UUID.randomUUID().toString())
            .actorUsername(UUID.randomUUID().toString());
    }
}
