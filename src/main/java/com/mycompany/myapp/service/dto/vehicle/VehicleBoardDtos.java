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
        String reportedBy,
        Instant pickupAt
    ) {}

    public record ItineraryOption(String code, String name) {}

    /** Xe CRM của một lộ trình xuất bến hôm nay + giờ đã báo đến/rời tại VP của NV. */
    public record DayBoard(String officeCode, String officeName, List<DayItem> items, boolean departPhotoRequired) {}

    /** {@code pickupAt}: giờ đón khách tại VP = giờ xuất bến + phút lệch của lộ trình (Danh mục VP → Lộ trình áp dụng). */
    public record DayItem(
        String externalTripId,
        String vehiclePlate,
        String driverName,
        String routeLabel,
        Instant plannedDepartAt,
        Instant arrivedAt,
        String arrivedBy,
        Instant departedAt,
        String departedBy,
        Instant pickupAt
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
        String reason,
        Instant pickupAt,
        /** Lượt báo rời có ảnh xe — xem qua {@code GET /api/vehicle-events/{id}/photo}. */
        boolean hasPhoto
    ) {}

    public record EventPhoto(Long eventId, String photo, Instant capturedAt, String capturedBy) {}

    public record PhotoPolicy(boolean departPhotoRequired) {}

    /** {@code itineraries}: lộ trình báo giờ của VP đang xem (rỗng khi xem toàn hệ thống). */
    public record Report(List<ReportItem> events, List<ItineraryOption> itineraries) {}

    /** Cấu hình lộ trình báo giờ của VP: mọi lộ trình qua điểm của VP, {@code selected} = VP báo giờ lộ trình đó. */
    public record ItineraryConfig(Long officeId, String officeName, List<ConfigOption> options) {}

    /** {@code offsetMinutes}: phút lệch giờ đón so với giờ xuất bến (âm = sớm), null = trùng giờ xuất bến. */
    public record ConfigOption(String code, String name, boolean selected, Integer offsetMinutes) {}

    public record ItineraryOffset(String code, Integer offsetMinutes) {}

    /** {@code offsets}: phút lệch theo mã lộ trình (chỉ áp cho lộ trình có trong {@code itineraryCodes}). */
    public record ItineraryConfigRequest(List<String> itineraryCodes, List<ItineraryOffset> offsets) {
        public ItineraryConfigRequest(List<String> itineraryCodes) {
            this(itineraryCodes, null);
        }
    }

    public record ReportRequest(
        String eventType,
        String source,
        String tripCode,
        String externalTripId,
        String vehiclePlate,
        String driverName,
        String routeLabel,
        Instant plannedDepartAt,
        String reason,
        /** Mã lộ trình đang chọn trên app — để lấy phút lệch giờ đón; app cũ không gửi thì suy theo tên tuyến. */
        String itineraryCode,
        /** Ảnh xe (data URL). App gửi khi báo rời nếu cấu hình bắt buộc; web không gửi. */
        String photo,
        /** true = app mobile. Web không gửi — web không bắt ảnh. */
        Boolean fromApp
    ) {
        public ReportRequest(
            String eventType,
            String source,
            String tripCode,
            String externalTripId,
            String vehiclePlate,
            String driverName,
            String routeLabel,
            Instant plannedDepartAt,
            String reason,
            String itineraryCode
        ) {
            this(
                eventType,
                source,
                tripCode,
                externalTripId,
                vehiclePlate,
                driverName,
                routeLabel,
                plannedDepartAt,
                reason,
                itineraryCode,
                null,
                null
            );
        }

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
            this(
                eventType,
                source,
                tripCode,
                externalTripId,
                vehiclePlate,
                driverName,
                routeLabel,
                plannedDepartAt,
                null,
                null,
                null,
                null
            );
        }

        public ReportRequest(
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
            this(
                eventType,
                source,
                tripCode,
                externalTripId,
                vehiclePlate,
                driverName,
                routeLabel,
                plannedDepartAt,
                reason,
                null,
                null,
                null
            );
        }
    }
}
