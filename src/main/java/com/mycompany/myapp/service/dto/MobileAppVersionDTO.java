package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * Payload chính sách phiên bản app mobile. App đọc bản public qua GET /api/mobile/app-version.
 */
public class MobileAppVersionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^\\d+(\\.\\d+){0,3}$", message = "must be dotted numbers, e.g. 1.40.0")
    private String minimumVersion;

    @Min(0)
    private Integer minimumAndroidVersionCode;

    private Boolean mandatoryUpdateEnabled;

    public String getMinimumVersion() {
        return minimumVersion;
    }

    public void setMinimumVersion(String minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    public Integer getMinimumAndroidVersionCode() {
        return minimumAndroidVersionCode;
    }

    public void setMinimumAndroidVersionCode(Integer minimumAndroidVersionCode) {
        this.minimumAndroidVersionCode = minimumAndroidVersionCode;
    }

    public Boolean getMandatoryUpdateEnabled() {
        return mandatoryUpdateEnabled;
    }

    public void setMandatoryUpdateEnabled(Boolean mandatoryUpdateEnabled) {
        this.mandatoryUpdateEnabled = mandatoryUpdateEnabled;
    }
}
