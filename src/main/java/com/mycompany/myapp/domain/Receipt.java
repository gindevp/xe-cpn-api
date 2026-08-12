package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A Receipt.
 */
@Entity
@Table(name = "receipt")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Receipt implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 40)
    @Column(name = "receipt_code", length = 40, nullable = false, unique = true)
    private String receiptCode;

    @NotNull
    @Size(max = 100)
    @Column(name = "payer_name", length = 100, nullable = false)
    private String payerName;

    @Size(max = 40)
    @Column(name = "payer_code", length = 40)
    private String payerCode;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "total_amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "created_by_username", length = 50, nullable = false)
    private String createdByUsername;

    @ManyToOne(fetch = FetchType.LAZY)
    private Office office;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Receipt id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptCode() {
        return this.receiptCode;
    }

    public Receipt receiptCode(String receiptCode) {
        this.setReceiptCode(receiptCode);
        return this;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public String getPayerName() {
        return this.payerName;
    }

    public Receipt payerName(String payerName) {
        this.setPayerName(payerName);
        return this;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public String getPayerCode() {
        return this.payerCode;
    }

    public Receipt payerCode(String payerCode) {
        this.setPayerCode(payerCode);
        return this;
    }

    public void setPayerCode(String payerCode) {
        this.payerCode = payerCode;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public Receipt totalAmount(BigDecimal totalAmount) {
        this.setTotalAmount(totalAmount);
        return this;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Receipt createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedByUsername() {
        return this.createdByUsername;
    }

    public Receipt createdByUsername(String createdByUsername) {
        this.setCreatedByUsername(createdByUsername);
        return this;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public Office getOffice() {
        return this.office;
    }

    public void setOffice(Office office) {
        this.office = office;
    }

    public Receipt office(Office office) {
        this.setOffice(office);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Receipt)) {
            return false;
        }
        return getId() != null && getId().equals(((Receipt) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Receipt{" +
            "id=" + getId() +
            ", receiptCode='" + getReceiptCode() + "'" +
            ", payerName='" + getPayerName() + "'" +
            ", payerCode='" + getPayerCode() + "'" +
            ", totalAmount=" + getTotalAmount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", createdByUsername='" + getCreatedByUsername() + "'" +
            "}";
    }
}
