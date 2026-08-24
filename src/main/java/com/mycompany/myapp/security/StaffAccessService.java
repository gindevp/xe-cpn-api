package com.mycompany.myapp.security;

import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.StaffProfileRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class StaffAccessService {

    private final StaffProfileRepository staffProfileRepository;
    private final PermissionService permissionService;

    public StaffAccessService(StaffProfileRepository staffProfileRepository, PermissionService permissionService) {
        this.staffProfileRepository = staffProfileRepository;
        this.permissionService = permissionService;
    }

    public Optional<StaffProfile> current() {
        return SecurityUtils.getCurrentUserLogin().flatMap(staffProfileRepository::findOneByUserLoginIgnoreCase);
    }

    public void requireWritable() {
        Optional<StaffProfile> profile = current();
        if (profile.isEmpty()) {
            return;
        }
        StaffProfile p = profile.get();
        if (Boolean.FALSE.equals(p.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile inactive");
        }
        if (p.getRoleCode() == RoleCode.KH) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer role cannot mutate ops APIs");
        }
        if (permissionService.isSystemAdmin()) {
            return;
        }
        if (!permissionService.hasAnyWrite(p)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Read-only permission group");
        }
    }

    /** Screen write (Y) on at least one of the given screens — ADMIN JWT always allowed. */
    public void requireScreenWrite(ScreenKey... screens) {
        if (permissionService.isSystemAdmin()) {
            return;
        }
        Optional<StaffProfile> profile = current();
        if (profile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile required");
        }
        requireWritable();
        StaffProfile p = profile.get();
        for (ScreenKey screen : screens) {
            if (permissionService.permOf(p, screen).canWrite()) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not allowed for this screen write");
    }

    public void requireForceCloseRole() {
        Optional<StaffProfile> profile = current();
        if (profile.isEmpty()) {
            if (permissionService.isSystemAdmin()) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Force close requires DH or AD");
        }
        RoleCode role = profile.get().getRoleCode();
        if (role != RoleCode.DH && role != RoleCode.AD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Force close requires DH or AD");
        }
    }

    public Optional<String> scopedOfficeCode() {
        return current()
            .filter(p -> !Boolean.TRUE.equals(p.getScopeAllOffices()))
            .map(p -> p.getOffice() != null ? p.getOffice().getCode() : null)
            .filter(code -> code != null && !code.isBlank());
    }
}
