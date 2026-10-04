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
        String reportedByName,
        String reason
    ) {}

    /** {@code itineraries}: lộ trình báo giờ của VP đang xem (rỗng khi xem toàn hệ thống). */
    public record Report(List<ReportItem> events, List<ItineraryOption> itineraries) {}

    /** Cấu hình lộ trình báo giờ của VP: mọi lộ trình qua điểm của VP, {@code selected} = VP báo giờ lộ trình đó. */
    public record ItineraryConfig(Long officeId, String officeName, List<ConfigOption> options) {}

    public record ConfigOption(String code, String name, boolean selected) {}

    public record ItineraryConfigRequest(List<String> itineraryCodes) {}

    public record ReportRequest(
        String eventType,
        String source,
        String tripCode,
        String externalTripId,
        String vehiclePlate,
        String driverName,
        String routeLabel,
        Instant plannedDepartAt,
        String reason
    ) {
        public ReportRequest(
            String eventType,
            String source,
            String tripCode,
            String externalTripId,
            String vehiclePlate,
            String driverName,
            String routeLabel,
            Instant plannedDepartAt
        ) {
            this(eventType, source, tripCode, externalTripId, vehiclePlate, driverName, routeLabel, plannedDepartAt, null);
        }
    }
}
