package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.ShipperService;
import com.mycompany.myapp.service.dto.ShipperDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Danh mục shipper nội bộ (ghi: quyền Danh mục — StaffWriteGuardFilter). */
@RestController
@RequestMapping("/api/shippers")
public class ShipperResource {

    private final ShipperService shipperService;

    public ShipperResource(ShipperService shipperService) {
        this.shipperService = shipperService;
    }

    @GetMapping("")
    public List<ShipperDTO> list(
        @RequestParam(value = "officeCode", required = false) String officeCode,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return shipperService.list(officeCode, includeInactive);
    }

    @PostMapping("")
    public ShipperDTO create(@RequestBody ShipperDTO body) {
        return shipperService.create(body);
    }

    @PutMapping("/{id}")
    public ShipperDTO update(@PathVariable("id") Long id, @RequestBody ShipperDTO body) {
        return shipperService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable("id") Long id) {
        shipperService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
