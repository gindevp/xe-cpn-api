package com.mycompany.myapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/** Một dòng: app có bắt chụp ảnh khi báo xe rời hay không. */
@Entity
@Table(name = "vehicle_report_policy")
public class VehicleReportPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @NotNull
    @Column(name = "depart_photo_required", nullable = false)
    private Boolean departPhotoRequired = Boolean.TRUE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getDepartPhotoRequired() {
        return departPhotoRequired;
    }

    public void setDepartPhotoRequired(Boolean departPhotoRequired) {
        this.departPhotoRequired = departPhotoRequired;
    }
}
