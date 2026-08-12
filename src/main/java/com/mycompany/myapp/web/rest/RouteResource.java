package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.RouteRepository;
import com.mycompany.myapp.service.RouteService;
import com.mycompany.myapp.service.dto.RouteDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Route}.
 */
@RestController
@RequestMapping("/api/routes")
public class RouteResource {

    private static final Logger LOG = LoggerFactory.getLogger(RouteResource.class);

    private static final String ENTITY_NAME = "route";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RouteService routeService;

    private final RouteRepository routeRepository;

    public RouteResource(RouteService routeService, RouteRepository routeRepository) {
        this.routeService = routeService;
        this.routeRepository = routeRepository;
    }

    /**
     * {@code POST  /routes} : Create a new route.
     *
     * @param routeDTO the routeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new routeDTO, or with status {@code 400 (Bad Request)} if the route has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RouteDTO> createRoute(@Valid @RequestBody RouteDTO routeDTO) throws URISyntaxException {
        LOG.debug("REST request to save Route : {}", routeDTO);
        if (routeDTO.getId() != null) {
            throw new BadRequestAlertException("A new route cannot already have an ID", ENTITY_NAME, "idexists");
        }
        routeDTO = routeService.save(routeDTO);
        return ResponseEntity.created(new URI("/api/routes/" + routeDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, routeDTO.getId().toString()))
            .body(routeDTO);
    }

    /**
     * {@code PUT  /routes/:id} : Updates an existing route.
     *
     * @param id the id of the routeDTO to save.
     * @param routeDTO the routeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated routeDTO,
     * or with status {@code 400 (Bad Request)} if the routeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the routeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RouteDTO> updateRoute(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RouteDTO routeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Route : {}, {}", id, routeDTO);
        if (routeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, routeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!routeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        routeDTO = routeService.update(routeDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, routeDTO.getId().toString()))
            .body(routeDTO);
    }

    /**
     * {@code PATCH  /routes/:id} : Partial updates given fields of an existing route, field will ignore if it is null
     *
     * @param id the id of the routeDTO to save.
     * @param routeDTO the routeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated routeDTO,
     * or with status {@code 400 (Bad Request)} if the routeDTO is not valid,
     * or with status {@code 404 (Not Found)} if the routeDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the routeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RouteDTO> partialUpdateRoute(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RouteDTO routeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Route partially : {}, {}", id, routeDTO);
        if (routeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, routeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!routeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RouteDTO> result = routeService.partialUpdate(routeDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, routeDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /routes} : get all the routes.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of routes in body.
     */
    @GetMapping("")
    public List<RouteDTO> getAllRoutes(@RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        LOG.debug("REST request to get all Routes");
        return routeService.findAll();
    }

    /**
     * {@code GET  /routes/:id} : get the "id" route.
     *
     * @param id the id of the routeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the routeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RouteDTO> getRoute(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Route : {}", id);
        Optional<RouteDTO> routeDTO = routeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(routeDTO);
    }

    /**
     * {@code DELETE  /routes/:id} : delete the "id" route.
     *
     * @param id the id of the routeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Route : {}", id);
        routeService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
