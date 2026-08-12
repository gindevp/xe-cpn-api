package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.PricingRuleRepository;
import com.mycompany.myapp.service.PricingRuleQueryService;
import com.mycompany.myapp.service.PricingRuleService;
import com.mycompany.myapp.service.criteria.PricingRuleCriteria;
import com.mycompany.myapp.service.dto.PricingRuleDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.PricingRule}.
 */
@RestController
@RequestMapping("/api/pricing-rules")
public class PricingRuleResource {

    private static final Logger LOG = LoggerFactory.getLogger(PricingRuleResource.class);

    private static final String ENTITY_NAME = "pricingRule";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PricingRuleService pricingRuleService;

    private final PricingRuleRepository pricingRuleRepository;

    private final PricingRuleQueryService pricingRuleQueryService;

    public PricingRuleResource(
        PricingRuleService pricingRuleService,
        PricingRuleRepository pricingRuleRepository,
        PricingRuleQueryService pricingRuleQueryService
    ) {
        this.pricingRuleService = pricingRuleService;
        this.pricingRuleRepository = pricingRuleRepository;
        this.pricingRuleQueryService = pricingRuleQueryService;
    }

    /**
     * {@code POST  /pricing-rules} : Create a new pricingRule.
     *
     * @param pricingRuleDTO the pricingRuleDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new pricingRuleDTO, or with status {@code 400 (Bad Request)} if the pricingRule has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PricingRuleDTO> createPricingRule(@Valid @RequestBody PricingRuleDTO pricingRuleDTO) throws URISyntaxException {
        LOG.debug("REST request to save PricingRule : {}", pricingRuleDTO);
        if (pricingRuleDTO.getId() != null) {
            throw new BadRequestAlertException("A new pricingRule cannot already have an ID", ENTITY_NAME, "idexists");
        }
        pricingRuleDTO = pricingRuleService.save(pricingRuleDTO);
        return ResponseEntity.created(new URI("/api/pricing-rules/" + pricingRuleDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, pricingRuleDTO.getId().toString()))
            .body(pricingRuleDTO);
    }

    /**
     * {@code PUT  /pricing-rules/:id} : Updates an existing pricingRule.
     *
     * @param id the id of the pricingRuleDTO to save.
     * @param pricingRuleDTO the pricingRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pricingRuleDTO,
     * or with status {@code 400 (Bad Request)} if the pricingRuleDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the pricingRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PricingRuleDTO> updatePricingRule(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PricingRuleDTO pricingRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update PricingRule : {}, {}", id, pricingRuleDTO);
        if (pricingRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pricingRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pricingRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        pricingRuleDTO = pricingRuleService.update(pricingRuleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pricingRuleDTO.getId().toString()))
            .body(pricingRuleDTO);
    }

    /**
     * {@code PATCH  /pricing-rules/:id} : Partial updates given fields of an existing pricingRule, field will ignore if it is null
     *
     * @param id the id of the pricingRuleDTO to save.
     * @param pricingRuleDTO the pricingRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pricingRuleDTO,
     * or with status {@code 400 (Bad Request)} if the pricingRuleDTO is not valid,
     * or with status {@code 404 (Not Found)} if the pricingRuleDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the pricingRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PricingRuleDTO> partialUpdatePricingRule(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PricingRuleDTO pricingRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update PricingRule partially : {}, {}", id, pricingRuleDTO);
        if (pricingRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pricingRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pricingRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PricingRuleDTO> result = pricingRuleService.partialUpdate(pricingRuleDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pricingRuleDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /pricing-rules} : get all the pricingRules.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of pricingRules in body.
     */
    @GetMapping("")
    public ResponseEntity<List<PricingRuleDTO>> getAllPricingRules(
        PricingRuleCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get PricingRules by criteria: {}", criteria);

        Page<PricingRuleDTO> page = pricingRuleQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /pricing-rules/count} : count all the pricingRules.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countPricingRules(PricingRuleCriteria criteria) {
        LOG.debug("REST request to count PricingRules by criteria: {}", criteria);
        return ResponseEntity.ok().body(pricingRuleQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /pricing-rules/:id} : get the "id" pricingRule.
     *
     * @param id the id of the pricingRuleDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the pricingRuleDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PricingRuleDTO> getPricingRule(@PathVariable("id") Long id) {
        LOG.debug("REST request to get PricingRule : {}", id);
        Optional<PricingRuleDTO> pricingRuleDTO = pricingRuleService.findOne(id);
        return ResponseUtil.wrapOrNotFound(pricingRuleDTO);
    }

    /**
     * {@code DELETE  /pricing-rules/:id} : delete the "id" pricingRule.
     *
     * @param id the id of the pricingRuleDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePricingRule(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete PricingRule : {}", id);
        pricingRuleService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
