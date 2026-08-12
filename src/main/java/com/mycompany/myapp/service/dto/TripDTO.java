package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.TripStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Trip} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TripDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 40)
    private String tripCode;

    @NotNull
    private TripStatus status;

    @NotNull
    private Instant departAt;

    @Min(value = 0)
    private Integer loadedCount;

    @Min(value = 0)
    private Integer scannedCount;

    private Instant closedAt;

    @NotNull
    private Boolean forceClosed;

    @Size(max = 255)
    private String forceCloseReason;

    @NotNull
    private OfficeDTO office;

    @NotNull
    private RouteDTO route;

    @NotNull
    private VehicleDTO vehicle;

    @NotNull
    private DriverDTO driver;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public Instant getDepartAt() {
        return departAt;
    }

    public void setDepartAt(Instant departAt) {
        this.departAt = departAt;
    }

    public Integer getLoadedCount() {
        return loadedCount;
    }

    public void setLoadedCount(Integer loadedCount) {
        this.loadedCount = loadedCount;
    }

    public Integer getScannedCount() {
        return scannedCount;
    }

    public void setScannedCount(Integer scannedCount) {
        this.scannedCount = scannedCount;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Boolean getForceClosed() {
        return forceClosed;
    }

    public void setForceClosed(Boolean forceClosed) {
        this.forceClosed = forceClosed;
    }

    public String getForceCloseReason() {
        return forceCloseReason;
    }

    public void setForceCloseReason(String forceCloseReason) {
        this.forceCloseReason = forceCloseReason;
    }

    public OfficeDTO getOffice() {
        return office;
    }

    public void setOffice(OfficeDTO office) {
        this.office = office;
    }

    public RouteDTO getRoute() {
        return route;
    }

    public void setRoute(RouteDTO route) {
        this.route = route;
    }

    public VehicleDTO getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleDTO vehicle) {
        this.vehicle = vehicle;
    }

    public DriverDTO getDriver() {
        return driver;
    }

    public void setDriver(DriverDTO driver) {
        this.driver = driver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TripDTO)) {
            return false;
        }

        TripDTO tripDTO = (TripDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, tripDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TripDTO{" +
            "id=" + getId() +
            ", tripCode='" + getTripCode() + "'" +
            ", status='" + getStatus() + "'" +
            ", departAt='" + getDepartAt() + "'" +
            ", loadedCount=" + getLoadedCount() +
            ", scannedCount=" + getScannedCount() +
            ", closedAt='" + getClosedAt() + "'" +
            ", forceClosed='" + getForceClosed() + "'" +
            ", forceCloseReason='" + getForceCloseReason() + "'" +
            ", office=" + getOffice() +
            ", route=" + getRoute() +
            ", vehicle=" + getVehicle() +
            ", driver=" + getDriver() +
            "}";
    }
}
