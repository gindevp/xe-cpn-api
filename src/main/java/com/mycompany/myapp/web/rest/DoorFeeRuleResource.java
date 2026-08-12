package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.DoorFeeRuleRepository;
import com.mycompany.myapp.service.DoorFeeRuleService;
import com.mycompany.myapp.service.dto.DoorFeeRuleDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.DoorFeeRule}.
 */
@RestController
@RequestMapping("/api/door-fee-rules")
public class DoorFeeRuleResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoorFeeRuleResource.class);

    private static final String ENTITY_NAME = "doorFeeRule";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DoorFeeRuleService doorFeeRuleService;

    private final DoorFeeRuleRepository doorFeeRuleRepository;

    public DoorFeeRuleResource(DoorFeeRuleService doorFeeRuleService, DoorFeeRuleRepository doorFeeRuleRepository) {
        this.doorFeeRuleService = doorFeeRuleService;
        this.doorFeeRuleRepository = doorFeeRuleRepository;
    }

    /**
     * {@code POST  /door-fee-rules} : Create a new doorFeeRule.
     *
     * @param doorFeeRuleDTO the doorFeeRuleDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new doorFeeRuleDTO, or with status {@code 400 (Bad Request)} if the doorFeeRule has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DoorFeeRuleDTO> createDoorFeeRule(@Valid @RequestBody DoorFeeRuleDTO doorFeeRuleDTO) throws URISyntaxException {
        LOG.debug("REST request to save DoorFeeRule : {}", doorFeeRuleDTO);
        if (doorFeeRuleDTO.getId() != null) {
            throw new BadRequestAlertException("A new doorFeeRule cannot already have an ID", ENTITY_NAME, "idexists");
        }
        doorFeeRuleDTO = doorFeeRuleService.save(doorFeeRuleDTO);
        return ResponseEntity.created(new URI("/api/door-fee-rules/" + doorFeeRuleDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, doorFeeRuleDTO.getId().toString()))
            .body(doorFeeRuleDTO);
    }

    /**
     * {@code PUT  /door-fee-rules/:id} : Updates an existing doorFeeRule.
     *
     * @param id the id of the doorFeeRuleDTO to save.
     * @param doorFeeRuleDTO the doorFeeRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated doorFeeRuleDTO,
     * or with status {@code 400 (Bad Request)} if the doorFeeRuleDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the doorFeeRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DoorFeeRuleDTO> updateDoorFeeRule(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody DoorFeeRuleDTO doorFeeRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update DoorFeeRule : {}, {}", id, doorFeeRuleDTO);
        if (doorFeeRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, doorFeeRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!doorFeeRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        doorFeeRuleDTO = doorFeeRuleService.update(doorFeeRuleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, doorFeeRuleDTO.getId().toString()))
            .body(doorFeeRuleDTO);
    }

    /**
     * {@code PATCH  /door-fee-rules/:id} : Partial updates given fields of an existing doorFeeRule, field will ignore if it is null
     *
     * @param id the id of the doorFeeRuleDTO to save.
     * @param doorFeeRuleDTO the doorFeeRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated doorFeeRuleDTO,
     * or with status {@code 400 (Bad Request)} if the doorFeeRuleDTO is not valid,
     * or with status {@code 404 (Not Found)} if the doorFeeRuleDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the doorFeeRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DoorFeeRuleDTO> partialUpdateDoorFeeRule(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody DoorFeeRuleDTO doorFeeRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update DoorFeeRule partially : {}, {}", id, doorFeeRuleDTO);
        if (doorFeeRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, doorFeeRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!doorFeeRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DoorFeeRuleDTO> result = doorFeeRuleService.partialUpdate(doorFeeRuleDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, doorFeeRuleDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /door-fee-rules} : get all the doorFeeRules.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of doorFeeRules in body.
     */
    @GetMapping("")
    public List<DoorFeeRuleDTO> getAllDoorFeeRules() {
        LOG.debug("REST request to get all DoorFeeRules");
        return doorFeeRuleService.findAll();
    }

    /**
     * {@code GET  /door-fee-rules/:id} : get the "id" doorFeeRule.
     *
     * @param id the id of the doorFeeRuleDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the doorFeeRuleDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DoorFeeRuleDTO> getDoorFeeRule(@PathVariable("id") Long id) {
        LOG.debug("REST request to get DoorFeeRule : {}", id);
        Optional<DoorFeeRuleDTO> doorFeeRuleDTO = doorFeeRuleService.findOne(id);
        return ResponseUtil.wrapOrNotFound(doorFeeRuleDTO);
    }

    /**
     * {@code DELETE  /door-fee-rules/:id} : delete the "id" doorFeeRule.
     *
     * @param id the id of the doorFeeRuleDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDoorFeeRule(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete DoorFeeRule : {}", id);
        doorFeeRuleService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
