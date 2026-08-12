package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.repository.RouteRepository;
import com.mycompany.myapp.service.dto.RouteDTO;
import com.mycompany.myapp.service.mapper.RouteMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Route}.
 */
@Service
@Transactional
public class RouteService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;

    private final RouteMapper routeMapper;

    public RouteService(RouteRepository routeRepository, RouteMapper routeMapper) {
        this.routeRepository = routeRepository;
        this.routeMapper = routeMapper;
    }

    /**
     * Save a route.
     *
     * @param routeDTO the entity to save.
     * @return the persisted entity.
     */
    public RouteDTO save(RouteDTO routeDTO) {
        LOG.debug("Request to save Route : {}", routeDTO);
        Route route = routeMapper.toEntity(routeDTO);
        route = routeRepository.save(route);
        return routeMapper.toDto(route);
    }

    /**
     * Update a route.
     *
     * @param routeDTO the entity to save.
     * @return the persisted entity.
     */
    public RouteDTO update(RouteDTO routeDTO) {
        LOG.debug("Request to update Route : {}", routeDTO);
        Route route = routeMapper.toEntity(routeDTO);
        route = routeRepository.save(route);
        return routeMapper.toDto(route);
    }

    /**
     * Partially update a route.
     *
     * @param routeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RouteDTO> partialUpdate(RouteDTO routeDTO) {
        LOG.debug("Request to partially update Route : {}", routeDTO);

        return routeRepository
            .findById(routeDTO.getId())
            .map(existingRoute -> {
                routeMapper.partialUpdate(existingRoute, routeDTO);

                return existingRoute;
            })
            .map(routeRepository::save)
            .map(routeMapper::toDto);
    }

    /**
     * Get all the routes.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RouteDTO> findAll() {
        LOG.debug("Request to get all Routes");
        return routeRepository.findAll().stream().map(routeMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the routes with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RouteDTO> findAllWithEagerRelationships(Pageable pageable) {
        return routeRepository.findAllWithEagerRelationships(pageable).map(routeMapper::toDto);
    }

    /**
     * Get one route by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RouteDTO> findOne(Long id) {
        LOG.debug("Request to get Route : {}", id);
        return routeRepository.findOneWithEagerRelationships(id).map(routeMapper::toDto);
    }

    /**
     * Delete the route by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Route : {}", id);
        routeRepository.deleteById(id);
    }
}
