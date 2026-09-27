package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.dto.vehicle.VehicleBoardDtos;
import com.mycompany.myapp.service.vehicle.VehicleBoardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicle-events")
public class VehicleEventResource {

    private final VehicleBoardService vehicleBoardService;

    public VehicleEventResource(VehicleBoardService vehicleBoardService) {
        this.vehicleBoardService = vehicleBoardService;
    }

    @GetMapping("/board")
    public VehicleBoardDtos.Board board() {
        return vehicleBoardService.board();
    }

    /** Ghi: NV có quyền Lên hàng hoặc Xuống hàng (StaffWriteGuardFilter). Báo lại cùng chuyến trả về giờ đã ghi. */
    @PostMapping
    public VehicleBoardDtos.Item report(@RequestBody VehicleBoardDtos.ReportRequest body) {
        return vehicleBoardService.report(body);
    }
}
