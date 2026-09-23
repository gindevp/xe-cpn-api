package com.mycompany.myapp.service.audit;

import com.mycompany.myapp.domain.AuditLog;
import com.mycompany.myapp.repository.AuditLogRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ghi {@code audit_log} cho thao tác nghiệp vụ (chuyến, xe…). */
@Service
public class AuditRecorder {

    private final AuditLogRepository auditLogRepository;

    public AuditRecorder(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Cùng transaction với thao tác: thao tác rollback thì audit cũng không còn. */
    @Transactional
    public void record(String action, String entityType, Object entityId, String detail) {
        auditLogRepository.save(build(action, entityType, entityId, detail));
    }

    /** Transaction riêng: vẫn lưu khi thao tác chính bị từ chối và rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejected(String action, String entityType, Object entityId, String detail) {
        auditLogRepository.save(build(action, entityType, entityId, detail));
    }

    private static AuditLog build(String action, String entityType, Object entityId, String detail) {
        AuditLog log = new AuditLog();
        log.setAction(cut(action, 100));
        log.setEntityType(cut(entityType, 100));
        log.setEntityId(cut(entityId == null ? "-" : String.valueOf(entityId), 100));
        log.setDetail(detail == null ? null : cut(detail, 255));
        log.setActedAt(Instant.now());
        log.setActedByUsername(cut(SecurityUtils.getCurrentUserLogin().orElse("system"), 50));
        return log;
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
