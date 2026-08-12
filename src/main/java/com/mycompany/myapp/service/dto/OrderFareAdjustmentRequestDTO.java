package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.ApprovalStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderFareAdjustmentRequest} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderFareAdjustmentRequestDTO implements Serializable {

    private Long id;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal requestedFareAmount;

    @NotNull
    @Size(max = 255)
    private String reason;

    @NotNull
    @Size(max = 50)
    private String requestedByUsername;

    @NotNull
    private Instant requestedAt;

    @NotNull
    private ApprovalStatus status;

    @Size(max = 50)
    private String approvedByUsername;

    private Instant approvedAt;

    @Size(max = 50)
    private String rejectedByUsername;

    private Instant rejectedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getRequestedFareAmount() {
        return requestedFareAmount;
    }

    public void setRequestedFareAmount(BigDecimal requestedFareAmount) {
        this.requestedFareAmount = requestedFareAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getApprovedByUsername() {
        return approvedByUsername;
    }

    public void setApprovedByUsername(String approvedByUsername) {
        this.approvedByUsername = approvedByUsername;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectedByUsername() {
        return rejectedByUsername;
    }

    public void setRejectedByUsername(String rejectedByUsername) {
        this.rejectedByUsername = rejectedByUsername;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderFareAdjustmentRequestDTO)) {
            return false;
        }

        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = (OrderFareAdjustmentRequestDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderFareAdjustmentRequestDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderFareAdjustmentRequestDTO{" +
            "id=" + getId() +
            ", requestedFareAmount=" + getRequestedFareAmount() +
            ", reason='" + getReason() + "'" +
            ", requestedByUsername='" + getRequestedByUsername() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", status='" + getStatus() + "'" +
            ", approvedByUsername='" + getApprovedByUsername() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", rejectedByUsername='" + getRejectedByUsername() + "'" +
            ", rejectedAt='" + getRejectedAt() + "'" +
            "}";
    }
}
