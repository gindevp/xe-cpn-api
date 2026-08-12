package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class PricingChangeLogTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static PricingChangeLog getPricingChangeLogSample1() {
        return new PricingChangeLog().id(1L).changedByUsername("changedByUsername1");
    }

    public static PricingChangeLog getPricingChangeLogSample2() {
        return new PricingChangeLog().id(2L).changedByUsername("changedByUsername2");
    }

    public static PricingChangeLog getPricingChangeLogRandomSampleGenerator() {
        return new PricingChangeLog().id(longCount.incrementAndGet()).changedByUsername(UUID.randomUUID().toString());
    }
}
