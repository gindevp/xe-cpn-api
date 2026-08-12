package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class DoorFeeRuleTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static DoorFeeRule getDoorFeeRuleSample1() {
        return new DoorFeeRule().id(1L);
    }

    public static DoorFeeRule getDoorFeeRuleSample2() {
        return new DoorFeeRule().id(2L);
    }

    public static DoorFeeRule getDoorFeeRuleRandomSampleGenerator() {
        return new DoorFeeRule().id(longCount.incrementAndGet());
    }
}
