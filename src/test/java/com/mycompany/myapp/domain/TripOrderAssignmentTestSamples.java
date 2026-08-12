package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class TripOrderAssignmentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static TripOrderAssignment getTripOrderAssignmentSample1() {
        return new TripOrderAssignment().id(1L).remark("remark1");
    }

    public static TripOrderAssignment getTripOrderAssignmentSample2() {
        return new TripOrderAssignment().id(2L).remark("remark2");
    }

    public static TripOrderAssignment getTripOrderAssignmentRandomSampleGenerator() {
        return new TripOrderAssignment().id(longCount.incrementAndGet()).remark(UUID.randomUUID().toString());
    }
}
