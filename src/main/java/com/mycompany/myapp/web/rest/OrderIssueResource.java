package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.service.OrderIssueQueryService;
import com.mycompany.myapp.service.OrderIssueService;
import com.mycompany.myapp.service.criteria.OrderIssueCriteria;
import com.mycompany.myapp.service.dto.OrderIssueDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.OrderIssue}.
 */
@RestController
@RequestMapping("/api/order-issues")
public class OrderIssueResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderIssueResource.class);

    private static final String ENTITY_NAME = "orderIssue";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderIssueService orderIssueService;

    private final OrderIssueRepository orderIssueRepository;

    private final OrderIssueQueryService orderIssueQueryService;

    public OrderIssueResource(
        OrderIssueService orderIssueService,
        OrderIssueRepository orderIssueRepository,
        OrderIssueQueryService orderIssueQueryService
    ) {
        this.orderIssueService = orderIssueService;
        this.orderIssueRepository = orderIssueRepository;
        this.orderIssueQueryService = orderIssueQueryService;
    }

    /**
     * {@code POST  /order-issues} : Create a new orderIssue.
     *
     * @param orderIssueDTO the orderIssueDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new orderIssueDTO, or with status {@code 400 (Bad Request)} if the orderIssue has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OrderIssueDTO> createOrderIssue(@Valid @RequestBody OrderIssueDTO orderIssueDTO) throws URISyntaxException {
        LOG.debug("REST request to save OrderIssue : {}", orderIssueDTO);
        if (orderIssueDTO.getId() != null) {
            throw new BadRequestAlertException("A new orderIssue cannot already have an ID", ENTITY_NAME, "idexists");
        }
        orderIssueDTO = orderIssueService.save(orderIssueDTO);
        return ResponseEntity.created(new URI("/api/order-issues/" + orderIssueDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, orderIssueDTO.getId().toString()))
            .body(orderIssueDTO);
    }

    /**
     * {@code PUT  /order-issues/:id} : Updates an existing orderIssue.
     *
     * @param id the id of the orderIssueDTO to save.
     * @param orderIssueDTO the orderIssueDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderIssueDTO,
     * or with status {@code 400 (Bad Request)} if the orderIssueDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the orderIssueDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderIssueDTO> updateOrderIssue(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OrderIssueDTO orderIssueDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OrderIssue : {}, {}", id, orderIssueDTO);
        if (orderIssueDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderIssueDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderIssueRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        orderIssueDTO = orderIssueService.update(orderIssueDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderIssueDTO.getId().toString()))
            .body(orderIssueDTO);
    }

    /**
     * {@code PATCH  /order-issues/:id} : Partial updates given fields of an existing orderIssue, field will ignore if it is null
     *
     * @param id the id of the orderIssueDTO to save.
     * @param orderIssueDTO the orderIssueDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderIssueDTO,
     * or with status {@code 400 (Bad Request)} if the orderIssueDTO is not valid,
     * or with status {@code 404 (Not Found)} if the orderIssueDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the orderIssueDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrderIssueDTO> partialUpdateOrderIssue(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OrderIssueDTO orderIssueDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OrderIssue partially : {}, {}", id, orderIssueDTO);
        if (orderIssueDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderIssueDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderIssueRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OrderIssueDTO> result = orderIssueService.partialUpdate(orderIssueDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderIssueDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /order-issues} : get all the orderIssues.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderIssues in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OrderIssueDTO>> getAllOrderIssues(
        OrderIssueCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get OrderIssues by criteria: {}", criteria);

        Page<OrderIssueDTO> page = orderIssueQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /order-issues/count} : count all the orderIssues.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countOrderIssues(OrderIssueCriteria criteria) {
        LOG.debug("REST request to count OrderIssues by criteria: {}", criteria);
        return ResponseEntity.ok().body(orderIssueQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /order-issues/:id} : get the "id" orderIssue.
     *
     * @param id the id of the orderIssueDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderIssueDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderIssueDTO> getOrderIssue(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderIssue : {}", id);
        Optional<OrderIssueDTO> orderIssueDTO = orderIssueService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderIssueDTO);
    }

    /**
     * {@code DELETE  /order-issues/:id} : delete the "id" orderIssue.
     *
     * @param id the id of the orderIssueDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderIssue(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OrderIssue : {}", id);
        orderIssueService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
