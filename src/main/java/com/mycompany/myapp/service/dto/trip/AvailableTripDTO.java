package com.mycompany.myapp.service.dto.trip;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lean trip row for "Xe khả dụng" UI — mapped from VTHK {@code search_trips}.
 */
public class AvailableTripDTO {

    private String externalTripId;
    private String vehiclePlate;
    private String driverName;
    private String driverPhone;
    private String routeLabel;
    private String itineraryCode;
    private String timeSlot;
    private Instant departAt;
    /** Trip end time from VTHK {@code NgayKetThuc}. */
    private Instant endAt;
    private String vehicleType;
    private Integer seatTotal;
    private Integer seatAvailable;
    /** CPN cargo already assigned to a local trip matching this VTHK trip (kg). */
    private BigDecimal usedKg;
    /** CPN order count already assigned. */
    private Integer usedOrderCount;
    /** Suggested plate for {@code POST /api/trips} when VTHK plate is blank. */
    private String assignVehiclePlate;
    private String assignDriverName;

    public String getExternalTripId() {
        return externalTripId;
    }

    public void setExternalTripId(String externalTripId) {
        this.externalTripId = externalTripId;
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

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    public void setRouteLabel(String routeLabel) {
        this.routeLabel = routeLabel;
    }

    public String getItineraryCode() {
        return itineraryCode;
    }

    public void setItineraryCode(String itineraryCode) {
        this.itineraryCode = itineraryCode;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Instant getDepartAt() {
        return departAt;
    }

    public void setDepartAt(Instant departAt) {
        this.departAt = departAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getSeatTotal() {
        return seatTotal;
    }

    public void setSeatTotal(Integer seatTotal) {
        this.seatTotal = seatTotal;
    }

    public Integer getSeatAvailable() {
        return seatAvailable;
    }

    public void setSeatAvailable(Integer seatAvailable) {
        this.seatAvailable = seatAvailable;
    }

    public BigDecimal getUsedKg() {
        return usedKg;
    }

    public void setUsedKg(BigDecimal usedKg) {
        this.usedKg = usedKg;
    }

    public Integer getUsedOrderCount() {
        return usedOrderCount;
    }

    public void setUsedOrderCount(Integer usedOrderCount) {
        this.usedOrderCount = usedOrderCount;
    }

    public String getAssignVehiclePlate() {
        return assignVehiclePlate;
    }

    public void setAssignVehiclePlate(String assignVehiclePlate) {
        this.assignVehiclePlate = assignVehiclePlate;
    }

    public String getAssignDriverName() {
        return assignDriverName;
    }

    public void setAssignDriverName(String assignDriverName) {
        this.assignDriverName = assignDriverName;
    }
}
