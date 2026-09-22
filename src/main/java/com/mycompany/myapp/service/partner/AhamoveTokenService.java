package com.mycompany.myapp.service.partner;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.service.partner.AhamoveAuthClient.AhamoveAuthException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cache Bearer Ahamove trên IntegrationConfig; refresh định kỳ 1 tuần.
 */
@Service
public class AhamoveTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(AhamoveTokenService.class);
    /** TTL cache token trên hệ thống (yêu cầu: 1 tuần lấy lại). */
    public static final Duration TOKEN_REFRESH_INTERVAL = Duration.ofDays(7);

    private final IntegrationConfigRepository integrationConfigRepository;
    private final AhamoveAuthClient ahamoveAuthClient;
    private final Object lock = new Object();

    public AhamoveTokenService(IntegrationConfigRepository integrationConfigRepository, AhamoveAuthClient ahamoveAuthClient) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.ahamoveAuthClient = ahamoveAuthClient;
    }

    /**
     * Token dùng gọi Ahamove API. Refresh nếu thiếu hoặc quá 7 ngày.
     * Legacy: nếu chưa có api_key nhưng còn token dán tay → dùng token đó.
     */
    @Transactional
    public Optional<String> resolveAccessToken() {
        synchronized (lock) {
            IntegrationConfig cfg = currentConfig();
            if (cfg.getId() == null) {
                return Optional.empty();
            }
            boolean hasCreds = notBlank(cfg.getAhamoveApiKey()) && notBlank(cfg.getAhamoveMobile());
            if (!hasCreds) {
                return Optional.ofNullable(blankToNull(cfg.getAhamoveToken()));
            }
            if (needsRefresh(cfg)) {
                refreshLocked(cfg);
            }
            return Optional.ofNullable(blankToNull(cfg.getAhamoveToken()));
        }
    }

    /** Ép lấy token mới từ Ahamove (sau khi lưu API key / nút Test). TX riêng để lỗi auth không làm rollback-only TX ngoài. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String refreshNow() {
        synchronized (lock) {
            IntegrationConfig cfg = currentConfig();
            if (cfg.getId() == null || !notBlank(cfg.getAhamoveApiKey()) || !notBlank(cfg.getAhamoveMobile())) {
                throw new AhamoveAuthException("Chưa cấu hình Ahamove API key + số điện thoại");
            }
            return refreshLocked(cfg);
        }
    }

    /** Cron: 03:00 mỗi thứ Hai — refresh nếu đã cấu hình và token quá hạn / sắp quá 7 ngày. */
    @Scheduled(cron = "0 0 3 * * MON")
    @Transactional
    public void weeklyRefresh() {
        synchronized (lock) {
            IntegrationConfig cfg = currentConfig();
            if (cfg.getId() == null || !notBlank(cfg.getAhamoveApiKey()) || !notBlank(cfg.getAhamoveMobile())) {
                return;
            }
            try {
                refreshLocked(cfg);
                LOG.info("Ahamove weekly token refresh OK");
            } catch (Exception e) {
                LOG.error("Ahamove weekly token refresh failed: {}", e.getMessage());
            }
        }
    }

    public boolean needsRefresh(IntegrationConfig cfg) {
        if (!notBlank(cfg.getAhamoveToken())) {
            return true;
        }
        Instant fetched = cfg.getAhamoveTokenFetchedAt();
        if (fetched == null) {
            return true;
        }
        return Instant.now().isAfter(fetched.plus(TOKEN_REFRESH_INTERVAL));
    }

    public String getBaseUrl() {
        return ahamoveAuthClient.getBaseUrl();
    }

    private String refreshLocked(IntegrationConfig cfg) {
        String token = ahamoveAuthClient.fetchAccessToken(cfg.getAhamoveApiKey(), cfg.getAhamoveMobile());
        cfg.setAhamoveToken(token);
        cfg.setAhamoveTokenFetchedAt(Instant.now());
        cfg.setUpdatedAt(Instant.now());
        integrationConfigRepository.save(cfg);
        return token;
    }

    private IntegrationConfig currentConfig() {
        return integrationConfigRepository.findAll().stream().findFirst().orElseGet(IntegrationConfig::new);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        return notBlank(s) ? s : null;
    }
}
