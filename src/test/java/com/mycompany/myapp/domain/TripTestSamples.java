package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TripTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Trip getTripSample1() {
        return new Trip().id(1L).tripCode("tripCode1").loadedCount(1).scannedCount(1).forceCloseReason("forceCloseReason1");
    }

    public static Trip getTripSample2() {
        return new Trip().id(2L).tripCode("tripCode2").loadedCount(2).scannedCount(2).forceCloseReason("forceCloseReason2");
    }

    public static Trip getTripRandomSampleGenerator() {
        return new Trip()
            .id(longCount.incrementAndGet())
            .tripCode(UUID.randomUUID().toString())
            .loadedCount(intCount.incrementAndGet())
            .scannedCount(intCount.incrementAndGet())
            .forceCloseReason(UUID.randomUUID().toString());
    }
}
