package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.service.OrderPodPhotoService;
import com.mycompany.myapp.service.dto.OrderPodPhotoDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.OrderPodPhoto}.
 */
@RestController
@RequestMapping("/api/order-pod-photos")
public class OrderPodPhotoResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderPodPhotoResource.class);

    private static final String ENTITY_NAME = "orderPodPhoto";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderPodPhotoService orderPodPhotoService;

    private final OrderPodPhotoRepository orderPodPhotoRepository;

    public OrderPodPhotoResource(OrderPodPhotoService orderPodPhotoService, OrderPodPhotoRepository orderPodPhotoRepository) {
        this.orderPodPhotoService = orderPodPhotoService;
        this.orderPodPhotoRepository = orderPodPhotoRepository;
    }

    /**
     * {@code POST  /order-pod-photos} : Create a new orderPodPhoto.
     *
     * @param orderPodPhotoDTO the orderPodPhotoDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new orderPodPhotoDTO, or with status {@code 400 (Bad Request)} if the orderPodPhoto has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OrderPodPhotoDTO> createOrderPodPhoto(@Valid @RequestBody OrderPodPhotoDTO orderPodPhotoDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save OrderPodPhoto : {}", orderPodPhotoDTO);
        if (orderPodPhotoDTO.getId() != null) {
            throw new BadRequestAlertException("A new orderPodPhoto cannot already have an ID", ENTITY_NAME, "idexists");
        }
        orderPodPhotoDTO = orderPodPhotoService.save(orderPodPhotoDTO);
        return ResponseEntity.created(new URI("/api/order-pod-photos/" + orderPodPhotoDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, orderPodPhotoDTO.getId().toString()))
            .body(orderPodPhotoDTO);
    }

    /**
     * {@code PUT  /order-pod-photos/:id} : Updates an existing orderPodPhoto.
     *
     * @param id the id of the orderPodPhotoDTO to save.
     * @param orderPodPhotoDTO the orderPodPhotoDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderPodPhotoDTO,
     * or with status {@code 400 (Bad Request)} if the orderPodPhotoDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the orderPodPhotoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderPodPhotoDTO> updateOrderPodPhoto(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OrderPodPhotoDTO orderPodPhotoDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OrderPodPhoto : {}, {}", id, orderPodPhotoDTO);
        if (orderPodPhotoDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderPodPhotoDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderPodPhotoRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        orderPodPhotoDTO = orderPodPhotoService.update(orderPodPhotoDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderPodPhotoDTO.getId().toString()))
            .body(orderPodPhotoDTO);
    }

    /**
     * {@code PATCH  /order-pod-photos/:id} : Partial updates given fields of an existing orderPodPhoto, field will ignore if it is null
     *
     * @param id the id of the orderPodPhotoDTO to save.
     * @param orderPodPhotoDTO the orderPodPhotoDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated orderPodPhotoDTO,
     * or with status {@code 400 (Bad Request)} if the orderPodPhotoDTO is not valid,
     * or with status {@code 404 (Not Found)} if the orderPodPhotoDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the orderPodPhotoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrderPodPhotoDTO> partialUpdateOrderPodPhoto(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OrderPodPhotoDTO orderPodPhotoDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OrderPodPhoto partially : {}, {}", id, orderPodPhotoDTO);
        if (orderPodPhotoDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, orderPodPhotoDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!orderPodPhotoRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OrderPodPhotoDTO> result = orderPodPhotoService.partialUpdate(orderPodPhotoDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderPodPhotoDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /order-pod-photos} : get all the orderPodPhotos.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderPodPhotos in body.
     */
    @GetMapping("")
    public List<OrderPodPhotoDTO> getAllOrderPodPhotos(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all OrderPodPhotos");
        return orderPodPhotoService.findAll();
    }

    /**
     * {@code GET  /order-pod-photos/:id} : get the "id" orderPodPhoto.
     *
     * @param id the id of the orderPodPhotoDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderPodPhotoDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderPodPhotoDTO> getOrderPodPhoto(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderPodPhoto : {}", id);
        Optional<OrderPodPhotoDTO> orderPodPhotoDTO = orderPodPhotoService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderPodPhotoDTO);
    }

    /**
     * {@code DELETE  /order-pod-photos/:id} : delete the "id" orderPodPhoto.
     *
     * @param id the id of the orderPodPhotoDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderPodPhoto(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OrderPodPhoto : {}", id);
        orderPodPhotoService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
