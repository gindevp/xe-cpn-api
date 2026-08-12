package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OrderDeliveryAttemptTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static OrderDeliveryAttempt getOrderDeliveryAttemptSample1() {
        return new OrderDeliveryAttempt().id(1L).attemptNo(1).reason("reason1").handledByUsername("handledByUsername1");
    }

    public static OrderDeliveryAttempt getOrderDeliveryAttemptSample2() {
        return new OrderDeliveryAttempt().id(2L).attemptNo(2).reason("reason2").handledByUsername("handledByUsername2");
    }

    public static OrderDeliveryAttempt getOrderDeliveryAttemptRandomSampleGenerator() {
        return new OrderDeliveryAttempt()
            .id(longCount.incrementAndGet())
            .attemptNo(intCount.incrementAndGet())
            .reason(UUID.randomUUID().toString())
            .handledByUsername(UUID.randomUUID().toString());
    }
}
