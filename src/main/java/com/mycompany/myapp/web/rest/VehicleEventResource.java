package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.dto.vehicle.VehicleBoardDtos;
import com.mycompany.myapp.service.vehicle.VehicleBoardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VehicleEventResource {

    private final VehicleBoardService vehicleBoardService;

    public VehicleEventResource(VehicleBoardService vehicleBoardService) {
        this.vehicleBoardService = vehicleBoardService;
    }

    /** Public (trang khách tạo đơn): mã VP → lộ trình VP báo giờ; VP không có trong map = chưa giới hạn. */
    @GetMapping("/offices/vehicle-itineraries")
    public java.util.Map<String, java.util.List<String>> allOfficeItineraries() {
        return vehicleBoardService.allOfficeItineraries();
    }

    /** Danh mục VP: lộ trình VP báo giờ xe đến/đi. */
    @GetMapping("/offices/{officeId}/vehicle-itineraries")
    public VehicleBoardDtos.ItineraryConfig itineraryConfig(@PathVariable Long officeId) {
        return vehicleBoardService.itineraryConfig(officeId);
    }

    /** Ghi: screen Master (StaffWriteGuardFilter, prefix /api/offices). */
    @PutMapping("/offices/{officeId}/vehicle-itineraries")
    public VehicleBoardDtos.ItineraryConfig saveItineraryConfig(
        @PathVariable Long officeId,
        @RequestBody VehicleBoardDtos.ItineraryConfigRequest body
    ) {
        return vehicleBoardService.saveItineraryConfig(officeId, body);
    }

    @GetMapping("/vehicle-events/board")
    public VehicleBoardDtos.Board board() {
        return vehicleBoardService.board();
    }

    /** Theo dõi quản trị (screen bao-gio-xe). officeCode bỏ trống = toàn hệ thống. */
    @GetMapping("/vehicle-events/itineraries")
    public java.util.List<VehicleBoardDtos.ItineraryOption> itineraries() {
        return vehicleBoardService.officeItineraries();
    }

    @GetMapping("/vehicle-events/day-trips")
    public VehicleBoardDtos.DayBoard dayTrips(@RequestParam String itineraryCode) {
        return vehicleBoardService.dayTrips(itineraryCode);
    }

    @GetMapping("/vehicle-events/photo-policy")
    public VehicleBoardDtos.PhotoPolicy photoPolicy() {
        return vehicleBoardService.photoPolicy();
    }

    @PutMapping("/vehicle-events/photo-policy")
    public VehicleBoardDtos.PhotoPolicy savePhotoPolicy(@RequestBody VehicleBoardDtos.PhotoPolicy body) {
        return vehicleBoardService.savePhotoPolicy(body != null && body.departPhotoRequired());
    }

    @GetMapping("/vehicle-events/report")
    public VehicleBoardDtos.Report reportList(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String officeCode
    ) {
        return vehicleBoardService.reportList(from, to, officeCode);
    }

    @GetMapping("/vehicle-events/{id}/photo")
    public VehicleBoardDtos.EventPhoto eventPhoto(@PathVariable Long id) {
        return vehicleBoardService.eventPhoto(id);
    }

    /** Ghi: NV có quyền Lên hàng hoặc Xuống hàng (StaffWriteGuardFilter). Báo lại cùng chuyến trả về giờ đã ghi. */
    @PostMapping("/vehicle-events")
    public VehicleBoardDtos.Item report(@RequestBody VehicleBoardDtos.ReportRequest body) {
        return vehicleBoardService.report(body);
    }
}
