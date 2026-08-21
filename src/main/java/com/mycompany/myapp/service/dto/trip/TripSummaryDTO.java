package com.mycompany.myapp.service.dto.trip;

import com.mycompany.myapp.domain.enumeration.AssignmentStatus;
import com.mycompany.myapp.domain.enumeration.TripStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TripSummaryDTO {

    private Long id;
    private String tripCode;
    private TripStatus status;
    private Instant departAt;
    private Instant closedAt;
    private Boolean forceClosed;
    private String forceCloseReason;
    private Integer loadedCount;
    private Integer scannedCount;
    private String officeCode;
    private String routeCode;
    private String routeName;
    private String itineraryLabel;
    private String vehiclePlate;
    private String driverName;
    private String driverCode;
    private List<AssignmentView> assignments = new ArrayList<>();

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

    public String getOfficeCode() {
        return officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getItineraryLabel() {
        return itineraryLabel;
    }

    public void setItineraryLabel(String itineraryLabel) {
        this.itineraryLabel = itineraryLabel;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverCode() {
        return driverCode;
    }

    public void setDriverCode(String driverCode) {
        this.driverCode = driverCode;
    }

    public List<AssignmentView> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentView> assignments) {
        this.assignments = assignments;
    }

    public static class AssignmentView {

        private String orderCode;
        private AssignmentStatus assignmentStatus;
        private Instant scannedAt;
        private Instant loadedAt;

        public String getOrderCode() {
            return orderCode;
        }

        public void setOrderCode(String orderCode) {
            this.orderCode = orderCode;
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
    }
}
