package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ProductPriceRuleRepository;
import com.mycompany.myapp.service.ProductPriceRuleService;
import com.mycompany.myapp.service.dto.ProductPriceRuleDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.ProductPriceRule}.
 */
@RestController
@RequestMapping("/api/product-price-rules")
public class ProductPriceRuleResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProductPriceRuleResource.class);

    private static final String ENTITY_NAME = "productPriceRule";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProductPriceRuleService productPriceRuleService;

    private final ProductPriceRuleRepository productPriceRuleRepository;

    public ProductPriceRuleResource(
        ProductPriceRuleService productPriceRuleService,
        ProductPriceRuleRepository productPriceRuleRepository
    ) {
        this.productPriceRuleService = productPriceRuleService;
        this.productPriceRuleRepository = productPriceRuleRepository;
    }

    /**
     * {@code POST  /product-price-rules} : Create a new productPriceRule.
     *
     * @param productPriceRuleDTO the productPriceRuleDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new productPriceRuleDTO, or with status {@code 400 (Bad Request)} if the productPriceRule has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ProductPriceRuleDTO> createProductPriceRule(@Valid @RequestBody ProductPriceRuleDTO productPriceRuleDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ProductPriceRule : {}", productPriceRuleDTO);
        if (productPriceRuleDTO.getId() != null) {
            throw new BadRequestAlertException("A new productPriceRule cannot already have an ID", ENTITY_NAME, "idexists");
        }
        productPriceRuleDTO = productPriceRuleService.save(productPriceRuleDTO);
        return ResponseEntity.created(new URI("/api/product-price-rules/" + productPriceRuleDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, productPriceRuleDTO.getId().toString()))
            .body(productPriceRuleDTO);
    }

    /**
     * {@code PUT  /product-price-rules/:id} : Updates an existing productPriceRule.
     *
     * @param id the id of the productPriceRuleDTO to save.
     * @param productPriceRuleDTO the productPriceRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated productPriceRuleDTO,
     * or with status {@code 400 (Bad Request)} if the productPriceRuleDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the productPriceRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductPriceRuleDTO> updateProductPriceRule(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ProductPriceRuleDTO productPriceRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ProductPriceRule : {}, {}", id, productPriceRuleDTO);
        if (productPriceRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, productPriceRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!productPriceRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        productPriceRuleDTO = productPriceRuleService.update(productPriceRuleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, productPriceRuleDTO.getId().toString()))
            .body(productPriceRuleDTO);
    }

    /**
     * {@code PATCH  /product-price-rules/:id} : Partial updates given fields of an existing productPriceRule, field will ignore if it is null
     *
     * @param id the id of the productPriceRuleDTO to save.
     * @param productPriceRuleDTO the productPriceRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated productPriceRuleDTO,
     * or with status {@code 400 (Bad Request)} if the productPriceRuleDTO is not valid,
     * or with status {@code 404 (Not Found)} if the productPriceRuleDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the productPriceRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProductPriceRuleDTO> partialUpdateProductPriceRule(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ProductPriceRuleDTO productPriceRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ProductPriceRule partially : {}, {}", id, productPriceRuleDTO);
        if (productPriceRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, productPriceRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!productPriceRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ProductPriceRuleDTO> result = productPriceRuleService.partialUpdate(productPriceRuleDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, productPriceRuleDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /product-price-rules} : get all the productPriceRules.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of productPriceRules in body.
     */
    @GetMapping("")
    public List<ProductPriceRuleDTO> getAllProductPriceRules() {
        LOG.debug("REST request to get all ProductPriceRules");
        return productPriceRuleService.findAll();
    }

    /**
     * {@code GET  /product-price-rules/:id} : get the "id" productPriceRule.
     *
     * @param id the id of the productPriceRuleDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the productPriceRuleDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductPriceRuleDTO> getProductPriceRule(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ProductPriceRule : {}", id);
        Optional<ProductPriceRuleDTO> productPriceRuleDTO = productPriceRuleService.findOne(id);
        return ResponseUtil.wrapOrNotFound(productPriceRuleDTO);
    }

    /**
     * {@code DELETE  /product-price-rules/:id} : delete the "id" productPriceRule.
     *
     * @param id the id of the productPriceRuleDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductPriceRule(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete ProductPriceRule : {}", id);
        productPriceRuleService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
