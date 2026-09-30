package com.mycompany.myapp.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.LoginTrust;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.UserSession;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.LoginTrustRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.repository.UserSessionRepository;
import com.mycompany.myapp.service.audit.AuditRecorder;
import com.mycompany.myapp.service.auth.LoginControlService.LoginDecision;
import com.mycompany.myapp.service.auth.LoginControlService.LoginRequestInfo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class LoginControlServiceTest {

    @Mock
    private LoginTrustRepository trustRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @Mock
    private AuditRecorder auditRecorder;

    private LoginControlService service;
    private final Instant exp = Instant.now().plusSeconds(3600);

    @BeforeEach
    void setUp() {
        service = new LoginControlService(trustRepository, sessionRepository, staffProfileRepository, auditRecorder);
        lenient().when(sessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(trustRepository.save(any(LoginTrust.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Authentication user(String login, String... authorities) {
        return new UsernamePasswordAuthenticationToken(
            login,
            "x",
            java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()
        );
    }

    private void role(String login, RoleCode role) {
        StaffProfile p = new StaffProfile();
        p.setRoleCode(role);
        lenient().when(staffProfileRepository.findOneByUserLoginIgnoreCase(login)).thenReturn(Optional.of(p));
    }

    private static LoginRequestInfo web(String ip) {
        return new LoginRequestInfo("WEB", ip, null, null, "Chrome");
    }

    @Test
    void dispatcherFromNewIp_isBlockedAndPendingSaved() {
        role("dh1", RoleCode.DH);

        LoginDecision d = service.decide(user("dh1", "ROLE_USER"), web("1.2.3.4"), exp);

        assertThat(d.allowed()).isFalse();
        assertThat(d.code()).isEqualTo("error.loginPendingApproval");
        ArgumentCaptor<LoginTrust> cap = ArgumentCaptor.forClass(LoginTrust.class);
        verify(trustRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(LoginTrust.PENDING);
        assertThat(cap.getValue().getTrustValue()).isEqualTo("1.2.3.4");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void dispatcherFromApprovedIp_opensSessionAndKicksOldOne() {
        role("dh1", RoleCode.DH);
        LoginTrust t = new LoginTrust();
        t.setStatus(LoginTrust.APPROVED);
        when(trustRepository.findOneByUserLoginIgnoreCaseAndKindAndTrustValue("dh1", LoginTrust.KIND_IP, "1.2.3.4")).thenReturn(
            Optional.of(t)
        );
        UserSession old = new UserSession();
        old.setSid("old");
        when(sessionRepository.findByUserLoginIgnoreCaseAndChannelAndRevokedAtIsNull("dh1", UserSession.WEB)).thenReturn(List.of(old));

        LoginDecision d = service.decide(user("dh1", "ROLE_USER"), web("1.2.3.4"), exp);

        assertThat(d.allowed()).isTrue();
        assertThat(d.sid()).isNotBlank();
        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(old.getRevokeReason()).isEqualTo("Đăng nhập nơi khác");
    }

    @Test
    void rejectedIp_staysBlocked() {
        role("kt1", RoleCode.KT);
        LoginTrust t = new LoginTrust();
        t.setStatus(LoginTrust.REJECTED);
        when(trustRepository.findOneByUserLoginIgnoreCaseAndKindAndTrustValue("kt1", LoginTrust.KIND_IP, "5.5.5.5")).thenReturn(
            Optional.of(t)
        );

        LoginDecision d = service.decide(user("kt1", "ROLE_USER"), web("5.5.5.5"), exp);

        assertThat(d.allowed()).isFalse();
        assertThat(d.code()).isEqualTo("error.loginRejected");
    }

    @Test
    void counterStaffOnWeb_isNotIpControlled() {
        role("q1", RoleCode.Q);

        LoginDecision d = service.decide(user("q1", "ROLE_USER"), web("9.9.9.9"), exp);

        assertThat(d.allowed()).isTrue();
        verify(trustRepository, never()).save(any());
    }

    @Test
    void appWithoutDeviceId_isBlocked() {
        role("q1", RoleCode.Q);

        LoginDecision d = service.decide(user("q1", "ROLE_USER"), new LoginRequestInfo("APP", "9.9.9.9", null, null, null), exp);

        assertThat(d.allowed()).isFalse();
        assertThat(d.code()).isEqualTo("error.deviceIdRequired");
    }

    @Test
    void appNewDevice_isPending() {
        role("q1", RoleCode.Q);

        LoginDecision d = service.decide(
            user("q1", "ROLE_USER"),
            new LoginRequestInfo("APP", "9.9.9.9", "dev-1", "Samsung A52", null),
            exp
        );

        assertThat(d.allowed()).isFalse();
        assertThat(d.code()).isEqualTo("error.loginPendingApproval");
        assertThat(d.kind()).isEqualTo(LoginTrust.KIND_DEVICE);
    }

    @Test
    void admin_bypassesApproval() {
        LoginDecision d = service.decide(user("admin", "ROLE_ADMIN"), new LoginRequestInfo("APP", "9.9.9.9", "dev-x", null, null), exp);

        assertThat(d.allowed()).isTrue();
        verify(trustRepository, never()).save(any());
    }

    @Test
    void resolveChannel_infersLegacyAppFromUserAgent() {
        assertThat(LoginControlService.resolveChannel("app", "okhttp/4.9.2")).isEqualTo("APP");
        assertThat(LoginControlService.resolveChannel(null, "okhttp/4.9.2")).isEqualTo(LoginControlService.CHANNEL_LEGACY_APP);
        assertThat(LoginControlService.resolveChannel(null, "XE/9 CFNetwork/1498 Darwin/23.6.0")).isEqualTo(
            LoginControlService.CHANNEL_LEGACY_APP
        );
        assertThat(LoginControlService.resolveChannel(null, "Mozilla/5.0 (Windows NT 10.0) Chrome/152")).isEqualTo("WEB");
    }

    @Test
    void legacyApp_opensAppSessionWithoutKickingWeb() {
        role("q1", RoleCode.Q);

        LoginDecision d = service.decide(
            user("q1", "ROLE_USER"),
            new LoginRequestInfo(LoginControlService.CHANNEL_LEGACY_APP, "9.9.9.9", null, null, "okhttp/4.9.2"),
            exp
        );

        assertThat(d.allowed()).isTrue();
        verify(sessionRepository).findByUserLoginIgnoreCaseAndChannelAndRevokedAtIsNull("q1", UserSession.APP);
        verify(sessionRepository, never()).findByUserLoginIgnoreCaseAndChannelAndRevokedAtIsNull("q1", UserSession.WEB);
        ArgumentCaptor<UserSession> cap = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(cap.capture());
        assertThat(cap.getValue().getChannel()).isEqualTo(UserSession.APP);
    }

    @Test
    void legacyApp_dispatcherSkipsIpApproval() {
        role("dh1", RoleCode.DH);

        LoginDecision d = service.decide(
            user("dh1", "ROLE_USER"),
            new LoginRequestInfo(LoginControlService.CHANNEL_LEGACY_APP, "1.2.3.4", null, null, "okhttp/4.9.2"),
            exp
        );

        assertThat(d.allowed()).isTrue();
        verify(trustRepository, never()).save(any());
        ArgumentCaptor<UserSession> cap = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(cap.capture());
        assertThat(cap.getValue().getChannel()).isEqualTo(UserSession.APP);
    }

    @Test
    void web_dispatcherStillNeedsApprovedIp() {
        role("dh1", RoleCode.DH);

        LoginDecision d = service.decide(
            user("dh1", "ROLE_USER"),
            new LoginRequestInfo(UserSession.WEB, "1.2.3.4", null, null, "Mozilla/5.0"),
            exp
        );

        assertThat(d.allowed()).isFalse();
        assertThat(d.code()).isEqualTo("error.loginPendingApproval");
        assertThat(d.kind()).isEqualTo(LoginTrust.KIND_IP);
    }
}
