package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.partner.AhamoveAuthClient;
import com.mycompany.myapp.service.partner.AhamoveAuthClient.AhamoveAuthException;
import com.mycompany.myapp.service.partner.AhamoveTokenService;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient;
import com.mycompany.myapp.service.partner.VtechAutoCallClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        if (incoming.getAhamovePaymentMethod() != null) {
            String pm = incoming.getAhamovePaymentMethod().trim().toUpperCase();
            if ("BALANCE".equals(pm) || "CASH".equals(pm)) {
                current.setAhamovePaymentMethod(pm);
            }
        }
        if (!notBlank(current.getAhamoveWebhookToken())) {
            current.setAhamoveWebhookToken(newWebhookToken());
        }
        if (incoming.getTelegramToken() != null) current.setTelegramToken(incoming.getTelegramToken());
        if (incoming.getTelegramChatId() != null) current.setTelegramChatId(incoming.getTelegramChatId());
        if (incoming.getWebhookUrl() != null) current.setWebhookUrl(incoming.getWebhookUrl());
        if (incoming.getWebhookSecret() != null) current.setWebhookSecret(incoming.getWebhookSecret());
        mergeAutoCall(current, incoming);

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

    /** Công tắc tự xuất HĐĐT (màn Cấu hình) — {@code enabled}, {@code since} (lúc bật gần nhất). */
    @Transactional(readOnly = true)
    public Map<String, Object> getMisaAutoIssue() {
        return misaAutoIssueView(getIntegrationConfig());
    }

    public Map<String, Object> putMisaAutoIssue(boolean enabled) {
        IntegrationConfig current = integrationConfigRepository.findAll().stream().findFirst().orElseGet(IntegrationConfig::new);
        IntegrationConfig incoming = new IntegrationConfig();
        incoming.setMisaAutoIssueEnabled(enabled);
        mergeMisaAutoIssue(current, incoming, Instant.now());
        current.setUpdatedAt(Instant.now());
        IntegrationConfig saved = integrationConfigRepository.save(current);
        LOG.info("MISA auto-issue {} since={}", enabled ? "ON" : "OFF", saved.getMisaAutoIssueSince());
        return misaAutoIssueView(saved);
    }

    private static Map<String, Object> misaAutoIssueView(IntegrationConfig cfg) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE.equals(cfg.getMisaAutoIssueEnabled()));
        out.put("since", cfg.getMisaAutoIssueSince());
        return out;
    }

    /** Tự xuất HĐ: null = không đổi; tắt → bật thì ghi lại mốc bật để không tự xuất bù đơn cũ. */
    static void mergeMisaAutoIssue(IntegrationConfig current, IntegrationConfig incoming, Instant now) {
        Boolean want = incoming.getMisaAutoIssueEnabled();
        if (want == null) {
            return;
        }
        if (want && !Boolean.TRUE.equals(current.getMisaAutoIssueEnabled())) {
            current.setMisaAutoIssueSince(now);
        }
        current.setMisaAutoIssueEnabled(want);
    }

    /** Auto Call: key/secret rỗng → giữ giá trị đã lưu; autocallEnabled là Boolean nên null = không đổi. */
    static void mergeAutoCall(IntegrationConfig current, IntegrationConfig incoming) {
        if (incoming.getAutocallEnabled() != null) {
            current.setAutocallEnabled(incoming.getAutocallEnabled());
        }
        if (notBlank(incoming.getAutocallBaseUrl())) {
            current.setAutocallBaseUrl(HhvnAutoCallClient.normalizeBaseUrl(incoming.getAutocallBaseUrl()));
        }
        String key = AhamoveAuthClient.sanitizeApiKey(incoming.getAutocallApiKey());
        if (key != null) {
            current.setAutocallApiKey(key);
        }
        if (notBlank(incoming.getAutocallWebhookSecret())) {
            current.setAutocallWebhookSecret(incoming.getAutocallWebhookSecret().trim());
        }
        if (notBlank(incoming.getAutocallProvider())) {
            String p = incoming.getAutocallProvider().trim().toUpperCase(java.util.Locale.ROOT);
            if (!IntegrationConfig.PROVIDER_HHVN.equals(p) && !IntegrationConfig.PROVIDER_VTECH.equals(p)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhà cung cấp Auto Call phải là HHVN hoặc VTECH");
            }
            current.setAutocallProvider(p);
        }
        if (notBlank(incoming.getAutocallVtechBaseUrl())) {
            current.setAutocallVtechBaseUrl(VtechAutoCallClient.normalizeBaseUrl(incoming.getAutocallVtechBaseUrl()));
        }
        String vtechKey = AhamoveAuthClient.sanitizeApiKey(incoming.getAutocallVtechApiKey());
        if (vtechKey != null) {
            current.setAutocallVtechApiKey(vtechKey);
        }
        if (current.isAutocallVtech() && !notBlank(current.getAutocallVtechWebhookToken())) {
            current.setAutocallVtechWebhookToken(newWebhookToken());
        }
        if (current.getAutocallProvider() == null) {
            current.setAutocallProvider(IntegrationConfig.PROVIDER_HHVN);
        }
        if (current.getAutocallEnabled() == null) {
            current.setAutocallEnabled(false);
        }
        if (incoming.getAutocallRetryEnabled() != null) current.setAutocallRetryEnabled(incoming.getAutocallRetryEnabled());
        if (incoming.getAutocallRetryIntervals() != null) {
            current.setAutocallRetryIntervals(normalizeRetryIntervals(incoming.getAutocallRetryIntervals()));
        }
        if (incoming.getAutocallRetryDays() != null) current.setAutocallRetryDays(clamp(incoming.getAutocallRetryDays(), 1, 7));
        if (incoming.getAutocallRetryNoAnswer() != null) current.setAutocallRetryNoAnswer(incoming.getAutocallRetryNoAnswer());
        if (incoming.getAutocallRetryCarrierError() != null) {
            current.setAutocallRetryCarrierError(incoming.getAutocallRetryCarrierError());
        }
        if (incoming.getAutocallRetrySendError() != null) current.setAutocallRetrySendError(incoming.getAutocallRetrySendError());
        String from = incoming.getAutocallCallFrom() != null
            ? normalizeHhmm(incoming.getAutocallCallFrom())
            : current.getAutocallCallFrom();
        String to = incoming.getAutocallCallTo() != null ? normalizeHhmm(incoming.getAutocallCallTo()) : current.getAutocallCallTo();
        if ((incoming.getAutocallCallFrom() != null && from == null) || (incoming.getAutocallCallTo() != null && to == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khung giờ gọi phải dạng HH:mm");
        }
        if (from != null && to != null && from.compareTo(to) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giờ bắt đầu gọi phải trước giờ kết thúc");
        }
        current.setAutocallCallFrom(from);
        current.setAutocallCallTo(to);
        if (current.getAutocallRetryEnabled() == null) {
            current.setAutocallRetryEnabled(false);
        }
    }

    private static final java.security.SecureRandom TOKEN_RANDOM = new java.security.SecureRandom();

    /** 32 ký tự hex — đủ khó đoán, an toàn khi đặt trong query string. */
    static String newWebhookToken() {
        byte[] b = new byte[16];
        TOKEN_RANDOM.nextBytes(b);
        return java.util.HexFormat.of().formatHex(b);
    }

    static final int MAX_RETRY_COUNT = 10;

    /** "60, 120" → "60,120"; mỗi khoảng 5–1440 phút, tối đa 10 lần; "" = không gọi lại. */
    static String normalizeRetryIntervals(String raw) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int v;
            try {
                v = Integer.parseInt(p);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng cách gọi lại phải là số phút");
            }
            out.add(String.valueOf(clamp(v, 5, 1440)));
        }
        if (out.size() > MAX_RETRY_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa " + MAX_RETRY_COUNT + " lần gọi lại");
        }
        return String.join(",", out);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** "8:00" → "08:00"; null nếu không hợp lệ. */
    static String normalizeHhmm(String raw) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s*(\\d{1,2}):(\\d{2})\\s*$").matcher(raw);
        if (!m.matches()) return null;
        int h = Integer.parseInt(m.group(1));
        int min = Integer.parseInt(m.group(2));
        if (h > 23 || min > 59) return null;
        return String.format("%02d:%02d", h, min);
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
        if (incoming.getDoorDeliveryOverKgStep() != null) base.setDoorDeliveryOverKgStep(incoming.getDoorDeliveryOverKgStep());
        if (incoming.getDoorDeliveryOverKgFee() != null) base.setDoorDeliveryOverKgFee(incoming.getDoorDeliveryOverKgFee());
        if (incoming.getDoorDeliveryOverKmStep() != null) base.setDoorDeliveryOverKmStep(incoming.getDoorDeliveryOverKmStep());
        if (incoming.getDoorDeliveryOverKmFee() != null) base.setDoorDeliveryOverKmFee(incoming.getDoorDeliveryOverKmFee());
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
        p.setDoorDeliveryOverKgStep(BigDecimal.ZERO);
        p.setDoorDeliveryOverKgFee(BigDecimal.ZERO);
        p.setDoorDeliveryOverKmStep(BigDecimal.ZERO);
        p.setDoorDeliveryOverKmFee(BigDecimal.ZERO);
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
