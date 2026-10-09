package com.mycompany.myapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/** Một trong hai database có thể chuyển qua lại. Mật khẩu không trả ra API. */
@Entity
@Table(name = "app_database_slot")
public class AppDatabaseSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "slot", length = 1, nullable = false)
    private String slot;

    @NotNull
    @Size(max = 40)
    @Column(name = "label", length = 40, nullable = false)
    private String label;

    @Size(max = 700)
    @Column(name = "jdbc_url", length = 700)
    private String jdbcUrl;

    @Size(max = 100)
    @Column(name = "db_username", length = 100)
    private String dbUsername;

    @Size(max = 255)
    @Column(name = "db_password", length = 255)
    private String dbPassword;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.FALSE;

    @Column(name = "last_test_ok")
    private Boolean lastTestOk;

    @Column(name = "last_test_at")
    private Instant lastTestAt;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getLastTestOk() {
        return lastTestOk;
    }

    public void setLastTestOk(Boolean lastTestOk) {
        this.lastTestOk = lastTestOk;
    }

    public Instant getLastTestAt() {
        return lastTestAt;
    }

    public void setLastTestAt(Instant lastTestAt) {
        this.lastTestAt = lastTestAt;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
}
