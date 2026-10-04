package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/** Ảnh đơn hàng khách gửi khi tạo đơn — tối đa 1 ảnh / đơn. */
@Entity
@Table(name = "order_goods_photo")
public class OrderGoodsPhoto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "order_id", nullable = false, unique = true)
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
}
