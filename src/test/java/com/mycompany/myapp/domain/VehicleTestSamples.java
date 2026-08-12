package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class VehicleTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Vehicle getVehicleSample1() {
        return new Vehicle().id(1L).plateNumber("plateNumber1");
    }

    public static Vehicle getVehicleSample2() {
        return new Vehicle().id(2L).plateNumber("plateNumber2");
    }

    public static Vehicle getVehicleRandomSampleGenerator() {
        return new Vehicle().id(longCount.incrementAndGet()).plateNumber(UUID.randomUUID().toString());
    }
}
