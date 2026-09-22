package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.config.MaintenancePolicyService;
import com.mycompany.myapp.service.dto.MaintenancePolicyDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MaintenancePolicyResource {

    private final MaintenancePolicyService maintenancePolicyService;

    public MaintenancePolicyResource(MaintenancePolicyService maintenancePolicyService) {
        this.maintenancePolicyService = maintenancePolicyService;
    }

    /** Public: web/app hỏi trước khi vào, không cần token. */
    @GetMapping("/api/maintenance")
    public MaintenancePolicyDTO getMaintenance() {
        return maintenancePolicyService.getPolicy();
    }

    /** Admin (ROLE_ADMIN + màn Bảo trì qua StaffWriteGuardFilter). */
    @PutMapping("/api/admin/maintenance")
    public MaintenancePolicyDTO putMaintenance(@Valid @RequestBody MaintenancePolicyDTO body) {
        try {
            return maintenancePolicyService.putPolicy(body);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
