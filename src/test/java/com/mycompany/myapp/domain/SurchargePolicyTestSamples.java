package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SurchargePolicyTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static SurchargePolicy getSurchargePolicySample1() {
        return new SurchargePolicy().id(1L).storageFreeDays(1);
    }

    public static SurchargePolicy getSurchargePolicySample2() {
        return new SurchargePolicy().id(2L).storageFreeDays(2);
    }

    public static SurchargePolicy getSurchargePolicyRandomSampleGenerator() {
        return new SurchargePolicy().id(longCount.incrementAndGet()).storageFreeDays(intCount.incrementAndGet());
    }
}
