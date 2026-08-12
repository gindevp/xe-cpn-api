package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class StaffProfileTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static StaffProfile getStaffProfileSample1() {
        return new StaffProfile().id(1L).staffCode("staffCode1").userLogin("userLogin1").displayName("displayName1");
    }

    public static StaffProfile getStaffProfileSample2() {
        return new StaffProfile().id(2L).staffCode("staffCode2").userLogin("userLogin2").displayName("displayName2");
    }

    public static StaffProfile getStaffProfileRandomSampleGenerator() {
        return new StaffProfile()
            .id(longCount.incrementAndGet())
            .staffCode(UUID.randomUUID().toString())
            .userLogin(UUID.randomUUID().toString())
            .displayName(UUID.randomUUID().toString());
    }
}
