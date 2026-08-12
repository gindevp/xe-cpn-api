package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Receipt} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReceiptDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 40)
    private String receiptCode;

    @NotNull
    @Size(max = 100)
    private String payerName;

    @Size(max = 40)
    private String payerCode;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal totalAmount;

    @NotNull
    private Instant createdAt;

    @NotNull
    @Size(max = 50)
    private String createdByUsername;

    private OfficeDTO office;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public String getPayerCode() {
        return payerCode;
    }

    public void setPayerCode(String payerCode) {
        this.payerCode = payerCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public OfficeDTO getOffice() {
        return office;
    }

    public void setOffice(OfficeDTO office) {
        this.office = office;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReceiptDTO)) {
            return false;
        }

        ReceiptDTO receiptDTO = (ReceiptDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, receiptDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReceiptDTO{" +
            "id=" + getId() +
            ", receiptCode='" + getReceiptCode() + "'" +
            ", payerName='" + getPayerName() + "'" +
            ", payerCode='" + getPayerCode() + "'" +
            ", totalAmount=" + getTotalAmount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", createdByUsername='" + getCreatedByUsername() + "'" +
            ", office=" + getOffice() +
            "}";
    }
}
