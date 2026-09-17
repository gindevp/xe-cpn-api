package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "inventory_check")
public class InventoryCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 20)
    @Column(name = "office_code", length = 20, nullable = false)
    private String officeCode;

    @NotNull
    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "checked_by_username", length = 50, nullable = false)
    private String checkedByUsername;

    @Size(max = 100)
    @Column(name = "checked_by_name", length = 100)
    private String checkedByName;

    @NotNull
    @Column(name = "system_count", nullable = false)
    private Integer systemCount;

    @NotNull
    @Column(name = "checked_count", nullable = false)
    private Integer checkedCount;

    @NotNull
    @Column(name = "missing_count", nullable = false)
    private Integer missingCount;

    @Column(name = "system_pkg_count")
    private Integer systemPkgCount;

    @Column(name = "checked_pkg_count")
    private Integer checkedPkgCount;

    @Column(name = "missing_pkg_count")
    private Integer missingPkgCount;

    @Column(name = "extra_pkg_count")
    private Integer extraPkgCount;

    @Lob
    @Column(name = "system_codes_json")
    private String systemCodesJson;

    @Lob
    @Column(name = "scanned_codes_json")
    private String scannedCodesJson;

    @Lob
    @Column(name = "missing_codes_json")
    private String missingCodesJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOfficeCode() {
        return officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getCheckedByUsername() {
        return checkedByUsername;
    }

    public void setCheckedByUsername(String checkedByUsername) {
        this.checkedByUsername = checkedByUsername;
    }

    public String getCheckedByName() {
        return checkedByName;
    }

    public void setCheckedByName(String checkedByName) {
        this.checkedByName = checkedByName;
    }

    public Integer getSystemCount() {
        return systemCount;
    }

    public void setSystemCount(Integer systemCount) {
        this.systemCount = systemCount;
    }

    public Integer getCheckedCount() {
        return checkedCount;
    }

    public void setCheckedCount(Integer checkedCount) {
        this.checkedCount = checkedCount;
    }

    public Integer getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(Integer missingCount) {
        this.missingCount = missingCount;
    }

    public Integer getSystemPkgCount() {
        return systemPkgCount;
    }

    public void setSystemPkgCount(Integer systemPkgCount) {
        this.systemPkgCount = systemPkgCount;
    }

    public Integer getCheckedPkgCount() {
        return checkedPkgCount;
    }

    public void setCheckedPkgCount(Integer checkedPkgCount) {
        this.checkedPkgCount = checkedPkgCount;
    }

    public Integer getMissingPkgCount() {
        return missingPkgCount;
    }

    public void setMissingPkgCount(Integer missingPkgCount) {
        this.missingPkgCount = missingPkgCount;
    }

    public Integer getExtraPkgCount() {
        return extraPkgCount;
    }

    public void setExtraPkgCount(Integer extraPkgCount) {
        this.extraPkgCount = extraPkgCount;
    }

    public String getSystemCodesJson() {
        return systemCodesJson;
    }

    public void setSystemCodesJson(String systemCodesJson) {
        this.systemCodesJson = systemCodesJson;
    }

    public String getScannedCodesJson() {
        return scannedCodesJson;
    }

    public void setScannedCodesJson(String scannedCodesJson) {
        this.scannedCodesJson = scannedCodesJson;
    }

    public String getMissingCodesJson() {
        return missingCodesJson;
    }

    public void setMissingCodesJson(String missingCodesJson) {
        this.missingCodesJson = missingCodesJson;
    }
}
