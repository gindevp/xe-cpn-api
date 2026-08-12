package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.OrderFareAdjustmentRequestRepository;
import com.mycompany.myapp.service.OrderFareAdjustmentRequestService;
import com.mycompany.myapp.service.dto.OrderFareAdjustmentRequestDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.OrderFareAdjustmentRequest}.
 */
@RestController
@RequestMapping("/api/order-fare-adjustment-requests")
public class OrderFareAdjustmentRequestResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderFareAdjustmentRequestResource.class);

    private static final String ENTITY_NAME = "orderFareAdjustmentRequest";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderFareAdjustmentRequestService orderFareAdjustmentRequestService;

    private final OrderFareAdjustmentRequestRepository orderFareAdjustmentRequestRepository;

    public OrderFareAdjustmentRequestResource(
        OrderFareAdjustmentRequestService orderFareAdjustmentRequestService,
        OrderFareAdjustmentRequestRepository orderFareAdjustmentRequestRepository
    ) {
        this.orderFareAdjustmentRequestService = orderFareAdjustmentRequestService;
        this.orderFareAdjustmentRequestRepository = orderFareAdjustmentRequestRepository;
    }

    /**
     * {@code POST  /order-fare-adjustment-requests} : Create a new orderFareAdjustmentRequest.
     *
     * @param orderFareAdjustmentRequestDTO the orderFareAdjustmentRequestDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new orderFareAdjustmentRequestDTO, or with status {@code 400 (Bad Request)} if the orderFareAdjustmentRequest has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OrderFareAdjustmentRequestDTO> createOrderFareAdjustmentRequest(
        @Valid @RequestBody OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save OrderFareAdjustmentRequest : {}", orderFareAdjustmentRequestDTO);
        if (orderFareAdjustmentRequestDTO.getId() != null) {
            throw new BadRequestAlertException("A new orderFareAdjustmentRequest cannot already have an ID", ENTITY_NAME, "idexists");
        }
        orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestService.save(orderFareAdjustmentRequestDTO);
        return ResponseEntity.created(new URI("/api/order-fare-adjustment-requests/" + orderFareAdjustmentRequestDTO.getId()))
            .headers(
                HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, orderFareAdjustmentRequestDTO.getId().toString())
            )
            .body(orderFareAdjustmentRequestDTO);
    }

    /**
     * {@code PUT  /order-fare-adjustment-requests/:id} : Updates an existing orderFareAdjustmentRequest.
     *
     * @param id the id of the orderFareAdjustmentRequestDTO to save.
     * @param orderFareAdjustmentRequestDTO the orderFareAdjustmentRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderFareAdjustmentRequestDTO,
     * or with status {@code 400 (Bad Request)} if the orderFareAdjustmentRequestDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the orderFareAdjustmentRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderFareAdjustmentRequestDTO> updateOrderFareAdjustmentRequest(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OrderFareAdjustmentRequest : {}, {}", id, orderFareAdjustmentRequestDTO);
        if (orderFareAdjustmentRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderFareAdjustmentRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderFareAdjustmentRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestService.update(orderFareAdjustmentRequestDTO);
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderFareAdjustmentRequestDTO.getId().toString())
            )
            .body(orderFareAdjustmentRequestDTO);
    }

    /**
     * {@code PATCH  /order-fare-adjustment-requests/:id} : Partial updates given fields of an existing orderFareAdjustmentRequest, field will ignore if it is null
     *
     * @param id the id of the orderFareAdjustmentRequestDTO to save.
     * @param orderFareAdjustmentRequestDTO the orderFareAdjustmentRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderFareAdjustmentRequestDTO,
     * or with status {@code 400 (Bad Request)} if the orderFareAdjustmentRequestDTO is not valid,
     * or with status {@code 404 (Not Found)} if the orderFareAdjustmentRequestDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the orderFareAdjustmentRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrderFareAdjustmentRequestDTO> partialUpdateOrderFareAdjustmentRequest(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OrderFareAdjustmentRequest partially : {}, {}", id, orderFareAdjustmentRequestDTO);
        if (orderFareAdjustmentRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderFareAdjustmentRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderFareAdjustmentRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OrderFareAdjustmentRequestDTO> result = orderFareAdjustmentRequestService.partialUpdate(orderFareAdjustmentRequestDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderFareAdjustmentRequestDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /order-fare-adjustment-requests} : get all the orderFareAdjustmentRequests.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderFareAdjustmentRequests in body.
     */
    @GetMapping("")
    public List<OrderFareAdjustmentRequestDTO> getAllOrderFareAdjustmentRequests(
        @RequestParam(name = "filter", required = false) String filter
    ) {
        if ("order-is-null".equals(filter)) {
            LOG.debug("REST request to get all OrderFareAdjustmentRequests where order is null");
            return orderFareAdjustmentRequestService.findAllWhereOrderIsNull();
        }
        LOG.debug("REST request to get all OrderFareAdjustmentRequests");
        return orderFareAdjustmentRequestService.findAll();
    }

    /**
     * {@code GET  /order-fare-adjustment-requests/:id} : get the "id" orderFareAdjustmentRequest.
     *
     * @param id the id of the orderFareAdjustmentRequestDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderFareAdjustmentRequestDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderFareAdjustmentRequestDTO> getOrderFareAdjustmentRequest(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderFareAdjustmentRequest : {}", id);
        Optional<OrderFareAdjustmentRequestDTO> orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderFareAdjustmentRequestDTO);
    }

    /**
     * {@code DELETE  /order-fare-adjustment-requests/:id} : delete the "id" orderFareAdjustmentRequest.
     *
     * @param id the id of the orderFareAdjustmentRequestDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderFareAdjustmentRequest(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OrderFareAdjustmentRequest : {}", id);
        orderFareAdjustmentRequestService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
