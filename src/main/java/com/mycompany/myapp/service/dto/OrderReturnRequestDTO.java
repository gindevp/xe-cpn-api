package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.ApprovalStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderReturnRequest} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderReturnRequestDTO implements Serializable {

    private Long id;

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
    private String decidedByUsername;

    private Instant decidedAt;

    @Size(max = 255)
    private String decisionNote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDecidedByUsername() {
        return decidedByUsername;
    }

    public void setDecidedByUsername(String decidedByUsername) {
        this.decidedByUsername = decidedByUsername;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderReturnRequestDTO)) {
            return false;
        }

        OrderReturnRequestDTO orderReturnRequestDTO = (OrderReturnRequestDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderReturnRequestDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderReturnRequestDTO{" +
            "id=" + getId() +
            ", reason='" + getReason() + "'" +
            ", requestedByUsername='" + getRequestedByUsername() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", status='" + getStatus() + "'" +
            ", decidedByUsername='" + getDecidedByUsername() + "'" +
            ", decidedAt='" + getDecidedAt() + "'" +
            ", decisionNote='" + getDecisionNote() + "'" +
            "}";
    }
}
