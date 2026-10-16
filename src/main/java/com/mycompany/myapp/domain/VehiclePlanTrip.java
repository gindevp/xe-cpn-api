package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/** Một chuyến bảng tài CRM đã chụp cuối ngày. Portal không trả lại ngày đã qua. */
@Entity
@Table(name = "vehicle_plan_trip")
public class VehiclePlanTrip implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @NotNull
    @Size(max = 60)
    @Column(name = "external_trip_id", length = 60, nullable = false)
    private String externalTripId;

    @Size(max = 60)
    @Column(name = "itinerary_code", length = 60)
    private String itineraryCode;

    @Size(max = 30)
    @Column(name = "vehicle_plate", length = 30)
    private String vehiclePlate;

    @Size(max = 120)
    @Column(name = "driver_name", length = 120)
    private String driverName;

    @Size(max = 120)
    @Column(name = "route_label", length = 120)
    private String routeLabel;

    @Column(name = "depart_at")
    private Instant departAt;

    @NotNull
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    public Long getId() {
        return id;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getExternalTripId() {
        return externalTripId;
    }

    public void setExternalTripId(String externalTripId) {
        this.externalTripId = externalTripId;
    }

    public String getItineraryCode() {
        return itineraryCode;
    }

    public void setItineraryCode(String itineraryCode) {
        this.itineraryCode = itineraryCode;
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

    public String getRouteLabel() {
        return routeLabel;
    }

    public void setRouteLabel(String routeLabel) {
        this.routeLabel = routeLabel;
    }

    public Instant getDepartAt() {
        return departAt;
    }

    public void setDepartAt(Instant departAt) {
        this.departAt = departAt;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }
}
