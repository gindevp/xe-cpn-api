package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.config.SessionPolicyService;
import com.mycompany.myapp.service.dto.SessionPolicyDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SessionPolicyResource {

    private final SessionPolicyService sessionPolicyService;

    public SessionPolicyResource(SessionPolicyService sessionPolicyService) {
        this.sessionPolicyService = sessionPolicyService;
    }

    /** Web đang đăng nhập hỏi giờ tự đăng xuất. */
    @GetMapping("/api/session-policy")
    public SessionPolicyDTO getPolicy() {
        return sessionPolicyService.getPolicy();
    }

    @PutMapping("/api/admin/session-policy")
    public SessionPolicyDTO putPolicy(@RequestBody SessionPolicyDTO body) {
        try {
            return sessionPolicyService.putPolicy(body);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
