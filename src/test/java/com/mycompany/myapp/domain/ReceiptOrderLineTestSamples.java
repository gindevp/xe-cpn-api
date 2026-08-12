package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ReceiptOrderLineTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static ReceiptOrderLine getReceiptOrderLineSample1() {
        return new ReceiptOrderLine().id(1L);
    }

    public static ReceiptOrderLine getReceiptOrderLineSample2() {
        return new ReceiptOrderLine().id(2L);
    }

    public static ReceiptOrderLine getReceiptOrderLineRandomSampleGenerator() {
        return new ReceiptOrderLine().id(longCount.incrementAndGet());
    }
}
