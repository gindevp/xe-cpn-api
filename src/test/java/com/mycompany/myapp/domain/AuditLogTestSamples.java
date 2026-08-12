package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class AuditLogTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static AuditLog getAuditLogSample1() {
        return new AuditLog()
            .id(1L)
            .action("action1")
            .entityType("entityType1")
            .entityId("entityId1")
            .detail("detail1")
            .actedByUsername("actedByUsername1");
    }

    public static AuditLog getAuditLogSample2() {
        return new AuditLog()
            .id(2L)
            .action("action2")
            .entityType("entityType2")
            .entityId("entityId2")
            .detail("detail2")
            .actedByUsername("actedByUsername2");
    }

    public static AuditLog getAuditLogRandomSampleGenerator() {
        return new AuditLog()
            .id(longCount.incrementAndGet())
            .action(UUID.randomUUID().toString())
            .entityType(UUID.randomUUID().toString())
            .entityId(UUID.randomUUID().toString())
            .detail(UUID.randomUUID().toString())
            .actedByUsername(UUID.randomUUID().toString());
    }
}
