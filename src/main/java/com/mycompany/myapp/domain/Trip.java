package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.TripStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A Trip.
 */
@Entity
@Table(name = "trip")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Trip implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 40)
    @Column(name = "trip_code", length = 40, nullable = false, unique = true)
    private String tripCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TripStatus status;

    @NotNull
    @Column(name = "depart_at", nullable = false)
    private Instant departAt;

    @Min(value = 0)
    @Column(name = "loaded_count")
    private Integer loadedCount;

    @Min(value = 0)
    @Column(name = "scanned_count")
    private Integer scannedCount;

    @Column(name = "closed_at")
    private Instant closedAt;

    @NotNull
    @Column(name = "force_closed", nullable = false)
    private Boolean forceClosed;

    @Size(max = 255)
    @Column(name = "force_close_reason", length = 255)
    private String forceCloseReason;

    @ManyToOne(optional = false)
    @NotNull
    private Office office;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "fromOffice", "toOffice" }, allowSetters = true)
    private Route route;

    @ManyToOne
    private Vehicle vehicle;

    @ManyToOne
    private Driver driver;

    @Size(max = 160)
    @Column(name = "itinerary_label", length = 160)
    private String itineraryLabel;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Trip id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTripCode() {
        return this.tripCode;
    }

    public Trip tripCode(String tripCode) {
        this.setTripCode(tripCode);
        return this;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public TripStatus getStatus() {
        return this.status;
    }

    public Trip status(TripStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public Instant getDepartAt() {
        return this.departAt;
    }

    public Trip departAt(Instant departAt) {
        this.setDepartAt(departAt);
        return this;
    }

    public void setDepartAt(Instant departAt) {
        this.departAt = departAt;
    }

    public Integer getLoadedCount() {
        return this.loadedCount;
    }

    public Trip loadedCount(Integer loadedCount) {
        this.setLoadedCount(loadedCount);
        return this;
    }

    public void setLoadedCount(Integer loadedCount) {
        this.loadedCount = loadedCount;
    }

    public Integer getScannedCount() {
        return this.scannedCount;
    }

    public Trip scannedCount(Integer scannedCount) {
        this.setScannedCount(scannedCount);
        return this;
    }

    public void setScannedCount(Integer scannedCount) {
        this.scannedCount = scannedCount;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public Trip closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Boolean getForceClosed() {
        return this.forceClosed;
    }

    public Trip forceClosed(Boolean forceClosed) {
        this.setForceClosed(forceClosed);
        return this;
    }

    public void setForceClosed(Boolean forceClosed) {
        this.forceClosed = forceClosed;
    }

    public String getForceCloseReason() {
        return this.forceCloseReason;
    }

    public Trip forceCloseReason(String forceCloseReason) {
        this.setForceCloseReason(forceCloseReason);
        return this;
    }

    public void setForceCloseReason(String forceCloseReason) {
        this.forceCloseReason = forceCloseReason;
    }

    public Office getOffice() {
        return this.office;
    }

    public void setOffice(Office office) {
        this.office = office;
    }

    public Trip office(Office office) {
        this.setOffice(office);
        return this;
    }

    public Route getRoute() {
        return this.route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Trip route(Route route) {
        this.setRoute(route);
        return this;
    }

    public Vehicle getVehicle() {
        return this.vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Trip vehicle(Vehicle vehicle) {
        this.setVehicle(vehicle);
        return this;
    }

    public Driver getDriver() {
        return this.driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Trip driver(Driver driver) {
        this.setDriver(driver);
        return this;
    }

    public String getItineraryLabel() {
        return itineraryLabel;
    }

    public void setItineraryLabel(String itineraryLabel) {
        this.itineraryLabel = itineraryLabel;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Trip)) {
            return false;
        }
        return getId() != null && getId().equals(((Trip) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Trip{" +
            "id=" + getId() +
            ", tripCode='" + getTripCode() + "'" +
            ", status='" + getStatus() + "'" +
            ", departAt='" + getDepartAt() + "'" +
            ", loadedCount=" + getLoadedCount() +
            ", scannedCount=" + getScannedCount() +
            ", closedAt='" + getClosedAt() + "'" +
            ", forceClosed='" + getForceClosed() + "'" +
            ", forceCloseReason='" + getForceCloseReason() + "'" +
            "}";
    }
}
