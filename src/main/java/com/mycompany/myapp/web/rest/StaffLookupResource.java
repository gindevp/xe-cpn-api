package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.StaffProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Tra cứu thông tin công việc của một nhân viên (popup người nộp tiền, người tác động…). */
@RestController
@RequestMapping("/api/staff-lookup")
public class StaffLookupResource {

    private final StaffProfileRepository staffProfileRepository;

    public StaffLookupResource(StaffProfileRepository staffProfileRepository) {
        this.staffProfileRepository = staffProfileRepository;
    }

    /** {@code key}: tài khoản đăng nhập hoặc mã nhân viên. */
    @GetMapping("/{key}")
    @Transactional(readOnly = true)
    public StaffCardDTO lookup(@PathVariable("key") String key) {
        String k = key == null ? "" : key.trim();
        if (k.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        StaffProfile p = staffProfileRepository
            .findOneByUserLoginIgnoreCase(k)
            .or(() -> staffProfileRepository.findOneByStaffCodeIgnoreCase(k))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        return new StaffCardDTO(
            p.getUserLogin(),
            p.getStaffCode(),
            p.getDisplayName(),
            p.getRoleCode() != null ? p.getRoleCode().name() : null,
            p.getRoleGroup() != null ? p.getRoleGroup().getName() : null,
            p.getOffice() != null ? p.getOffice().getCode() : null,
            p.getOffice() != null ? p.getOffice().getName() : null,
            Boolean.TRUE.equals(p.getScopeAllOffices()),
            Boolean.TRUE.equals(p.getActive())
        );
    }

    public record StaffCardDTO(
        String username,
        String staffCode,
        String displayName,
        String roleCode,
        String roleGroupName,
        String officeCode,
        String officeName,
        boolean allOffices,
        boolean active
    ) {}
}
