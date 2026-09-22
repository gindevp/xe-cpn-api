package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * Chính sách bảo trì — bảng singleton (1 row), đọc/ghi qua facade.
 * enabled=false hoặc bảng rỗng → không chặn kênh nào.
 */
@Entity
@Table(name = "maintenance_policy")
public class MaintenancePolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.FALSE;

    @Column(name = "block_all", nullable = false)
    private Boolean blockAll = Boolean.FALSE;

    @Column(name = "block_app_staff", nullable = false)
    private Boolean blockAppStaff = Boolean.FALSE;

    @Column(name = "block_app_customer", nullable = false)
    private Boolean blockAppCustomer = Boolean.FALSE;

    @Column(name = "block_web_staff", nullable = false)
    private Boolean blockWebStaff = Boolean.FALSE;

    @Column(name = "block_web_customer", nullable = false)
    private Boolean blockWebCustomer = Boolean.FALSE;

    @Size(max = 200)
    @Column(name = "title", length = 200)
    private String title;

    @Size(max = 2000)
    @Column(name = "message", length = 2000)
    private String message;

    @Lob
    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getBlockAll() {
        return blockAll;
    }

    public void setBlockAll(Boolean blockAll) {
        this.blockAll = blockAll;
    }

    public Boolean getBlockAppStaff() {
        return blockAppStaff;
    }

    public void setBlockAppStaff(Boolean blockAppStaff) {
        this.blockAppStaff = blockAppStaff;
    }

    public Boolean getBlockAppCustomer() {
        return blockAppCustomer;
    }

    public void setBlockAppCustomer(Boolean blockAppCustomer) {
        this.blockAppCustomer = blockAppCustomer;
    }

    public Boolean getBlockWebStaff() {
        return blockWebStaff;
    }

    public void setBlockWebStaff(Boolean blockWebStaff) {
        this.blockWebStaff = blockWebStaff;
    }

    public Boolean getBlockWebCustomer() {
        return blockWebCustomer;
    }

    public void setBlockWebCustomer(Boolean blockWebCustomer) {
        this.blockWebCustomer = blockWebCustomer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MaintenancePolicy)) {
            return false;
        }
        return getId() != null && getId().equals(((MaintenancePolicy) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
