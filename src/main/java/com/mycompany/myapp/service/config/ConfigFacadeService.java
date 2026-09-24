package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.partner.AhamoveAuthClient;
import com.mycompany.myapp.service.partner.AhamoveAuthClient.AhamoveAuthException;
import com.mycompany.myapp.service.partner.AhamoveTokenService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfigFacadeService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigFacadeService.class);

    private final SurchargePolicyRepository surchargePolicyRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final AhamoveTokenService ahamoveTokenService;

    public ConfigFacadeService(
        SurchargePolicyRepository surchargePolicyRepository,
        IntegrationConfigRepository integrationConfigRepository,
        AhamoveTokenService ahamoveTokenService
    ) {
        this.surchargePolicyRepository = surchargePolicyRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.ahamoveTokenService = ahamoveTokenService;
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
        boolean created = current == null;
        if (created) {
            current = new IntegrationConfig();
        }

        boolean ahamoveCredsChanged = false;
        if (incoming.getAhamoveApiKey() != null) {
            String sanitized = AhamoveAuthClient.sanitizeApiKey(incoming.getAhamoveApiKey());
            // Chuỗi rỗng / chỉ khoảng trắng → bỏ qua, không xóa key đã lưu.
            if (sanitized != null) {
                ahamoveCredsChanged |= !Objects.equals(blankToNull(current.getAhamoveApiKey()), sanitized);
                current.setAhamoveApiKey(sanitized);
            }
        }
        if (incoming.getAhamoveMobile() != null) {
            String normalized = AhamoveAuthClient.normalizeMobile(incoming.getAhamoveMobile());
            if (normalized != null) {
                ahamoveCredsChanged |= !Objects.equals(blankToNull(current.getAhamoveMobile()), normalized);
                current.setAhamoveMobile(normalized);
            }
        }
        // Không nhận ahamoveToken từ client — token do BE tự quản lý.
        if (incoming.getGrabToken() != null) current.setGrabToken(incoming.getGrabToken());
        if (incoming.getXanhsmToken() != null) current.setXanhsmToken(incoming.getXanhsmToken());
        if (notBlank(incoming.getDistanceApiToken())) {
            current.setDistanceApiToken(incoming.getDistanceApiToken().trim());
        }
        if (incoming.getMapProvider() != null) {
            String p = incoming.getMapProvider().trim().toUpperCase();
            if ("GOONG".equals(p) || "OSM".equals(p)) {
                current.setMapProvider(p);
            }
        }
        if (notBlank(incoming.getGoongMapTilesKey())) {
            current.setGoongMapTilesKey(incoming.getGoongMapTilesKey().trim());
        }
        if (incoming.getTelegramToken() != null) current.setTelegramToken(incoming.getTelegramToken());
        if (incoming.getTelegramChatId() != null) current.setTelegramChatId(incoming.getTelegramChatId());
        if (incoming.getWebhookUrl() != null) current.setWebhookUrl(incoming.getWebhookUrl());
        if (incoming.getWebhookSecret() != null) current.setWebhookSecret(incoming.getWebhookSecret());

        if (current.getMapProvider() == null || current.getMapProvider().isBlank()) {
            current.setMapProvider("OSM");
        }
        if (ahamoveCredsChanged) {
            current.setAhamoveToken(null);
            current.setAhamoveTokenFetchedAt(null);
        }
        current.setUpdatedAt(Instant.now());
        IntegrationConfig saved = integrationConfigRepository.save(current);

        if (notBlank(saved.getAhamoveApiKey()) && notBlank(saved.getAhamoveMobile()) && (ahamoveCredsChanged || created)) {
            try {
                ahamoveTokenService.refreshNow();
            } catch (AhamoveAuthException e) {
                LOG.warn("Ahamove token refresh after save failed: {}", e.getMessage());
            } catch (Exception e) {
                // Không để UnexpectedRollback / lỗi mạng làm fail cả lần Lưu api_key+mobile.
                LOG.warn("Ahamove token refresh after save failed: {}", e.getMessage());
            }
        }
        return integrationConfigRepository.findById(saved.getId()).orElse(saved);
    }

    public Map<String, Object> testIntegration() {
        IntegrationConfig cfg = getIntegrationConfig();
        Map<String, Object> out = new HashMap<>();
        out.put("grabConfigured", notBlank(cfg.getGrabToken()));
        out.put("xanhsmConfigured", notBlank(cfg.getXanhsmToken()));
        out.put("telegramConfigured", notBlank(cfg.getTelegramToken()));
        out.put("webhookConfigured", notBlank(cfg.getWebhookUrl()));
        out.put("ahamoveConfigured", notBlank(cfg.getAhamoveApiKey()) && notBlank(cfg.getAhamoveMobile()));
        out.put("ahamoveTokenPresent", notBlank(cfg.getAhamoveToken()));
        out.put("ahamoveTokenFetchedAt", cfg.getAhamoveTokenFetchedAt() != null ? cfg.getAhamoveTokenFetchedAt().toString() : null);

        boolean ok = true;
        if (notBlank(cfg.getAhamoveApiKey()) && notBlank(cfg.getAhamoveMobile())) {
            try {
                ahamoveTokenService.refreshNow();
                out.put("ahamoveTokenOk", true);
                out.put("ahamoveTokenPresent", true);
            } catch (Exception e) {
                ok = false;
                out.put("ahamoveTokenOk", false);
                out.put("ahamoveError", e.getMessage());
            }
        }
        out.put("ok", ok);
        out.put("testedBy", SecurityUtils.getCurrentUserLogin().orElse("system"));
        out.put("testedAt", Instant.now().toString());
        return out;
    }

    /**
     * Thử lấy Bearer token Ahamove từ API key + SĐT (body hoặc đã lưu).
     * Không dùng chung TX với refresh — tránh UnexpectedRollbackException khi auth fail.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> testAhamove(IntegrationConfig incoming) {
        Map<String, Object> out = new HashMap<>();
        IntegrationConfig current = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        if (current == null) {
            current = new IntegrationConfig();
        }
        boolean changed = false;
        if (incoming != null) {
            if (notBlank(incoming.getAhamoveApiKey())) {
                current.setAhamoveApiKey(AhamoveAuthClient.sanitizeApiKey(incoming.getAhamoveApiKey()));
                changed = true;
            }
            if (notBlank(incoming.getAhamoveMobile())) {
                current.setAhamoveMobile(AhamoveAuthClient.normalizeMobile(incoming.getAhamoveMobile()));
                changed = true;
            }
        }
        if (!notBlank(current.getAhamoveApiKey()) || !notBlank(current.getAhamoveMobile())) {
            out.put("ok", false);
            out.put("ahamoveTokenOk", false);
            out.put("ahamoveError", "Cần API Key + SĐT Ahamove (đủ để gọi /accounts/token)");
            out.put("testedAt", Instant.now().toString());
            return out;
        }
        if (current.getId() == null || changed) {
            if (changed) {
                current.setAhamoveToken(null);
                current.setAhamoveTokenFetchedAt(null);
            }
            current.setUpdatedAt(Instant.now());
            // Repo @Transactional — lưu api_key + mobile trước khi gọi Ahamove.
            current = integrationConfigRepository.save(current);
        }
        try {
            ahamoveTokenService.refreshNow();
            IntegrationConfig fresh = integrationConfigRepository.findById(current.getId()).orElse(current);
            out.put("ok", true);
            out.put("ahamoveTokenOk", true);
            out.put("ahamoveTokenPresent", notBlank(fresh.getAhamoveToken()));
            out.put("ahamoveTokenFetchedAt", fresh.getAhamoveTokenFetchedAt() != null ? fresh.getAhamoveTokenFetchedAt().toString() : null);
            out.put("message", "Lấy token Ahamove thành công");
            out.put("ahamoveBaseUrl", ahamoveTokenService.getBaseUrl());
        } catch (Exception e) {
            String msg = rootMessage(e);
            LOG.warn("testAhamove failed: {}", msg);
            out.put("ok", false);
            out.put("ahamoveTokenOk", false);
            out.put("ahamoveError", msg);
            out.put("message", "Lấy token Ahamove thất bại");
            out.put("ahamoveBaseUrl", ahamoveTokenService.getBaseUrl());
        }
        out.put("testedBy", SecurityUtils.getCurrentUserLogin().orElse("system"));
        out.put("testedAt", Instant.now().toString());
        return out;
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        String last = e.getMessage();
        while (cur != null) {
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
                last = cur.getMessage();
            }
            if (cur.getCause() == null || cur.getCause() == cur) {
                break;
            }
            cur = cur.getCause();
        }
        return last != null ? last : e.getClass().getSimpleName();
    }

    private SurchargePolicy merge(SurchargePolicy base, SurchargePolicy incoming) {
        if (incoming.getHomeDeliveryEnabled() != null) base.setHomeDeliveryEnabled(incoming.getHomeDeliveryEnabled());
        if (incoming.getDefaultHomeDeliveryAmount() != null) base.setDefaultHomeDeliveryAmount(incoming.getDefaultHomeDeliveryAmount());
        if (incoming.getCodEnabled() != null) base.setCodEnabled(incoming.getCodEnabled());
        if (incoming.getCodPercent() != null) base.setCodPercent(incoming.getCodPercent());
        if (incoming.getCodMinFee() != null) base.setCodMinFee(incoming.getCodMinFee());
        if (incoming.getCodTiersJson() != null) base.setCodTiersJson(incoming.getCodTiersJson());
        if (incoming.getStorageEnabled() != null) base.setStorageEnabled(incoming.getStorageEnabled());
        if (incoming.getStorageFreeDays() != null) base.setStorageFreeDays(incoming.getStorageFreeDays());
        if (incoming.getStorageFeePerDay() != null) base.setStorageFeePerDay(incoming.getStorageFeePerDay());
        if (incoming.getInsuranceEnabled() != null) base.setInsuranceEnabled(incoming.getInsuranceEnabled());
        if (incoming.getInsuranceThreshold() != null) base.setInsuranceThreshold(incoming.getInsuranceThreshold());
        if (incoming.getInsurancePercentUnder() != null) base.setInsurancePercentUnder(incoming.getInsurancePercentUnder());
        if (incoming.getInsurancePercentOver() != null) base.setInsurancePercentOver(incoming.getInsurancePercentOver());
        if (incoming.getRefundEnabled() != null) base.setRefundEnabled(incoming.getRefundEnabled());
        if (incoming.getRefundPercent() != null) base.setRefundPercent(incoming.getRefundPercent());
        if (incoming.getDoorOverKgStep() != null) base.setDoorOverKgStep(incoming.getDoorOverKgStep());
        if (incoming.getDoorOverKgFee() != null) base.setDoorOverKgFee(incoming.getDoorOverKgFee());
        if (incoming.getDoorOverKmStep() != null) base.setDoorOverKmStep(incoming.getDoorOverKmStep());
        if (incoming.getDoorOverKmFee() != null) base.setDoorOverKmFee(incoming.getDoorOverKmFee());
        return base;
    }

    private SurchargePolicy defaultSurcharge() {
        SurchargePolicy p = new SurchargePolicy();
        p.setHomeDeliveryEnabled(true);
        p.setDefaultHomeDeliveryAmount(BigDecimal.valueOf(10_000));
        p.setCodEnabled(true);
        p.setCodPercent(BigDecimal.ONE);
        p.setCodMinFee(BigDecimal.valueOf(5_000));
        p.setCodTiersJson(
            "[" +
            "{\"minAmount\":0,\"maxAmount\":2000000,\"feeAmount\":30000,\"feePercent\":null}," +
            "{\"minAmount\":2000000,\"maxAmount\":5000000,\"feeAmount\":40000,\"feePercent\":null}," +
            "{\"minAmount\":5000000,\"maxAmount\":10000000,\"feeAmount\":60000,\"feePercent\":null}," +
            "{\"minAmount\":10000000,\"maxAmount\":15000000,\"feeAmount\":80000,\"feePercent\":null}," +
            "{\"minAmount\":15000000,\"maxAmount\":20000000,\"feeAmount\":100000,\"feePercent\":null}," +
            "{\"minAmount\":20000000,\"maxAmount\":null,\"feeAmount\":null,\"feePercent\":1}" +
            "]"
        );
        p.setStorageEnabled(false);
        p.setStorageFreeDays(3);
        p.setStorageFeePerDay(BigDecimal.valueOf(5_000));
        p.setInsuranceEnabled(false);
        p.setInsuranceThreshold(BigDecimal.valueOf(1_000_000));
        p.setInsurancePercentUnder(BigDecimal.valueOf(0.5));
        p.setInsurancePercentOver(BigDecimal.ONE);
        p.setRefundEnabled(true);
        p.setRefundPercent(BigDecimal.valueOf(100));
        p.setDoorOverKgStep(BigDecimal.ZERO);
        p.setDoorOverKgFee(BigDecimal.ZERO);
        p.setDoorOverKmStep(BigDecimal.ZERO);
        p.setDoorOverKmFee(BigDecimal.ZERO);
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        return notBlank(s) ? s.trim() : null;
    }
}
