package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.enumeration.TripStatus;
import com.mycompany.myapp.service.dto.trip.AssignOrdersToTripRequest;
import com.mycompany.myapp.service.dto.trip.AvailableTripDTO;
import com.mycompany.myapp.service.dto.trip.CloseTripRequest;
import com.mycompany.myapp.service.dto.trip.CreateTripRequest;
import com.mycompany.myapp.service.dto.trip.HandoverRequest;
import com.mycompany.myapp.service.dto.trip.ScanInRequest;
import com.mycompany.myapp.service.dto.trip.ScanOutRequest;
import com.mycompany.myapp.service.dto.trip.TripSummaryDTO;
import com.mycompany.myapp.service.dto.trip.TripTransitionRequest;
import com.mycompany.myapp.service.dto.trip.TripTransitionResponse;
import com.mycompany.myapp.service.partner.AvailableTripSearchService;
import com.mycompany.myapp.service.trip.TripFacadeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * FE-facing trip facade. Generated CRUD remains at {@code /api/trips} entity resource —
 * this controller uses the same path for business operations; see note in docs/M5.
 */
@RestController
@RequestMapping("/api/trips")
public class TripFacadeResource {

    private static final Logger LOG = LoggerFactory.getLogger(TripFacadeResource.class);

    private final TripFacadeService tripFacadeService;
    private final AvailableTripSearchService availableTripSearchService;

    public TripFacadeResource(TripFacadeService tripFacadeService, AvailableTripSearchService availableTripSearchService) {
        this.tripFacadeService = tripFacadeService;
        this.availableTripSearchService = availableTripSearchService;
    }

    /**
     * Xe khả dụng từ CRM {@code get_list_trips}. Bắt buộc {@code itineraryCode} (mã hoặc tên lộ trình).
     * Cửa sổ giờ luôn now → now+1h (VN); {@code date}/{@code timeSlot} bỏ qua nếu có.
     */
    @GetMapping("/available")
    public List<AvailableTripDTO> available(
        @RequestParam(required = false) String date,
        @RequestParam(required = false) String itineraryCode,
        @RequestParam(required = false) String lfid,
        @RequestParam(required = false) String ltid,
        @RequestParam(required = false) String timeSlot
    ) {
        return availableTripSearchService.search(date, itineraryCode, lfid, ltid, timeSlot);
    }

    @GetMapping("")
    public ResponseEntity<ListPage> list(
        @RequestParam(required = false) TripStatus status,
        @RequestParam(required = false) String officeCode,
        @RequestParam(required = false) String routeCode,
        @RequestParam(required = false) String keyword,
        Pageable pageable
    ) {
        Page<TripSummaryDTO> page = tripFacadeService.list(status, officeCode, routeCode, keyword, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .body(new ListPage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @PostMapping("")
    public ResponseEntity<TripSummaryDTO> create(@Valid @RequestBody CreateTripRequest request) throws URISyntaxException {
        LOG.debug("REST create trip");
        TripSummaryDTO created = tripFacadeService.create(request);
        return ResponseEntity.created(new URI("/api/trips/" + created.getTripCode())).body(created);
    }

    /** Scan-in without trip path (uses order.currentTrip). */
    @PostMapping("/scan-in")
    public TripSummaryDTO scanInGlobal(@Valid @RequestBody ScanInRequest request) {
        return tripFacadeService.scanIn(null, request);
    }

    @PostMapping("/assign-orders")
    public TripSummaryDTO assignOrders(@Valid @RequestBody AssignOrdersToTripRequest request) {
        return tripFacadeService.assignOrders(request);
    }

    @GetMapping("/{tripCode}")
    public TripSummaryDTO get(@PathVariable String tripCode) {
        return tripFacadeService.getByCode(tripCode);
    }

    @PostMapping("/{tripCode}/transition")
    public TripTransitionResponse transition(@PathVariable String tripCode, @Valid @RequestBody TripTransitionRequest request) {
        return tripFacadeService.transition(tripCode, request);
    }

    @PostMapping("/{tripCode}/scan-out")
    public TripSummaryDTO scanOut(@PathVariable String tripCode, @Valid @RequestBody ScanOutRequest request) {
        return tripFacadeService.scanOut(tripCode, request);
    }

    @DeleteMapping("/{tripCode}/scan-out/{orderCode}")
    public TripSummaryDTO removeScanOut(@PathVariable String tripCode, @PathVariable String orderCode) {
        return tripFacadeService.removeScanOut(tripCode, orderCode);
    }

    @PostMapping("/{tripCode}/handover")
    public TripSummaryDTO handover(@PathVariable String tripCode, @Valid @RequestBody HandoverRequest request) {
        return tripFacadeService.handover(tripCode, request);
    }

    @PostMapping("/{tripCode}/scan-in")
    public TripSummaryDTO scanIn(@PathVariable String tripCode, @Valid @RequestBody ScanInRequest request) {
        return tripFacadeService.scanIn(tripCode, request);
    }

    @PostMapping("/{tripCode}/close")
    public TripSummaryDTO close(@PathVariable String tripCode, @RequestBody(required = false) CloseTripRequest request) {
        return tripFacadeService.close(tripCode, request != null ? request : new CloseTripRequest());
    }

    public record ListPage(List<TripSummaryDTO> content, int page, int size, long totalElements) {}
}
