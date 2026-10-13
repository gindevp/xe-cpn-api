package com.mycompany.myapp.service.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 00:10 giờ VN: cộng phí tồn kho cho đơn còn nằm kho giao. */
@Component
public class StorageFeeJob {

    private static final Logger LOG = LoggerFactory.getLogger(StorageFeeJob.class);

    private final SurchargeFeeService surchargeFeeService;

    public StorageFeeJob(SurchargeFeeService surchargeFeeService) {
        this.surchargeFeeService = surchargeFeeService;
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void refreshStorageFees() {
        int changed = surchargeFeeService.refreshSittingOrders();
        LOG.info("Storage fee refresh updated {} orders", changed);
    }
}
