package com.mycompany.myapp.service.staff;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FE /tai-khoan fields: username, role, office, active (+ optional password).
 */
@Service
@Transactional
public class StaffAdminFacadeService {

    private static final String ENTITY = "staffAdmin";

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final OfficeRepository officeRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffAdminFacadeService(
        UserRepository userRepository,
        StaffProfileRepository staffProfileRepository,
        OfficeRepository officeRepository,
        AuthorityRepository authorityRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.officeRepository = officeRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StaffUserDTO> list() {
        return staffProfileRepository.findAll().stream().map(this::toDto).toList();
    }

    public StaffUserDTO upsert(StaffUserDTO req) {
        if (req == null || req.username() == null || req.username().isBlank()) {
            throw new BadRequestAlertException("username required", ENTITY, "usernameRequired");
        }
        String login = req.username().trim().toLowerCase();
        RoleCode role;
        try {
            role = RoleCode.valueOf(req.roleCode() == null ? "Q" : req.roleCode().trim().toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestAlertException("Invalid roleCode", ENTITY, "invalidRole");
        }

        User user = userRepository.findOneByLogin(login).orElse(null);
        if (user == null) {
            if (req.password() == null || req.password().isBlank()) {
                throw new BadRequestAlertException("password required for new user", ENTITY, "passwordRequired");
            }
            user = new User();
            user.setLogin(login);
            user.setActivated(Boolean.TRUE.equals(req.active()));
            user.setPassword(passwordEncoder.encode(req.password()));
            user.setLangKey("vi");
            Set<Authority> authorities = new HashSet<>();
            authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
            if (role == RoleCode.AD) {
                authorityRepository.findById(AuthoritiesConstants.ADMIN).ifPresent(authorities::add);
            }
            user.setAuthorities(authorities);
            user = userRepository.save(user);
        } else {
            if (req.active() != null) {
                user.setActivated(req.active());
            }
            if (req.password() != null && !req.password().isBlank()) {
                user.setPassword(passwordEncoder.encode(req.password()));
            }
            user = userRepository.save(user);
        }

        StaffProfile profile = staffProfileRepository.findOneByUserLoginIgnoreCase(login).orElse(null);
        if (profile == null) {
            profile = new StaffProfile();
            profile.setUserLogin(login);
            profile.setStaffCode(login.toUpperCase().replace(".", ""));
            profile.setDisplayName(login);
        }
        profile.setRoleCode(role);
        profile.setActive(req.active() == null || Boolean.TRUE.equals(req.active()));
        boolean allOffices = req.officeCode() != null && "ALL".equalsIgnoreCase(req.officeCode());
        profile.setScopeAllOffices(allOffices);
        // StaffProfile.office is @NotNull — ALL scope still anchors to a home office (GP preferred).
        Office office;
        if (!allOffices && req.officeCode() != null && !req.officeCode().isBlank()) {
            office = officeRepository
                .findOneByCode(req.officeCode().trim().toUpperCase())
                .orElseThrow(() -> new BadRequestAlertException("Office not found", ENTITY, "officeNotFound"));
        } else {
            office = officeRepository
                .findOneByCode("GP")
                .or(() -> officeRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new BadRequestAlertException("No office available", ENTITY, "officeMissing"));
        }
        profile.setOffice(office);
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(login + "@cpn.local");
            userRepository.save(user);
        }
        profile = staffProfileRepository.save(profile);
        return toDto(profile);
    }

    private StaffUserDTO toDto(StaffProfile p) {
        boolean active = !Boolean.FALSE.equals(p.getActive());
        Boolean activated = userRepository.findOneByLogin(p.getUserLogin()).map(User::isActivated).orElse(active);
        return new StaffUserDTO(
            p.getUserLogin(),
            p.getRoleCode() != null ? p.getRoleCode().name() : null,
            Boolean.TRUE.equals(p.getScopeAllOffices()) ? "ALL" : (p.getOffice() != null ? p.getOffice().getCode() : null),
            activated,
            null
        );
    }

    public record StaffUserDTO(String username, String roleCode, String officeCode, Boolean active, String password) {}
}
