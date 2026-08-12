package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.SurchargePolicyRepository;
import com.mycompany.myapp.service.SurchargePolicyService;
import com.mycompany.myapp.service.dto.SurchargePolicyDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.SurchargePolicy}.
 */
@RestController
@RequestMapping("/api/surcharge-policies")
public class SurchargePolicyResource {

    private static final Logger LOG = LoggerFactory.getLogger(SurchargePolicyResource.class);

    private static final String ENTITY_NAME = "surchargePolicy";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final SurchargePolicyService surchargePolicyService;

    private final SurchargePolicyRepository surchargePolicyRepository;

    public SurchargePolicyResource(SurchargePolicyService surchargePolicyService, SurchargePolicyRepository surchargePolicyRepository) {
        this.surchargePolicyService = surchargePolicyService;
        this.surchargePolicyRepository = surchargePolicyRepository;
    }

    /**
     * {@code POST  /surcharge-policies} : Create a new surchargePolicy.
     *
     * @param surchargePolicyDTO the surchargePolicyDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new surchargePolicyDTO, or with status {@code 400 (Bad Request)} if the surchargePolicy has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SurchargePolicyDTO> createSurchargePolicy(@Valid @RequestBody SurchargePolicyDTO surchargePolicyDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save SurchargePolicy : {}", surchargePolicyDTO);
        if (surchargePolicyDTO.getId() != null) {
            throw new BadRequestAlertException("A new surchargePolicy cannot already have an ID", ENTITY_NAME, "idexists");
        }
        surchargePolicyDTO = surchargePolicyService.save(surchargePolicyDTO);
        return ResponseEntity.created(new URI("/api/surcharge-policies/" + surchargePolicyDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, surchargePolicyDTO.getId().toString()))
            .body(surchargePolicyDTO);
    }

    /**
     * {@code PUT  /surcharge-policies/:id} : Updates an existing surchargePolicy.
     *
     * @param id the id of the surchargePolicyDTO to save.
     * @param surchargePolicyDTO the surchargePolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated surchargePolicyDTO,
     * or with status {@code 400 (Bad Request)} if the surchargePolicyDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the surchargePolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SurchargePolicyDTO> updateSurchargePolicy(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SurchargePolicyDTO surchargePolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SurchargePolicy : {}, {}", id, surchargePolicyDTO);
        if (surchargePolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, surchargePolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!surchargePolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        surchargePolicyDTO = surchargePolicyService.update(surchargePolicyDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, surchargePolicyDTO.getId().toString()))
            .body(surchargePolicyDTO);
    }

    /**
     * {@code PATCH  /surcharge-policies/:id} : Partial updates given fields of an existing surchargePolicy, field will ignore if it is null
     *
     * @param id the id of the surchargePolicyDTO to save.
     * @param surchargePolicyDTO the surchargePolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated surchargePolicyDTO,
     * or with status {@code 400 (Bad Request)} if the surchargePolicyDTO is not valid,
     * or with status {@code 404 (Not Found)} if the surchargePolicyDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the surchargePolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SurchargePolicyDTO> partialUpdateSurchargePolicy(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SurchargePolicyDTO surchargePolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update SurchargePolicy partially : {}, {}", id, surchargePolicyDTO);
        if (surchargePolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, surchargePolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!surchargePolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SurchargePolicyDTO> result = surchargePolicyService.partialUpdate(surchargePolicyDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, surchargePolicyDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /surcharge-policies} : get all the surchargePolicies.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of surchargePolicies in body.
     */
    @GetMapping("")
    public List<SurchargePolicyDTO> getAllSurchargePolicies() {
        LOG.debug("REST request to get all SurchargePolicies");
        return surchargePolicyService.findAll();
    }

    /**
     * {@code GET  /surcharge-policies/:id} : get the "id" surchargePolicy.
     *
     * @param id the id of the surchargePolicyDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the surchargePolicyDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SurchargePolicyDTO> getSurchargePolicy(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SurchargePolicy : {}", id);
        Optional<SurchargePolicyDTO> surchargePolicyDTO = surchargePolicyService.findOne(id);
        return ResponseUtil.wrapOrNotFound(surchargePolicyDTO);
    }

    /**
     * {@code DELETE  /surcharge-policies/:id} : delete the "id" surchargePolicy.
     *
     * @param id the id of the surchargePolicyDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurchargePolicy(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SurchargePolicy : {}", id);
        surchargePolicyService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
