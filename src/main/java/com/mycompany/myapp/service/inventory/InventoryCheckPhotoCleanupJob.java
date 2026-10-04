package com.mycompany.myapp.service.inventory;

import com.mycompany.myapp.repository.InventoryCheckPhotoRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Ảnh kiểm kho giữ 90 ngày; ảnh của phiên không bấm Hoàn tất xoá sau 24 giờ. Biên bản không bị xoá. */
@Component
public class InventoryCheckPhotoCleanupJob {

    static final Duration RETENTION = Duration.ofDays(90);
    static final Duration ABANDONED_AFTER = Duration.ofHours(24);

    private static final Logger LOG = LoggerFactory.getLogger(InventoryCheckPhotoCleanupJob.class);

    private final InventoryCheckPhotoRepository photoRepository;

    public InventoryCheckPhotoCleanupJob(InventoryCheckPhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    @Scheduled(cron = "0 20 * * * *")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        int expired = photoRepository.deleteCapturedBefore(now.minus(RETENTION));
        int abandoned = photoRepository.deleteAbandonedBefore(now.minus(ABANDONED_AFTER));
        if (expired > 0 || abandoned > 0) {
            LOG.info("Inventory check photos cleaned: expired={}, abandoned={}", expired, abandoned);
        }
    }
}
