package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.auth.LoginControlService;
import com.mycompany.myapp.service.auth.LoginControlService.SessionDTO;
import com.mycompany.myapp.service.auth.LoginControlService.TrustDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin: duyệt IP / thiết bị đăng nhập và quản lý phiên ({@code /api/admin/**} chỉ ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/login-control")
public class LoginControlResource {

    private final LoginControlService loginControlService;

    public LoginControlResource(LoginControlService loginControlService) {
        this.loginControlService = loginControlService;
    }

    @GetMapping("/trusts")
    public List<TrustDTO> trusts(@RequestParam(required = false) String status) {
        return loginControlService.listTrusts(status);
    }

    @PostMapping("/trusts/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        loginControlService.approveTrust(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trusts/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        loginControlService.rejectTrust(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trusts/{id}/revoke")
    public ResponseEntity<Void> revokeTrust(@PathVariable Long id) {
        loginControlService.revokeTrust(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public List<SessionDTO> sessions() {
        return loginControlService.listActiveSessions();
    }

    @PostMapping("/sessions/{id}/revoke")
    public ResponseEntity<Void> revokeSession(@PathVariable Long id) {
        loginControlService.revokeSession(id);
        return ResponseEntity.noContent().build();
    }
}
