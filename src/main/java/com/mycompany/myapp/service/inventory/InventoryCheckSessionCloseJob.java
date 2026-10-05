package com.mycompany.myapp.service.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 23:59 giờ VN: phiên kiểm kho còn mở chuyển Bỏ dở (phiên mở lại trở về biên bản đã chốt). */
@Component
public class InventoryCheckSessionCloseJob {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryCheckSessionCloseJob.class);

    private final InventoryCheckSessionService sessionService;

    public InventoryCheckSessionCloseJob(InventoryCheckSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Ho_Chi_Minh")
    public void closeOpenSessions() {
        int n = sessionService.closeAllOpen();
        if (n > 0) {
            LOG.info("Inventory check sessions closed at end of day: {}", n);
        }
    }
}
