package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.ApprovalStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A OrderReturnRequest.
 */
@Entity
@Table(name = "order_return_request")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderReturnRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "reason", length = 255, nullable = false)
    private String reason;

    @NotNull
    @Size(max = 50)
    @Column(name = "requested_by_username", length = 50, nullable = false)
    private String requestedByUsername;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status;

    @Size(max = 50)
    @Column(name = "decided_by_username", length = 50)
    private String decidedByUsername;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Size(max = 255)
    @Column(name = "decision_note", length = 255)
    private String decisionNote;

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
    /**
     * History link to the shipment order (ManyToOne). Independent of {@link ShipmentOrder#getReturnRequest()} current pointer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private ShipmentOrder order;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public OrderReturnRequest id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReason() {
        return this.reason;
    }

    public OrderReturnRequest reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestedByUsername() {
        return this.requestedByUsername;
    }

    public OrderReturnRequest requestedByUsername(String requestedByUsername) {
        this.setRequestedByUsername(requestedByUsername);
        return this;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }

    public OrderReturnRequest requestedAt(Instant requestedAt) {
        this.setRequestedAt(requestedAt);
        return this;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ApprovalStatus getStatus() {
        return this.status;
    }

    public OrderReturnRequest status(ApprovalStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getDecidedByUsername() {
        return this.decidedByUsername;
    }

    public OrderReturnRequest decidedByUsername(String decidedByUsername) {
        this.setDecidedByUsername(decidedByUsername);
        return this;
    }

    public void setDecidedByUsername(String decidedByUsername) {
        this.decidedByUsername = decidedByUsername;
    }

    public Instant getDecidedAt() {
        return this.decidedAt;
    }

    public OrderReturnRequest decidedAt(Instant decidedAt) {
        this.setDecidedAt(decidedAt);
        return this;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecisionNote() {
        return this.decisionNote;
    }

    public OrderReturnRequest decisionNote(String decisionNote) {
        this.setDecisionNote(decisionNote);
        return this;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    /**
     * History link only — does not mutate {@link ShipmentOrder#getReturnRequest()} current pointer.
     */
    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public OrderReturnRequest order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderReturnRequest)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderReturnRequest) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderReturnRequest{" +
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
