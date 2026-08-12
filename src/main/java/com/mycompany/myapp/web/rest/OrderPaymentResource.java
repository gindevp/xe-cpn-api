package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.service.OrderPaymentService;
import com.mycompany.myapp.service.dto.OrderPaymentDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.OrderPayment}.
 */
@RestController
@RequestMapping("/api/order-payments")
public class OrderPaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderPaymentResource.class);

    private static final String ENTITY_NAME = "orderPayment";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderPaymentService orderPaymentService;

    private final OrderPaymentRepository orderPaymentRepository;

    public OrderPaymentResource(OrderPaymentService orderPaymentService, OrderPaymentRepository orderPaymentRepository) {
        this.orderPaymentService = orderPaymentService;
        this.orderPaymentRepository = orderPaymentRepository;
    }

    /**
     * {@code POST  /order-payments} : Create a new orderPayment.
     *
     * @param orderPaymentDTO the orderPaymentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new orderPaymentDTO, or with status {@code 400 (Bad Request)} if the orderPayment has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OrderPaymentDTO> createOrderPayment(@Valid @RequestBody OrderPaymentDTO orderPaymentDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save OrderPayment : {}", orderPaymentDTO);
        if (orderPaymentDTO.getId() != null) {
            throw new BadRequestAlertException("A new orderPayment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        orderPaymentDTO = orderPaymentService.save(orderPaymentDTO);
        return ResponseEntity.created(new URI("/api/order-payments/" + orderPaymentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, orderPaymentDTO.getId().toString()))
            .body(orderPaymentDTO);
    }

    /**
     * {@code PUT  /order-payments/:id} : Updates an existing orderPayment.
     *
     * @param id the id of the orderPaymentDTO to save.
     * @param orderPaymentDTO the orderPaymentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderPaymentDTO,
     * or with status {@code 400 (Bad Request)} if the orderPaymentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the orderPaymentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderPaymentDTO> updateOrderPayment(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OrderPaymentDTO orderPaymentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OrderPayment : {}, {}", id, orderPaymentDTO);
        if (orderPaymentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderPaymentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderPaymentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        orderPaymentDTO = orderPaymentService.update(orderPaymentDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderPaymentDTO.getId().toString()))
            .body(orderPaymentDTO);
    }

    /**
     * {@code PATCH  /order-payments/:id} : Partial updates given fields of an existing orderPayment, field will ignore if it is null
     *
     * @param id the id of the orderPaymentDTO to save.
     * @param orderPaymentDTO the orderPaymentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderPaymentDTO,
     * or with status {@code 400 (Bad Request)} if the orderPaymentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the orderPaymentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the orderPaymentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrderPaymentDTO> partialUpdateOrderPayment(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OrderPaymentDTO orderPaymentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OrderPayment partially : {}, {}", id, orderPaymentDTO);
        if (orderPaymentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderPaymentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderPaymentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OrderPaymentDTO> result = orderPaymentService.partialUpdate(orderPaymentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderPaymentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /order-payments} : get all the orderPayments.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderPayments in body.
     */
    @GetMapping("")
    public List<OrderPaymentDTO> getAllOrderPayments(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all OrderPayments");
        return orderPaymentService.findAll();
    }

    /**
     * {@code GET  /order-payments/:id} : get the "id" orderPayment.
     *
     * @param id the id of the orderPaymentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderPaymentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderPaymentDTO> getOrderPayment(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderPayment : {}", id);
        Optional<OrderPaymentDTO> orderPaymentDTO = orderPaymentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderPaymentDTO);
    }

    /**
     * {@code DELETE  /order-payments/:id} : delete the "id" orderPayment.
     *
     * @param id the id of the orderPaymentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderPayment(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OrderPayment : {}", id);
        orderPaymentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
