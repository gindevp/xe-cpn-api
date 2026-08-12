package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderIssue} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderIssueDTO implements Serializable {

    private Long id;

    @NotNull
    private IssueType issueType;

    @NotNull
    private IssueStatus issueStatus;

    @Size(max = 255)
    private String reason;

    @NotNull
    private Instant openedAt;

    @NotNull
    @Size(max = 50)
    private String openedByUsername;

    private Instant resolvedAt;

    @Size(max = 50)
    private String resolvedByUsername;

    @Size(max = 255)
    private String resolutionNote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public IssueStatus getIssueStatus() {
        return issueStatus;
    }

    public void setIssueStatus(IssueStatus issueStatus) {
        this.issueStatus = issueStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public String getOpenedByUsername() {
        return openedByUsername;
    }

    public void setOpenedByUsername(String openedByUsername) {
        this.openedByUsername = openedByUsername;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedByUsername() {
        return resolvedByUsername;
    }

    public void setResolvedByUsername(String resolvedByUsername) {
        this.resolvedByUsername = resolvedByUsername;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderIssueDTO)) {
            return false;
        }

        OrderIssueDTO orderIssueDTO = (OrderIssueDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderIssueDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderIssueDTO{" +
            "id=" + getId() +
            ", issueType='" + getIssueType() + "'" +
            ", issueStatus='" + getIssueStatus() + "'" +
            ", reason='" + getReason() + "'" +
            ", openedAt='" + getOpenedAt() + "'" +
            ", openedByUsername='" + getOpenedByUsername() + "'" +
            ", resolvedAt='" + getResolvedAt() + "'" +
            ", resolvedByUsername='" + getResolvedByUsername() + "'" +
            ", resolutionNote='" + getResolutionNote() + "'" +
            "}";
    }
}
