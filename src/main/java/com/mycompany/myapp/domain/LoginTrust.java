package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

/** IP (web) hoặc thiết bị (app) của một tài khoản — chờ / đã được admin duyệt. */
@Entity
@Table(name = "login_trust")
public class LoginTrust implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KIND_IP = "IP";
    public static final String KIND_DEVICE = "DEVICE";

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_login", length = 50, nullable = false)
    private String userLogin;

    @Column(name = "kind", length = 10, nullable = false)
    private String kind;

    @Column(name = "trust_value", length = 100, nullable = false)
    private String trustValue;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "status", length = 10, nullable = false)
    private String status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "last_ip", length = 64)
    private String lastIp;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 50)
    private String decidedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getTrustValue() {
        return trustValue;
    }

    public void setTrustValue(String trustValue) {
        this.trustValue = trustValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }
}
