package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/** Giờ thực tế xe đến/rời một văn phòng. Chỉ ghi nhận — không đổi trạng thái chuyến hay đơn. */
@Entity
@Table(name = "vehicle_office_event")
public class VehicleOfficeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum EventType {
        ARRIVE,
        DEPART,
    }

    /** TRIP = chuyến trong hệ thống, CRM = chuyến Limousine lấy từ CRM. */
    public enum Source {
        TRIP,
        CRM,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 10, nullable = false)
    private EventType eventType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 10, nullable = false)
    private Source source;

    /** {@code T:<tripCode>} hoặc {@code C:<externalTripId>}. */
    @NotNull
    @Column(name = "trip_key", length = 80, nullable = false)
    private String tripKey;

    @Column(name = "trip_code", length = 60)
    private String tripCode;

    @Column(name = "external_trip_id", length = 60)
    private String externalTripId;

    @Column(name = "vehicle_plate", length = 30)
    private String vehiclePlate;

    @Column(name = "driver_name", length = 120)
    private String driverName;

    @Column(name = "route_label", length = 120)
    private String routeLabel;

    @Column(name = "planned_depart_at")
    private Instant plannedDepartAt;

    @NotNull
    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(name = "reported_by", length = 50)
    private String reportedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Office getOffice() {
        return office;
    }

    public void setOffice(Office office) {
        this.office = office;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getTripKey() {
        return tripKey;
    }

    public void setTripKey(String tripKey) {
        this.tripKey = tripKey;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

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

    public String getRouteLabel() {
        return routeLabel;
    }

    public void setRouteLabel(String routeLabel) {
        this.routeLabel = routeLabel;
    }

    public Instant getPlannedDepartAt() {
        return plannedDepartAt;
    }

    public void setPlannedDepartAt(Instant plannedDepartAt) {
        this.plannedDepartAt = plannedDepartAt;
    }

    public Instant getEventAt() {
        return eventAt;
    }

    public void setEventAt(Instant eventAt) {
        this.eventAt = eventAt;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }
}
