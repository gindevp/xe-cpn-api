package com.mycompany.myapp.service.dto.vehicle;

import java.time.Instant;
import java.util.List;

public final class VehicleBoardDtos {

    private VehicleBoardDtos() {}

    /** {@code crmWarning} khác null khi không lấy được chuyến Limousine từ CRM (danh sách vẫn có chuyến trong hệ thống). */
    public record Board(String officeCode, String officeName, List<Item> items, String crmWarning) {}

    public record Item(
        String key,
        String eventType,
        String source,
        String tripCode,
        String externalTripId,
        String vehiclePlate,
        String driverName,
        String routeLabel,
        Instant plannedDepartAt,
        Instant reportedAt,
        String reportedBy
    ) {}

    public record ItineraryOption(String code, String name) {}

    /** Xe CRM của một lộ trình xuất bến hôm nay + giờ đã báo đến/rời tại VP của NV. */
    public record DayBoard(String officeCode, String officeName, List<DayItem> items) {}

    public record DayItem(
        String externalTripId,
        String vehiclePlate,
        String driverName,
        String routeLabel,
        Instant plannedDepartAt,
        Instant arrivedAt,
        String arrivedBy,
        Instant departedAt,
        String departedBy
    ) {}

    public record ReportItem(
        Long id,
        String officeCode,
        String officeName,
        String eventType,
        String source,
        String tripKey,
        String tripCode,
        String externalTripId,
        String vehiclePlate,
        String driverName,
        String routeLabel,
        Instant plannedDepartAt,
        Instant eventAt,
        String reportedBy,
        String reportedByName
    ) {}

    public record Report(List<ReportItem> events) {}

    public record ReportRequest(
        String eventType,
        String source,
        String tripCode,
        String externalTripId,
        String vehiclePlate,
        String driverName,
        String routeLabel,
        Instant plannedDepartAt
    ) {}
}
