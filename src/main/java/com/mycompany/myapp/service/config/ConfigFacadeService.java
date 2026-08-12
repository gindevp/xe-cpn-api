package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfigFacadeService {

    private final SurchargePolicyRepository surchargePolicyRepository;
    private final IntegrationConfigRepository integrationConfigRepository;

    public ConfigFacadeService(
        SurchargePolicyRepository surchargePolicyRepository,
        IntegrationConfigRepository integrationConfigRepository
    ) {
        this.surchargePolicyRepository = surchargePolicyRepository;
        this.integrationConfigRepository = integrationConfigRepository;
    }

    @Transactional(readOnly = true)
    public SurchargePolicy getSurchargePolicy() {
        return surchargePolicyRepository.findAll().stream().findFirst().orElseGet(this::defaultSurcharge);
    }

    public SurchargePolicy putSurchargePolicy(SurchargePolicy incoming) {
        SurchargePolicy current = surchargePolicyRepository.findAll().stream().findFirst().orElse(null);
        if (current == null) {
            SurchargePolicy created = merge(defaultSurcharge(), incoming);
            created.setId(null);
            created.setUpdatedAt(Instant.now());
            return surchargePolicyRepository.save(created);
        }
        SurchargePolicy merged = merge(current, incoming);
        merged.setUpdatedAt(Instant.now());
        return surchargePolicyRepository.save(merged);
    }

    @Transactional(readOnly = true)
    public IntegrationConfig getIntegrationConfig() {
        return integrationConfigRepository.findAll().stream().findFirst().orElseGet(IntegrationConfig::new);
    }

    public IntegrationConfig putIntegrationConfig(IntegrationConfig incoming) {
        IntegrationConfig current = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        if (current == null) {
            incoming.setId(null);
            return integrationConfigRepository.save(incoming);
        }
        if (incoming.getAhamoveToken() != null) current.setAhamoveToken(incoming.getAhamoveToken());
        if (incoming.getGrabToken() != null) current.setGrabToken(incoming.getGrabToken());
        if (incoming.getXanhsmToken() != null) current.setXanhsmToken(incoming.getXanhsmToken());
        if (incoming.getDistanceApiToken() != null) current.setDistanceApiToken(incoming.getDistanceApiToken());
        if (incoming.getTelegramToken() != null) current.setTelegramToken(incoming.getTelegramToken());
        if (incoming.getTelegramChatId() != null) current.setTelegramChatId(incoming.getTelegramChatId());
        if (incoming.getWebhookUrl() != null) current.setWebhookUrl(incoming.getWebhookUrl());
        if (incoming.getWebhookSecret() != null) current.setWebhookSecret(incoming.getWebhookSecret());
        return integrationConfigRepository.save(current);
    }

    public Map<String, Object> testIntegration() {
        IntegrationConfig cfg = getIntegrationConfig();
        Map<String, Object> out = new HashMap<>();
        out.put("ok", true);
        out.put("ahamoveConfigured", notBlank(cfg.getAhamoveToken()));
        out.put("grabConfigured", notBlank(cfg.getGrabToken()));
        out.put("xanhsmConfigured", notBlank(cfg.getXanhsmToken()));
        out.put("telegramConfigured", notBlank(cfg.getTelegramToken()));
        out.put("webhookConfigured", notBlank(cfg.getWebhookUrl()));
        out.put("testedBy", SecurityUtils.getCurrentUserLogin().orElse("system"));
        out.put("testedAt", Instant.now().toString());
        return out;
    }

    private SurchargePolicy merge(SurchargePolicy base, SurchargePolicy incoming) {
        if (incoming.getHomeDeliveryEnabled() != null) base.setHomeDeliveryEnabled(incoming.getHomeDeliveryEnabled());
        if (incoming.getDefaultHomeDeliveryAmount() != null) base.setDefaultHomeDeliveryAmount(incoming.getDefaultHomeDeliveryAmount());
        if (incoming.getCodEnabled() != null) base.setCodEnabled(incoming.getCodEnabled());
        if (incoming.getCodPercent() != null) base.setCodPercent(incoming.getCodPercent());
        if (incoming.getCodMinFee() != null) base.setCodMinFee(incoming.getCodMinFee());
        if (incoming.getStorageEnabled() != null) base.setStorageEnabled(incoming.getStorageEnabled());
        if (incoming.getStorageFreeDays() != null) base.setStorageFreeDays(incoming.getStorageFreeDays());
        if (incoming.getStorageFeePerDay() != null) base.setStorageFeePerDay(incoming.getStorageFeePerDay());
        if (incoming.getInsuranceEnabled() != null) base.setInsuranceEnabled(incoming.getInsuranceEnabled());
        if (incoming.getInsuranceThreshold() != null) base.setInsuranceThreshold(incoming.getInsuranceThreshold());
        if (incoming.getInsurancePercentUnder() != null) base.setInsurancePercentUnder(incoming.getInsurancePercentUnder());
        if (incoming.getInsurancePercentOver() != null) base.setInsurancePercentOver(incoming.getInsurancePercentOver());
        if (incoming.getRefundEnabled() != null) base.setRefundEnabled(incoming.getRefundEnabled());
        if (incoming.getRefundPercent() != null) base.setRefundPercent(incoming.getRefundPercent());
        return base;
    }

    private SurchargePolicy defaultSurcharge() {
        SurchargePolicy p = new SurchargePolicy();
        p.setHomeDeliveryEnabled(true);
        p.setDefaultHomeDeliveryAmount(BigDecimal.valueOf(10_000));
        p.setCodEnabled(true);
        p.setCodPercent(BigDecimal.ONE);
        p.setCodMinFee(BigDecimal.valueOf(5_000));
        p.setStorageEnabled(false);
        p.setStorageFreeDays(3);
        p.setStorageFeePerDay(BigDecimal.valueOf(5_000));
        p.setInsuranceEnabled(false);
        p.setInsuranceThreshold(BigDecimal.valueOf(1_000_000));
        p.setInsurancePercentUnder(BigDecimal.valueOf(0.5));
        p.setInsurancePercentOver(BigDecimal.ONE);
        p.setRefundEnabled(true);
        p.setRefundPercent(BigDecimal.valueOf(100));
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
