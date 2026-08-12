package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PricingRuleTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static PricingRule getPricingRuleSample1() {
        return new PricingRule().id(1L).ruleCode("ruleCode1").tierLabel("tierLabel1").dimDivisor(1).stepGram(1);
    }

    public static PricingRule getPricingRuleSample2() {
        return new PricingRule().id(2L).ruleCode("ruleCode2").tierLabel("tierLabel2").dimDivisor(2).stepGram(2);
    }

    public static PricingRule getPricingRuleRandomSampleGenerator() {
        return new PricingRule()
            .id(longCount.incrementAndGet())
            .ruleCode(UUID.randomUUID().toString())
            .tierLabel(UUID.randomUUID().toString())
            .dimDivisor(intCount.incrementAndGet())
            .stepGram(intCount.incrementAndGet());
    }
}
