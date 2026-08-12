package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.service.ReceiptOrderLineService;
import com.mycompany.myapp.service.dto.ReceiptOrderLineDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.ReceiptOrderLine}.
 */
@RestController
@RequestMapping("/api/receipt-order-lines")
public class ReceiptOrderLineResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiptOrderLineResource.class);

    private static final String ENTITY_NAME = "receiptOrderLine";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReceiptOrderLineService receiptOrderLineService;

    private final ReceiptOrderLineRepository receiptOrderLineRepository;

    public ReceiptOrderLineResource(
        ReceiptOrderLineService receiptOrderLineService,
        ReceiptOrderLineRepository receiptOrderLineRepository
    ) {
        this.receiptOrderLineService = receiptOrderLineService;
        this.receiptOrderLineRepository = receiptOrderLineRepository;
    }

    /**
     * {@code POST  /receipt-order-lines} : Create a new receiptOrderLine.
     *
     * @param receiptOrderLineDTO the receiptOrderLineDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new receiptOrderLineDTO, or with status {@code 400 (Bad Request)} if the receiptOrderLine has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ReceiptOrderLineDTO> createReceiptOrderLine(@Valid @RequestBody ReceiptOrderLineDTO receiptOrderLineDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ReceiptOrderLine : {}", receiptOrderLineDTO);
        if (receiptOrderLineDTO.getId() != null) {
            throw new BadRequestAlertException("A new receiptOrderLine cannot already have an ID", ENTITY_NAME, "idexists");
        }
        receiptOrderLineDTO = receiptOrderLineService.save(receiptOrderLineDTO);
        return ResponseEntity.created(new URI("/api/receipt-order-lines/" + receiptOrderLineDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, receiptOrderLineDTO.getId().toString()))
            .body(receiptOrderLineDTO);
    }

    /**
     * {@code PUT  /receipt-order-lines/:id} : Updates an existing receiptOrderLine.
     *
     * @param id the id of the receiptOrderLineDTO to save.
     * @param receiptOrderLineDTO the receiptOrderLineDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated receiptOrderLineDTO,
     * or with status {@code 400 (Bad Request)} if the receiptOrderLineDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the receiptOrderLineDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReceiptOrderLineDTO> updateReceiptOrderLine(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ReceiptOrderLineDTO receiptOrderLineDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ReceiptOrderLine : {}, {}", id, receiptOrderLineDTO);
        if (receiptOrderLineDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, receiptOrderLineDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!receiptOrderLineRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        receiptOrderLineDTO = receiptOrderLineService.update(receiptOrderLineDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, receiptOrderLineDTO.getId().toString()))
            .body(receiptOrderLineDTO);
    }

    /**
     * {@code PATCH  /receipt-order-lines/:id} : Partial updates given fields of an existing receiptOrderLine, field will ignore if it is null
     *
     * @param id the id of the receiptOrderLineDTO to save.
     * @param receiptOrderLineDTO the receiptOrderLineDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated receiptOrderLineDTO,
     * or with status {@code 400 (Bad Request)} if the receiptOrderLineDTO is not valid,
     * or with status {@code 404 (Not Found)} if the receiptOrderLineDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the receiptOrderLineDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ReceiptOrderLineDTO> partialUpdateReceiptOrderLine(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ReceiptOrderLineDTO receiptOrderLineDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ReceiptOrderLine partially : {}, {}", id, receiptOrderLineDTO);
        if (receiptOrderLineDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, receiptOrderLineDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!receiptOrderLineRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ReceiptOrderLineDTO> result = receiptOrderLineService.partialUpdate(receiptOrderLineDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, receiptOrderLineDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /receipt-order-lines} : get all the receiptOrderLines.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of receiptOrderLines in body.
     */
    @GetMapping("")
    public List<ReceiptOrderLineDTO> getAllReceiptOrderLines(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all ReceiptOrderLines");
        return receiptOrderLineService.findAll();
    }

    /**
     * {@code GET  /receipt-order-lines/:id} : get the "id" receiptOrderLine.
     *
     * @param id the id of the receiptOrderLineDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the receiptOrderLineDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReceiptOrderLineDTO> getReceiptOrderLine(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ReceiptOrderLine : {}", id);
        Optional<ReceiptOrderLineDTO> receiptOrderLineDTO = receiptOrderLineService.findOne(id);
        return ResponseUtil.wrapOrNotFound(receiptOrderLineDTO);
    }

    /**
     * {@code DELETE  /receipt-order-lines/:id} : delete the "id" receiptOrderLine.
     *
     * @param id the id of the receiptOrderLineDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReceiptOrderLine(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete ReceiptOrderLine : {}", id);
        receiptOrderLineService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
