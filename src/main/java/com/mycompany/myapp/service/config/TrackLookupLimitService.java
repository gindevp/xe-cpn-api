package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.TrackLookupCounter;
import com.mycompany.myapp.domain.TrackLookupPolicy;
import com.mycompany.myapp.repository.TrackLookupCounterRepository;
import com.mycompany.myapp.repository.TrackLookupPolicyRepository;
import com.mycompany.myapp.service.dto.TrackLookupPolicyDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chặn tra cứu công khai khi một thiết bị vượt số lần cấu hình trong ngày (giờ VN). */
@Service
public class TrackLookupLimitService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int CACHE_MS = 15_000;
    private static final int DEFAULT_LIMIT = 30;

    private final TrackLookupPolicyRepository policyRepository;
    private final TrackLookupCounterRepository counterRepository;
    private volatile TrackLookupPolicyDTO cached;
    private volatile long cachedAt;

    public TrackLookupLimitService(TrackLookupPolicyRepository policyRepository, TrackLookupCounterRepository counterRepository) {
        this.policyRepository = policyRepository;
        this.counterRepository = counterRepository;
    }

    @Transactional(readOnly = true)
    public TrackLookupPolicyDTO getPolicy() {
        TrackLookupPolicyDTO hit = cached;
        if (hit != null && System.currentTimeMillis() - cachedAt < CACHE_MS) {
            return hit;
        }
        TrackLookupPolicyDTO fresh = toDto(current());
        cached = fresh;
        cachedAt = System.currentTimeMillis();
        return fresh;
    }

    @Transactional
    public TrackLookupPolicyDTO putPolicy(TrackLookupPolicyDTO incoming) {
        int limit = incoming == null ? DEFAULT_LIMIT : incoming.getDailyLimit();
        int refresh = incoming == null ? 60 : incoming.getQrRefreshSeconds();
        if (limit < 0 || limit > 10000) {
            throw new IllegalArgumentException("Số lần mỗi ngày phải từ 0 đến 10000. 0 = không giới hạn.");
        }
        if (refresh < 15 || refresh > 36000) {
            throw new IllegalArgumentException("Thời gian làm mới QR phải từ 15 đến 36000 giây.");
        }
        String quietFrom = clock(incoming == null ? "21:00" : incoming.getQrQuietFrom());
        String quietTo = clock(incoming == null ? "07:00" : incoming.getQrQuietTo());
        if (quietFrom.equals(quietTo)) {
            throw new IllegalArgumentException("Giờ tắt QR và giờ bật lại phải khác nhau.");
        }
        TrackLookupPolicy row = current();
        row.setEnabled(incoming == null || incoming.isEnabled());
        row.setDailyLimit(limit);
        row.setQrRefreshSeconds(refresh);
        row.setQrQuietEnabled(incoming == null || incoming.isQrQuietEnabled());
        row.setQrQuietFrom(quietFrom);
        row.setQrQuietTo(quietTo);
        row.setUpdatedAt(Instant.now());
        TrackLookupPolicyDTO saved = toDto(policyRepository.save(row));
        cached = saved;
        cachedAt = System.currentTimeMillis();
        return saved;
    }

    /** Đếm một lần tra cứu. Vượt mức thì báo lỗi, không trả đơn. */
    @Transactional
    public void consume(String deviceIdHeader, String clientIp) {
        TrackLookupPolicyDTO policy = getPolicy();
        if (!policy.isEnabled() || policy.getDailyLimit() <= 0) {
            return;
        }
        String key = deviceKey(deviceIdHeader, clientIp);
        LocalDate day = LocalDate.now(VN);
        TrackLookupCounter row = counterRepository.findByDeviceKeyAndDayVn(key, day).orElse(null);
        if (row == null) {
            row = new TrackLookupCounter();
            row.setDeviceKey(key);
            row.setDayVn(day);
            row.setHits(1);
            try {
                counterRepository.saveAndFlush(row);
                return;
            } catch (DataIntegrityViolationException ex) {
                row = counterRepository.findByDeviceKeyAndDayVn(key, day).orElseThrow(() -> ex);
            }
        }
        int hits = row.getHits() == null ? 0 : row.getHits();
        if (hits >= policy.getDailyLimit()) {
            throw new BadRequestAlertException(
                "Thiết bị này đã tra cứu đủ " + policy.getDailyLimit() + " lần trong ngày. Thử lại vào ngày mai.",
                "track",
                "lookuplimit"
            );
        }
        row.setHits(hits + 1);
        counterRepository.save(row);
    }

    /** Đang trong khung tắt QR (giờ Việt Nam). Khung qua nửa đêm khi giờ tắt muộn hơn giờ bật. */
    public boolean qrQuietNow() {
        TrackLookupPolicyDTO policy = getPolicy();
        if (!policy.isQrQuietEnabled()) {
            return false;
        }
        LocalTime from = LocalTime.parse(policy.getQrQuietFrom());
        LocalTime to = LocalTime.parse(policy.getQrQuietTo());
        if (from.equals(to)) {
            return false;
        }
        LocalTime now = LocalTime.now(VN);
        if (from.isBefore(to)) {
            return !now.isBefore(from) && now.isBefore(to);
        }
        return !now.isBefore(from) || now.isBefore(to);
    }

    private static String clock(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Giờ tắt QR phải dạng HH:mm, ví dụ 21:00");
        }
        try {
            LocalTime time = LocalTime.parse(raw.trim());
            if (time.getSecond() != 0 || time.getNano() != 0) {
                throw new IllegalArgumentException("Giờ tắt QR chỉ nhận giờ và phút");
            }
            return String.format("%02d:%02d", time.getHour(), time.getMinute());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Giờ tắt QR phải dạng HH:mm, ví dụ 21:00");
        }
    }

    static String deviceKey(String deviceIdHeader, String clientIp) {
        String id = deviceIdHeader == null ? "" : deviceIdHeader.trim();
        if (id.matches("[A-Za-z0-9._-]{8,80}")) {
            return "d:" + id.toLowerCase(Locale.ROOT);
        }
        String ip = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        if (ip.length() > 60) {
            ip = ip.substring(0, 60);
        }
        return "ip:" + ip;
    }

    private TrackLookupPolicy current() {
        return policyRepository.findAll().stream().findFirst().orElseGet(TrackLookupLimitService::defaultRow);
    }

    private static TrackLookupPolicy defaultRow() {
        TrackLookupPolicy row = new TrackLookupPolicy();
        row.setEnabled(true);
        row.setDailyLimit(DEFAULT_LIMIT);
        row.setQrRefreshSeconds(60);
        row.setQrQuietEnabled(true);
        row.setQrQuietFrom("21:00");
        row.setQrQuietTo("07:00");
        return row;
    }

    private static TrackLookupPolicyDTO toDto(TrackLookupPolicy row) {
        TrackLookupPolicyDTO dto = new TrackLookupPolicyDTO();
        dto.setEnabled(row.getEnabled() == null || row.getEnabled());
        dto.setDailyLimit(row.getDailyLimit() == null ? DEFAULT_LIMIT : row.getDailyLimit());
        int refresh = row.getQrRefreshSeconds() == null ? 60 : row.getQrRefreshSeconds();
        dto.setQrRefreshSeconds(refresh < 15 || refresh > 36000 ? 60 : refresh);
        dto.setQrQuietEnabled(row.getQrQuietEnabled() == null || row.getQrQuietEnabled());
        dto.setQrQuietFrom(row.getQrQuietFrom() == null || row.getQrQuietFrom().isBlank() ? "21:00" : row.getQrQuietFrom());
        dto.setQrQuietTo(row.getQrQuietTo() == null || row.getQrQuietTo().isBlank() ? "07:00" : row.getQrQuietTo());
        return dto;
    }
}
