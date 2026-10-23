package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/** Ảnh bill / chứng từ chuyển COD (màn Quản lý đơn COD). Nhiều ảnh / đơn. */
@Entity
@Table(name = "order_cod_bill_photo")
public class OrderCodBillPhoto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "order_id", nullable = false)
    private Long orderId;

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

    @Min(1)
    @Max(9)
    @Column(name = "sequence_no")
    private Integer sequenceNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }
}
