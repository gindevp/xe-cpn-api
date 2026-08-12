package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OfficeTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Office getOfficeSample1() {
        return new Office().id(1L).code("code1").name("name1");
    }

    public static Office getOfficeSample2() {
        return new Office().id(2L).code("code2").name("name2");
    }

    public static Office getOfficeRandomSampleGenerator() {
        return new Office().id(longCount.incrementAndGet()).code(UUID.randomUUID().toString()).name(UUID.randomUUID().toString());
    }
}
