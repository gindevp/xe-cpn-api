package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ItineraryRepository;
import com.mycompany.myapp.service.ItineraryService;
import com.mycompany.myapp.service.dto.ItineraryDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Itinerary} (Lộ trình master).
 */
@RestController
@RequestMapping("/api/itineraries")
public class ItineraryResource {

    private static final Logger LOG = LoggerFactory.getLogger(ItineraryResource.class);
    private static final String ENTITY_NAME = "itinerary";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ItineraryService itineraryService;
    private final ItineraryRepository itineraryRepository;

    public ItineraryResource(ItineraryService itineraryService, ItineraryRepository itineraryRepository) {
        this.itineraryService = itineraryService;
        this.itineraryRepository = itineraryRepository;
    }

    @PostMapping("")
    public ResponseEntity<ItineraryDTO> createItinerary(@Valid @RequestBody ItineraryDTO itineraryDTO) throws URISyntaxException {
        LOG.debug("REST request to save Itinerary : {}", itineraryDTO);
        if (itineraryDTO.getId() != null) {
            throw new BadRequestAlertException("A new itinerary cannot already have an ID", ENTITY_NAME, "idexists");
        }
        itineraryDTO = itineraryService.save(itineraryDTO);
        return ResponseEntity.created(new URI("/api/itineraries/" + itineraryDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, itineraryDTO.getId().toString()))
            .body(itineraryDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItineraryDTO> updateItinerary(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ItineraryDTO itineraryDTO
    ) {
        LOG.debug("REST request to update Itinerary : {}, {}", id, itineraryDTO);
        if (itineraryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, itineraryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!itineraryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        itineraryDTO = itineraryService.update(itineraryDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, itineraryDTO.getId().toString()))
            .body(itineraryDTO);
    }

    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ItineraryDTO> partialUpdateItinerary(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ItineraryDTO itineraryDTO
    ) {
        LOG.debug("REST request to partial update Itinerary : {}, {}", id, itineraryDTO);
        if (itineraryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, itineraryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!itineraryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<ItineraryDTO> result = itineraryService.partialUpdate(itineraryDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, itineraryDTO.getId().toString())
        );
    }

    /**
     * {@code GET /itineraries} : list itineraries, optionally filtered by branch.
     * @param branchId optional parent Branch id
     * @param activeOnly when true (default), only active rows
     */
    @GetMapping("")
    public List<ItineraryDTO> getAllItineraries(
        @RequestParam(name = "branchId", required = false) Long branchId,
        @RequestParam(name = "activeOnly", required = false, defaultValue = "true") boolean activeOnly
    ) {
        LOG.debug("REST request to get Itineraries branchId={}", branchId);
        return itineraryService.findAll(branchId, activeOnly);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItineraryDTO> getItinerary(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Itinerary : {}", id);
        return ResponseUtil.wrapOrNotFound(itineraryService.findOne(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItinerary(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Itinerary : {}", id);
        itineraryService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
