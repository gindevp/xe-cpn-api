package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.PermissionService;
import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.ScreenPerm;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.security.RoleGroupService;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Permission catalog, current-user permissions, and permission group admin.
 */
@RestController
@RequestMapping("/api")
public class PermissionGroupResource {

    private final RoleGroupService roleGroupService;
    private final PermissionService permissionService;
    private final StaffAccessService staffAccessService;

    public PermissionGroupResource(
        RoleGroupService roleGroupService,
        PermissionService permissionService,
        StaffAccessService staffAccessService
    ) {
        this.roleGroupService = roleGroupService;
        this.permissionService = permissionService;
        this.staffAccessService = staffAccessService;
    }

    /** Screen catalog for the admin matrix UI. */
    @GetMapping("/permissions/screens")
    public List<ScreenDTO> screens() {
        return Arrays.stream(ScreenKey.values())
            .filter(s -> !s.derived())
            .map(s -> new ScreenDTO(s.key(), s.label(), s.module(), s.hidden()))
            .toList();
    }

    /** Effective permissions of the caller, consumed by web + mobile to gate the UI. */
    @GetMapping("/permissions/me")
    public MyPermissionsDTO myPermissions() {
        Map<ScreenKey, ScreenPerm> perms = permissionService.isSystemAdmin()
            ? permissionService.fullAccessMap()
            : staffAccessService.current().map(permissionService::permissionsOf).orElseGet(permissionService::fullAccessMap);
        Map<String, String> out = new LinkedHashMap<>();
        perms.forEach((screen, perm) -> out.put(screen.key(), perm.name()));
        String groupCode = staffAccessService.current().flatMap(permissionService::groupCodeOf).orElse(null);
        return new MyPermissionsDTO(groupCode, permissionService.isSystemAdmin(), out);
    }

    @GetMapping("/permission-groups")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<RoleGroupService.RoleGroupDTO> list() {
        return roleGroupService.list();
    }

    @PostMapping("/permission-groups")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public RoleGroupService.RoleGroupDTO create(@RequestBody RoleGroupService.RoleGroupDTO body) {
        return roleGroupService.create(body);
    }

    @PutMapping("/permission-groups/{code}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public RoleGroupService.RoleGroupDTO update(@PathVariable String code, @RequestBody RoleGroupService.RoleGroupDTO body) {
        return roleGroupService.update(code, body);
    }

    @DeleteMapping("/permission-groups/{code}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        roleGroupService.delete(code);
        return ResponseEntity.noContent().build();
    }

    public record ScreenDTO(String key, String label, String module, boolean hidden) {}

    public record MyPermissionsDTO(String groupCode, boolean systemAdmin, Map<String, String> screens) {}
}
