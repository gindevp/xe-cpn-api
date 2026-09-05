package com.mycompany.myapp.service.staff;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.RoleGroup;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.PermissionService;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.security.RoleGroupService;
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
    private final RoleGroupService roleGroupService;
    private final PermissionService permissionService;

    public StaffAdminFacadeService(
        UserRepository userRepository,
        StaffProfileRepository staffProfileRepository,
        OfficeRepository officeRepository,
        AuthorityRepository authorityRepository,
        PasswordEncoder passwordEncoder,
        RoleGroupService roleGroupService,
        PermissionService permissionService
    ) {
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.officeRepository = officeRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleGroupService = roleGroupService;
        this.permissionService = permissionService;
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
        RoleGroup group = null;
        if (req.roleGroupCode() != null && !req.roleGroupCode().isBlank()) {
            group = roleGroupService
                .findByCode(req.roleGroupCode())
                .orElseThrow(() -> new BadRequestAlertException("Nhóm quyền không tồn tại", ENTITY, "roleGroupNotFound"));
        }
        RoleCode role;
        if (group != null) {
            role = group.getBaseRoleCode();
        } else {
            try {
                role = RoleCode.valueOf(req.roleCode() == null ? "DH" : req.roleCode().trim().toUpperCase());
            } catch (Exception ex) {
                throw new BadRequestAlertException("Invalid roleCode", ENTITY, "invalidRole");
            }
            // Không chọn nhóm → dùng nhóm dựng sẵn của chức danh.
            group = roleGroupService.findByCode(role.name()).orElse(null);
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
            syncAdminAuthority(user, role);
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
        profile.setRoleGroup(group);
        profile.setActive(req.active() == null || Boolean.TRUE.equals(req.active()));
        boolean allOffices = req.officeCode() != null && "ALL".equalsIgnoreCase(req.officeCode());
        profile.setScopeAllOffices(allOffices);
        // StaffProfile.office is @NotNull — ALL scope still needs a home office (any master office).
        Office office;
        if (!allOffices && req.officeCode() != null && !req.officeCode().isBlank()) {
            office = officeRepository
                .findOneByCode(req.officeCode().trim().toUpperCase())
                .orElseThrow(() -> new BadRequestAlertException("Office not found", ENTITY, "officeNotFound"));
        } else {
            office = officeRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestAlertException("No office available", ENTITY, "officeMissing"));
        }
        profile.setOffice(office);
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(login + "@cpn.local");
            userRepository.save(user);
        }
        profile = staffProfileRepository.save(profile);
        permissionService.invalidateCache();
        return toDto(profile);
    }

    public void delete(String loginRaw) {
        if (loginRaw == null || loginRaw.isBlank()) {
            throw new BadRequestAlertException("username required", ENTITY, "usernameRequired");
        }
        String login = loginRaw.trim().toLowerCase();
        boolean self = SecurityUtils.getCurrentUserLogin().filter(cur -> cur.equalsIgnoreCase(login)).isPresent();
        if (self) {
            throw new BadRequestAlertException("Không thể xóa chính tài khoản đang đăng nhập", ENTITY, "cannotDeleteSelf");
        }
        staffProfileRepository.findOneByUserLoginIgnoreCase(login).ifPresent(staffProfileRepository::delete);
        userRepository.findOneByLogin(login).ifPresent(userRepository::delete);
        permissionService.invalidateCache();
    }

    /** ROLE_ADMIN follows the AD job title, and nobody may demote their own admin account. */
    private void syncAdminAuthority(User user, RoleCode role) {
        Set<Authority> authorities = new HashSet<>(user.getAuthorities());
        boolean hasAdmin = authorities.stream().anyMatch(a -> AuthoritiesConstants.ADMIN.equals(a.getName()));
        if (role == RoleCode.AD) {
            if (!hasAdmin) {
                authorityRepository.findById(AuthoritiesConstants.ADMIN).ifPresent(authorities::add);
                user.setAuthorities(authorities);
            }
            return;
        }
        if (!hasAdmin) {
            return;
        }
        boolean self = SecurityUtils.getCurrentUserLogin().filter(login -> login.equalsIgnoreCase(user.getLogin())).isPresent();
        if (self) {
            throw new BadRequestAlertException("Không thể tự bỏ quyền Admin của chính mình", ENTITY, "cannotDemoteSelf");
        }
        authorities.removeIf(a -> AuthoritiesConstants.ADMIN.equals(a.getName()));
        user.setAuthorities(authorities);
    }

    private StaffUserDTO toDto(StaffProfile p) {
        boolean active = !Boolean.FALSE.equals(p.getActive());
        Boolean activated = userRepository.findOneByLogin(p.getUserLogin()).map(User::isActivated).orElse(active);
        return new StaffUserDTO(
            p.getUserLogin(),
            p.getRoleCode() != null ? p.getRoleCode().name() : null,
            Boolean.TRUE.equals(p.getScopeAllOffices()) ? "ALL" : (p.getOffice() != null ? p.getOffice().getCode() : null),
            activated,
            null,
            p.getRoleGroup() != null ? p.getRoleGroup().getCode() : (p.getRoleCode() != null ? p.getRoleCode().name() : null)
        );
    }

    public record StaffUserDTO(
        String username,
        String roleCode,
        String officeCode,
        Boolean active,
        String password,
        String roleGroupCode
    ) {}
}
