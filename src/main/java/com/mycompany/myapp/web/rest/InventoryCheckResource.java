package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.dto.inventory.CreateInventoryCheckRequest;
import com.mycompany.myapp.service.dto.inventory.InventoryCheckDTO;
import com.mycompany.myapp.service.inventory.InventoryCheckService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory-checks")
public class InventoryCheckResource {

    private final InventoryCheckService inventoryCheckService;

    public InventoryCheckResource(InventoryCheckService inventoryCheckService) {
        this.inventoryCheckService = inventoryCheckService;
    }

    @GetMapping("")
    public List<InventoryCheckDTO> list(@RequestParam(required = false) String officeCode) {
        return inventoryCheckService.list(officeCode);
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
}
