package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.service.OrderEventService;
import com.mycompany.myapp.service.dto.OrderEventDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.OrderEvent}.
 */
@RestController
@RequestMapping("/api/order-events")
public class OrderEventResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderEventResource.class);

    private static final String ENTITY_NAME = "orderEvent";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderEventService orderEventService;

    private final OrderEventRepository orderEventRepository;

    public OrderEventResource(OrderEventService orderEventService, OrderEventRepository orderEventRepository) {
        this.orderEventService = orderEventService;
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * {@code POST  /order-events} : Create a new orderEvent.
     *
     * @param orderEventDTO the orderEventDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new orderEventDTO, or with status {@code 400 (Bad Request)} if the orderEvent has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OrderEventDTO> createOrderEvent(@Valid @RequestBody OrderEventDTO orderEventDTO) throws URISyntaxException {
        LOG.debug("REST request to save OrderEvent : {}", orderEventDTO);
        if (orderEventDTO.getId() != null) {
            throw new BadRequestAlertException("A new orderEvent cannot already have an ID", ENTITY_NAME, "idexists");
        }
        orderEventDTO = orderEventService.save(orderEventDTO);
        return ResponseEntity.created(new URI("/api/order-events/" + orderEventDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, orderEventDTO.getId().toString()))
            .body(orderEventDTO);
    }

    /**
     * {@code PUT  /order-events/:id} : Updates an existing orderEvent.
     *
     * @param id the id of the orderEventDTO to save.
     * @param orderEventDTO the orderEventDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderEventDTO,
     * or with status {@code 400 (Bad Request)} if the orderEventDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the orderEventDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderEventDTO> updateOrderEvent(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OrderEventDTO orderEventDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OrderEvent : {}, {}", id, orderEventDTO);
        if (orderEventDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderEventDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderEventRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        orderEventDTO = orderEventService.update(orderEventDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderEventDTO.getId().toString()))
            .body(orderEventDTO);
    }

    /**
     * {@code PATCH  /order-events/:id} : Partial updates given fields of an existing orderEvent, field will ignore if it is null
     *
     * @param id the id of the orderEventDTO to save.
     * @param orderEventDTO the orderEventDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderEventDTO,
     * or with status {@code 400 (Bad Request)} if the orderEventDTO is not valid,
     * or with status {@code 404 (Not Found)} if the orderEventDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the orderEventDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrderEventDTO> partialUpdateOrderEvent(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OrderEventDTO orderEventDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OrderEvent partially : {}, {}", id, orderEventDTO);
        if (orderEventDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderEventDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderEventRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OrderEventDTO> result = orderEventService.partialUpdate(orderEventDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderEventDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /order-events} : get all the orderEvents.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderEvents in body.
     */
    @GetMapping("")
    public List<OrderEventDTO> getAllOrderEvents(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all OrderEvents");
        return orderEventService.findAll();
    }

    /**
     * {@code GET  /order-events/:id} : get the "id" orderEvent.
     *
     * @param id the id of the orderEventDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderEventDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderEventDTO> getOrderEvent(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderEvent : {}", id);
        Optional<OrderEventDTO> orderEventDTO = orderEventService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderEventDTO);
    }

    /**
     * {@code DELETE  /order-events/:id} : delete the "id" orderEvent.
     *
     * @param id the id of the orderEventDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderEvent(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OrderEvent : {}", id);
        orderEventService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
