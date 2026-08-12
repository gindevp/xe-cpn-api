package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.service.ReceiptQueryService;
import com.mycompany.myapp.service.ReceiptService;
import com.mycompany.myapp.service.criteria.ReceiptCriteria;
import com.mycompany.myapp.service.dto.ReceiptDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Receipt}.
 */
@RestController
@RequestMapping("/api/receipt-entities")
public class ReceiptResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiptResource.class);

    private static final String ENTITY_NAME = "receipt";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReceiptService receiptService;

    private final ReceiptRepository receiptRepository;

    private final ReceiptQueryService receiptQueryService;

    public ReceiptResource(ReceiptService receiptService, ReceiptRepository receiptRepository, ReceiptQueryService receiptQueryService) {
        this.receiptService = receiptService;
        this.receiptRepository = receiptRepository;
        this.receiptQueryService = receiptQueryService;
    }

    /**
     * {@code POST  /receipts} : Create a new receipt.
     *
     * @param receiptDTO the receiptDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new receiptDTO, or with status {@code 400 (Bad Request)} if the receipt has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ReceiptDTO> createReceipt(@Valid @RequestBody ReceiptDTO receiptDTO) throws URISyntaxException {
        LOG.debug("REST request to save Receipt : {}", receiptDTO);
        if (receiptDTO.getId() != null) {
            throw new BadRequestAlertException("A new receipt cannot already have an ID", ENTITY_NAME, "idexists");
        }
        receiptDTO = receiptService.save(receiptDTO);
        return ResponseEntity.created(new URI("/api/receipts/" + receiptDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, receiptDTO.getId().toString()))
            .body(receiptDTO);
    }

    /**
     * {@code PUT  /receipts/:id} : Updates an existing receipt.
     *
     * @param id the id of the receiptDTO to save.
     * @param receiptDTO the receiptDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated receiptDTO,
     * or with status {@code 400 (Bad Request)} if the receiptDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the receiptDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReceiptDTO> updateReceipt(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ReceiptDTO receiptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Receipt : {}, {}", id, receiptDTO);
        if (receiptDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, receiptDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!receiptRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        receiptDTO = receiptService.update(receiptDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, receiptDTO.getId().toString()))
            .body(receiptDTO);
    }

    /**
     * {@code PATCH  /receipts/:id} : Partial updates given fields of an existing receipt, field will ignore if it is null
     *
     * @param id the id of the receiptDTO to save.
     * @param receiptDTO the receiptDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated receiptDTO,
     * or with status {@code 400 (Bad Request)} if the receiptDTO is not valid,
     * or with status {@code 404 (Not Found)} if the receiptDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the receiptDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ReceiptDTO> partialUpdateReceipt(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ReceiptDTO receiptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Receipt partially : {}, {}", id, receiptDTO);
        if (receiptDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, receiptDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!receiptRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ReceiptDTO> result = receiptService.partialUpdate(receiptDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, receiptDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /receipts} : get all the receipts.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of receipts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ReceiptDTO>> getAllReceipts(
        ReceiptCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Receipts by criteria: {}", criteria);

        Page<ReceiptDTO> page = receiptQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /receipts/count} : count all the receipts.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countReceipts(ReceiptCriteria criteria) {
        LOG.debug("REST request to count Receipts by criteria: {}", criteria);
        return ResponseEntity.ok().body(receiptQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /receipts/:id} : get the "id" receipt.
     *
     * @param id the id of the receiptDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the receiptDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReceiptDTO> getReceipt(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Receipt : {}", id);
        Optional<ReceiptDTO> receiptDTO = receiptService.findOne(id);
        return ResponseUtil.wrapOrNotFound(receiptDTO);
    }

    /**
     * {@code DELETE  /receipts/:id} : delete the "id" receipt.
     *
     * @param id the id of the receiptDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReceipt(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Receipt : {}", id);
        receiptService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
