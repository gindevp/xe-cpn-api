package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.service.TripQueryService;
import com.mycompany.myapp.service.TripService;
import com.mycompany.myapp.service.criteria.TripCriteria;
import com.mycompany.myapp.service.dto.TripDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Trip}.
 * Raw JHipster CRUD moved off {@code /api/trips} so FE facade can own that path (M5).
 */
@RestController
@RequestMapping("/api/trip-entities")
public class TripResource {

    private static final Logger LOG = LoggerFactory.getLogger(TripResource.class);

    private static final String ENTITY_NAME = "trip";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final TripService tripService;

    private final TripRepository tripRepository;

    private final TripQueryService tripQueryService;

    public TripResource(TripService tripService, TripRepository tripRepository, TripQueryService tripQueryService) {
        this.tripService = tripService;
        this.tripRepository = tripRepository;
        this.tripQueryService = tripQueryService;
    }

    /**
     * {@code POST  /trips} : Create a new trip.
     *
     * @param tripDTO the tripDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new tripDTO, or with status {@code 400 (Bad Request)} if the trip has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<TripDTO> createTrip(@Valid @RequestBody TripDTO tripDTO) throws URISyntaxException {
        LOG.debug("REST request to save Trip : {}", tripDTO);
        if (tripDTO.getId() != null) {
            throw new BadRequestAlertException("A new trip cannot already have an ID", ENTITY_NAME, "idexists");
        }
        tripDTO = tripService.save(tripDTO);
        return ResponseEntity.created(new URI("/api/trips/" + tripDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, tripDTO.getId().toString()))
            .body(tripDTO);
    }

    /**
     * {@code PUT  /trips/:id} : Updates an existing trip.
     *
     * @param id the id of the tripDTO to save.
     * @param tripDTO the tripDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tripDTO,
     * or with status {@code 400 (Bad Request)} if the tripDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the tripDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TripDTO> updateTrip(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody TripDTO tripDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Trip : {}, {}", id, tripDTO);
        if (tripDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, tripDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!tripRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        tripDTO = tripService.update(tripDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, tripDTO.getId().toString()))
            .body(tripDTO);
    }

    /**
     * {@code PATCH  /trips/:id} : Partial updates given fields of an existing trip, field will ignore if it is null
     *
     * @param id the id of the tripDTO to save.
     * @param tripDTO the tripDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tripDTO,
     * or with status {@code 400 (Bad Request)} if the tripDTO is not valid,
     * or with status {@code 404 (Not Found)} if the tripDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the tripDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<TripDTO> partialUpdateTrip(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody TripDTO tripDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Trip partially : {}, {}", id, tripDTO);
        if (tripDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, tripDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!tripRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<TripDTO> result = tripService.partialUpdate(tripDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, tripDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /trips} : get all the trips.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of trips in body.
     */
    @GetMapping("")
    public ResponseEntity<List<TripDTO>> getAllTrips(
        TripCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Trips by criteria: {}", criteria);

        Page<TripDTO> page = tripQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /trips/count} : count all the trips.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countTrips(TripCriteria criteria) {
        LOG.debug("REST request to count Trips by criteria: {}", criteria);
        return ResponseEntity.ok().body(tripQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /trips/:id} : get the "id" trip.
     *
     * @param id the id of the tripDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the tripDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripDTO> getTrip(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Trip : {}", id);
        Optional<TripDTO> tripDTO = tripService.findOne(id);
        return ResponseUtil.wrapOrNotFound(tripDTO);
    }

    /**
     * {@code DELETE  /trips/:id} : delete the "id" trip.
     *
     * @param id the id of the tripDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Trip : {}", id);
        tripService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
