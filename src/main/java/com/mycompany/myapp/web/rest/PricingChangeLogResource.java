package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.PricingChangeLogRepository;
import com.mycompany.myapp.service.PricingChangeLogService;
import com.mycompany.myapp.service.dto.PricingChangeLogDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.PricingChangeLog}.
 */
@RestController
@RequestMapping("/api/pricing-change-logs")
public class PricingChangeLogResource {

    private static final Logger LOG = LoggerFactory.getLogger(PricingChangeLogResource.class);

    private static final String ENTITY_NAME = "pricingChangeLog";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PricingChangeLogService pricingChangeLogService;

    private final PricingChangeLogRepository pricingChangeLogRepository;

    public PricingChangeLogResource(
        PricingChangeLogService pricingChangeLogService,
        PricingChangeLogRepository pricingChangeLogRepository
    ) {
        this.pricingChangeLogService = pricingChangeLogService;
        this.pricingChangeLogRepository = pricingChangeLogRepository;
    }

    /**
     * {@code POST  /pricing-change-logs} : Create a new pricingChangeLog.
     *
     * @param pricingChangeLogDTO the pricingChangeLogDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new pricingChangeLogDTO, or with status {@code 400 (Bad Request)} if the pricingChangeLog has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PricingChangeLogDTO> createPricingChangeLog(@Valid @RequestBody PricingChangeLogDTO pricingChangeLogDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save PricingChangeLog : {}", pricingChangeLogDTO);
        if (pricingChangeLogDTO.getId() != null) {
            throw new BadRequestAlertException("A new pricingChangeLog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        pricingChangeLogDTO = pricingChangeLogService.save(pricingChangeLogDTO);
        return ResponseEntity.created(new URI("/api/pricing-change-logs/" + pricingChangeLogDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, pricingChangeLogDTO.getId().toString()))
            .body(pricingChangeLogDTO);
    }

    /**
     * {@code PUT  /pricing-change-logs/:id} : Updates an existing pricingChangeLog.
     *
     * @param id the id of the pricingChangeLogDTO to save.
     * @param pricingChangeLogDTO the pricingChangeLogDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pricingChangeLogDTO,
     * or with status {@code 400 (Bad Request)} if the pricingChangeLogDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the pricingChangeLogDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PricingChangeLogDTO> updatePricingChangeLog(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PricingChangeLogDTO pricingChangeLogDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update PricingChangeLog : {}, {}", id, pricingChangeLogDTO);
        if (pricingChangeLogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pricingChangeLogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pricingChangeLogRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        pricingChangeLogDTO = pricingChangeLogService.update(pricingChangeLogDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pricingChangeLogDTO.getId().toString()))
            .body(pricingChangeLogDTO);
    }

    /**
     * {@code PATCH  /pricing-change-logs/:id} : Partial updates given fields of an existing pricingChangeLog, field will ignore if it is null
     *
     * @param id the id of the pricingChangeLogDTO to save.
     * @param pricingChangeLogDTO the pricingChangeLogDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pricingChangeLogDTO,
     * or with status {@code 400 (Bad Request)} if the pricingChangeLogDTO is not valid,
     * or with status {@code 404 (Not Found)} if the pricingChangeLogDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the pricingChangeLogDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PricingChangeLogDTO> partialUpdatePricingChangeLog(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PricingChangeLogDTO pricingChangeLogDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update PricingChangeLog partially : {}, {}", id, pricingChangeLogDTO);
        if (pricingChangeLogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pricingChangeLogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pricingChangeLogRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PricingChangeLogDTO> result = pricingChangeLogService.partialUpdate(pricingChangeLogDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pricingChangeLogDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /pricing-change-logs} : get all the pricingChangeLogs.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of pricingChangeLogs in body.
     */
    @GetMapping("")
    public List<PricingChangeLogDTO> getAllPricingChangeLogs(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all PricingChangeLogs");
        return pricingChangeLogService.findAll();
    }

    /**
     * {@code GET  /pricing-change-logs/:id} : get the "id" pricingChangeLog.
     *
     * @param id the id of the pricingChangeLogDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the pricingChangeLogDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PricingChangeLogDTO> getPricingChangeLog(@PathVariable("id") Long id) {
        LOG.debug("REST request to get PricingChangeLog : {}", id);
        Optional<PricingChangeLogDTO> pricingChangeLogDTO = pricingChangeLogService.findOne(id);
        return ResponseUtil.wrapOrNotFound(pricingChangeLogDTO);
    }

    /**
     * {@code DELETE  /pricing-change-logs/:id} : delete the "id" pricingChangeLog.
     *
     * @param id the id of the pricingChangeLogDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePricingChangeLog(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete PricingChangeLog : {}", id);
        pricingChangeLogService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
