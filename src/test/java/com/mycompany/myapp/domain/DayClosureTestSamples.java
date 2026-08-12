package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DayClosureTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static DayClosure getDayClosureSample1() {
        return new DayClosure().id(1L).confirmedByUsername("confirmedByUsername1").reopenedByUsername("reopenedByUsername1");
    }

    public static DayClosure getDayClosureSample2() {
        return new DayClosure().id(2L).confirmedByUsername("confirmedByUsername2").reopenedByUsername("reopenedByUsername2");
    }

    public static DayClosure getDayClosureRandomSampleGenerator() {
        return new DayClosure()
            .id(longCount.incrementAndGet())
            .confirmedByUsername(UUID.randomUUID().toString())
            .reopenedByUsername(UUID.randomUUID().toString());
    }
}
