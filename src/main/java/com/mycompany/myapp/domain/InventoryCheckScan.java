package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/** Một kiện đã quét trong phiên kiểm kho dùng chung của VP. */
@Entity
@Table(name = "inventory_check_scan")
public class InventoryCheckScan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "check_id", nullable = false)
    private Long checkId;

    @NotNull
    @Size(max = 50)
    @Column(name = "order_code", length = 50, nullable = false)
    private String orderCode;

    @NotNull
    @Column(name = "package_seq", nullable = false)
    private Integer packageSeq;

    @NotNull
    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "scanned_by_username", length = 50, nullable = false)
    private String scannedByUsername;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCheckId() {
        return checkId;
    }

    public void setCheckId(Long checkId) {
        this.checkId = checkId;
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

    public Instant getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Instant scannedAt) {
        this.scannedAt = scannedAt;
    }

    public String getScannedByUsername() {
        return scannedByUsername;
    }

    public void setScannedByUsername(String scannedByUsername) {
        this.scannedByUsername = scannedByUsername;
    }
}
