package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.TripOrderAssignmentRepository;
import com.mycompany.myapp.service.TripOrderAssignmentService;
import com.mycompany.myapp.service.dto.TripOrderAssignmentDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.TripOrderAssignment}.
 */
@RestController
@RequestMapping("/api/trip-order-assignments")
public class TripOrderAssignmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(TripOrderAssignmentResource.class);

    private static final String ENTITY_NAME = "tripOrderAssignment";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final TripOrderAssignmentService tripOrderAssignmentService;

    private final TripOrderAssignmentRepository tripOrderAssignmentRepository;

    public TripOrderAssignmentResource(
        TripOrderAssignmentService tripOrderAssignmentService,
        TripOrderAssignmentRepository tripOrderAssignmentRepository
    ) {
        this.tripOrderAssignmentService = tripOrderAssignmentService;
        this.tripOrderAssignmentRepository = tripOrderAssignmentRepository;
    }

    /**
     * {@code POST  /trip-order-assignments} : Create a new tripOrderAssignment.
     *
     * @param tripOrderAssignmentDTO the tripOrderAssignmentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new tripOrderAssignmentDTO, or with status {@code 400 (Bad Request)} if the tripOrderAssignment has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<TripOrderAssignmentDTO> createTripOrderAssignment(
        @Valid @RequestBody TripOrderAssignmentDTO tripOrderAssignmentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save TripOrderAssignment : {}", tripOrderAssignmentDTO);
        if (tripOrderAssignmentDTO.getId() != null) {
            throw new BadRequestAlertException("A new tripOrderAssignment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        tripOrderAssignmentDTO = tripOrderAssignmentService.save(tripOrderAssignmentDTO);
        return ResponseEntity.created(new URI("/api/trip-order-assignments/" + tripOrderAssignmentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, tripOrderAssignmentDTO.getId().toString()))
            .body(tripOrderAssignmentDTO);
    }

    /**
     * {@code PUT  /trip-order-assignments/:id} : Updates an existing tripOrderAssignment.
     *
     * @param id the id of the tripOrderAssignmentDTO to save.
     * @param tripOrderAssignmentDTO the tripOrderAssignmentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tripOrderAssignmentDTO,
     * or with status {@code 400 (Bad Request)} if the tripOrderAssignmentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the tripOrderAssignmentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TripOrderAssignmentDTO> updateTripOrderAssignment(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody TripOrderAssignmentDTO tripOrderAssignmentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update TripOrderAssignment : {}, {}", id, tripOrderAssignmentDTO);
        if (tripOrderAssignmentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, tripOrderAssignmentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!tripOrderAssignmentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        tripOrderAssignmentDTO = tripOrderAssignmentService.update(tripOrderAssignmentDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, tripOrderAssignmentDTO.getId().toString()))
            .body(tripOrderAssignmentDTO);
    }

    /**
     * {@code PATCH  /trip-order-assignments/:id} : Partial updates given fields of an existing tripOrderAssignment, field will ignore if it is null
     *
     * @param id the id of the tripOrderAssignmentDTO to save.
     * @param tripOrderAssignmentDTO the tripOrderAssignmentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tripOrderAssignmentDTO,
     * or with status {@code 400 (Bad Request)} if the tripOrderAssignmentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the tripOrderAssignmentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the tripOrderAssignmentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<TripOrderAssignmentDTO> partialUpdateTripOrderAssignment(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody TripOrderAssignmentDTO tripOrderAssignmentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update TripOrderAssignment partially : {}, {}", id, tripOrderAssignmentDTO);
        if (tripOrderAssignmentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, tripOrderAssignmentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!tripOrderAssignmentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<TripOrderAssignmentDTO> result = tripOrderAssignmentService.partialUpdate(tripOrderAssignmentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, tripOrderAssignmentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /trip-order-assignments} : get all the tripOrderAssignments.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of tripOrderAssignments in body.
     */
    @GetMapping("")
    public List<TripOrderAssignmentDTO> getAllTripOrderAssignments(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all TripOrderAssignments");
        return tripOrderAssignmentService.findAll();
    }

    /**
     * {@code GET  /trip-order-assignments/:id} : get the "id" tripOrderAssignment.
     *
     * @param id the id of the tripOrderAssignmentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the tripOrderAssignmentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripOrderAssignmentDTO> getTripOrderAssignment(@PathVariable("id") Long id) {
        LOG.debug("REST request to get TripOrderAssignment : {}", id);
        Optional<TripOrderAssignmentDTO> tripOrderAssignmentDTO = tripOrderAssignmentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(tripOrderAssignmentDTO);
    }

    /**
     * {@code DELETE  /trip-order-assignments/:id} : delete the "id" tripOrderAssignment.
     *
     * @param id the id of the tripOrderAssignmentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTripOrderAssignment(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete TripOrderAssignment : {}", id);
        tripOrderAssignmentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
