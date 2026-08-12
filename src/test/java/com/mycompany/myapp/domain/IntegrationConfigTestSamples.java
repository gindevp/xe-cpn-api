package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class IntegrationConfigTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static IntegrationConfig getIntegrationConfigSample1() {
        return new IntegrationConfig()
            .id(1L)
            .ahamoveToken("ahamoveToken1")
            .grabToken("grabToken1")
            .xanhsmToken("xanhsmToken1")
            .distanceApiToken("distanceApiToken1")
            .telegramToken("telegramToken1")
            .telegramChatId("telegramChatId1")
            .webhookUrl("webhookUrl1")
            .webhookSecret("webhookSecret1");
    }

    public static IntegrationConfig getIntegrationConfigSample2() {
        return new IntegrationConfig()
            .id(2L)
            .ahamoveToken("ahamoveToken2")
            .grabToken("grabToken2")
            .xanhsmToken("xanhsmToken2")
            .distanceApiToken("distanceApiToken2")
            .telegramToken("telegramToken2")
            .telegramChatId("telegramChatId2")
            .webhookUrl("webhookUrl2")
            .webhookSecret("webhookSecret2");
    }

    public static IntegrationConfig getIntegrationConfigRandomSampleGenerator() {
        return new IntegrationConfig()
            .id(longCount.incrementAndGet())
            .ahamoveToken(UUID.randomUUID().toString())
            .grabToken(UUID.randomUUID().toString())
            .xanhsmToken(UUID.randomUUID().toString())
            .distanceApiToken(UUID.randomUUID().toString())
            .telegramToken(UUID.randomUUID().toString())
            .telegramChatId(UUID.randomUUID().toString())
            .webhookUrl(UUID.randomUUID().toString())
            .webhookSecret(UUID.randomUUID().toString());
    }
}
