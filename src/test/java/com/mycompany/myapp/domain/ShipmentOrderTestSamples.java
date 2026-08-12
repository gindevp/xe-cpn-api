package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ShipmentOrderTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static ShipmentOrder getShipmentOrderSample1() {
        return new ShipmentOrder()
            .id(1L)
            .orderCode("orderCode1")
            .draftCode("draftCode1")
            .senderName("senderName1")
            .senderPhone("senderPhone1")
            .receiverName("receiverName1")
            .receiverPhone("receiverPhone1")
            .deliveryAddress("deliveryAddress1")
            .pickupAddress("pickupAddress1")
            .pickupStaffUsername("pickupStaffUsername1")
            .receiverActualName("receiverActualName1")
            .receiverActualPhone("receiverActualPhone1")
            .quantity(1)
            .dimensionsText("dimensionsText1")
            .shelfNumber(1)
            .cancelReason("cancelReason1")
            .labelReprintCount(1)
            .failCount(1)
            .partnerCode("partnerCode1");
    }

    public static ShipmentOrder getShipmentOrderSample2() {
        return new ShipmentOrder()
            .id(2L)
            .orderCode("orderCode2")
            .draftCode("draftCode2")
            .senderName("senderName2")
            .senderPhone("senderPhone2")
            .receiverName("receiverName2")
            .receiverPhone("receiverPhone2")
            .deliveryAddress("deliveryAddress2")
            .pickupAddress("pickupAddress2")
            .pickupStaffUsername("pickupStaffUsername2")
            .receiverActualName("receiverActualName2")
            .receiverActualPhone("receiverActualPhone2")
            .quantity(2)
            .dimensionsText("dimensionsText2")
            .shelfNumber(2)
            .cancelReason("cancelReason2")
            .labelReprintCount(2)
            .failCount(2)
            .partnerCode("partnerCode2");
    }

    public static ShipmentOrder getShipmentOrderRandomSampleGenerator() {
        return new ShipmentOrder()
            .id(longCount.incrementAndGet())
            .orderCode(UUID.randomUUID().toString())
            .draftCode(UUID.randomUUID().toString())
            .senderName(UUID.randomUUID().toString())
            .senderPhone(UUID.randomUUID().toString())
            .receiverName(UUID.randomUUID().toString())
            .receiverPhone(UUID.randomUUID().toString())
            .deliveryAddress(UUID.randomUUID().toString())
            .pickupAddress(UUID.randomUUID().toString())
            .pickupStaffUsername(UUID.randomUUID().toString())
            .receiverActualName(UUID.randomUUID().toString())
            .receiverActualPhone(UUID.randomUUID().toString())
            .quantity(intCount.incrementAndGet())
            .dimensionsText(UUID.randomUUID().toString())
            .shelfNumber(intCount.incrementAndGet())
            .cancelReason(UUID.randomUUID().toString())
            .labelReprintCount(intCount.incrementAndGet())
            .failCount(intCount.incrementAndGet())
            .partnerCode(UUID.randomUUID().toString());
    }
}
