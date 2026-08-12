package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.service.DayClosureQueryService;
import com.mycompany.myapp.service.DayClosureService;
import com.mycompany.myapp.service.criteria.DayClosureCriteria;
import com.mycompany.myapp.service.dto.DayClosureDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.DayClosure}.
 */
@RestController
@RequestMapping("/api/day-closure-entities")
public class DayClosureResource {

    private static final Logger LOG = LoggerFactory.getLogger(DayClosureResource.class);

    private static final String ENTITY_NAME = "dayClosure";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DayClosureService dayClosureService;

    private final DayClosureRepository dayClosureRepository;

    private final DayClosureQueryService dayClosureQueryService;

    public DayClosureResource(
        DayClosureService dayClosureService,
        DayClosureRepository dayClosureRepository,
        DayClosureQueryService dayClosureQueryService
    ) {
        this.dayClosureService = dayClosureService;
        this.dayClosureRepository = dayClosureRepository;
        this.dayClosureQueryService = dayClosureQueryService;
    }

    /**
     * {@code POST  /day-closures} : Create a new dayClosure.
     *
     * @param dayClosureDTO the dayClosureDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new dayClosureDTO, or with status {@code 400 (Bad Request)} if the dayClosure has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DayClosureDTO> createDayClosure(@Valid @RequestBody DayClosureDTO dayClosureDTO) throws URISyntaxException {
        LOG.debug("REST request to save DayClosure : {}", dayClosureDTO);
        if (dayClosureDTO.getId() != null) {
            throw new BadRequestAlertException("A new dayClosure cannot already have an ID", ENTITY_NAME, "idexists");
        }
        dayClosureDTO = dayClosureService.save(dayClosureDTO);
        return ResponseEntity.created(new URI("/api/day-closures/" + dayClosureDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, dayClosureDTO.getId().toString()))
            .body(dayClosureDTO);
    }

    /**
     * {@code PUT  /day-closures/:id} : Updates an existing dayClosure.
     *
     * @param id the id of the dayClosureDTO to save.
     * @param dayClosureDTO the dayClosureDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated dayClosureDTO,
     * or with status {@code 400 (Bad Request)} if the dayClosureDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the dayClosureDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DayClosureDTO> updateDayClosure(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody DayClosureDTO dayClosureDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update DayClosure : {}, {}", id, dayClosureDTO);
        if (dayClosureDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dayClosureDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!dayClosureRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        dayClosureDTO = dayClosureService.update(dayClosureDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, dayClosureDTO.getId().toString()))
            .body(dayClosureDTO);
    }

    /**
     * {@code PATCH  /day-closures/:id} : Partial updates given fields of an existing dayClosure, field will ignore if it is null
     *
     * @param id the id of the dayClosureDTO to save.
     * @param dayClosureDTO the dayClosureDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated dayClosureDTO,
     * or with status {@code 400 (Bad Request)} if the dayClosureDTO is not valid,
     * or with status {@code 404 (Not Found)} if the dayClosureDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the dayClosureDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DayClosureDTO> partialUpdateDayClosure(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody DayClosureDTO dayClosureDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update DayClosure partially : {}, {}", id, dayClosureDTO);
        if (dayClosureDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dayClosureDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!dayClosureRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DayClosureDTO> result = dayClosureService.partialUpdate(dayClosureDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, dayClosureDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /day-closures} : get all the dayClosures.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of dayClosures in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DayClosureDTO>> getAllDayClosures(
        DayClosureCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get DayClosures by criteria: {}", criteria);

        Page<DayClosureDTO> page = dayClosureQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /day-closures/count} : count all the dayClosures.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDayClosures(DayClosureCriteria criteria) {
        LOG.debug("REST request to count DayClosures by criteria: {}", criteria);
        return ResponseEntity.ok().body(dayClosureQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /day-closures/:id} : get the "id" dayClosure.
     *
     * @param id the id of the dayClosureDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the dayClosureDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DayClosureDTO> getDayClosure(@PathVariable("id") Long id) {
        LOG.debug("REST request to get DayClosure : {}", id);
        Optional<DayClosureDTO> dayClosureDTO = dayClosureService.findOne(id);
        return ResponseUtil.wrapOrNotFound(dayClosureDTO);
    }

    /**
     * {@code DELETE  /day-closures/:id} : delete the "id" dayClosure.
     *
     * @param id the id of the dayClosureDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDayClosure(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete DayClosure : {}", id);
        dayClosureService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
