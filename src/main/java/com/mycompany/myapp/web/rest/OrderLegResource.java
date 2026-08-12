package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.service.OrderLegService;
import com.mycompany.myapp.service.dto.OrderLegDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.OrderLeg}.
 */
@RestController
@RequestMapping("/api/order-legs")
public class OrderLegResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderLegResource.class);

    private static final String ENTITY_NAME = "orderLeg";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderLegService orderLegService;

    private final OrderLegRepository orderLegRepository;

    public OrderLegResource(OrderLegService orderLegService, OrderLegRepository orderLegRepository) {
        this.orderLegService = orderLegService;
        this.orderLegRepository = orderLegRepository;
    }

    /**
     * {@code POST  /order-legs} : Create a new orderLeg.
     *
     * @param orderLegDTO the orderLegDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new orderLegDTO, or with status {@code 400 (Bad Request)} if the orderLeg has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OrderLegDTO> createOrderLeg(@Valid @RequestBody OrderLegDTO orderLegDTO) throws URISyntaxException {
        LOG.debug("REST request to save OrderLeg : {}", orderLegDTO);
        if (orderLegDTO.getId() != null) {
            throw new BadRequestAlertException("A new orderLeg cannot already have an ID", ENTITY_NAME, "idexists");
        }
        orderLegDTO = orderLegService.save(orderLegDTO);
        return ResponseEntity.created(new URI("/api/order-legs/" + orderLegDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, orderLegDTO.getId().toString()))
            .body(orderLegDTO);
    }

    /**
     * {@code PUT  /order-legs/:id} : Updates an existing orderLeg.
     *
     * @param id the id of the orderLegDTO to save.
     * @param orderLegDTO the orderLegDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderLegDTO,
     * or with status {@code 400 (Bad Request)} if the orderLegDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the orderLegDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderLegDTO> updateOrderLeg(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OrderLegDTO orderLegDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OrderLeg : {}, {}", id, orderLegDTO);
        if (orderLegDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderLegDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderLegRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        orderLegDTO = orderLegService.update(orderLegDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderLegDTO.getId().toString()))
            .body(orderLegDTO);
    }

    /**
     * {@code PATCH  /order-legs/:id} : Partial updates given fields of an existing orderLeg, field will ignore if it is null
     *
     * @param id the id of the orderLegDTO to save.
     * @param orderLegDTO the orderLegDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderLegDTO,
     * or with status {@code 400 (Bad Request)} if the orderLegDTO is not valid,
     * or with status {@code 404 (Not Found)} if the orderLegDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the orderLegDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrderLegDTO> partialUpdateOrderLeg(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OrderLegDTO orderLegDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OrderLeg partially : {}, {}", id, orderLegDTO);
        if (orderLegDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderLegDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderLegRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OrderLegDTO> result = orderLegService.partialUpdate(orderLegDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderLegDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /order-legs} : get all the orderLegs.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderLegs in body.
     */
    @GetMapping("")
    public List<OrderLegDTO> getAllOrderLegs(@RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        LOG.debug("REST request to get all OrderLegs");
        return orderLegService.findAll();
    }

    /**
     * {@code GET  /order-legs/:id} : get the "id" orderLeg.
     *
     * @param id the id of the orderLegDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderLegDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderLegDTO> getOrderLeg(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderLeg : {}", id);
        Optional<OrderLegDTO> orderLegDTO = orderLegService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderLegDTO);
    }

    /**
     * {@code DELETE  /order-legs/:id} : delete the "id" orderLeg.
     *
     * @param id the id of the orderLegDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderLeg(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OrderLeg : {}", id);
        orderLegService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
