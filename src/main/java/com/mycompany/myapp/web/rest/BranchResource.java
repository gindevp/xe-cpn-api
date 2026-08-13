package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.BranchRepository;
import com.mycompany.myapp.service.BranchService;
import com.mycompany.myapp.service.dto.BranchDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Branch} (Tuyến master).
 */
@RestController
@RequestMapping("/api/branches")
public class BranchResource {

    private static final Logger LOG = LoggerFactory.getLogger(BranchResource.class);
    private static final String ENTITY_NAME = "branch";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final BranchService branchService;
    private final BranchRepository branchRepository;

    public BranchResource(BranchService branchService, BranchRepository branchRepository) {
        this.branchService = branchService;
        this.branchRepository = branchRepository;
    }

    @PostMapping("")
    public ResponseEntity<BranchDTO> createBranch(@Valid @RequestBody BranchDTO branchDTO) throws URISyntaxException {
        LOG.debug("REST request to save Branch : {}", branchDTO);
        if (branchDTO.getId() != null) {
            throw new BadRequestAlertException("A new branch cannot already have an ID", ENTITY_NAME, "idexists");
        }
        branchDTO = branchService.save(branchDTO);
        return ResponseEntity.created(new URI("/api/branches/" + branchDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, branchDTO.getId().toString()))
            .body(branchDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchDTO> updateBranch(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody BranchDTO branchDTO
    ) {
        LOG.debug("REST request to update Branch : {}, {}", id, branchDTO);
        if (branchDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, branchDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!branchRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        branchDTO = branchService.update(branchDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, branchDTO.getId().toString()))
            .body(branchDTO);
    }

    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<BranchDTO> partialUpdateBranch(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody BranchDTO branchDTO
    ) {
        LOG.debug("REST request to partial update Branch : {}, {}", id, branchDTO);
        if (branchDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, branchDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!branchRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<BranchDTO> result = branchService.partialUpdate(branchDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, branchDTO.getId().toString())
        );
    }

    /**
     * {@code GET /branches} : list branches.
     * @param activeOnly when true (default), only active rows.
     */
    @GetMapping("")
    public List<BranchDTO> getAllBranches(@RequestParam(name = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        LOG.debug("REST request to get all Branches");
        return branchService.findAll(activeOnly);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDTO> getBranch(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Branch : {}", id);
        return ResponseUtil.wrapOrNotFound(branchService.findOne(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Branch : {}", id);
        branchService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
