package com.mycompany.myapp.service.criteria;

import com.mycompany.myapp.domain.enumeration.TripStatus;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.Trip} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.TripResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /trips?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TripCriteria implements Serializable, Criteria {

    /**
     * Class for filtering TripStatus
     */
    public static class TripStatusFilter extends Filter<TripStatus> {

        public TripStatusFilter() {}

        public TripStatusFilter(TripStatusFilter filter) {
            super(filter);
        }

        @Override
        public TripStatusFilter copy() {
            return new TripStatusFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter tripCode;

    private TripStatusFilter status;

    private InstantFilter departAt;

    private IntegerFilter loadedCount;

    private IntegerFilter scannedCount;

    private InstantFilter closedAt;

    private BooleanFilter forceClosed;

    private StringFilter forceCloseReason;

    private LongFilter officeId;

    private LongFilter routeId;

    private LongFilter vehicleId;

    private LongFilter driverId;

    private Boolean distinct;

    public TripCriteria() {}

    public TripCriteria(TripCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.tripCode = other.optionalTripCode().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(TripStatusFilter::copy).orElse(null);
        this.departAt = other.optionalDepartAt().map(InstantFilter::copy).orElse(null);
        this.loadedCount = other.optionalLoadedCount().map(IntegerFilter::copy).orElse(null);
        this.scannedCount = other.optionalScannedCount().map(IntegerFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.forceClosed = other.optionalForceClosed().map(BooleanFilter::copy).orElse(null);
        this.forceCloseReason = other.optionalForceCloseReason().map(StringFilter::copy).orElse(null);
        this.officeId = other.optionalOfficeId().map(LongFilter::copy).orElse(null);
        this.routeId = other.optionalRouteId().map(LongFilter::copy).orElse(null);
        this.vehicleId = other.optionalVehicleId().map(LongFilter::copy).orElse(null);
        this.driverId = other.optionalDriverId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public TripCriteria copy() {
        return new TripCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getTripCode() {
        return tripCode;
    }

    public Optional<StringFilter> optionalTripCode() {
        return Optional.ofNullable(tripCode);
    }

    public StringFilter tripCode() {
        if (tripCode == null) {
            setTripCode(new StringFilter());
        }
        return tripCode;
    }

    public void setTripCode(StringFilter tripCode) {
        this.tripCode = tripCode;
    }

    public TripStatusFilter getStatus() {
        return status;
    }

    public Optional<TripStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public TripStatusFilter status() {
        if (status == null) {
            setStatus(new TripStatusFilter());
        }
        return status;
    }

    public void setStatus(TripStatusFilter status) {
        this.status = status;
    }

    public InstantFilter getDepartAt() {
        return departAt;
    }

    public Optional<InstantFilter> optionalDepartAt() {
        return Optional.ofNullable(departAt);
    }

    public InstantFilter departAt() {
        if (departAt == null) {
            setDepartAt(new InstantFilter());
        }
        return departAt;
    }

    public void setDepartAt(InstantFilter departAt) {
        this.departAt = departAt;
    }

    public IntegerFilter getLoadedCount() {
        return loadedCount;
    }

    public Optional<IntegerFilter> optionalLoadedCount() {
        return Optional.ofNullable(loadedCount);
    }

    public IntegerFilter loadedCount() {
        if (loadedCount == null) {
            setLoadedCount(new IntegerFilter());
        }
        return loadedCount;
    }

    public void setLoadedCount(IntegerFilter loadedCount) {
        this.loadedCount = loadedCount;
    }

    public IntegerFilter getScannedCount() {
        return scannedCount;
    }

    public Optional<IntegerFilter> optionalScannedCount() {
        return Optional.ofNullable(scannedCount);
    }

    public IntegerFilter scannedCount() {
        if (scannedCount == null) {
            setScannedCount(new IntegerFilter());
        }
        return scannedCount;
    }

    public void setScannedCount(IntegerFilter scannedCount) {
        this.scannedCount = scannedCount;
    }

    public InstantFilter getClosedAt() {
        return closedAt;
    }

    public Optional<InstantFilter> optionalClosedAt() {
        return Optional.ofNullable(closedAt);
    }

    public InstantFilter closedAt() {
        if (closedAt == null) {
            setClosedAt(new InstantFilter());
        }
        return closedAt;
    }

    public void setClosedAt(InstantFilter closedAt) {
        this.closedAt = closedAt;
    }

    public BooleanFilter getForceClosed() {
        return forceClosed;
    }

    public Optional<BooleanFilter> optionalForceClosed() {
        return Optional.ofNullable(forceClosed);
    }

    public BooleanFilter forceClosed() {
        if (forceClosed == null) {
            setForceClosed(new BooleanFilter());
        }
        return forceClosed;
    }

    public void setForceClosed(BooleanFilter forceClosed) {
        this.forceClosed = forceClosed;
    }

    public StringFilter getForceCloseReason() {
        return forceCloseReason;
    }

    public Optional<StringFilter> optionalForceCloseReason() {
        return Optional.ofNullable(forceCloseReason);
    }

    public StringFilter forceCloseReason() {
        if (forceCloseReason == null) {
            setForceCloseReason(new StringFilter());
        }
        return forceCloseReason;
    }

    public void setForceCloseReason(StringFilter forceCloseReason) {
        this.forceCloseReason = forceCloseReason;
    }

    public LongFilter getOfficeId() {
        return officeId;
    }

    public Optional<LongFilter> optionalOfficeId() {
        return Optional.ofNullable(officeId);
    }

    public LongFilter officeId() {
        if (officeId == null) {
            setOfficeId(new LongFilter());
        }
        return officeId;
    }

    public void setOfficeId(LongFilter officeId) {
        this.officeId = officeId;
    }

    public LongFilter getRouteId() {
        return routeId;
    }

    public Optional<LongFilter> optionalRouteId() {
        return Optional.ofNullable(routeId);
    }

    public LongFilter routeId() {
        if (routeId == null) {
            setRouteId(new LongFilter());
        }
        return routeId;
    }

    public void setRouteId(LongFilter routeId) {
        this.routeId = routeId;
    }

    public LongFilter getVehicleId() {
        return vehicleId;
    }

    public Optional<LongFilter> optionalVehicleId() {
        return Optional.ofNullable(vehicleId);
    }

    public LongFilter vehicleId() {
        if (vehicleId == null) {
            setVehicleId(new LongFilter());
        }
        return vehicleId;
    }

    public void setVehicleId(LongFilter vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LongFilter getDriverId() {
        return driverId;
    }

    public Optional<LongFilter> optionalDriverId() {
        return Optional.ofNullable(driverId);
    }

    public LongFilter driverId() {
        if (driverId == null) {
            setDriverId(new LongFilter());
        }
        return driverId;
    }

    public void setDriverId(LongFilter driverId) {
        this.driverId = driverId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TripCriteria that = (TripCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(tripCode, that.tripCode) &&
            Objects.equals(status, that.status) &&
            Objects.equals(departAt, that.departAt) &&
            Objects.equals(loadedCount, that.loadedCount) &&
            Objects.equals(scannedCount, that.scannedCount) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(forceClosed, that.forceClosed) &&
            Objects.equals(forceCloseReason, that.forceCloseReason) &&
            Objects.equals(officeId, that.officeId) &&
            Objects.equals(routeId, that.routeId) &&
            Objects.equals(vehicleId, that.vehicleId) &&
            Objects.equals(driverId, that.driverId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            tripCode,
            status,
            departAt,
            loadedCount,
            scannedCount,
            closedAt,
            forceClosed,
            forceCloseReason,
            officeId,
            routeId,
            vehicleId,
            driverId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TripCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTripCode().map(f -> "tripCode=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalDepartAt().map(f -> "departAt=" + f + ", ").orElse("") +
            optionalLoadedCount().map(f -> "loadedCount=" + f + ", ").orElse("") +
            optionalScannedCount().map(f -> "scannedCount=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalForceClosed().map(f -> "forceClosed=" + f + ", ").orElse("") +
            optionalForceCloseReason().map(f -> "forceCloseReason=" + f + ", ").orElse("") +
            optionalOfficeId().map(f -> "officeId=" + f + ", ").orElse("") +
            optionalRouteId().map(f -> "routeId=" + f + ", ").orElse("") +
            optionalVehicleId().map(f -> "vehicleId=" + f + ", ").orElse("") +
            optionalDriverId().map(f -> "driverId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
