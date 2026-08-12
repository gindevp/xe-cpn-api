package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ProductPriceRuleTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static ProductPriceRule getProductPriceRuleSample1() {
        return new ProductPriceRule().id(1L).groupName("groupName1").productName("productName1").note("note1");
    }

    public static ProductPriceRule getProductPriceRuleSample2() {
        return new ProductPriceRule().id(2L).groupName("groupName2").productName("productName2").note("note2");
    }

    public static ProductPriceRule getProductPriceRuleRandomSampleGenerator() {
        return new ProductPriceRule()
            .id(longCount.incrementAndGet())
            .groupName(UUID.randomUUID().toString())
            .productName(UUID.randomUUID().toString())
            .note(UUID.randomUUID().toString());
    }
}
