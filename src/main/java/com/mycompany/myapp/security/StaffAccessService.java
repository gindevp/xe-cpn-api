package com.mycompany.myapp.security;

import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.StaffProfileRepository;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class StaffAccessService {

    private final StaffProfileRepository staffProfileRepository;

    public StaffAccessService(StaffProfileRepository staffProfileRepository) {
        this.staffProfileRepository = staffProfileRepository;
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
        if (p.getRoleCode() == RoleCode.BL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Read-only role (BL)");
        }
        if (p.getRoleCode() == RoleCode.KH) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer role cannot mutate ops APIs");
        }
    }

    /** FE rbac.ts screen write (Y) — ADMIN JWT without profile always allowed. */
    public void requireAnyRole(Set<RoleCode> allowed) {
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            return;
        }
        Optional<StaffProfile> profile = current();
        if (profile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile required");
        }
        requireWritable();
        RoleCode role = profile.get().getRoleCode();
        if (role == null || !allowed.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not allowed for this screen write");
        }
    }

    public void requireForceCloseRole() {
        Optional<StaffProfile> profile = current();
        if (profile.isEmpty()) {
            if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
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

    public static Set<RoleCode> roles(RoleCode... codes) {
        return EnumSet.copyOf(Set.of(codes));
    }
}
