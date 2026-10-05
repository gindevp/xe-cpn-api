package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.dto.inventory.CreateInventoryCheckRequest;
import com.mycompany.myapp.service.dto.inventory.InventoryCheckDTO;
import com.mycompany.myapp.service.inventory.InventoryCheckService;
import com.mycompany.myapp.service.inventory.InventoryCheckSessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory-checks")
public class InventoryCheckResource {

    private final InventoryCheckService inventoryCheckService;
    private final InventoryCheckSessionService sessionService;

    public InventoryCheckResource(InventoryCheckService inventoryCheckService, InventoryCheckSessionService sessionService) {
        this.inventoryCheckService = inventoryCheckService;
        this.sessionService = sessionService;
    }

    @GetMapping("")
    public List<InventoryCheckDTO> list(
        @RequestParam(required = false) String officeCode,
        @RequestParam(defaultValue = "false") boolean includeAbandoned
    ) {
        return inventoryCheckService.list(officeCode, includeAbandoned);
    }

    public record OpenRequest(String officeCode) {}

    @GetMapping("/open")
    public ResponseEntity<InventoryCheckDTO> findOpen(@RequestParam(required = false) String officeCode) {
        return sessionService.findOpen(officeCode).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/open")
    public InventoryCheckDTO openOrJoin(@RequestBody(required = false) OpenRequest request) {
        String office = request != null ? request.officeCode() : null;
        try {
            return sessionService.openOrJoin(office);
        } catch (DataIntegrityViolationException e) {
            return sessionService.openOrJoin(office);
        }
    }

    @PostMapping("/{id}/scans")
    public InventoryCheckSessionService.ScanResult addScan(
        @PathVariable Long id,
        @RequestBody InventoryCheckSessionService.ScanRequest request
    ) {
        try {
            return sessionService.addScan(id, request);
        } catch (DataIntegrityViolationException e) {
            return sessionService.existingScan(id, request);
        }
    }

    @GetMapping("/{id}/scans")
    public InventoryCheckSessionService.ScansResponse scans(@PathVariable Long id, @RequestParam(required = false) Long sinceId) {
        return sessionService.scans(id, sinceId);
    }

    @PostMapping("/{id}/complete")
    public InventoryCheckDTO complete(@PathVariable Long id, @RequestBody InventoryCheckSessionService.CompleteRequest request) {
        return sessionService.complete(id, request);
    }

    @PostMapping("/{id}/reopen")
    public InventoryCheckDTO reopen(@PathVariable Long id) {
        return sessionService.reopen(id);
    }

    @GetMapping("/{id}")
    public InventoryCheckDTO get(@PathVariable Long id) {
        return inventoryCheckService.get(id);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<InventoryCheckDTO> create(@Valid @RequestBody CreateInventoryCheckRequest request) throws URISyntaxException {
        InventoryCheckDTO created = inventoryCheckService.create(request);
        return ResponseEntity.created(new URI("/api/inventory-checks/" + created.getId())).body(created);
    }

    @PostMapping("/photos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadPhoto(@RequestBody InventoryCheckService.UploadPhotoRequest request) {
        inventoryCheckService.uploadPhoto(request);
    }

    @GetMapping("/{id}/photo-orders")
    public List<InventoryCheckService.PhotoOrderCount> photoOrders(@PathVariable Long id) {
        return inventoryCheckService.photoOrders(id);
    }

    @GetMapping("/{id}/thumbnails")
    public List<InventoryCheckService.ThumbnailDTO> thumbnails(@PathVariable Long id, @RequestParam List<String> codes) {
        return inventoryCheckService.thumbnails(id, codes);
    }

    @GetMapping("/{id}/photos")
    public List<InventoryCheckService.PhotoDTO> photos(@PathVariable Long id, @RequestParam String orderCode) {
        return inventoryCheckService.photos(id, orderCode);
    }
}
