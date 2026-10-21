package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/** Cấu hình thu hộ OnePay PayCollect (tài khoản ảo) — một dòng. Partner key không bao giờ trả ra API. */
@Entity
@Table(name = "onepay_config")
public class OnePayConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ENV_SANDBOX = "SANDBOX";
    public static final String ENV_PRODUCTION = "PRODUCTION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tắt: IPN OnePay gọi về bị từ chối, không tạo được hoá đơn. */
    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    /** SANDBOX = mtf.onepay.vn, PRODUCTION = onepay.vn. */
    @NotNull
    @Size(max = 12)
    @Column(name = "environment", length = 12, nullable = false)
    private String environment = ENV_SANDBOX;

    @Size(max = 50)
    @Column(name = "partner_id", length = 50)
    private String partnerId;

    @Size(max = 255)
    @Column(name = "partner_key", length = 255)
    private String partnerKey;

    /**
     * Cột cũ — không còn dùng. OnePay quyết định ngân hàng VA; CPN không gửi {@code bank_id}.
     * Giữ cột để tránh migration destructive trên demo/PROD.
     */
    @Size(max = 20)
    @Column(name = "bank_swift", length = 20)
    private String bankSwift;

    /** URL IPN đã đăng ký với OnePay — dùng dựng lại chuỗi ký khi kiểm chữ ký IPN. */
    @Size(max = 255)
    @Column(name = "ipn_url", length = 255)
    private String ipnUrl;

    @NotNull
    @Column(name = "invoice_ttl_hours", nullable = false)
    private Integer invoiceTtlHours = 24;

    @NotNull
    @Column(name = "signature_required", nullable = false)
    private Boolean signatureRequired = true;

    /** Bỏ kiểm chứng chỉ SSL khi gọi OnePay — chỉ dùng khi JVM không trust được CA (giống VTHK). */
    @NotNull
    @Column(name = "insecure_ssl", nullable = false)
    private Boolean insecureSsl = false;

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

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerKey() {
        return partnerKey;
    }

    public void setPartnerKey(String partnerKey) {
        this.partnerKey = partnerKey;
    }

    public String getBankSwift() {
        return bankSwift;
    }

    public void setBankSwift(String bankSwift) {
        this.bankSwift = bankSwift;
    }

    public String getIpnUrl() {
        return ipnUrl;
    }

    public void setIpnUrl(String ipnUrl) {
        this.ipnUrl = ipnUrl;
    }

    public Integer getInvoiceTtlHours() {
        return invoiceTtlHours;
    }

    public void setInvoiceTtlHours(Integer invoiceTtlHours) {
        this.invoiceTtlHours = invoiceTtlHours;
    }

    public Boolean getSignatureRequired() {
        return signatureRequired;
    }

    public void setSignatureRequired(Boolean signatureRequired) {
        this.signatureRequired = signatureRequired;
    }

    public Boolean getInsecureSsl() {
        return insecureSsl;
    }

    public void setInsecureSsl(Boolean insecureSsl) {
        this.insecureSsl = insecureSsl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
