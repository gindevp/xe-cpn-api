package com.mycompany.myapp.security;

import com.mycompany.myapp.domain.RoleGroup;
import com.mycompany.myapp.domain.RoleGroupScreen;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.RoleGroupRepository;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves effective screen permissions of a staff profile from its role group, falling back to the
 * built-in matrix when no group is assigned.
 */
@Service
@Transactional(readOnly = true)
public class PermissionService {

    private final RoleGroupRepository roleGroupRepository;

    /** Keyed by lowercase login; cleared whenever a group or an assignment changes. */
    private final Map<String, Map<ScreenKey, ScreenPerm>> cache = new ConcurrentHashMap<>();

    public PermissionService(RoleGroupRepository roleGroupRepository) {
        this.roleGroupRepository = roleGroupRepository;
    }

    public void invalidateCache() {
        cache.clear();
    }

    /** ROLE_ADMIN in the JWT always wins, so an admin can never be locked out by group edits. */
    public boolean isSystemAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);
    }

    public Map<ScreenKey, ScreenPerm> permissionsOf(StaffProfile profile) {
        if (profile == null) {
            return fullAccess();
        }
        String key = profile.getUserLogin() == null ? "" : profile.getUserLogin().toLowerCase();
        Map<ScreenKey, ScreenPerm> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Map<ScreenKey, ScreenPerm> resolved = resolve(profile);
        cache.put(key, resolved);
        return resolved;
    }

    public ScreenPerm permOf(StaffProfile profile, ScreenKey screen) {
        return permissionsOf(profile).getOrDefault(screen, ScreenPerm.N);
    }

    /** A profile with no write grant at all is read-only everywhere (BL by default). */
    public boolean hasAnyWrite(StaffProfile profile) {
        return permissionsOf(profile).values().stream().anyMatch(ScreenPerm::canWrite);
    }

    public Optional<String> groupCodeOf(StaffProfile profile) {
        if (profile == null) {
            return Optional.empty();
        }
        if (profile.getRoleGroup() != null) {
            return Optional.ofNullable(profile.getRoleGroup().getCode());
        }
        return Optional.ofNullable(profile.getRoleCode()).map(Enum::name);
    }

    private Map<ScreenKey, ScreenPerm> resolve(StaffProfile profile) {
        RoleGroup group = profile.getRoleGroup();
        if (group == null && profile.getRoleCode() != null) {
            group = roleGroupRepository.findOneByCodeIgnoreCase(profile.getRoleCode().name()).orElse(null);
        }
        if (group == null || Boolean.FALSE.equals(group.getActive())) {
            Map<ScreenKey, ScreenPerm> fallback = profile.getRoleCode() == null
                ? emptyAccess()
                : new EnumMap<>(DefaultScreenMatrix.forRole(profile.getRoleCode()));
            return deriveAggregates(fallback);
        }
        Map<ScreenKey, ScreenPerm> out = emptyAccess();
        for (RoleGroupScreen grant : group.getScreens()) {
            ScreenKey.fromKey(grant.getScreenKey()).ifPresent(screen -> out.put(screen, grant.getPermLevel()));
        }
        return deriveAggregates(out);
    }

    /** Trang tác vụ app mở theo tác vụ được cấp, không cấp riêng. */
    private static Map<ScreenKey, ScreenPerm> deriveAggregates(Map<ScreenKey, ScreenPerm> perms) {
        ScreenPerm best = ScreenPerm.N;
        for (ScreenKey task : ScreenKey.appTasks()) {
            ScreenPerm p = perms.getOrDefault(task, ScreenPerm.N);
            if (p == ScreenPerm.Y) {
                best = ScreenPerm.Y;
                break;
            }
            if (p == ScreenPerm.R) {
                best = ScreenPerm.R;
            }
        }
        perms.put(ScreenKey.TAC_VU, best);
        return perms;
    }

    private static Map<ScreenKey, ScreenPerm> emptyAccess() {
        Map<ScreenKey, ScreenPerm> out = new EnumMap<>(ScreenKey.class);
        for (ScreenKey screen : ScreenKey.values()) {
            out.put(screen, ScreenPerm.N);
        }
        return out;
    }

    private static Map<ScreenKey, ScreenPerm> fullAccess() {
        Map<ScreenKey, ScreenPerm> out = new EnumMap<>(ScreenKey.class);
        for (ScreenKey screen : ScreenKey.values()) {
            out.put(screen, ScreenPerm.Y);
        }
        return out;
    }

    public Map<ScreenKey, ScreenPerm> fullAccessMap() {
        return fullAccess();
    }
}
