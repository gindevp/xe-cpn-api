package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

/** Một dòng: số lần tra cứu công khai mỗi thiết bị trong ngày. */
@Entity
@Table(name = "track_lookup_policy")
public class TrackLookupPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @NotNull
    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit = 30;

    /** false = mã QR văn phòng giữ nguyên, không đổi theo giây. */
    @NotNull
    @Column(name = "qr_auto_refresh", nullable = false)
    private Boolean qrAutoRefresh = Boolean.TRUE;

    /** Số giây một mã QR màn hình văn phòng còn hiệu lực trước khi đổi mã mới. */
    @NotNull
    @Column(name = "qr_refresh_seconds", nullable = false)
    private Integer qrRefreshSeconds = 60;

    /** Tắt mã QR từ giờ này tới giờ hôm sau (giờ VN), rồi màn hình tự phát mã mới. */
    @NotNull
    @Column(name = "qr_quiet_enabled", nullable = false)
    private Boolean qrQuietEnabled = Boolean.TRUE;

    @NotNull
    @Column(name = "qr_quiet_from", length = 5, nullable = false)
    private String qrQuietFrom = "21:00";

    @NotNull
    @Column(name = "qr_quiet_to", length = 5, nullable = false)
    private String qrQuietTo = "07:00";

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

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Boolean getQrAutoRefresh() {
        return qrAutoRefresh;
    }

    public void setQrAutoRefresh(Boolean qrAutoRefresh) {
        this.qrAutoRefresh = qrAutoRefresh;
    }

    public Integer getQrRefreshSeconds() {
        return qrRefreshSeconds;
    }

    public void setQrRefreshSeconds(Integer qrRefreshSeconds) {
        this.qrRefreshSeconds = qrRefreshSeconds;
    }

    public Boolean getQrQuietEnabled() {
        return qrQuietEnabled;
    }

    public void setQrQuietEnabled(Boolean qrQuietEnabled) {
        this.qrQuietEnabled = qrQuietEnabled;
    }

    public String getQrQuietFrom() {
        return qrQuietFrom;
    }

    public void setQrQuietFrom(String qrQuietFrom) {
        this.qrQuietFrom = qrQuietFrom;
    }

    public String getQrQuietTo() {
        return qrQuietTo;
    }

    public void setQrQuietTo(String qrQuietTo) {
        this.qrQuietTo = qrQuietTo;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
