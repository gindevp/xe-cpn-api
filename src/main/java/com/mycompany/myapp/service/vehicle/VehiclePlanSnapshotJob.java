package com.mycompany.myapp.service.vehicle;

import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 23:50 giờ VN: chụp bảng tài CRM của ngày hôm đó. Portal không cho xem lại ngày đã qua. */
@Component
public class VehiclePlanSnapshotJob {

    private static final Logger LOG = LoggerFactory.getLogger(VehiclePlanSnapshotJob.class);
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VehicleBoardService vehicleBoardService;

    public VehiclePlanSnapshotJob(VehicleBoardService vehicleBoardService) {
        this.vehicleBoardService = vehicleBoardService;
    }

    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Ho_Chi_Minh")
    public void captureToday() {
        LocalDate today = LocalDate.now(VN);
        int saved = vehicleBoardService.capturePlan(today);
        LOG.info("Vehicle plan snapshot {} saved {} trips", today, saved);
    }
}
