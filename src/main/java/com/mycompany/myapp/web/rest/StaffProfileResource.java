package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.service.StaffProfileService;
import com.mycompany.myapp.service.dto.StaffProfileDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.StaffProfile}.
 */
@RestController
@RequestMapping("/api/staff-profiles")
public class StaffProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(StaffProfileResource.class);

    private static final String ENTITY_NAME = "staffProfile";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final StaffProfileService staffProfileService;

    private final StaffProfileRepository staffProfileRepository;

    public StaffProfileResource(StaffProfileService staffProfileService, StaffProfileRepository staffProfileRepository) {
        this.staffProfileService = staffProfileService;
        this.staffProfileRepository = staffProfileRepository;
    }

    /**
     * {@code POST  /staff-profiles} : Create a new staffProfile.
     *
     * @param staffProfileDTO the staffProfileDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new staffProfileDTO, or with status {@code 400 (Bad Request)} if the staffProfile has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<StaffProfileDTO> createStaffProfile(@Valid @RequestBody StaffProfileDTO staffProfileDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save StaffProfile : {}", staffProfileDTO);
        if (staffProfileDTO.getId() != null) {
            throw new BadRequestAlertException("A new staffProfile cannot already have an ID", ENTITY_NAME, "idexists");
        }
        staffProfileDTO = staffProfileService.save(staffProfileDTO);
        return ResponseEntity.created(new URI("/api/staff-profiles/" + staffProfileDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, staffProfileDTO.getId().toString()))
            .body(staffProfileDTO);
    }

    /**
     * {@code PUT  /staff-profiles/:id} : Updates an existing staffProfile.
     *
     * @param id the id of the staffProfileDTO to save.
     * @param staffProfileDTO the staffProfileDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated staffProfileDTO,
     * or with status {@code 400 (Bad Request)} if the staffProfileDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the staffProfileDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<StaffProfileDTO> updateStaffProfile(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody StaffProfileDTO staffProfileDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update StaffProfile : {}, {}", id, staffProfileDTO);
        if (staffProfileDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, staffProfileDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!staffProfileRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        staffProfileDTO = staffProfileService.update(staffProfileDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, staffProfileDTO.getId().toString()))
            .body(staffProfileDTO);
    }

    /**
     * {@code PATCH  /staff-profiles/:id} : Partial updates given fields of an existing staffProfile, field will ignore if it is null
     *
     * @param id the id of the staffProfileDTO to save.
     * @param staffProfileDTO the staffProfileDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated staffProfileDTO,
     * or with status {@code 400 (Bad Request)} if the staffProfileDTO is not valid,
     * or with status {@code 404 (Not Found)} if the staffProfileDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the staffProfileDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<StaffProfileDTO> partialUpdateStaffProfile(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody StaffProfileDTO staffProfileDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update StaffProfile partially : {}, {}", id, staffProfileDTO);
        if (staffProfileDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, staffProfileDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!staffProfileRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<StaffProfileDTO> result = staffProfileService.partialUpdate(staffProfileDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, staffProfileDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /staff-profiles} : get all the staffProfiles.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of staffProfiles in body.
     */
    @GetMapping("")
    public List<StaffProfileDTO> getAllStaffProfiles(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all StaffProfiles");
        return staffProfileService.findAll();
    }

    /**
     * {@code GET  /staff-profiles/:id} : get the "id" staffProfile.
     *
     * @param id the id of the staffProfileDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the staffProfileDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StaffProfileDTO> getStaffProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to get StaffProfile : {}", id);
        Optional<StaffProfileDTO> staffProfileDTO = staffProfileService.findOne(id);
        return ResponseUtil.wrapOrNotFound(staffProfileDTO);
    }

    /**
     * {@code DELETE  /staff-profiles/:id} : delete the "id" staffProfile.
     *
     * @param id the id of the staffProfileDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaffProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete StaffProfile : {}", id);
        staffProfileService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
