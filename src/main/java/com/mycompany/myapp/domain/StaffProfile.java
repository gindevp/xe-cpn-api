package com.mycompany.myapp.domain;

import com.mycompany.myapp.domain.enumeration.RoleCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * A StaffProfile.
 */
@Entity
@Table(name = "staff_profile")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StaffProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Size(max = 30)
    @Column(name = "staff_code", length = 30, unique = true)
    private String staffCode;

    @NotNull
    @Size(max = 50)
    @Column(name = "user_login", length = 50, nullable = false, unique = true)
    private String userLogin;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false)
    private RoleCode roleCode;

    @NotNull
    @Column(name = "scope_all_offices", nullable = false)
    private Boolean scopeAllOffices;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @ManyToOne(optional = false)
    @NotNull
    private Office office;

    /** Null = fall back to the built-in group matching {@link #roleCode}. */
    @ManyToOne
    @JoinColumn(name = "role_group_id")
    private RoleGroup roleGroup;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public StaffProfile id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStaffCode() {
        return this.staffCode;
    }

    public StaffProfile staffCode(String staffCode) {
        this.setStaffCode(staffCode);
        return this;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getUserLogin() {
        return this.userLogin;
    }

    public StaffProfile userLogin(String userLogin) {
        this.setUserLogin(userLogin);
        return this;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public StaffProfile displayName(String displayName) {
        this.setDisplayName(displayName);
        return this;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public RoleCode getRoleCode() {
        return this.roleCode;
    }

    public StaffProfile roleCode(RoleCode roleCode) {
        this.setRoleCode(roleCode);
        return this;
    }

    public void setRoleCode(RoleCode roleCode) {
        this.roleCode = roleCode;
    }

    public Boolean getScopeAllOffices() {
        return this.scopeAllOffices;
    }

    public StaffProfile scopeAllOffices(Boolean scopeAllOffices) {
        this.setScopeAllOffices(scopeAllOffices);
        return this;
    }

    public void setScopeAllOffices(Boolean scopeAllOffices) {
        this.scopeAllOffices = scopeAllOffices;
    }

    public Boolean getActive() {
        return this.active;
    }

    public StaffProfile active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Office getOffice() {
        return this.office;
    }

    public void setOffice(Office office) {
        this.office = office;
    }

    public StaffProfile office(Office office) {
        this.setOffice(office);
        return this;
    }

    public RoleGroup getRoleGroup() {
        return this.roleGroup;
    }

    public void setRoleGroup(RoleGroup roleGroup) {
        this.roleGroup = roleGroup;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StaffProfile)) {
            return false;
        }
        return getId() != null && getId().equals(((StaffProfile) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StaffProfile{" +
            "id=" + getId() +
            ", staffCode='" + getStaffCode() + "'" +
            ", userLogin='" + getUserLogin() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", roleCode='" + getRoleCode() + "'" +
            ", scopeAllOffices='" + getScopeAllOffices() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
