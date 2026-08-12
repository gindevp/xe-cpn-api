package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.ShipmentOrderQueryService;
import com.mycompany.myapp.service.ShipmentOrderService;
import com.mycompany.myapp.service.criteria.ShipmentOrderCriteria;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.ShipmentOrder}.
 */
@RestController
@RequestMapping("/api/shipment-orders")
public class ShipmentOrderResource {

    private static final Logger LOG = LoggerFactory.getLogger(ShipmentOrderResource.class);

    private static final String ENTITY_NAME = "shipmentOrder";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ShipmentOrderService shipmentOrderService;

    private final ShipmentOrderRepository shipmentOrderRepository;

    private final ShipmentOrderQueryService shipmentOrderQueryService;

    public ShipmentOrderResource(
        ShipmentOrderService shipmentOrderService,
        ShipmentOrderRepository shipmentOrderRepository,
        ShipmentOrderQueryService shipmentOrderQueryService
    ) {
        this.shipmentOrderService = shipmentOrderService;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.shipmentOrderQueryService = shipmentOrderQueryService;
    }

    /**
     * {@code POST  /shipment-orders} : Create a new shipmentOrder.
     *
     * @param shipmentOrderDTO the shipmentOrderDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new shipmentOrderDTO, or with status {@code 400 (Bad Request)} if the shipmentOrder has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ShipmentOrderDTO> createShipmentOrder(@Valid @RequestBody ShipmentOrderDTO shipmentOrderDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ShipmentOrder : {}", shipmentOrderDTO);
        if (shipmentOrderDTO.getId() != null) {
            throw new BadRequestAlertException("A new shipmentOrder cannot already have an ID", ENTITY_NAME, "idexists");
        }
        shipmentOrderDTO = shipmentOrderService.save(shipmentOrderDTO);
        return ResponseEntity.created(new URI("/api/shipment-orders/" + shipmentOrderDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, shipmentOrderDTO.getId().toString()))
            .body(shipmentOrderDTO);
    }

    /**
     * {@code PUT  /shipment-orders/:id} : Updates an existing shipmentOrder.
     *
     * @param id the id of the shipmentOrderDTO to save.
     * @param shipmentOrderDTO the shipmentOrderDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated shipmentOrderDTO,
     * or with status {@code 400 (Bad Request)} if the shipmentOrderDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the shipmentOrderDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShipmentOrderDTO> updateShipmentOrder(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ShipmentOrderDTO shipmentOrderDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ShipmentOrder : {}, {}", id, shipmentOrderDTO);
        if (shipmentOrderDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, shipmentOrderDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!shipmentOrderRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        shipmentOrderDTO = shipmentOrderService.update(shipmentOrderDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, shipmentOrderDTO.getId().toString()))
            .body(shipmentOrderDTO);
    }

    /**
     * {@code PATCH  /shipment-orders/:id} : Partial updates given fields of an existing shipmentOrder, field will ignore if it is null
     *
     * @param id the id of the shipmentOrderDTO to save.
     * @param shipmentOrderDTO the shipmentOrderDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated shipmentOrderDTO,
     * or with status {@code 400 (Bad Request)} if the shipmentOrderDTO is not valid,
     * or with status {@code 404 (Not Found)} if the shipmentOrderDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the shipmentOrderDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ShipmentOrderDTO> partialUpdateShipmentOrder(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ShipmentOrderDTO shipmentOrderDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ShipmentOrder partially : {}, {}", id, shipmentOrderDTO);
        if (shipmentOrderDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, shipmentOrderDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!shipmentOrderRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ShipmentOrderDTO> result = shipmentOrderService.partialUpdate(shipmentOrderDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, shipmentOrderDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /shipment-orders} : get all the shipmentOrders.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of shipmentOrders in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ShipmentOrderDTO>> getAllShipmentOrders(
        ShipmentOrderCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ShipmentOrders by criteria: {}", criteria);

        Page<ShipmentOrderDTO> page = shipmentOrderQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /shipment-orders/count} : count all the shipmentOrders.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countShipmentOrders(ShipmentOrderCriteria criteria) {
        LOG.debug("REST request to count ShipmentOrders by criteria: {}", criteria);
        return ResponseEntity.ok().body(shipmentOrderQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /shipment-orders/:id} : get the "id" shipmentOrder.
     *
     * @param id the id of the shipmentOrderDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the shipmentOrderDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentOrderDTO> getShipmentOrder(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ShipmentOrder : {}", id);
        Optional<ShipmentOrderDTO> shipmentOrderDTO = shipmentOrderService.findOne(id);
        return ResponseUtil.wrapOrNotFound(shipmentOrderDTO);
    }

    /**
     * {@code DELETE  /shipment-orders/:id} : delete the "id" shipmentOrder.
     *
     * @param id the id of the shipmentOrderDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipmentOrder(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete ShipmentOrder : {}", id);
        shipmentOrderService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
