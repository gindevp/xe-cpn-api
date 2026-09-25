package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.SessionPolicy;
import com.mycompany.myapp.repository.SessionPolicyRepository;
import com.mycompany.myapp.security.SessionCutoff;
import com.mycompany.myapp.service.dto.SessionPolicyDTO;
import java.time.Instant;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionPolicyService {

    private static final long CACHE_MS = 15_000;

    private final SessionPolicyRepository repository;
    private volatile SessionPolicyDTO cached;
    private volatile long cachedAt;

    public SessionPolicyService(SessionPolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SessionPolicyDTO getPolicy() {
        SessionPolicyDTO hit = cached;
        if (hit != null && System.currentTimeMillis() - cachedAt < CACHE_MS) {
            return hit;
        }
        SessionPolicyDTO fresh = toDto(current());
        cached = fresh;
        cachedAt = System.currentTimeMillis();
        return fresh;
    }

    @Transactional
    public SessionPolicyDTO putPolicy(SessionPolicyDTO incoming) {
        LocalTime time = SessionCutoff.parseTime(incoming.getLogoutTime());
        SessionPolicy row = current();
        row.setEnabled(incoming.isEnabled());
        row.setLogoutTime(String.format("%02d:%02d", time.getHour(), time.getMinute()));
        row.setUpdatedAt(Instant.now());
        SessionPolicyDTO saved = toDto(repository.save(row));
        cached = saved;
        cachedAt = System.currentTimeMillis();
        return saved;
    }

    /** Hết hạn JWT không được vượt quá mốc đăng xuất kế tiếp. Null = không cắt. */
    @Transactional(readOnly = true)
    public Instant capExpiry(Instant issuedAt, Instant normalExpiry) {
        SessionPolicyDTO policy = getPolicy();
        if (!policy.isEnabled()) {
            return normalExpiry;
        }
        Instant end = SessionCutoff.nextEnd(issuedAt, SessionCutoff.parseTime(policy.getLogoutTime()));
        return end.isBefore(normalExpiry) ? end : normalExpiry;
    }

    /** Token phát hành trước mốc này thì đã hết phiên. */
    @Transactional(readOnly = true)
    public boolean sessionEnded(Instant issuedAt, Instant now) {
        SessionPolicyDTO policy = getPolicy();
        if (!policy.isEnabled() || issuedAt == null) {
            return false;
        }
        Instant end = SessionCutoff.nextEnd(issuedAt, SessionCutoff.parseTime(policy.getLogoutTime()));
        return !now.isBefore(end);
    }

    private SessionPolicy current() {
        return repository.findAll().stream().findFirst().orElseGet(SessionPolicyService::defaultRow);
    }

    private static SessionPolicy defaultRow() {
        SessionPolicy row = new SessionPolicy();
        row.setEnabled(true);
        row.setLogoutTime(SessionCutoff.DEFAULT_TIME);
        return row;
    }

    private static SessionPolicyDTO toDto(SessionPolicy row) {
        SessionPolicyDTO dto = new SessionPolicyDTO();
        dto.setEnabled(Boolean.TRUE.equals(row.getEnabled()));
        dto.setLogoutTime(row.getLogoutTime() == null ? SessionCutoff.DEFAULT_TIME : row.getLogoutTime());
        dto.setTimeZone(SessionCutoff.ZONE.getId());
        return dto;
    }
}
