package com.mycompany.myapp.domain;

import com.mycompany.myapp.domain.enumeration.RoleCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Permission group (chức danh) — a named set of screen permissions assignable to staff.
 */
@Entity
@Table(name = "role_group")
public class RoleGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 30)
    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    /** Legacy RoleCode kept for non-screen logic (office scope, force close, report filters). */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "base_role_code", length = 10, nullable = false)
    private RoleCode baseRoleCode;

    /** Seeded job titles (Q, BX, G, KT, TCN, DH, BL, AD) — cannot be deleted. */
    @NotNull
    @Column(name = "builtin", nullable = false)
    private Boolean builtin = Boolean.FALSE;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "roleGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RoleGroupScreen> screens = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoleCode getBaseRoleCode() {
        return baseRoleCode;
    }

    public void setBaseRoleCode(RoleCode baseRoleCode) {
        this.baseRoleCode = baseRoleCode;
    }

    public Boolean getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Boolean builtin) {
        this.builtin = builtin;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<RoleGroupScreen> getScreens() {
        return screens;
    }

    public void setScreens(Set<RoleGroupScreen> screens) {
        this.screens = screens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleGroup)) {
            return false;
        }
        return getId() != null && getId().equals(((RoleGroup) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RoleGroup{id=" + id + ", code='" + code + "', builtin=" + builtin + "}";
    }
}
