package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.AssignmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A TripOrderAssignment.
 */
@Entity
@Table(name = "trip_order_assignment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TripOrderAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false)
    private AssignmentStatus assignmentStatus;

    @Column(name = "scanned_at")
    private Instant scannedAt;

    @Column(name = "loaded_at")
    private Instant loadedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Size(max = 255)
    @Column(name = "remark", length = 255)
    private String remark;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "office", "route", "vehicle", "driver" }, allowSetters = true)
    private Trip trip;

    @ManyToOne(optional = false)
    @NotNull
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
    private ShipmentOrder order;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public TripOrderAssignment id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AssignmentStatus getAssignmentStatus() {
        return this.assignmentStatus;
    }

    public TripOrderAssignment assignmentStatus(AssignmentStatus assignmentStatus) {
        this.setAssignmentStatus(assignmentStatus);
        return this;
    }

    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public Instant getScannedAt() {
        return this.scannedAt;
    }

    public TripOrderAssignment scannedAt(Instant scannedAt) {
        this.setScannedAt(scannedAt);
        return this;
    }

    public void setScannedAt(Instant scannedAt) {
        this.scannedAt = scannedAt;
    }

    public Instant getLoadedAt() {
        return this.loadedAt;
    }

    public TripOrderAssignment loadedAt(Instant loadedAt) {
        this.setLoadedAt(loadedAt);
        return this;
    }

    public void setLoadedAt(Instant loadedAt) {
        this.loadedAt = loadedAt;
    }

    public Instant getRemovedAt() {
        return this.removedAt;
    }

    public TripOrderAssignment removedAt(Instant removedAt) {
        this.setRemovedAt(removedAt);
        return this;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemark() {
        return this.remark;
    }

    public TripOrderAssignment remark(String remark) {
        this.setRemark(remark);
        return this;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Trip getTrip() {
        return this.trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public TripOrderAssignment trip(Trip trip) {
        this.setTrip(trip);
        return this;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public TripOrderAssignment order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TripOrderAssignment)) {
            return false;
        }
        return getId() != null && getId().equals(((TripOrderAssignment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TripOrderAssignment{" +
            "id=" + getId() +
            ", assignmentStatus='" + getAssignmentStatus() + "'" +
            ", scannedAt='" + getScannedAt() + "'" +
            ", loadedAt='" + getLoadedAt() + "'" +
            ", removedAt='" + getRemovedAt() + "'" +
            ", remark='" + getRemark() + "'" +
            "}";
    }
}
