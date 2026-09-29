package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Khoản cần nộp phiếu thu của một phần đơn (SENDER / DELIVERY) đã được admin hủy — không còn phải lập phiếu. */
@Entity
@Table(name = "receipt_waiver")
public class ReceiptWaiver implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ShipmentOrder order;

    @NotNull
    @Size(max = 10)
    @Column(name = "portion", length = 10, nullable = false)
    private String portion;

    @NotNull
    @Column(name = "amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal amount;

    @Size(max = 50)
    @Column(name = "owner_username", length = 50)
    private String ownerUsername;

    @NotNull
    @Size(max = 255)
    @Column(name = "reason", length = 255, nullable = false)
    private String reason;

    @NotNull
    @Column(name = "waived_at", nullable = false)
    private Instant waivedAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "waived_by_username", length = 50, nullable = false)
    private String waivedByUsername;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ShipmentOrder getOrder() {
        return order;
    }

    public void setOrder(ShipmentOrder order) {
        this.order = order;
    }

    public String getPortion() {
        return portion;
    }

    public void setPortion(String portion) {
        this.portion = portion;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getWaivedAt() {
        return waivedAt;
    }

    public void setWaivedAt(Instant waivedAt) {
        this.waivedAt = waivedAt;
    }

    public String getWaivedByUsername() {
        return waivedByUsername;
    }

    public void setWaivedByUsername(String waivedByUsername) {
        this.waivedByUsername = waivedByUsername;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReceiptWaiver)) {
            return false;
        }
        return id != null && id.equals(((ReceiptWaiver) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
