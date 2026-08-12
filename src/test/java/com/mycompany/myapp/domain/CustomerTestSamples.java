package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CustomerTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Customer getCustomerSample1() {
        return new Customer().id(1L).phone("phone1").name("name1").orderCount(1);
    }

    public static Customer getCustomerSample2() {
        return new Customer().id(2L).phone("phone2").name("name2").orderCount(2);
    }

    public static Customer getCustomerRandomSampleGenerator() {
        return new Customer()
            .id(longCount.incrementAndGet())
            .phone(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .orderCount(intCount.incrementAndGet());
    }
}
