package com.mycompany.myapp.service.dto.inventory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InventoryCheckDTO {

    private Long id;
    private String officeCode;
    private Instant checkedAt;
    private String checkedByUsername;
    private String checkedByName;
    private Integer systemCount;
    private Integer checkedCount;
    private Integer missingCount;
    private Integer systemPkgCount;
    private Integer checkedPkgCount;
    private Integer missingPkgCount;
    private Integer extraPkgCount;
    private List<String> systemCodes = new ArrayList<>();
    private List<String> scannedCodes = new ArrayList<>();
    private List<String> missingCodes = new ArrayList<>();

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

    public List<String> getSystemCodes() {
        return systemCodes;
    }

    public void setSystemCodes(List<String> systemCodes) {
        this.systemCodes = systemCodes;
    }

    public List<String> getScannedCodes() {
        return scannedCodes;
    }

    public void setScannedCodes(List<String> scannedCodes) {
        this.scannedCodes = scannedCodes;
    }

    public List<String> getMissingCodes() {
        return missingCodes;
    }

    public void setMissingCodes(List<String> missingCodes) {
        this.missingCodes = missingCodes;
    }
}
