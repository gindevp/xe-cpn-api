package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * Chính sách phiên bản tối thiểu của app mobile — bảng singleton (1 row), đọc/ghi qua facade.
 * Đổi chính sách chỉ cần sửa dữ liệu, không cần phát hành bản app mới.
 */
@Entity
@Table(name = "mobile_app_version_policy")
public class MobileAppVersionPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 30)
    @Column(name = "minimum_version", length = 30, nullable = false)
    private String minimumVersion;

    @Column(name = "minimum_android_version_code")
    private Integer minimumAndroidVersionCode;

    @Column(name = "mandatory_update_enabled")
    private Boolean mandatoryUpdateEnabled;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMinimumVersion() {
        return this.minimumVersion;
    }

    public void setMinimumVersion(String minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    public Integer getMinimumAndroidVersionCode() {
        return this.minimumAndroidVersionCode;
    }

    public void setMinimumAndroidVersionCode(Integer minimumAndroidVersionCode) {
        this.minimumAndroidVersionCode = minimumAndroidVersionCode;
    }

    public Boolean getMandatoryUpdateEnabled() {
        return this.mandatoryUpdateEnabled;
    }

    public void setMandatoryUpdateEnabled(Boolean mandatoryUpdateEnabled) {
        this.mandatoryUpdateEnabled = mandatoryUpdateEnabled;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MobileAppVersionPolicy)) {
            return false;
        }
        return getId() != null && getId().equals(((MobileAppVersionPolicy) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MobileAppVersionPolicy{" +
            "id=" + getId() +
            ", minimumVersion='" + getMinimumVersion() + "'" +
            ", minimumAndroidVersionCode=" + getMinimumAndroidVersionCode() +
            ", mandatoryUpdateEnabled='" + getMandatoryUpdateEnabled() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
