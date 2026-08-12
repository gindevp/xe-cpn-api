package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OrderPodPhotoTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static OrderPodPhoto getOrderPodPhotoSample1() {
        return new OrderPodPhoto().id(1L).photoUrl("photoUrl1").capturedByUsername("capturedByUsername1").sequenceNo(1);
    }

    public static OrderPodPhoto getOrderPodPhotoSample2() {
        return new OrderPodPhoto().id(2L).photoUrl("photoUrl2").capturedByUsername("capturedByUsername2").sequenceNo(2);
    }

    public static OrderPodPhoto getOrderPodPhotoRandomSampleGenerator() {
        return new OrderPodPhoto()
            .id(longCount.incrementAndGet())
            .photoUrl(UUID.randomUUID().toString())
            .capturedByUsername(UUID.randomUUID().toString())
            .sequenceNo(intCount.incrementAndGet());
    }
}
