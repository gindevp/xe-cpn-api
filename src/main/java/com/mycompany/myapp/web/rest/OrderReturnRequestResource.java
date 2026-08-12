package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.OrderReturnRequestRepository;
import com.mycompany.myapp.service.OrderReturnRequestService;
import com.mycompany.myapp.service.dto.OrderReturnRequestDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.OrderReturnRequest}.
 */
@RestController
@RequestMapping("/api/order-return-requests")
public class OrderReturnRequestResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderReturnRequestResource.class);

    private static final String ENTITY_NAME = "orderReturnRequest";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderReturnRequestService orderReturnRequestService;

    private final OrderReturnRequestRepository orderReturnRequestRepository;

    public OrderReturnRequestResource(
        OrderReturnRequestService orderReturnRequestService,
        OrderReturnRequestRepository orderReturnRequestRepository
    ) {
        this.orderReturnRequestService = orderReturnRequestService;
        this.orderReturnRequestRepository = orderReturnRequestRepository;
    }

    /**
     * {@code POST  /order-return-requests} : Create a new orderReturnRequest.
     *
     * @param orderReturnRequestDTO the orderReturnRequestDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new orderReturnRequestDTO, or with status {@code 400 (Bad Request)} if the orderReturnRequest has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OrderReturnRequestDTO> createOrderReturnRequest(@Valid @RequestBody OrderReturnRequestDTO orderReturnRequestDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save OrderReturnRequest : {}", orderReturnRequestDTO);
        if (orderReturnRequestDTO.getId() != null) {
            throw new BadRequestAlertException("A new orderReturnRequest cannot already have an ID", ENTITY_NAME, "idexists");
        }
        orderReturnRequestDTO = orderReturnRequestService.save(orderReturnRequestDTO);
        return ResponseEntity.created(new URI("/api/order-return-requests/" + orderReturnRequestDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, orderReturnRequestDTO.getId().toString()))
            .body(orderReturnRequestDTO);
    }

    /**
     * {@code PUT  /order-return-requests/:id} : Updates an existing orderReturnRequest.
     *
     * @param id the id of the orderReturnRequestDTO to save.
     * @param orderReturnRequestDTO the orderReturnRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderReturnRequestDTO,
     * or with status {@code 400 (Bad Request)} if the orderReturnRequestDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the orderReturnRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderReturnRequestDTO> updateOrderReturnRequest(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OrderReturnRequestDTO orderReturnRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OrderReturnRequest : {}, {}", id, orderReturnRequestDTO);
        if (orderReturnRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderReturnRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderReturnRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        orderReturnRequestDTO = orderReturnRequestService.update(orderReturnRequestDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderReturnRequestDTO.getId().toString()))
            .body(orderReturnRequestDTO);
    }

    /**
     * {@code PATCH  /order-return-requests/:id} : Partial updates given fields of an existing orderReturnRequest, field will ignore if it is null
     *
     * @param id the id of the orderReturnRequestDTO to save.
     * @param orderReturnRequestDTO the orderReturnRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderReturnRequestDTO,
     * or with status {@code 400 (Bad Request)} if the orderReturnRequestDTO is not valid,
     * or with status {@code 404 (Not Found)} if the orderReturnRequestDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the orderReturnRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrderReturnRequestDTO> partialUpdateOrderReturnRequest(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OrderReturnRequestDTO orderReturnRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OrderReturnRequest partially : {}, {}", id, orderReturnRequestDTO);
        if (orderReturnRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderReturnRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderReturnRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OrderReturnRequestDTO> result = orderReturnRequestService.partialUpdate(orderReturnRequestDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderReturnRequestDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /order-return-requests} : get all the orderReturnRequests.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderReturnRequests in body.
     */
    @GetMapping("")
    public List<OrderReturnRequestDTO> getAllOrderReturnRequests(@RequestParam(name = "filter", required = false) String filter) {
        if ("order-is-null".equals(filter)) {
            LOG.debug("REST request to get all OrderReturnRequests where order is null");
            return orderReturnRequestService.findAllWhereOrderIsNull();
        }
        LOG.debug("REST request to get all OrderReturnRequests");
        return orderReturnRequestService.findAll();
    }

    /**
     * {@code GET  /order-return-requests/:id} : get the "id" orderReturnRequest.
     *
     * @param id the id of the orderReturnRequestDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderReturnRequestDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderReturnRequestDTO> getOrderReturnRequest(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderReturnRequest : {}", id);
        Optional<OrderReturnRequestDTO> orderReturnRequestDTO = orderReturnRequestService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderReturnRequestDTO);
    }

    /**
     * {@code DELETE  /order-return-requests/:id} : delete the "id" orderReturnRequest.
     *
     * @param id the id of the orderReturnRequestDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderReturnRequest(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OrderReturnRequest : {}", id);
        orderReturnRequestService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
