package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Phí đối tác giao (Ahamove, thanh toán tiền mặt) người bàn giao đã trả tài xế. Chưa gắn phiếu ({@code receipt} null) = còn
 * được trừ vào phiếu thu tiếp theo của người đó.
 */
@Entity
@Table(name = "partner_fee_expense")
public class PartnerFeeExpense implements Serializable {

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
    @Size(max = 20)
    @Column(name = "partner_code", length = 20, nullable = false)
    private String partnerCode;

    @NotNull
    @Size(max = 64)
    @Column(name = "partner_order_id", length = 64, nullable = false, unique = true)
    private String partnerOrderId;

    @NotNull
    @Column(name = "amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Size(max = 50)
    @Column(name = "payer_username", length = 50, nullable = false)
    private String payerUsername;

    @NotNull
    @Column(name = "incurred_at", nullable = false)
    private Instant incurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;

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

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public String getPartnerOrderId() {
        return partnerOrderId;
    }

    public void setPartnerOrderId(String partnerOrderId) {
        this.partnerOrderId = partnerOrderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayerUsername() {
        return payerUsername;
    }

    public void setPayerUsername(String payerUsername) {
        this.payerUsername = payerUsername;
    }

    public Instant getIncurredAt() {
        return incurredAt;
    }

    public void setIncurredAt(Instant incurredAt) {
        this.incurredAt = incurredAt;
    }

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PartnerFeeExpense)) {
            return false;
        }
        return id != null && id.equals(((PartnerFeeExpense) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
