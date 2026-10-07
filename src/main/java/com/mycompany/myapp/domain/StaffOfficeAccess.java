package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/** Một VP nhân viên được phép chuyển sang làm việc. */
@Entity
@Table(
    name = "staff_office_access",
    uniqueConstraints = { @UniqueConstraint(name = "uk_staff_office_access", columnNames = { "staff_profile_id", "office_id" }) }
)
public class StaffOfficeAccess implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "staff_profile_id", nullable = false)
    private Long staffProfileId;

    @NotNull
    @Column(name = "office_id", nullable = false)
    private Long officeId;

    public StaffOfficeAccess() {}

    public StaffOfficeAccess(Long staffProfileId, Long officeId) {
        this.staffProfileId = staffProfileId;
        this.officeId = officeId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStaffProfileId() {
        return staffProfileId;
    }

    public void setStaffProfileId(Long staffProfileId) {
        this.staffProfileId = staffProfileId;
    }

    public Long getOfficeId() {
        return officeId;
    }

    public void setOfficeId(Long officeId) {
        this.officeId = officeId;
    }
}
