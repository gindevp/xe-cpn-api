package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.ApprovalStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A OrderFareAdjustmentRequest.
 */
@Entity
@Table(name = "order_fare_adjustment_request")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderFareAdjustmentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "requested_fare_amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal requestedFareAmount;

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
    @Column(name = "approved_by_username", length = 50)
    private String approvedByUsername;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Size(max = 50)
    @Column(name = "rejected_by_username", length = 50)
    private String rejectedByUsername;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

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
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "fareAdjustmentRequest")
    private ShipmentOrder order;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public OrderFareAdjustmentRequest id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getRequestedFareAmount() {
        return this.requestedFareAmount;
    }

    public OrderFareAdjustmentRequest requestedFareAmount(BigDecimal requestedFareAmount) {
        this.setRequestedFareAmount(requestedFareAmount);
        return this;
    }

    public void setRequestedFareAmount(BigDecimal requestedFareAmount) {
        this.requestedFareAmount = requestedFareAmount;
    }

    public String getReason() {
        return this.reason;
    }

    public OrderFareAdjustmentRequest reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestedByUsername() {
        return this.requestedByUsername;
    }

    public OrderFareAdjustmentRequest requestedByUsername(String requestedByUsername) {
        this.setRequestedByUsername(requestedByUsername);
        return this;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }

    public OrderFareAdjustmentRequest requestedAt(Instant requestedAt) {
        this.setRequestedAt(requestedAt);
        return this;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ApprovalStatus getStatus() {
        return this.status;
    }

    public OrderFareAdjustmentRequest status(ApprovalStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getApprovedByUsername() {
        return this.approvedByUsername;
    }

    public OrderFareAdjustmentRequest approvedByUsername(String approvedByUsername) {
        this.setApprovedByUsername(approvedByUsername);
        return this;
    }

    public void setApprovedByUsername(String approvedByUsername) {
        this.approvedByUsername = approvedByUsername;
    }

    public Instant getApprovedAt() {
        return this.approvedAt;
    }

    public OrderFareAdjustmentRequest approvedAt(Instant approvedAt) {
        this.setApprovedAt(approvedAt);
        return this;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectedByUsername() {
        return this.rejectedByUsername;
    }

    public OrderFareAdjustmentRequest rejectedByUsername(String rejectedByUsername) {
        this.setRejectedByUsername(rejectedByUsername);
        return this;
    }

    public void setRejectedByUsername(String rejectedByUsername) {
        this.rejectedByUsername = rejectedByUsername;
    }

    public Instant getRejectedAt() {
        return this.rejectedAt;
    }

    public OrderFareAdjustmentRequest rejectedAt(Instant rejectedAt) {
        this.setRejectedAt(rejectedAt);
        return this;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        if (this.order != null) {
            this.order.setFareAdjustmentRequest(null);
        }
        if (shipmentOrder != null) {
            shipmentOrder.setFareAdjustmentRequest(this);
        }
        this.order = shipmentOrder;
    }

    public OrderFareAdjustmentRequest order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderFareAdjustmentRequest)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderFareAdjustmentRequest) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderFareAdjustmentRequest{" +
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
