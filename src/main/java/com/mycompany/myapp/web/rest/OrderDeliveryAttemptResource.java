package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.service.OrderDeliveryAttemptService;
import com.mycompany.myapp.service.dto.OrderDeliveryAttemptDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.OrderDeliveryAttempt}.
 */
@RestController
@RequestMapping("/api/order-delivery-attempts")
public class OrderDeliveryAttemptResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderDeliveryAttemptResource.class);

    private static final String ENTITY_NAME = "orderDeliveryAttempt";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderDeliveryAttemptService orderDeliveryAttemptService;

    private final OrderDeliveryAttemptRepository orderDeliveryAttemptRepository;

    public OrderDeliveryAttemptResource(
        OrderDeliveryAttemptService orderDeliveryAttemptService,
        OrderDeliveryAttemptRepository orderDeliveryAttemptRepository
    ) {
        this.orderDeliveryAttemptService = orderDeliveryAttemptService;
        this.orderDeliveryAttemptRepository = orderDeliveryAttemptRepository;
    }

    /**
     * {@code POST  /order-delivery-attempts} : Create a new orderDeliveryAttempt.
     *
     * @param orderDeliveryAttemptDTO the orderDeliveryAttemptDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new orderDeliveryAttemptDTO, or with status {@code 400 (Bad Request)} if the orderDeliveryAttempt has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OrderDeliveryAttemptDTO> createOrderDeliveryAttempt(
        @Valid @RequestBody OrderDeliveryAttemptDTO orderDeliveryAttemptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save OrderDeliveryAttempt : {}", orderDeliveryAttemptDTO);
        if (orderDeliveryAttemptDTO.getId() != null) {
            throw new BadRequestAlertException("A new orderDeliveryAttempt cannot already have an ID", ENTITY_NAME, "idexists");
        }
        orderDeliveryAttemptDTO = orderDeliveryAttemptService.save(orderDeliveryAttemptDTO);
        return ResponseEntity.created(new URI("/api/order-delivery-attempts/" + orderDeliveryAttemptDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, orderDeliveryAttemptDTO.getId().toString()))
            .body(orderDeliveryAttemptDTO);
    }

    /**
     * {@code PUT  /order-delivery-attempts/:id} : Updates an existing orderDeliveryAttempt.
     *
     * @param id the id of the orderDeliveryAttemptDTO to save.
     * @param orderDeliveryAttemptDTO the orderDeliveryAttemptDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderDeliveryAttemptDTO,
     * or with status {@code 400 (Bad Request)} if the orderDeliveryAttemptDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the orderDeliveryAttemptDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderDeliveryAttemptDTO> updateOrderDeliveryAttempt(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OrderDeliveryAttemptDTO orderDeliveryAttemptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OrderDeliveryAttempt : {}, {}", id, orderDeliveryAttemptDTO);
        if (orderDeliveryAttemptDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderDeliveryAttemptDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderDeliveryAttemptRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        orderDeliveryAttemptDTO = orderDeliveryAttemptService.update(orderDeliveryAttemptDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderDeliveryAttemptDTO.getId().toString()))
            .body(orderDeliveryAttemptDTO);
    }

    /**
     * {@code PATCH  /order-delivery-attempts/:id} : Partial updates given fields of an existing orderDeliveryAttempt, field will ignore if it is null
     *
     * @param id the id of the orderDeliveryAttemptDTO to save.
     * @param orderDeliveryAttemptDTO the orderDeliveryAttemptDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderDeliveryAttemptDTO,
     * or with status {@code 400 (Bad Request)} if the orderDeliveryAttemptDTO is not valid,
     * or with status {@code 404 (Not Found)} if the orderDeliveryAttemptDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the orderDeliveryAttemptDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrderDeliveryAttemptDTO> partialUpdateOrderDeliveryAttempt(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OrderDeliveryAttemptDTO orderDeliveryAttemptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OrderDeliveryAttempt partially : {}, {}", id, orderDeliveryAttemptDTO);
        if (orderDeliveryAttemptDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderDeliveryAttemptDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderDeliveryAttemptRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OrderDeliveryAttemptDTO> result = orderDeliveryAttemptService.partialUpdate(orderDeliveryAttemptDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderDeliveryAttemptDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /order-delivery-attempts} : get all the orderDeliveryAttempts.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderDeliveryAttempts in body.
     */
    @GetMapping("")
    public List<OrderDeliveryAttemptDTO> getAllOrderDeliveryAttempts(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all OrderDeliveryAttempts");
        return orderDeliveryAttemptService.findAll();
    }

    /**
     * {@code GET  /order-delivery-attempts/:id} : get the "id" orderDeliveryAttempt.
     *
     * @param id the id of the orderDeliveryAttemptDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderDeliveryAttemptDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDeliveryAttemptDTO> getOrderDeliveryAttempt(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderDeliveryAttempt : {}", id);
        Optional<OrderDeliveryAttemptDTO> orderDeliveryAttemptDTO = orderDeliveryAttemptService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderDeliveryAttemptDTO);
    }

    /**
     * {@code DELETE  /order-delivery-attempts/:id} : delete the "id" orderDeliveryAttempt.
     *
     * @param id the id of the orderDeliveryAttemptDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderDeliveryAttempt(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OrderDeliveryAttempt : {}", id);
        orderDeliveryAttemptService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
