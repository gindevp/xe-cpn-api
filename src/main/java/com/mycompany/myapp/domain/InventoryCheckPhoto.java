package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/** Ảnh kiện chụp khi quét kiểm kho — gắn với phiên quét, ghép vào biên bản qua session_key. */
@Entity
@Table(name = "inventory_check_photo")
public class InventoryCheckPhoto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 64)
    @Column(name = "session_key", length = 64, nullable = false)
    private String sessionKey;

    @NotNull
    @Size(max = 20)
    @Column(name = "office_code", length = 20, nullable = false)
    private String officeCode;

    @NotNull
    @Size(max = 50)
    @Column(name = "order_code", length = 50, nullable = false)
    private String orderCode;

    @NotNull
    @Column(name = "package_seq", nullable = false)
    private Integer packageSeq;

    @NotNull
    @Lob
    @Column(name = "photo_url", nullable = false, columnDefinition = "LONGTEXT")
    private String photoUrl;

    @NotNull
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "captured_by_username", length = 50, nullable = false)
    private String capturedByUsername;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getOfficeCode() {
        return officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getPackageSeq() {
        return packageSeq;
    }

    public void setPackageSeq(Integer packageSeq) {
        this.packageSeq = packageSeq;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getCapturedByUsername() {
        return capturedByUsername;
    }

    public void setCapturedByUsername(String capturedByUsername) {
        this.capturedByUsername = capturedByUsername;
    }
}
