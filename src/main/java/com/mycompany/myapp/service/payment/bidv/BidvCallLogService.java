package com.mycompany.myapp.service.payment.bidv;

import com.mycompany.myapp.domain.BidvCallLog;
import com.mycompany.myapp.repository.BidvCallLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ghi nhật ký cuộc gọi BIDV ở transaction riêng: gạch nợ lỗi / rollback vẫn còn dấu vết. */
@Service
public class BidvCallLogService {

    private static final Logger LOG = LoggerFactory.getLogger(BidvCallLogService.class);
    private static final int MAX_BODY = 4000;

    private final BidvCallLogRepository repository;

    public BidvCallLogService(BidvCallLogRepository repository) {
        this.repository = repository;
    }

    public record Entry(
        String endpoint,
        String clientIp,
        String customerId,
        String transId,
        BigDecimal amount,
        String resultCode,
        String resultDesc,
        String checksumVariant,
        long durationMs,
        String requestBody,
        String responseBody
    ) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Entry e) {
        try {
            BidvCallLog log = new BidvCallLog();
            log.setCalledAt(Instant.now());
            log.setEndpoint(e.endpoint());
            log.setClientIp(cut(e.clientIp(), 64));
            log.setCustomerId(cut(e.customerId(), 64));
            log.setTransId(cut(e.transId(), 64));
            log.setAmount(e.amount());
            log.setResultCode(cut(e.resultCode(), 5));
            log.setResultDesc(cut(e.resultDesc(), 255));
            log.setChecksumVariant(cut(e.checksumVariant(), 20));
            log.setDurationMs((int) Math.min(Integer.MAX_VALUE, Math.max(0, e.durationMs())));
            log.setRequestBody(cut(e.requestBody(), MAX_BODY));
            log.setResponseBody(cut(e.responseBody(), MAX_BODY));
            repository.save(log);
        } catch (RuntimeException ex) {
            LOG.warn("BIDV call log not saved ({} {}): {}", e.endpoint(), e.transId(), ex.getMessage());
        }
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }
}
