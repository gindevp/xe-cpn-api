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
