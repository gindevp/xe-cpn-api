package com.mycompany.myapp.service.auth;

import com.mycompany.myapp.domain.LoginTrust;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.UserSession;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.LoginTrustRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.repository.UserSessionRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.audit.AuditRecorder;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm soát đăng nhập: web DH/KT phải dùng IP đã duyệt, app (mọi tài khoản trừ admin) phải dùng thiết bị đã duyệt.
 * Mỗi tài khoản giữ tối đa 1 phiên web + 1 phiên app — đăng nhập nơi mới thì phiên cũ bị thu hồi.
 */
@Service
public class LoginControlService {

    private static final Set<RoleCode> IP_CONTROLLED_ROLES = Set.of(RoleCode.DH, RoleCode.KT);
    private static final long CACHE_MS = 10_000;
    private static final Duration TOUCH_EVERY = Duration.ofSeconds(60);

    private final LoginTrustRepository trustRepository;
    private final UserSessionRepository sessionRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AuditRecorder auditRecorder;

    private record CacheHit(boolean active, long at) {}

    private final Map<String, CacheHit> activeCache = new ConcurrentHashMap<>();

    public LoginControlService(
        LoginTrustRepository trustRepository,
        UserSessionRepository sessionRepository,
        StaffProfileRepository staffProfileRepository,
        AuditRecorder auditRecorder
    ) {
        this.trustRepository = trustRepository;
        this.sessionRepository = sessionRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.auditRecorder = auditRecorder;
    }

    /**
     * App bản cũ trên store: không gửi {@code client}/mã thiết bị. Vẫn tính là phiên APP (không đá phiên web),
     * nhưng áp luật như web vì chưa có mã thiết bị để duyệt.
     */
    public static final String CHANNEL_LEGACY_APP = "APP_LEGACY";

    public record LoginRequestInfo(String channel, String ip, String deviceId, String deviceName, String userAgent) {}

    /** Không gửi client: trình duyệt luôn có "Mozilla" trong User-Agent, app React Native (okhttp / CFNetwork) thì không. */
    public static String resolveChannel(String client, String userAgent) {
        if (client != null && !client.isBlank()) {
            return client.trim().toUpperCase();
        }
        if (userAgent == null || !userAgent.contains("Mozilla")) {
            return CHANNEL_LEGACY_APP;
        }
        return UserSession.WEB;
    }

    /** allowed=false: {@code code} là mã lỗi cho FE, {@code message} hiển thị cho người dùng. */
    public record LoginDecision(boolean allowed, String sid, String code, String message, String kind, String value) {
        static LoginDecision blocked(String code, String message, String kind, String value) {
            return new LoginDecision(false, null, code, message, kind, value);
        }
    }

    /** Không throw khi bị chặn để bản ghi chờ duyệt vẫn được lưu. */
    @Transactional
    public LoginDecision decide(Authentication auth, LoginRequestInfo info, Instant expiresAt) {
        String login = auth.getName().toLowerCase();
        boolean admin = isAdmin(auth, login);
        boolean legacyApp = CHANNEL_LEGACY_APP.equals(info.channel());
        String channel = legacyApp || UserSession.APP.equals(info.channel()) ? UserSession.APP : UserSession.WEB;
        Instant now = Instant.now();

        if (!admin) {
            if (UserSession.APP.equals(channel) && !legacyApp) {
                String deviceId = trimTo(info.deviceId(), 100);
                if (deviceId == null) {
                    return LoginDecision.blocked(
                        "error.deviceIdRequired",
                        "Phiên bản app chưa hỗ trợ duyệt thiết bị — cập nhật app để đăng nhập",
                        LoginTrust.KIND_DEVICE,
                        null
                    );
                }
                LoginDecision d = requireTrust(login, LoginTrust.KIND_DEVICE, deviceId, info.deviceName(), info.ip(), now);
                if (d != null) {
                    return d;
                }
            } else if (roleOf(login).map(IP_CONTROLLED_ROLES::contains).orElse(false)) {
                String ip = trimTo(info.ip(), 100);
                if (ip == null) {
                    return LoginDecision.blocked("error.loginIpUnknown", "Không xác định được IP máy đăng nhập", LoginTrust.KIND_IP, null);
                }
                LoginDecision d = requireTrust(login, LoginTrust.KIND_IP, ip, info.userAgent(), ip, now);
                if (d != null) {
                    return d;
                }
            }
        }

        for (UserSession old : sessionRepository.findByUserLoginIgnoreCaseAndChannelAndRevokedAtIsNull(login, channel)) {
            revoke(old, "system", "Đăng nhập nơi khác", now);
        }
        UserSession s = new UserSession();
        s.setSid(UUID.randomUUID().toString());
        s.setUserLogin(login);
        s.setChannel(channel);
        s.setIp(trimTo(info.ip(), 64));
        s.setDeviceId(trimTo(info.deviceId(), 100));
        s.setDeviceName(trimTo(info.deviceName(), 255));
        s.setUserAgent(trimTo(info.userAgent(), 255));
        s.setCreatedAt(now);
        s.setLastSeenAt(now);
        s.setExpiresAt(expiresAt);
        sessionRepository.save(s);
        return new LoginDecision(true, s.getSid(), null, null, null, null);
    }

    /** null = được phép. */
    private LoginDecision requireTrust(String login, String kind, String value, String label, String ip, Instant now) {
        LoginTrust t = trustRepository.findOneByUserLoginIgnoreCaseAndKindAndTrustValue(login, kind, value).orElse(null);
        if (t != null && LoginTrust.APPROVED.equals(t.getStatus())) {
            t.setLastSeenAt(now);
            t.setLastIp(trimTo(ip, 64));
            if (label != null && !label.isBlank()) {
                t.setLabel(trimTo(label, 255));
            }
            trustRepository.save(t);
            return null;
        }
        String what = LoginTrust.KIND_IP.equals(kind) ? "IP " + value : "Thiết bị này";
        if (t != null && LoginTrust.REJECTED.equals(t.getStatus())) {
            return LoginDecision.blocked("error.loginRejected", what + " đã bị admin từ chối — liên hệ admin", kind, value);
        }
        if (t == null) {
            t = new LoginTrust();
            t.setUserLogin(login);
            t.setKind(kind);
            t.setTrustValue(value);
        }
        t.setStatus(LoginTrust.PENDING);
        t.setRequestedAt(now);
        t.setLastSeenAt(now);
        t.setLastIp(trimTo(ip, 64));
        t.setLabel(trimTo(label, 255));
        trustRepository.save(t);
        return LoginDecision.blocked(
            "error.loginPendingApproval",
            what + " chưa được duyệt — đã gửi yêu cầu, chờ admin duyệt rồi đăng nhập lại",
            kind,
            value
        );
    }

    /** Filter gọi mỗi request; cache ngắn để không chạm DB liên tục. */
    @Transactional
    public boolean isActive(String sid) {
        long nowMs = System.currentTimeMillis();
        CacheHit hit = activeCache.get(sid);
        if (hit != null && nowMs - hit.at() < CACHE_MS) {
            return hit.active();
        }
        Instant now = Instant.now();
        UserSession s = sessionRepository.findOneBySid(sid).orElse(null);
        boolean active = s != null && s.getRevokedAt() == null && (s.getExpiresAt() == null || s.getExpiresAt().isAfter(now));
        if (active && (s.getLastSeenAt() == null || s.getLastSeenAt().plus(TOUCH_EVERY).isBefore(now))) {
            s.setLastSeenAt(now);
            sessionRepository.save(s);
        }
        activeCache.put(sid, new CacheHit(active, nowMs));
        return active;
    }

    @Transactional
    public void logoutSelf(String sid) {
        if (sid == null) {
            return;
        }
        sessionRepository
            .findOneBySid(sid)
            .filter(s -> s.getRevokedAt() == null)
            .ifPresent(s -> revoke(s, s.getUserLogin(), "Đăng xuất", Instant.now()));
    }

    // ---- Admin ----

    public record TrustDTO(
        Long id,
        String userLogin,
        String displayName,
        String roleCode,
        String kind,
        String value,
        String label,
        String status,
        Instant requestedAt,
        Instant lastSeenAt,
        String lastIp,
        Instant decidedAt,
        String decidedBy
    ) {}

    public record SessionDTO(
        Long id,
        String userLogin,
        String displayName,
        String roleCode,
        String channel,
        String ip,
        String deviceId,
        String deviceName,
        String userAgent,
        Instant createdAt,
        Instant lastSeenAt,
        Instant expiresAt
    ) {}

    @Transactional(readOnly = true)
    public List<TrustDTO> listTrusts(String status) {
        List<LoginTrust> rows = status == null || status.isBlank()
            ? trustRepository.findByStatusInOrderByRequestedAtDesc(List.of(LoginTrust.PENDING, LoginTrust.APPROVED, LoginTrust.REJECTED))
            : trustRepository.findByStatusOrderByRequestedAtDesc(status.trim().toUpperCase());
        Map<String, Optional<StaffProfile>> profiles = new HashMap<>();
        List<TrustDTO> out = new ArrayList<>();
        for (LoginTrust t : rows) {
            Optional<StaffProfile> p = profiles.computeIfAbsent(t.getUserLogin(), staffProfileRepository::findOneByUserLoginIgnoreCase);
            out.add(
                new TrustDTO(
                    t.getId(),
                    t.getUserLogin(),
                    p.map(StaffProfile::getDisplayName).orElse(null),
                    p.map(StaffProfile::getRoleCode).map(Enum::name).orElse(null),
                    t.getKind(),
                    t.getTrustValue(),
                    t.getLabel(),
                    t.getStatus(),
                    t.getRequestedAt(),
                    t.getLastSeenAt(),
                    t.getLastIp(),
                    t.getDecidedAt(),
                    t.getDecidedBy()
                )
            );
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<SessionDTO> listActiveSessions() {
        Map<String, Optional<StaffProfile>> profiles = new HashMap<>();
        List<SessionDTO> out = new ArrayList<>();
        for (UserSession s : sessionRepository.findByRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtDesc(Instant.now())) {
            Optional<StaffProfile> p = profiles.computeIfAbsent(s.getUserLogin(), staffProfileRepository::findOneByUserLoginIgnoreCase);
            out.add(
                new SessionDTO(
                    s.getId(),
                    s.getUserLogin(),
                    p.map(StaffProfile::getDisplayName).orElse(null),
                    p.map(StaffProfile::getRoleCode).map(Enum::name).orElse(null),
                    s.getChannel(),
                    s.getIp(),
                    s.getDeviceId(),
                    s.getDeviceName(),
                    s.getUserAgent(),
                    s.getCreatedAt(),
                    s.getLastSeenAt(),
                    s.getExpiresAt()
                )
            );
        }
        return out;
    }

    @Transactional
    public void approveTrust(Long id) {
        LoginTrust t = requireTrustRow(id);
        decideTrust(t, LoginTrust.APPROVED);
        auditRecorder.record("LOGIN_TRUST_APPROVE", "LoginTrust", t.getUserLogin(), describe(t));
    }

    @Transactional
    public void rejectTrust(Long id) {
        LoginTrust t = requireTrustRow(id);
        decideTrust(t, LoginTrust.REJECTED);
        revokeSessionsUsing(t, "Admin từ chối " + kindLabel(t));
        auditRecorder.record("LOGIN_TRUST_REJECT", "LoginTrust", t.getUserLogin(), describe(t));
    }

    /** Thu hồi IP / thiết bị đã duyệt — lần sau phải xin duyệt lại; phiên đang dùng nó bị đăng xuất. */
    @Transactional
    public void revokeTrust(Long id) {
        LoginTrust t = requireTrustRow(id);
        decideTrust(t, LoginTrust.REVOKED);
        revokeSessionsUsing(t, "Admin thu hồi " + kindLabel(t));
        auditRecorder.record("LOGIN_TRUST_REVOKE", "LoginTrust", t.getUserLogin(), describe(t));
    }

    @Transactional
    public void revokeSession(Long id) {
        UserSession s = sessionRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Session not found", "loginControl", "sessionNotFound"));
        if (s.getRevokedAt() != null) {
            return;
        }
        revoke(s, actor(), "Admin đăng xuất từ xa", Instant.now());
        auditRecorder.record(
            "SESSION_REVOKE",
            "UserSession",
            s.getUserLogin(),
            s.getChannel() + " · " + (s.getDeviceName() != null ? s.getDeviceName() : s.getIp())
        );
    }

    private void revokeSessionsUsing(LoginTrust t, String reason) {
        Instant now = Instant.now();
        for (UserSession s : sessionRepository.findByUserLoginIgnoreCaseAndRevokedAtIsNull(t.getUserLogin())) {
            boolean match = LoginTrust.KIND_IP.equals(t.getKind())
                ? UserSession.WEB.equals(s.getChannel()) && t.getTrustValue().equals(s.getIp())
                : t.getTrustValue().equals(s.getDeviceId());
            if (match) {
                revoke(s, actor(), reason, now);
            }
        }
    }

    private void revoke(UserSession s, String by, String reason, Instant now) {
        s.setRevokedAt(now);
        s.setRevokedBy(by);
        s.setRevokeReason(trimTo(reason, 255));
        sessionRepository.save(s);
        activeCache.remove(s.getSid());
    }

    private void decideTrust(LoginTrust t, String status) {
        t.setStatus(status);
        t.setDecidedAt(Instant.now());
        t.setDecidedBy(actor());
        trustRepository.save(t);
    }

    private LoginTrust requireTrustRow(Long id) {
        return trustRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Trust not found", "loginControl", "trustNotFound"));
    }

    private static String describe(LoginTrust t) {
        return kindLabel(t) + " " + t.getTrustValue() + (t.getLabel() != null ? " · " + t.getLabel() : "");
    }

    private static String kindLabel(LoginTrust t) {
        return LoginTrust.KIND_IP.equals(t.getKind()) ? "IP" : "thiết bị";
    }

    private boolean isAdmin(Authentication auth, String login) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (AuthoritiesConstants.ADMIN.equals(a.getAuthority())) {
                return true;
            }
        }
        return roleOf(login).map(r -> r == RoleCode.AD).orElse(false);
    }

    private Optional<RoleCode> roleOf(String login) {
        return staffProfileRepository.findOneByUserLoginIgnoreCase(login).map(StaffProfile::getRoleCode);
    }

    private static String actor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }

    private static String trimTo(String v, int max) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }
}
