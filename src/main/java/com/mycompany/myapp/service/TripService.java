package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.service.dto.TripDTO;
import com.mycompany.myapp.service.mapper.TripMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Trip}.
 */
@Service
@Transactional
public class TripService {

    private static final Logger LOG = LoggerFactory.getLogger(TripService.class);

    private final TripRepository tripRepository;

    private final TripMapper tripMapper;

    public TripService(TripRepository tripRepository, TripMapper tripMapper) {
        this.tripRepository = tripRepository;
        this.tripMapper = tripMapper;
    }

    /**
     * Save a trip.
     *
     * @param tripDTO the entity to save.
     * @return the persisted entity.
     */
    public TripDTO save(TripDTO tripDTO) {
        LOG.debug("Request to save Trip : {}", tripDTO);
        Trip trip = tripMapper.toEntity(tripDTO);
        trip = tripRepository.save(trip);
        return tripMapper.toDto(trip);
    }

    /**
     * Update a trip.
     *
     * @param tripDTO the entity to save.
     * @return the persisted entity.
     */
    public TripDTO update(TripDTO tripDTO) {
        LOG.debug("Request to update Trip : {}", tripDTO);
        Trip trip = tripMapper.toEntity(tripDTO);
        trip = tripRepository.save(trip);
        return tripMapper.toDto(trip);
    }

    /**
     * Partially update a trip.
     *
     * @param tripDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<TripDTO> partialUpdate(TripDTO tripDTO) {
        LOG.debug("Request to partially update Trip : {}", tripDTO);

        return tripRepository
            .findById(tripDTO.getId())
            .map(existingTrip -> {
                tripMapper.partialUpdate(existingTrip, tripDTO);

                return existingTrip;
            })
            .map(tripRepository::save)
            .map(tripMapper::toDto);
    }

    /**
     * Get all the trips with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<TripDTO> findAllWithEagerRelationships(Pageable pageable) {
        return tripRepository.findAllWithEagerRelationships(pageable).map(tripMapper::toDto);
    }

    /**
     * Get one trip by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<TripDTO> findOne(Long id) {
        LOG.debug("Request to get Trip : {}", id);
        return tripRepository.findOneWithEagerRelationships(id).map(tripMapper::toDto);
    }

    /**
     * Delete the trip by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Trip : {}", id);
        tripRepository.deleteById(id);
    }
}
