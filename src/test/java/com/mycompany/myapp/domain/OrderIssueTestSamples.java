package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OrderIssueTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static OrderIssue getOrderIssueSample1() {
        return new OrderIssue()
            .id(1L)
            .reason("reason1")
            .openedByUsername("openedByUsername1")
            .resolvedByUsername("resolvedByUsername1")
            .resolutionNote("resolutionNote1");
    }

    public static OrderIssue getOrderIssueSample2() {
        return new OrderIssue()
            .id(2L)
            .reason("reason2")
            .openedByUsername("openedByUsername2")
            .resolvedByUsername("resolvedByUsername2")
            .resolutionNote("resolutionNote2");
    }

    public static OrderIssue getOrderIssueRandomSampleGenerator() {
        return new OrderIssue()
            .id(longCount.incrementAndGet())
            .reason(UUID.randomUUID().toString())
            .openedByUsername(UUID.randomUUID().toString())
            .resolvedByUsername(UUID.randomUUID().toString())
            .resolutionNote(UUID.randomUUID().toString());
    }
}
