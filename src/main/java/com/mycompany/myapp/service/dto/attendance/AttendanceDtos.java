package com.mycompany.myapp.service.dto.attendance;

import java.time.Instant;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() {}

    public record Today(String officeCode, String officeName, boolean wifiConfigured, List<AttendanceItemDTO> items) {}

    public record CheckInRequest(String photo) {}

    public record ClientIp(String ip, String forwardedFor) {}

    public record OfficeNetworkItem(Long id, String ipAddress, String label, String createdBy, Instant createdAt) {}

    public record OfficeNetworkRequest(String ipAddress, String label) {}

    public record ReportStaff(String login, String staffCode, String displayName, String officeCode, String officeName, boolean active) {}

    public record Report(List<ReportStaff> staff, List<AttendanceAdminRecordDTO> records) {}

    public record Photo(Long id, String photo) {}
}
