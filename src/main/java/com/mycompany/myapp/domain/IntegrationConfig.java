package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A IntegrationConfig.
 */
@Entity
@Table(name = "integration_config")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IntegrationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Partner API key — cấu hình trên màn Tích hợp. */
    @Size(max = 255)
    @Column(name = "ahamove_api_key", length = 255)
    private String ahamoveApiKey;

    /** SĐT account Ahamove gắn partner (dùng đổi token). */
    @Size(max = 32)
    @Column(name = "ahamove_mobile", length = 32)
    private String ahamoveMobile;

    /** Bearer JWT cache — BE tự lấy, không nhập từ UI. */
    @Size(max = 2000)
    @Column(name = "ahamove_token", length = 2000)
    private String ahamoveToken;

    @Column(name = "ahamove_token_fetched_at")
    private Instant ahamoveTokenFetchedAt;

    @Size(max = 255)
    @Column(name = "grab_token", length = 255)
    private String grabToken;

    @Size(max = 255)
    @Column(name = "xanhsm_token", length = 255)
    private String xanhsmToken;

    @Size(max = 255)
    @Column(name = "distance_api_token", length = 255)
    private String distanceApiToken;

    /** OSM | GOONG — bản đồ pin trên FE. */
    @Size(max = 16)
    @Column(name = "map_provider", length = 16, nullable = false)
    private String mapProvider = "OSM";

    /** Goong Map tiles key (goong-js) — khác REST Places key. */
    @Size(max = 255)
    @Column(name = "goong_map_tiles_key", length = 255)
    private String goongMapTilesKey;

    /** Ahamove gọi webhook kèm {@code ?token=} hoặc header {@code apikey} — CPN tự sinh. */
    @Size(max = 64)
    @Column(name = "ahamove_webhook_token", length = 64)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String ahamoveWebhookToken;

    /** BALANCE (trừ ví tài khoản Ahamove) | CASH (VP trả tiền mặt cho tài xế lúc lấy hàng). */
    @Size(max = 32)
    @Column(name = "ahamove_payment_method", length = 32)
    private String ahamovePaymentMethod;

    /** Số tài xế gọi lúc lấy hàng. Trống thì dùng {@link #ahamoveMobile}. */
    @Size(max = 32)
    @Column(name = "ahamove_sender_mobile", length = 32)
    private String ahamoveSenderMobile;

    @Size(max = 255)
    @Column(name = "telegram_token", length = 255)
    private String telegramToken;

    @Size(max = 100)
    @Column(name = "telegram_chat_id", length = 100)
    private String telegramChatId;

    @Size(max = 255)
    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    @Size(max = 255)
    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    /** Auto Call HHVN Tech — bật/tắt gửi cuộc gọi tự động. */
    @Column(name = "autocall_enabled", nullable = false)
    private Boolean autocallEnabled;

    @Size(max = 255)
    @Column(name = "autocall_base_url", length = 255)
    private String autocallBaseUrl;

    /** xk_test_… (sandbox) | xk_live_… — không trả về FE. */
    @Size(max = 255)
    @Column(name = "autocall_api_key", length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String autocallApiKey;

    /** HMAC secret xác minh webhook call.finished — không trả về FE. */
    @Size(max = 255)
    @Column(name = "autocall_webhook_secret", length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String autocallWebhookSecret;

    /** HHVN | VTECH — nhà cung cấp đang dùng để gửi cuộc gọi; cấu hình mỗi bên lưu riêng. */
    @Size(max = 10)
    @Column(name = "autocall_provider", length = 10, nullable = false)
    private String autocallProvider;

    /** Vtech (tongdai.ai) — API import contact vào chiến dịch callbot. */
    @Size(max = 255)
    @Column(name = "autocall_vtech_base_url", length = 255)
    private String autocallVtechBaseUrl;

    /** Key chiến dịch callbot Vtech — không trả về FE. */
    @Size(max = 255)
    @Column(name = "autocall_vtech_api_key", length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String autocallVtechApiKey;

    /** Vtech không ký webhook — CPN sinh token, Vtech gửi kèm trong URL (?token=) hoặc body_extra. */
    @Size(max = 64)
    @Column(name = "autocall_vtech_webhook_token", length = 64)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String autocallVtechWebhookToken;

    /** CPN tự gọi lại khi cuộc gọi không thành công (ngoài lượt gọi lại nội bộ của HHVN). */
    @Column(name = "autocall_retry_enabled", nullable = false)
    private Boolean autocallRetryEnabled;

    /**
     * Khoảng cách (phút) trước mỗi lần gọi lại, tính từ khi có kết quả cuộc trước — "60,120" = cuộc 2 sau cuộc 1
     * 60', cuộc 3 sau cuộc 2 120'. Số phần tử = số lần gọi lại tối đa (tính trên mọi ngày).
     */
    @Size(max = 100)
    @Column(name = "autocall_retry_intervals", length = 100)
    private String autocallRetryIntervals;

    /**
     * Số ngày gọi tối đa: 1 = chỉ 1 đợt; N > 1 = gọi đủ đợt (cuộc 1 + các lần gọi lại) vẫn chưa được thì
     * hôm sau gọi lại từ đầu khung giờ một đợt mới, tối đa N đợt.
     */
    @Column(name = "autocall_retry_days")
    private Integer autocallRetryDays;

    @Column(name = "autocall_retry_no_answer")
    private Boolean autocallRetryNoAnswer;

    @Column(name = "autocall_retry_carrier_error")
    private Boolean autocallRetryCarrierError;

    @Column(name = "autocall_retry_send_error")
    private Boolean autocallRetrySendError;

    /**
     * Khung giờ được gọi (cả cuộc đầu lẫn gọi lại, khi bật gọi lại), HH:mm giờ Việt Nam.
     * Ngoài khung → dời sang đầu khung kế tiếp.
     */
    @Size(max = 5)
    @Column(name = "autocall_call_from", length = 5)
    private String autocallCallFrom;

    @Size(max = 5)
    @Column(name = "autocall_call_to", length = 5)
    private String autocallCallTo;

    /** Tự xuất HĐĐT sau mốc thanh toán + 3 tiếng (DN nếu khách yêu cầu trước hạn, còn lại cá nhân); mặc định tắt. */
    @Column(name = "misa_auto_issue_enabled", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean misaAutoIssueEnabled;

    /** Lúc bật tự xuất — chỉ áp đơn có mốc (nhập kho gửi / giao) sau thời điểm này; đơn cũ xuất bù bằng tay. */
    @Column(name = "misa_auto_issue_since")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant misaAutoIssueSince;

    /** Tài khoản nhận tiền NV nộp — mã BIN ngân hàng theo VietQR. */
    @Column(name = "deposit_bank_bin", length = 20)
    private String depositBankBin;

    @Column(name = "deposit_bank_name", length = 255)
    private String depositBankName;

    @Column(name = "deposit_account_no", length = 50)
    private String depositAccountNo;

    @Column(name = "deposit_account_name", length = 255)
    private String depositAccountName;

    /** Mẫu nội dung chuyển khoản, biến: {MA_NV} {TEN_NV} {MA_PHIEU} {MA_VP} {NGAY}. */
    @Column(name = "deposit_content_template", length = 255)
    private String depositContentTemplate;

    /** MinIO (S3 path-style). Ảnh/file nặng; DB chỉ giữ minio:key. */
    @Size(max = 255)
    @Column(name = "minio_endpoint", length = 255)
    private String minioEndpoint;

    @Size(max = 64)
    @Column(name = "minio_bucket", length = 64)
    private String minioBucket;

    @Size(max = 32)
    @Column(name = "minio_region", length = 32)
    private String minioRegion;

    @Size(max = 128)
    @Column(name = "minio_access_key", length = 128)
    private String minioAccessKey;

    @Size(max = 255)
    @Column(name = "minio_secret_key", length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String minioSecretKey;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public IntegrationConfig id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAhamoveApiKey() {
        return this.ahamoveApiKey;
    }

    public IntegrationConfig ahamoveApiKey(String ahamoveApiKey) {
        this.setAhamoveApiKey(ahamoveApiKey);
        return this;
    }

    public void setAhamoveApiKey(String ahamoveApiKey) {
        this.ahamoveApiKey = ahamoveApiKey;
    }

    public String getAhamoveMobile() {
        return this.ahamoveMobile;
    }

    public IntegrationConfig ahamoveMobile(String ahamoveMobile) {
        this.setAhamoveMobile(ahamoveMobile);
        return this;
    }

    public void setAhamoveMobile(String ahamoveMobile) {
        this.ahamoveMobile = ahamoveMobile;
    }

    @JsonIgnore
    public String getAhamoveToken() {
        return this.ahamoveToken;
    }

    public IntegrationConfig ahamoveToken(String ahamoveToken) {
        this.setAhamoveToken(ahamoveToken);
        return this;
    }

    public void setAhamoveToken(String ahamoveToken) {
        this.ahamoveToken = ahamoveToken;
    }

    public Instant getAhamoveTokenFetchedAt() {
        return this.ahamoveTokenFetchedAt;
    }

    public IntegrationConfig ahamoveTokenFetchedAt(Instant ahamoveTokenFetchedAt) {
        this.setAhamoveTokenFetchedAt(ahamoveTokenFetchedAt);
        return this;
    }

    public void setAhamoveTokenFetchedAt(Instant ahamoveTokenFetchedAt) {
        this.ahamoveTokenFetchedAt = ahamoveTokenFetchedAt;
    }

    public String getGrabToken() {
        return this.grabToken;
    }

    public IntegrationConfig grabToken(String grabToken) {
        this.setGrabToken(grabToken);
        return this;
    }

    public void setGrabToken(String grabToken) {
        this.grabToken = grabToken;
    }

    public String getXanhsmToken() {
        return this.xanhsmToken;
    }

    public IntegrationConfig xanhsmToken(String xanhsmToken) {
        this.setXanhsmToken(xanhsmToken);
        return this;
    }

    public void setXanhsmToken(String xanhsmToken) {
        this.xanhsmToken = xanhsmToken;
    }

    public String getDistanceApiToken() {
        return this.distanceApiToken;
    }

    public IntegrationConfig distanceApiToken(String distanceApiToken) {
        this.setDistanceApiToken(distanceApiToken);
        return this;
    }

    public void setDistanceApiToken(String distanceApiToken) {
        this.distanceApiToken = distanceApiToken;
    }

    public String getMapProvider() {
        return this.mapProvider;
    }

    public IntegrationConfig mapProvider(String mapProvider) {
        this.setMapProvider(mapProvider);
        return this;
    }

    public void setMapProvider(String mapProvider) {
        this.mapProvider = mapProvider != null && !mapProvider.isBlank() ? mapProvider.trim().toUpperCase() : "OSM";
    }

    public String getGoongMapTilesKey() {
        return this.goongMapTilesKey;
    }

    public IntegrationConfig goongMapTilesKey(String goongMapTilesKey) {
        this.setGoongMapTilesKey(goongMapTilesKey);
        return this;
    }

    public void setGoongMapTilesKey(String goongMapTilesKey) {
        this.goongMapTilesKey = goongMapTilesKey;
    }

    public String getTelegramToken() {
        return this.telegramToken;
    }

    public IntegrationConfig telegramToken(String telegramToken) {
        this.setTelegramToken(telegramToken);
        return this;
    }

    public void setTelegramToken(String telegramToken) {
        this.telegramToken = telegramToken;
    }

    public String getTelegramChatId() {
        return this.telegramChatId;
    }

    public IntegrationConfig telegramChatId(String telegramChatId) {
        this.setTelegramChatId(telegramChatId);
        return this;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public IntegrationConfig webhookUrl(String webhookUrl) {
        this.setWebhookUrl(webhookUrl);
        return this;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return this.webhookSecret;
    }

    public IntegrationConfig webhookSecret(String webhookSecret) {
        this.setWebhookSecret(webhookSecret);
        return this;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Boolean getAutocallEnabled() {
        return this.autocallEnabled;
    }

    public void setAutocallEnabled(Boolean autocallEnabled) {
        this.autocallEnabled = autocallEnabled;
    }

    public String getAutocallBaseUrl() {
        return this.autocallBaseUrl;
    }

    public void setAutocallBaseUrl(String autocallBaseUrl) {
        this.autocallBaseUrl = autocallBaseUrl;
    }

    public String getAutocallApiKey() {
        return this.autocallApiKey;
    }

    public void setAutocallApiKey(String autocallApiKey) {
        this.autocallApiKey = autocallApiKey;
    }

    public String getAutocallWebhookSecret() {
        return this.autocallWebhookSecret;
    }

    public void setAutocallWebhookSecret(String autocallWebhookSecret) {
        this.autocallWebhookSecret = autocallWebhookSecret;
    }

    public Boolean getAutocallRetryEnabled() {
        return autocallRetryEnabled;
    }

    public void setAutocallRetryEnabled(Boolean autocallRetryEnabled) {
        this.autocallRetryEnabled = autocallRetryEnabled;
    }

    public String getAutocallRetryIntervals() {
        return autocallRetryIntervals;
    }

    public void setAutocallRetryIntervals(String autocallRetryIntervals) {
        this.autocallRetryIntervals = autocallRetryIntervals;
    }

    public Integer getAutocallRetryDays() {
        return autocallRetryDays;
    }

    public void setAutocallRetryDays(Integer autocallRetryDays) {
        this.autocallRetryDays = autocallRetryDays;
    }

    public Boolean getAutocallRetryNoAnswer() {
        return autocallRetryNoAnswer;
    }

    public void setAutocallRetryNoAnswer(Boolean autocallRetryNoAnswer) {
        this.autocallRetryNoAnswer = autocallRetryNoAnswer;
    }

    public Boolean getAutocallRetryCarrierError() {
        return autocallRetryCarrierError;
    }

    public void setAutocallRetryCarrierError(Boolean autocallRetryCarrierError) {
        this.autocallRetryCarrierError = autocallRetryCarrierError;
    }

    public Boolean getAutocallRetrySendError() {
        return autocallRetrySendError;
    }

    public void setAutocallRetrySendError(Boolean autocallRetrySendError) {
        this.autocallRetrySendError = autocallRetrySendError;
    }

    public String getAutocallCallFrom() {
        return autocallCallFrom;
    }

    public void setAutocallCallFrom(String autocallCallFrom) {
        this.autocallCallFrom = autocallCallFrom;
    }

    public String getAutocallCallTo() {
        return autocallCallTo;
    }

    public void setAutocallCallTo(String autocallCallTo) {
        this.autocallCallTo = autocallCallTo;
    }

    public String getAutocallProvider() {
        return autocallProvider;
    }

    public void setAutocallProvider(String autocallProvider) {
        this.autocallProvider = autocallProvider;
    }

    public String getAutocallVtechBaseUrl() {
        return autocallVtechBaseUrl;
    }

    public void setAutocallVtechBaseUrl(String autocallVtechBaseUrl) {
        this.autocallVtechBaseUrl = autocallVtechBaseUrl;
    }

    public String getAutocallVtechApiKey() {
        return autocallVtechApiKey;
    }

    public void setAutocallVtechApiKey(String autocallVtechApiKey) {
        this.autocallVtechApiKey = autocallVtechApiKey;
    }

    public String getAhamoveWebhookToken() {
        return ahamoveWebhookToken;
    }

    public void setAhamoveWebhookToken(String ahamoveWebhookToken) {
        this.ahamoveWebhookToken = ahamoveWebhookToken;
    }

    public String getAhamovePaymentMethod() {
        return ahamovePaymentMethod;
    }

    public void setAhamovePaymentMethod(String ahamovePaymentMethod) {
        this.ahamovePaymentMethod = ahamovePaymentMethod;
    }

    public String getAhamoveSenderMobile() {
        return ahamoveSenderMobile;
    }

    public void setAhamoveSenderMobile(String ahamoveSenderMobile) {
        this.ahamoveSenderMobile = ahamoveSenderMobile;
    }

    public String getAutocallVtechWebhookToken() {
        return autocallVtechWebhookToken;
    }

    public void setAutocallVtechWebhookToken(String autocallVtechWebhookToken) {
        this.autocallVtechWebhookToken = autocallVtechWebhookToken;
    }

    public static final String PROVIDER_HHVN = "HHVN";
    public static final String PROVIDER_VTECH = "VTECH";

    @JsonIgnore
    public boolean isAutocallVtech() {
        return PROVIDER_VTECH.equals(autocallProvider);
    }

    /** Nhà cung cấp đang chọn, mặc định HHVN. */
    @JsonIgnore
    public String getAutocallActiveProvider() {
        return isAutocallVtech() ? PROVIDER_VTECH : PROVIDER_HHVN;
    }

    /** Key của nhà cung cấp đang chọn. */
    @JsonIgnore
    public String getAutocallActiveApiKey() {
        return isAutocallVtech() ? autocallVtechApiKey : autocallApiKey;
    }

    @JsonIgnore
    public boolean isAutocallActiveKeyConfigured() {
        String k = getAutocallActiveApiKey();
        return k != null && !k.isBlank();
    }

    /** Chỉ key HHVN xk_test_ là sandbox; Vtech luôn gọi thật. */
    @JsonIgnore
    public boolean isAutocallSandbox() {
        return !isAutocallVtech() && autocallApiKey != null && autocallApiKey.startsWith("xk_test_");
    }

    @JsonProperty(value = "autocallVtechApiKeyConfigured", access = JsonProperty.Access.READ_ONLY)
    public boolean isAutocallVtechApiKeyConfigured() {
        return autocallVtechApiKey != null && !autocallVtechApiKey.isBlank();
    }

    @JsonProperty(value = "autocallVtechApiKeySuffix", access = JsonProperty.Access.READ_ONLY)
    public String getAutocallVtechApiKeySuffix() {
        if (!isAutocallVtechApiKeyConfigured() || autocallVtechApiKey.length() <= 4) {
            return null;
        }
        return autocallVtechApiKey.substring(autocallVtechApiKey.length() - 4);
    }

    /** Không khởi tạo field = false: body PUT thiếu field phải giữ null để merge không tắt Auto Call. */
    @PrePersist
    @PreUpdate
    void defaultAutocallEnabled() {
        if (autocallEnabled == null) {
            autocallEnabled = false;
        }
        if (autocallProvider == null) {
            autocallProvider = PROVIDER_HHVN;
        }
        if (autocallRetryEnabled == null) {
            autocallRetryEnabled = false;
        }
        if (misaAutoIssueEnabled == null) {
            misaAutoIssueEnabled = false;
        }
    }

    public Boolean getMisaAutoIssueEnabled() {
        return misaAutoIssueEnabled;
    }

    public void setMisaAutoIssueEnabled(Boolean misaAutoIssueEnabled) {
        this.misaAutoIssueEnabled = misaAutoIssueEnabled;
    }

    public Instant getMisaAutoIssueSince() {
        return misaAutoIssueSince;
    }

    public void setMisaAutoIssueSince(Instant misaAutoIssueSince) {
        this.misaAutoIssueSince = misaAutoIssueSince;
    }

    public String getDepositBankBin() {
        return depositBankBin;
    }

    public void setDepositBankBin(String depositBankBin) {
        this.depositBankBin = depositBankBin;
    }

    public String getDepositBankName() {
        return depositBankName;
    }

    public void setDepositBankName(String depositBankName) {
        this.depositBankName = depositBankName;
    }

    public String getDepositAccountNo() {
        return depositAccountNo;
    }

    public void setDepositAccountNo(String depositAccountNo) {
        this.depositAccountNo = depositAccountNo;
    }

    public String getDepositAccountName() {
        return depositAccountName;
    }

    public void setDepositAccountName(String depositAccountName) {
        this.depositAccountName = depositAccountName;
    }

    public String getDepositContentTemplate() {
        return depositContentTemplate;
    }

    public void setDepositContentTemplate(String depositContentTemplate) {
        this.depositContentTemplate = depositContentTemplate;
    }

    @JsonProperty(value = "autocallApiKeyConfigured", access = JsonProperty.Access.READ_ONLY)
    public boolean isAutocallApiKeyConfigured() {
        return autocallApiKey != null && !autocallApiKey.isBlank();
    }

    /** SANDBOX | LIVE | UNKNOWN theo tiền tố key; null khi chưa có key. */
    @JsonProperty(value = "autocallApiKeyMode", access = JsonProperty.Access.READ_ONLY)
    public String getAutocallApiKeyMode() {
        if (!isAutocallApiKeyConfigured()) {
            return null;
        }
        if (autocallApiKey.startsWith("xk_test_")) {
            return "SANDBOX";
        }
        if (autocallApiKey.startsWith("xk_live_")) {
            return "LIVE";
        }
        return "UNKNOWN";
    }

    @JsonProperty(value = "autocallApiKeySuffix", access = JsonProperty.Access.READ_ONLY)
    public String getAutocallApiKeySuffix() {
        if (!isAutocallApiKeyConfigured() || autocallApiKey.length() <= 4) {
            return null;
        }
        return autocallApiKey.substring(autocallApiKey.length() - 4);
    }

    @JsonProperty(value = "autocallWebhookSecretConfigured", access = JsonProperty.Access.READ_ONLY)
    public boolean isAutocallWebhookSecretConfigured() {
        return autocallWebhookSecret != null && !autocallWebhookSecret.isBlank();
    }

    public String getMinioEndpoint() {
        return minioEndpoint;
    }

    public void setMinioEndpoint(String minioEndpoint) {
        this.minioEndpoint = minioEndpoint;
    }

    public String getMinioBucket() {
        return minioBucket;
    }

    public void setMinioBucket(String minioBucket) {
        this.minioBucket = minioBucket;
    }

    public String getMinioRegion() {
        return minioRegion;
    }

    public void setMinioRegion(String minioRegion) {
        this.minioRegion = minioRegion;
    }

    public String getMinioAccessKey() {
        return minioAccessKey;
    }

    public void setMinioAccessKey(String minioAccessKey) {
        this.minioAccessKey = minioAccessKey;
    }

    @JsonIgnore
    public String getMinioSecretKey() {
        return minioSecretKey;
    }

    public void setMinioSecretKey(String minioSecretKey) {
        this.minioSecretKey = minioSecretKey;
    }

    @JsonProperty(value = "minioSecretConfigured", access = JsonProperty.Access.READ_ONLY)
    public boolean isMinioSecretConfigured() {
        return minioSecretKey != null && !minioSecretKey.isBlank();
    }

    @JsonProperty(value = "minioConfigured", access = JsonProperty.Access.READ_ONLY)
    public boolean isMinioConfigured() {
        return (
            minioEndpoint != null &&
            !minioEndpoint.isBlank() &&
            minioBucket != null &&
            !minioBucket.isBlank() &&
            minioAccessKey != null &&
            !minioAccessKey.isBlank() &&
            isMinioSecretConfigured()
        );
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public IntegrationConfig updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegrationConfig)) {
            return false;
        }
        return getId() != null && getId().equals(((IntegrationConfig) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IntegrationConfig{" +
            "id=" + getId() +
            ", ahamoveApiKey='" + (getAhamoveApiKey() != null ? "***" : null) + "'" +
            ", ahamoveMobile='" + getAhamoveMobile() + "'" +
            ", ahamoveTokenFetchedAt='" + getAhamoveTokenFetchedAt() + "'" +
            ", grabToken='" + getGrabToken() + "'" +
            ", xanhsmToken='" + getXanhsmToken() + "'" +
            ", distanceApiToken='" + getDistanceApiToken() + "'" +
            ", mapProvider='" + getMapProvider() + "'" +
            ", goongMapTilesKey='" + (getGoongMapTilesKey() != null ? "***" : null) + "'" +
            ", telegramToken='" + getTelegramToken() + "'" +
            ", telegramChatId='" + getTelegramChatId() + "'" +
            ", webhookUrl='" + getWebhookUrl() + "'" +
            ", webhookSecret='" + getWebhookSecret() + "'" +
            ", autocallEnabled='" + getAutocallEnabled() + "'" +
            ", autocallBaseUrl='" + getAutocallBaseUrl() + "'" +
            ", autocallApiKey='" + (isAutocallApiKeyConfigured() ? "***" : null) + "'" +
            ", autocallWebhookSecret='" + (isAutocallWebhookSecretConfigured() ? "***" : null) + "'" +
            ", autocallProvider='" + getAutocallProvider() + "'" +
            ", autocallVtechApiKey='" + (isAutocallVtechApiKeyConfigured() ? "***" : null) + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
