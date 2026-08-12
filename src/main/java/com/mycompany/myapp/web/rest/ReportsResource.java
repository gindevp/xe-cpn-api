package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.service.finance.FinanceFacadeService;
import com.mycompany.myapp.service.report.InventoryHourlyReportService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportsResource {

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final TripRepository tripRepository;
    private final FinanceFacadeService financeFacadeService;
    private final InventoryHourlyReportService inventoryHourlyReportService;

    public ReportsResource(
        ShipmentOrderRepository shipmentOrderRepository,
        TripRepository tripRepository,
        FinanceFacadeService financeFacadeService,
        InventoryHourlyReportService inventoryHourlyReportService
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.tripRepository = tripRepository;
        this.financeFacadeService = financeFacadeService;
        this.inventoryHourlyReportService = inventoryHourlyReportService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(
        @RequestParam(required = false) String officeCode,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Map<String, Object> out = new HashMap<>();
        out.put("ordersTotal", shipmentOrderRepository.count());
        out.put("ordersDelivered", shipmentOrderRepository.count((root, q, cb) -> cb.equal(root.get("status"), OrderStatus.DELIVERED)));
        out.put("ordersInTransit", shipmentOrderRepository.count((root, q, cb) -> cb.equal(root.get("status"), OrderStatus.IN_TRANSIT)));
        out.put("ordersAtDest", shipmentOrderRepository.count((root, q, cb) -> cb.equal(root.get("status"), OrderStatus.AT_DEST)));
        out.put("ordersFailed", shipmentOrderRepository.count((root, q, cb) -> cb.equal(root.get("status"), OrderStatus.FAILED_DELIVERY)));
        out.put("ordersReturning", shipmentOrderRepository.count((root, q, cb) -> cb.equal(root.get("status"), OrderStatus.RETURNING)));
        out.put("tripsTotal", tripRepository.count());
        if (date != null || officeCode != null) {
            out.put("collections", financeFacadeService.collectionsReport(officeCode, date));
        }
        return out;
    }

    @GetMapping("/collections")
    public Map<String, Object> collections(
        @RequestParam(required = false) String officeCode,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return financeFacadeService.collectionsReport(officeCode, date);
    }

    @GetMapping("/inventory")
    public Map<String, Object> inventory(@RequestParam(required = false) String officeCode) {
        return inventoryHourlyReportService.inventory(officeCode);
    }

    @GetMapping("/hourly")
    public Map<String, Object> hourly(@RequestParam(required = false) String officeCode) {
        return inventoryHourlyReportService.hourly(officeCode);
    }
}
