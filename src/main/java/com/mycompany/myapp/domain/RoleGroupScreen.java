package com.mycompany.myapp.domain;

import com.mycompany.myapp.security.ScreenPerm;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * One screen grant inside a {@link RoleGroup}. `screenKey` uses the FE rbac.ts key (e.g. "pod-quay").
 */
@Entity
@Table(name = "role_group_screen")
public class RoleGroupScreen implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_group_id", nullable = false)
    private RoleGroup roleGroup;

    @NotNull
    @Size(max = 50)
    @Column(name = "screen_key", length = 50, nullable = false)
    private String screenKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "perm_level", length = 1, nullable = false)
    private ScreenPerm permLevel;

    public RoleGroupScreen() {}

    public RoleGroupScreen(RoleGroup roleGroup, String screenKey, ScreenPerm permLevel) {
        this.roleGroup = roleGroup;
        this.screenKey = screenKey;
        this.permLevel = permLevel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RoleGroup getRoleGroup() {
        return roleGroup;
    }

    public void setRoleGroup(RoleGroup roleGroup) {
        this.roleGroup = roleGroup;
    }

    public String getScreenKey() {
        return screenKey;
    }

    public void setScreenKey(String screenKey) {
        this.screenKey = screenKey;
    }

    public ScreenPerm getPermLevel() {
        return permLevel;
    }

    public void setPermLevel(ScreenPerm permLevel) {
        this.permLevel = permLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleGroupScreen)) {
            return false;
        }
        return getId() != null && getId().equals(((RoleGroupScreen) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
