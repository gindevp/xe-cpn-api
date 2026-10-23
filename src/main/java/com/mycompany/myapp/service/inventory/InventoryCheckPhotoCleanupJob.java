package com.mycompany.myapp.service.inventory;

import com.mycompany.myapp.repository.InventoryCheckPhotoRepository;
import com.mycompany.myapp.service.storage.StoredMedia;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ảnh phiên kiểm kho không bấm Hoàn tất → xoá sau 24 giờ (MinIO + dòng DB).
 * TTL 60 ngày ảnh đã gắn biên bản do {@link com.mycompany.myapp.service.storage.OperationalMediaRetentionJob}.
 */
@Component
public class InventoryCheckPhotoCleanupJob {

    static final Duration ABANDONED_AFTER = Duration.ofHours(24);
    private static final int BATCH = 80;
    private static final int MAX_BATCHES = 20;

    private static final Logger LOG = LoggerFactory.getLogger(InventoryCheckPhotoCleanupJob.class);

    private final InventoryCheckPhotoRepository photoRepository;
    private final StoredMedia storedMedia;

    public InventoryCheckPhotoCleanupJob(InventoryCheckPhotoRepository photoRepository, StoredMedia storedMedia) {
        this.photoRepository = photoRepository;
        this.storedMedia = storedMedia;
    }

    @Scheduled(cron = "0 20 * * * *")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(ABANDONED_AFTER);
        int abandoned = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            List<Object[]> rows = photoRepository.findAbandonedIdUrl(cutoff, PageRequest.of(0, BATCH));
            if (rows.isEmpty()) {
                break;
            }
            List<Long> ids = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                String url = (String) r[1];
                if (url != null && !url.isBlank()) {
                    storedMedia.deleteStored(url);
                }
                ids.add((Long) r[0]);
            }
            abandoned += photoRepository.deleteByIdIn(ids);
        }
        if (abandoned > 0) {
            LOG.info("Inventory check photos cleaned: abandoned={}", abandoned);
        }
    }
}
