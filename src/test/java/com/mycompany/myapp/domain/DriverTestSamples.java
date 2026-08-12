package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DriverTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Driver getDriverSample1() {
        return new Driver().id(1L).driverCode("driverCode1").fullName("fullName1").phone("phone1");
    }

    public static Driver getDriverSample2() {
        return new Driver().id(2L).driverCode("driverCode2").fullName("fullName2").phone("phone2");
    }

    public static Driver getDriverRandomSampleGenerator() {
        return new Driver()
            .id(longCount.incrementAndGet())
            .driverCode(UUID.randomUUID().toString())
            .fullName(UUID.randomUUID().toString())
            .phone(UUID.randomUUID().toString());
    }
}
