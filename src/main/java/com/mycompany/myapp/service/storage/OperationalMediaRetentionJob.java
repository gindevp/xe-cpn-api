package com.mycompany.myapp.service.storage;

import com.mycompany.myapp.repository.AttendanceRecordRepository;
import com.mycompany.myapp.repository.InventoryCheckPhotoRepository;
import com.mycompany.myapp.repository.VehicleEventPhotoRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * TTL ảnh vận hành trên MinIO: chấm công, chấm xe, kiểm kho (mặc định 60 ngày).
 * Xóa object MinIO rồi dọn DB: giữ bản ghi chấm công (null ảnh), xóa dòng ảnh xe / kiểm kho.
 */
@Component
public class OperationalMediaRetentionJob {

    private static final Logger LOG = LoggerFactory.getLogger(OperationalMediaRetentionJob.class);
    private static final int BATCH = 80;
    private static final int MAX_BATCHES = 40;

    private final StoredMedia storedMedia;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final VehicleEventPhotoRepository vehicleEventPhotoRepository;
    private final InventoryCheckPhotoRepository inventoryCheckPhotoRepository;
    private final TransactionTemplate tx;
    private final int retentionDays;

    public OperationalMediaRetentionJob(
        StoredMedia storedMedia,
        AttendanceRecordRepository attendanceRecordRepository,
        VehicleEventPhotoRepository vehicleEventPhotoRepository,
        InventoryCheckPhotoRepository inventoryCheckPhotoRepository,
        TransactionTemplate tx,
        @Value("${cpn.media.operational-retention-days:60}") int retentionDays
    ) {
        this.storedMedia = storedMedia;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.vehicleEventPhotoRepository = vehicleEventPhotoRepository;
        this.inventoryCheckPhotoRepository = inventoryCheckPhotoRepository;
        this.tx = tx;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void run() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int attendance = purgeAttendance(cutoff);
        int vehicle = purgeVehicle(cutoff);
        int inventory = purgeInventory(cutoff);
        if (attendance > 0 || vehicle > 0 || inventory > 0) {
            LOG.info(
                "Operational media retention ({}d): attendancePhotos={}, vehiclePhotos={}, inventoryPhotos={}",
                retentionDays,
                attendance,
                vehicle,
                inventory
            );
        }
    }

    int purgeAttendance(Instant cutoff) {
        int cleared = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            Integer n = tx.execute(status -> {
                List<Object[]> rows = attendanceRecordRepository.findExpiredPhotos(cutoff, PageRequest.of(0, BATCH));
                if (rows.isEmpty()) {
                    return 0;
                }
                List<Long> ids = new ArrayList<>(rows.size());
                for (Object[] r : rows) {
                    deleteMinio((String) r[1]);
                    ids.add((Long) r[0]);
                }
                return attendanceRecordRepository.clearPhotosByIdIn(ids);
            });
            if (n == null || n == 0) {
                break;
            }
            cleared += n;
        }
        return cleared;
    }

    int purgeVehicle(Instant cutoff) {
        int deleted = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            Integer n = tx.execute(status -> {
                List<Object[]> rows = vehicleEventPhotoRepository.findExpiredIdUrl(cutoff, PageRequest.of(0, BATCH));
                if (rows.isEmpty()) {
                    return 0;
                }
                List<Long> ids = new ArrayList<>(rows.size());
                for (Object[] r : rows) {
                    deleteMinio((String) r[1]);
                    ids.add((Long) r[0]);
                }
                return vehicleEventPhotoRepository.deleteByIdIn(ids);
            });
            if (n == null || n == 0) {
                break;
            }
            deleted += n;
        }
        return deleted;
    }

    int purgeInventory(Instant cutoff) {
        int deleted = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            Integer n = tx.execute(status -> {
                List<Object[]> rows = inventoryCheckPhotoRepository.findExpiredIdUrl(cutoff, PageRequest.of(0, BATCH));
                if (rows.isEmpty()) {
                    return 0;
                }
                List<Long> ids = new ArrayList<>(rows.size());
                for (Object[] r : rows) {
                    deleteMinio((String) r[1]);
                    ids.add((Long) r[0]);
                }
                return inventoryCheckPhotoRepository.deleteByIdIn(ids);
            });
            if (n == null || n == 0) {
                break;
            }
            deleted += n;
        }
        return deleted;
    }

    private void deleteMinio(String stored) {
        if (stored == null || stored.isBlank()) {
            return;
        }
        storedMedia.deleteStored(stored);
    }
}
