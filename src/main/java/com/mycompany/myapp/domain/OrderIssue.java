package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A OrderIssue.
 */
@Entity
@Table(name = "order_issue")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private IssueType issueType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status", nullable = false)
    private IssueStatus issueStatus;

    @Size(max = 255)
    @Column(name = "reason", length = 255)
    private String reason;

    @NotNull
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "opened_by_username", length = 50, nullable = false)
    private String openedByUsername;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Size(max = 50)
    @Column(name = "resolved_by_username", length = 50)
    private String resolvedByUsername;

    @Size(max = 255)
    @Column(name = "resolution_note", length = 255)
    private String resolutionNote;

    /**
     * History link to the shipment order (ManyToOne). Independent of {@link ShipmentOrder#getIssue()} current pointer.
     */
    @JsonIgnoreProperties(
        value = {
            "issue",
            "returnRequest",
            "fareAdjustmentRequest",
            "senderCustomer",
            "fromOffice",
            "toOffice",
            "hubOffice",
            "finalToOffice",
            "currentTrip",
        },
        allowSetters = true
    )
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private ShipmentOrder order;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public OrderIssue id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IssueType getIssueType() {
        return this.issueType;
    }

    public OrderIssue issueType(IssueType issueType) {
        this.setIssueType(issueType);
        return this;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public IssueStatus getIssueStatus() {
        return this.issueStatus;
    }

    public OrderIssue issueStatus(IssueStatus issueStatus) {
        this.setIssueStatus(issueStatus);
        return this;
    }

    public void setIssueStatus(IssueStatus issueStatus) {
        this.issueStatus = issueStatus;
    }

    public String getReason() {
        return this.reason;
    }

    public OrderIssue reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getOpenedAt() {
        return this.openedAt;
    }

    public OrderIssue openedAt(Instant openedAt) {
        this.setOpenedAt(openedAt);
        return this;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public String getOpenedByUsername() {
        return this.openedByUsername;
    }

    public OrderIssue openedByUsername(String openedByUsername) {
        this.setOpenedByUsername(openedByUsername);
        return this;
    }

    public void setOpenedByUsername(String openedByUsername) {
        this.openedByUsername = openedByUsername;
    }

    public Instant getResolvedAt() {
        return this.resolvedAt;
    }

    public OrderIssue resolvedAt(Instant resolvedAt) {
        this.setResolvedAt(resolvedAt);
        return this;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedByUsername() {
        return this.resolvedByUsername;
    }

    public OrderIssue resolvedByUsername(String resolvedByUsername) {
        this.setResolvedByUsername(resolvedByUsername);
        return this;
    }

    public void setResolvedByUsername(String resolvedByUsername) {
        this.resolvedByUsername = resolvedByUsername;
    }

    public String getResolutionNote() {
        return this.resolutionNote;
    }

    public OrderIssue resolutionNote(String resolutionNote) {
        this.setResolutionNote(resolutionNote);
        return this;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    /**
     * History link only — does not mutate {@link ShipmentOrder#getIssue()} current pointer.
     */
    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public OrderIssue order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderIssue)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderIssue) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderIssue{" +
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
