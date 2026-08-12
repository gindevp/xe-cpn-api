package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.AssignmentStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.TripOrderAssignment} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TripOrderAssignmentDTO implements Serializable {

    private Long id;

    @NotNull
    private AssignmentStatus assignmentStatus;

    private Instant scannedAt;

    private Instant loadedAt;

    private Instant removedAt;

    @Size(max = 255)
    private String remark;

    @NotNull
    private TripDTO trip;

    @NotNull
    private ShipmentOrderDTO order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Instant scannedAt) {
        this.scannedAt = scannedAt;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(Instant loadedAt) {
        this.loadedAt = loadedAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public TripDTO getTrip() {
        return trip;
    }

    public void setTrip(TripDTO trip) {
        this.trip = trip;
    }

    public ShipmentOrderDTO getOrder() {
        return order;
    }

    public void setOrder(ShipmentOrderDTO order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TripOrderAssignmentDTO)) {
            return false;
        }

        TripOrderAssignmentDTO tripOrderAssignmentDTO = (TripOrderAssignmentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, tripOrderAssignmentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TripOrderAssignmentDTO{" +
            "id=" + getId() +
            ", assignmentStatus='" + getAssignmentStatus() + "'" +
            ", scannedAt='" + getScannedAt() + "'" +
            ", loadedAt='" + getLoadedAt() + "'" +
            ", removedAt='" + getRemovedAt() + "'" +
            ", remark='" + getRemark() + "'" +
            ", trip=" + getTrip() +
            ", order=" + getOrder() +
            "}";
    }
}
