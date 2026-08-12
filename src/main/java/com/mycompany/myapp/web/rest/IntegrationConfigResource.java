package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.service.IntegrationConfigService;
import com.mycompany.myapp.service.dto.IntegrationConfigDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.IntegrationConfig}.
 */
@RestController
@RequestMapping("/api/integration-configs")
public class IntegrationConfigResource {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationConfigResource.class);

    private static final String ENTITY_NAME = "integrationConfig";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final IntegrationConfigService integrationConfigService;

    private final IntegrationConfigRepository integrationConfigRepository;

    public IntegrationConfigResource(
        IntegrationConfigService integrationConfigService,
        IntegrationConfigRepository integrationConfigRepository
    ) {
        this.integrationConfigService = integrationConfigService;
        this.integrationConfigRepository = integrationConfigRepository;
    }

    /**
     * {@code POST  /integration-configs} : Create a new integrationConfig.
     *
     * @param integrationConfigDTO the integrationConfigDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new integrationConfigDTO, or with status {@code 400 (Bad Request)} if the integrationConfig has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<IntegrationConfigDTO> createIntegrationConfig(@Valid @RequestBody IntegrationConfigDTO integrationConfigDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save IntegrationConfig : {}", integrationConfigDTO);
        if (integrationConfigDTO.getId() != null) {
            throw new BadRequestAlertException("A new integrationConfig cannot already have an ID", ENTITY_NAME, "idexists");
        }
        integrationConfigDTO = integrationConfigService.save(integrationConfigDTO);
        return ResponseEntity.created(new URI("/api/integration-configs/" + integrationConfigDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, integrationConfigDTO.getId().toString()))
            .body(integrationConfigDTO);
    }

    /**
     * {@code PUT  /integration-configs/:id} : Updates an existing integrationConfig.
     *
     * @param id the id of the integrationConfigDTO to save.
     * @param integrationConfigDTO the integrationConfigDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated integrationConfigDTO,
     * or with status {@code 400 (Bad Request)} if the integrationConfigDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the integrationConfigDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<IntegrationConfigDTO> updateIntegrationConfig(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody IntegrationConfigDTO integrationConfigDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update IntegrationConfig : {}, {}", id, integrationConfigDTO);
        if (integrationConfigDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, integrationConfigDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!integrationConfigRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        integrationConfigDTO = integrationConfigService.update(integrationConfigDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, integrationConfigDTO.getId().toString()))
            .body(integrationConfigDTO);
    }

    /**
     * {@code PATCH  /integration-configs/:id} : Partial updates given fields of an existing integrationConfig, field will ignore if it is null
     *
     * @param id the id of the integrationConfigDTO to save.
     * @param integrationConfigDTO the integrationConfigDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated integrationConfigDTO,
     * or with status {@code 400 (Bad Request)} if the integrationConfigDTO is not valid,
     * or with status {@code 404 (Not Found)} if the integrationConfigDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the integrationConfigDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<IntegrationConfigDTO> partialUpdateIntegrationConfig(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody IntegrationConfigDTO integrationConfigDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update IntegrationConfig partially : {}, {}", id, integrationConfigDTO);
        if (integrationConfigDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, integrationConfigDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!integrationConfigRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<IntegrationConfigDTO> result = integrationConfigService.partialUpdate(integrationConfigDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, integrationConfigDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /integration-configs} : get all the integrationConfigs.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of integrationConfigs in body.
     */
    @GetMapping("")
    public List<IntegrationConfigDTO> getAllIntegrationConfigs() {
        LOG.debug("REST request to get all IntegrationConfigs");
        return integrationConfigService.findAll();
    }

    /**
     * {@code GET  /integration-configs/:id} : get the "id" integrationConfig.
     *
     * @param id the id of the integrationConfigDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the integrationConfigDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IntegrationConfigDTO> getIntegrationConfig(@PathVariable("id") Long id) {
        LOG.debug("REST request to get IntegrationConfig : {}", id);
        Optional<IntegrationConfigDTO> integrationConfigDTO = integrationConfigService.findOne(id);
        return ResponseUtil.wrapOrNotFound(integrationConfigDTO);
    }

    /**
     * {@code DELETE  /integration-configs/:id} : delete the "id" integrationConfig.
     *
     * @param id the id of the integrationConfigDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntegrationConfig(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete IntegrationConfig : {}", id);
        integrationConfigService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
