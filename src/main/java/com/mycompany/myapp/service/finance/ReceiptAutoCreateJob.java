package com.mycompany.myapp.service.finance;

import com.mycompany.myapp.service.finance.FinanceFacadeService.CreateReceiptRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 00:05 giờ VN: lập phiếu thu cho khoản đã nhận tiền trước nửa đêm. Khoản chưa xác nhận bù được để lại. */
@Component
public class ReceiptAutoCreateJob {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiptAutoCreateJob.class);
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final FinanceFacadeService financeFacadeService;

    public ReceiptAutoCreateJob(FinanceFacadeService financeFacadeService) {
        this.financeFacadeService = financeFacadeService;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void createOvernightReceipts() {
        Instant cutoff = LocalDate.now(VN).atStartOfDay(VN).toInstant();
        List<CreateReceiptRequest> requests = financeFacadeService.overnightReceiptRequests(cutoff);
        int created = 0;
        for (CreateReceiptRequest req : requests) {
            try {
                ReceiptDTO dto = financeFacadeService.createReceipt(req);
                created++;
                LOG.info("Auto receipt {} for {} · {}", dto.receiptCode(), req.payerCode(), dto.totalAmount());
            } catch (RuntimeException ex) {
                LOG.warn("Auto receipt skipped for {}: {}", req.payerCode(), ex.getMessage());
            }
        }
        if (!requests.isEmpty()) {
            LOG.info("Overnight receipts: {} created, {} skipped", created, requests.size() - created);
        }
    }
}
