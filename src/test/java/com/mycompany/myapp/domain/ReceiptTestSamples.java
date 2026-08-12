package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ReceiptTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Receipt getReceiptSample1() {
        return new Receipt()
            .id(1L)
            .receiptCode("receiptCode1")
            .payerName("payerName1")
            .payerCode("payerCode1")
            .createdByUsername("createdByUsername1");
    }

    public static Receipt getReceiptSample2() {
        return new Receipt()
            .id(2L)
            .receiptCode("receiptCode2")
            .payerName("payerName2")
            .payerCode("payerCode2")
            .createdByUsername("createdByUsername2");
    }

    public static Receipt getReceiptRandomSampleGenerator() {
        return new Receipt()
            .id(longCount.incrementAndGet())
            .receiptCode(UUID.randomUUID().toString())
            .payerName(UUID.randomUUID().toString())
            .payerCode(UUID.randomUUID().toString())
            .createdByUsername(UUID.randomUUID().toString());
    }
}
