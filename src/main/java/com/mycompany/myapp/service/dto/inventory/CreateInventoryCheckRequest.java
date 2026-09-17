package com.mycompany.myapp.service.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class CreateInventoryCheckRequest {

    @NotBlank
    @Size(max = 20)
    private String officeCode;

    private List<@NotBlank @Size(max = 40) String> systemCodes = new ArrayList<>();
    private List<@NotBlank @Size(max = 40) String> scannedCodes = new ArrayList<>();
    private List<@NotBlank @Size(max = 40) String> missingCodes = new ArrayList<>();

    private Integer systemPkgCount;
    private Integer checkedPkgCount;
    private Integer missingPkgCount;
    private Integer extraPkgCount;

    /** ISO-8601 optional; default now. */
    private String checkedAt;

    public String getOfficeCode() {
        return officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
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

    public String getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(String checkedAt) {
        this.checkedAt = checkedAt;
    }
}
