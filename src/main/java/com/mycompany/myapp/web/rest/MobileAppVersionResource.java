package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.config.MobileAppVersionService;
import com.mycompany.myapp.service.dto.MobileAppVersionDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class MobileAppVersionResource {

    private final MobileAppVersionService mobileAppVersionService;

    public MobileAppVersionResource(MobileAppVersionService mobileAppVersionService) {
        this.mobileAppVersionService = mobileAppVersionService;
    }

    /** Public: app mobile hỏi trước khi đăng nhập, không có token. */
    @GetMapping("/api/mobile/app-version")
    public MobileAppVersionDTO getMobileAppVersion() {
        return mobileAppVersionService.getPolicy();
    }

    /** Admin (ROLE_ADMIN qua SecurityConfiguration + screen Tích hợp qua StaffWriteGuardFilter). */
    @PutMapping("/api/admin/mobile-app-version")
    public MobileAppVersionDTO putMobileAppVersion(@Valid @RequestBody MobileAppVersionDTO body) {
        return mobileAppVersionService.putPolicy(body);
    }
}
