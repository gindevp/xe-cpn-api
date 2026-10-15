package com.mycompany.myapp.service.payment.onepay;

import com.mycompany.myapp.domain.OnePayCallLog;
import com.mycompany.myapp.repository.OnePayCallLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ghi nhật ký ở transaction riêng: xử lý lỗi / rollback vẫn còn dấu vết. */
@Service
public class OnePayCallLogService {

    private static final Logger LOG = LoggerFactory.getLogger(OnePayCallLogService.class);
    private static final int MAX_BODY = 4000;
    /** Ảnh QR base64 OnePay trả về dài hàng chục KB — bỏ khỏi nhật ký. */
    private static final String IMAGE_FIELD = "\"image\"\\s*:\\s*\"[^\"]*\"";

    private final OnePayCallLogRepository repository;

    public OnePayCallLogService(OnePayCallLogRepository repository) {
        this.repository = repository;
    }

    public record Entry(
        String direction,
        String action,
        Integer httpStatus,
        String result,
        String message,
        String reference,
        String transId,
        BigDecimal amount,
        String clientIp,
        long durationMs,
        String requestBody,
        String responseBody
    ) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Entry e) {
        try {
            OnePayCallLog log = new OnePayCallLog();
            log.setCalledAt(Instant.now());
            log.setDirection(e.direction());
            log.setAction(cut(e.action(), 16));
            log.setHttpStatus(e.httpStatus());
            log.setResult(cut(e.result() == null ? "?" : e.result(), 20));
            log.setMessage(cut(e.message(), 255));
            log.setReference(cut(e.reference(), 64));
            log.setTransId(cut(e.transId(), 64));
            log.setAmount(e.amount());
            log.setClientIp(cut(e.clientIp(), 64));
            log.setDurationMs((int) Math.min(Integer.MAX_VALUE, Math.max(0, e.durationMs())));
            log.setRequestBody(cut(stripImages(e.requestBody()), MAX_BODY));
            log.setResponseBody(cut(stripImages(e.responseBody()), MAX_BODY));
            repository.save(log);
        } catch (RuntimeException ex) {
            LOG.warn("OnePay call log not saved ({} {}): {}", e.action(), e.reference(), ex.getMessage());
        }
    }

    static String stripImages(String s) {
        return s == null ? null : s.replaceAll(IMAGE_FIELD, "\"image\":\"…\"");
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }
}
