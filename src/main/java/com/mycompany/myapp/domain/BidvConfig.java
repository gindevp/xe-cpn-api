package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/** Cấu hình thu hộ BIDV qua tài khoản định danh — một dòng. Secret không bao giờ trả ra API. */
@Entity
@Table(name = "bidv_config")
public class BidvConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String MODE_TEST = "TEST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    /** TEST: getbill/paybill chỉ trả lời hóa đơn test, không đụng đơn / phiếu thu. */
    @NotNull
    @Size(max = 10)
    @Column(name = "mode", length = 10, nullable = false)
    private String mode = MODE_TEST;

    /** Mã dịch vụ BIDV cấp cho CPN. */
    @Size(max = 50)
    @Column(name = "service_id", length = 50)
    private String serviceId;

    /** Secret code BIDV cấp — dùng tính checksum. */
    @Size(max = 255)
    @Column(name = "secret_code", length = 255)
    private String secretCode;

    /** Mã đầu tài khoản định danh; BIDV có thể gửi customerId kèm hoặc không kèm tiền tố này. */
    @Size(max = 20)
    @Column(name = "va_prefix", length = 20)
    private String vaPrefix;

    /** IP BIDV được gọi vào, cách nhau bởi dấu phẩy / xuống dòng. Trống = không chặn IP. */
    @Size(max = 1000)
    @Column(name = "allowed_ips", length = 1000)
    private String allowedIps;

    /** API key CPN cấp cho BIDV (header X-API-KEY hoặc Authorization: Bearer). */
    @Size(max = 100)
    @Column(name = "api_key", length = 100)
    private String apiKey;

    @NotNull
    @Column(name = "api_key_required", nullable = false)
    private Boolean apiKeyRequired = true;

    @NotNull
    @Column(name = "checksum_required", nullable = false)
    private Boolean checksumRequired = true;

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getSecretCode() {
        return secretCode;
    }

    public void setSecretCode(String secretCode) {
        this.secretCode = secretCode;
    }

    public String getVaPrefix() {
        return vaPrefix;
    }

    public void setVaPrefix(String vaPrefix) {
        this.vaPrefix = vaPrefix;
    }

    public String getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(String allowedIps) {
        this.allowedIps = allowedIps;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Boolean getApiKeyRequired() {
        return apiKeyRequired;
    }

    public void setApiKeyRequired(Boolean apiKeyRequired) {
        this.apiKeyRequired = apiKeyRequired;
    }

    public Boolean getChecksumRequired() {
        return checksumRequired;
    }

    public void setChecksumRequired(Boolean checksumRequired) {
        this.checksumRequired = checksumRequired;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
