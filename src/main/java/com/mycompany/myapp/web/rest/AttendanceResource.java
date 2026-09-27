package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.ClientIpResolver;
import com.mycompany.myapp.service.attendance.AttendanceService;
import com.mycompany.myapp.service.dto.attendance.AttendanceDtos;
import com.mycompany.myapp.service.dto.attendance.AttendanceItemDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AttendanceResource {

    private final AttendanceService attendanceService;

    public AttendanceResource(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/api/attendance/today")
    public AttendanceDtos.Today today() {
        return attendanceService.today();
    }

    /** Mọi NV có hồ sơ đều chấm được (không cần quyền ghi màn hình) — xem StaffWriteGuardFilter. */
    @PostMapping("/api/attendance/check-in")
    public AttendanceItemDTO checkIn(@RequestBody AttendanceDtos.CheckInRequest body, HttpServletRequest request) {
        return attendanceService.checkIn(body == null ? null : body.photo(), ClientIpResolver.resolve(request));
    }

    /** IP công cộng server đang thấy — web admin dùng cho nút "Lấy IP máy này". */
    @GetMapping("/api/attendance/my-ip")
    public AttendanceDtos.ClientIp myIp(HttpServletRequest request) {
        return new AttendanceDtos.ClientIp(ClientIpResolver.resolve(request), request.getHeader("X-Forwarded-For"));
    }

    /** Bảng công quản trị (screen cham-cong). officeCode / login bỏ trống = toàn hệ thống. */
    @GetMapping("/api/attendance/report")
    public AttendanceDtos.Report report(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String officeCode,
        @RequestParam(required = false) String login
    ) {
        return attendanceService.report(from, to, officeCode, login);
    }

    @GetMapping("/api/attendance/records/{id}/photo")
    public AttendanceDtos.Photo photo(@PathVariable Long id) {
        return attendanceService.photo(id);
    }

    @GetMapping("/api/offices/{officeId}/networks")
    public List<AttendanceDtos.OfficeNetworkItem> listNetworks(@PathVariable Long officeId) {
        return attendanceService.listNetworks(officeId);
    }

    /** Ghi: screen Master (StaffWriteGuardFilter, prefix /api/offices). */
    @PostMapping("/api/offices/{officeId}/networks")
    public AttendanceDtos.OfficeNetworkItem addNetwork(@PathVariable Long officeId, @RequestBody AttendanceDtos.OfficeNetworkRequest body) {
        return attendanceService.addNetwork(officeId, body);
    }

    @DeleteMapping("/api/offices/{officeId}/networks/{networkId}")
    public ResponseEntity<Void> deleteNetwork(@PathVariable Long officeId, @PathVariable Long networkId) {
        attendanceService.deleteNetwork(officeId, networkId);
        return ResponseEntity.noContent().build();
    }
}
