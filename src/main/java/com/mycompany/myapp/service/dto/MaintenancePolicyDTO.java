package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * Payload chính sách bảo trì. Client đọc public GET /api/maintenance rồi tự lọc kênh.
 */
public class MaintenancePolicyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean enabled;
    private Boolean blockAll;
    private Boolean blockAppStaff;
    private Boolean blockAppCustomer;
    private Boolean blockWebStaff;
    private Boolean blockWebCustomer;

    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String message;

    /** Data-URL JPEG (nén phía client) hoặc URL ảnh; null = không ảnh. */
    private String imageUrl;

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
}
