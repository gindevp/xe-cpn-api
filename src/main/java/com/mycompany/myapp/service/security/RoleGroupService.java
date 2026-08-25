package com.mycompany.myapp.service.security;

import com.mycompany.myapp.domain.RoleGroup;
import com.mycompany.myapp.domain.RoleGroupScreen;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.RoleGroupRepository;
import com.mycompany.myapp.security.DefaultScreenMatrix;
import com.mycompany.myapp.security.PermissionService;
import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.ScreenPerm;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for permission groups (chức danh) + idempotent seeding of the built-in groups from
 * {@link DefaultScreenMatrix}, so a fresh or existing DB keeps today's behaviour.
 */
@Service
@Transactional
@Order(50)
public class RoleGroupService implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(RoleGroupService.class);
    private static final String ENTITY = "roleGroup";
    /** Admin group is always full access and must stay editable-proof. */
    public static final String ADMIN_GROUP_CODE = "AD";

    private final RoleGroupRepository roleGroupRepository;
    private final PermissionService permissionService;

    public RoleGroupService(RoleGroupRepository roleGroupRepository, PermissionService permissionService) {
        this.roleGroupRepository = roleGroupRepository;
        this.permissionService = permissionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedBuiltins();
    }

    public void seedBuiltins() {
        for (RoleCode role : DefaultScreenMatrix.builtinRoles()) {
            if (roleGroupRepository.findOneByCodeIgnoreCase(role.name()).isPresent()) {
                continue;
            }
            RoleGroup group = new RoleGroup();
            group.setCode(role.name());
            group.setName(DefaultScreenMatrix.labelOf(role));
            group.setDescription("Nhóm quyền dựng sẵn theo chức danh " + DefaultScreenMatrix.labelOf(role));
            group.setBaseRoleCode(role);
            group.setBuiltin(Boolean.TRUE);
            group.setActive(Boolean.TRUE);
            applyScreens(group, defaultScreensOf(role));
            roleGroupRepository.save(group);
            LOG.info("Seeded built-in permission group {}", role.name());
        }
        permissionService.invalidateCache();
    }

    @Transactional(readOnly = true)
    public List<RoleGroupDTO> list() {
        List<RoleGroupDTO> out = new ArrayList<>();
        for (RoleGroup g : roleGroupRepository.findAllOrdered()) {
            out.add(toDto(g, roleGroupRepository.countStaffUsingGroup(g.getId())));
        }
        return out;
    }

    public RoleGroupDTO create(RoleGroupDTO req) {
        String code = normalizeCode(req.code());
        if (roleGroupRepository.findOneByCodeIgnoreCase(code).isPresent()) {
            throw new BadRequestAlertException("Mã nhóm quyền đã tồn tại", ENTITY, "codeExists");
        }
        RoleGroup group = new RoleGroup();
        group.setCode(code);
        group.setBuiltin(Boolean.FALSE);
        applyEditable(group, req);
        RoleGroup saved = roleGroupRepository.save(group);
        permissionService.invalidateCache();
        return toDto(saved, 0);
    }

    public RoleGroupDTO update(String code, RoleGroupDTO req) {
        RoleGroup group = require(code);
        if (isAdminGroup(group)) {
            throw new BadRequestAlertException("Không thể sửa nhóm quyền Admin", ENTITY, "adminGroupLocked");
        }
        applyEditable(group, req);
        RoleGroup saved = roleGroupRepository.save(group);
        permissionService.invalidateCache();
        return toDto(saved, roleGroupRepository.countStaffUsingGroup(saved.getId()));
    }

    public void delete(String code) {
        RoleGroup group = require(code);
        if (Boolean.TRUE.equals(group.getBuiltin())) {
            throw new BadRequestAlertException("Không thể xoá nhóm quyền dựng sẵn", ENTITY, "builtinLocked");
        }
        long inUse = roleGroupRepository.countStaffUsingGroup(group.getId());
        if (inUse > 0) {
            throw new BadRequestAlertException("Còn " + inUse + " tài khoản đang dùng nhóm này", ENTITY, "groupInUse");
        }
        roleGroupRepository.delete(group);
        permissionService.invalidateCache();
    }

    @Transactional(readOnly = true)
    public Optional<RoleGroup> findByCode(String code) {
        return code == null || code.isBlank() ? Optional.empty() : roleGroupRepository.findOneByCodeIgnoreCase(code.trim());
    }

    private void applyEditable(RoleGroup group, RoleGroupDTO req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new BadRequestAlertException("Tên nhóm quyền bắt buộc", ENTITY, "nameRequired");
        }
        group.setName(req.name().trim());
        group.setDescription(req.description() == null ? null : req.description().trim());
        group.setActive(req.active() == null || Boolean.TRUE.equals(req.active()));
        RoleCode base;
        try {
            base = RoleCode.valueOf(req.baseRoleCode() == null ? "DH" : req.baseRoleCode().trim().toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestAlertException("Chức danh gốc không hợp lệ", ENTITY, "invalidBaseRole");
        }
        if (base == RoleCode.KH) {
            throw new BadRequestAlertException("Không dùng chức danh gốc KH cho nhân viên", ENTITY, "invalidBaseRole");
        }
        if (!Boolean.TRUE.equals(group.getBuiltin())) {
            group.setBaseRoleCode(base);
        } else if (group.getBaseRoleCode() == null) {
            group.setBaseRoleCode(base);
        }
        // Partial update: bỏ trống screens là chỉ sửa thông tin nhóm, không xoá quyền đang có.
        if (req.screens() != null) {
            applyScreens(group, parseScreens(req.screens()));
        }
    }

    private static Map<ScreenKey, ScreenPerm> parseScreens(Map<String, String> raw) {
        Map<ScreenKey, ScreenPerm> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        raw.forEach((key, value) -> ScreenKey.fromKey(key).ifPresent(screen -> out.put(screen, ScreenPerm.fromCode(value))));
        return out;
    }

    private static Map<ScreenKey, ScreenPerm> defaultScreensOf(RoleCode role) {
        return DefaultScreenMatrix.forRole(role);
    }

    /**
     * Syncs the grant rows in place; N is not persisted so absence means "no access".
     *
     * <p>Không clear-rồi-add-lại: Hibernate flush insert trước delete nên key giữ nguyên sẽ đụng
     * unique index (role_group_id, screen_key).
     */
    private static void applyScreens(RoleGroup group, Map<ScreenKey, ScreenPerm> perms) {
        Map<String, ScreenPerm> target = new LinkedHashMap<>();
        perms.forEach((screen, perm) -> {
            if (!screen.derived() && perm != null && perm != ScreenPerm.N) {
                target.put(screen.key(), perm);
            }
        });
        group
            .getScreens()
            .removeIf(row -> {
                ScreenPerm wanted = target.remove(row.getScreenKey());
                if (wanted == null) {
                    return true;
                }
                row.setPermLevel(wanted);
                return false;
            });
        target.forEach((key, perm) -> group.getScreens().add(new RoleGroupScreen(group, key, perm)));
    }

    private RoleGroup require(String code) {
        return findByCode(code).orElseThrow(() -> new BadRequestAlertException("Không tìm thấy nhóm quyền", ENTITY, "notFound"));
    }

    private static boolean isAdminGroup(RoleGroup group) {
        return ADMIN_GROUP_CODE.equalsIgnoreCase(group.getCode());
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestAlertException("Mã nhóm quyền bắt buộc", ENTITY, "codeRequired");
        }
        String out = code.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "");
        if (out.isBlank() || out.length() > 30) {
            throw new BadRequestAlertException("Mã nhóm quyền không hợp lệ (A-Z, 0-9, _)", ENTITY, "invalidCode");
        }
        return out;
    }

    private static RoleGroupDTO toDto(RoleGroup g, long staffCount) {
        Map<String, String> screens = new LinkedHashMap<>();
        for (ScreenKey screen : ScreenKey.values()) {
            if (!screen.derived()) {
                screens.put(screen.key(), ScreenPerm.N.name());
            }
        }
        boolean adminGroup = isAdminGroup(g);
        for (RoleGroupScreen grant : g.getScreens()) {
            ScreenKey.fromKey(grant.getScreenKey()).ifPresent(screen -> screens.put(screen.key(), grant.getPermLevel().name()));
        }
        if (adminGroup) {
            screens.replaceAll((k, v) -> ScreenPerm.Y.name());
        }
        return new RoleGroupDTO(
            g.getCode(),
            g.getName(),
            g.getDescription(),
            g.getBaseRoleCode() == null ? null : g.getBaseRoleCode().name(),
            Boolean.TRUE.equals(g.getBuiltin()),
            !Boolean.FALSE.equals(g.getActive()),
            adminGroup,
            screens,
            staffCount
        );
    }

    public record RoleGroupDTO(
        String code,
        String name,
        String description,
        String baseRoleCode,
        Boolean builtin,
        Boolean active,
        Boolean locked,
        Map<String, String> screens,
        Long staffCount
    ) {}
}
