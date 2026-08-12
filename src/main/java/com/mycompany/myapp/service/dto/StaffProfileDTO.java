package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.RoleCode;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.StaffProfile} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StaffProfileDTO implements Serializable {

    private Long id;

    @Size(max = 30)
    private String staffCode;

    @NotNull
    @Size(max = 50)
    private String userLogin;

    @Size(max = 100)
    private String displayName;

    @NotNull
    private RoleCode roleCode;

    @NotNull
    private Boolean scopeAllOffices;

    @NotNull
    private Boolean active;

    @NotNull
    private OfficeDTO office;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public RoleCode getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(RoleCode roleCode) {
        this.roleCode = roleCode;
    }

    public Boolean getScopeAllOffices() {
        return scopeAllOffices;
    }

    public void setScopeAllOffices(Boolean scopeAllOffices) {
        this.scopeAllOffices = scopeAllOffices;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OfficeDTO getOffice() {
        return office;
    }

    public void setOffice(OfficeDTO office) {
        this.office = office;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StaffProfileDTO)) {
            return false;
        }

        StaffProfileDTO staffProfileDTO = (StaffProfileDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, staffProfileDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StaffProfileDTO{" +
            "id=" + getId() +
            ", staffCode='" + getStaffCode() + "'" +
            ", userLogin='" + getUserLogin() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", roleCode='" + getRoleCode() + "'" +
            ", scopeAllOffices='" + getScopeAllOffices() + "'" +
            ", active='" + getActive() + "'" +
            ", office=" + getOffice() +
            "}";
    }
}
