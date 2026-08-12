package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.TripOrderAssignment;
import com.mycompany.myapp.repository.TripOrderAssignmentRepository;
import com.mycompany.myapp.service.dto.TripOrderAssignmentDTO;
import com.mycompany.myapp.service.mapper.TripOrderAssignmentMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.TripOrderAssignment}.
 */
@Service
@Transactional
public class TripOrderAssignmentService {

    private static final Logger LOG = LoggerFactory.getLogger(TripOrderAssignmentService.class);

    private final TripOrderAssignmentRepository tripOrderAssignmentRepository;

    private final TripOrderAssignmentMapper tripOrderAssignmentMapper;

    public TripOrderAssignmentService(
        TripOrderAssignmentRepository tripOrderAssignmentRepository,
        TripOrderAssignmentMapper tripOrderAssignmentMapper
    ) {
        this.tripOrderAssignmentRepository = tripOrderAssignmentRepository;
        this.tripOrderAssignmentMapper = tripOrderAssignmentMapper;
    }

    /**
     * Save a tripOrderAssignment.
     *
     * @param tripOrderAssignmentDTO the entity to save.
     * @return the persisted entity.
     */
    public TripOrderAssignmentDTO save(TripOrderAssignmentDTO tripOrderAssignmentDTO) {
        LOG.debug("Request to save TripOrderAssignment : {}", tripOrderAssignmentDTO);
        TripOrderAssignment tripOrderAssignment = tripOrderAssignmentMapper.toEntity(tripOrderAssignmentDTO);
        tripOrderAssignment = tripOrderAssignmentRepository.save(tripOrderAssignment);
        return tripOrderAssignmentMapper.toDto(tripOrderAssignment);
    }

    /**
     * Update a tripOrderAssignment.
     *
     * @param tripOrderAssignmentDTO the entity to save.
     * @return the persisted entity.
     */
    public TripOrderAssignmentDTO update(TripOrderAssignmentDTO tripOrderAssignmentDTO) {
        LOG.debug("Request to update TripOrderAssignment : {}", tripOrderAssignmentDTO);
        TripOrderAssignment tripOrderAssignment = tripOrderAssignmentMapper.toEntity(tripOrderAssignmentDTO);
        tripOrderAssignment = tripOrderAssignmentRepository.save(tripOrderAssignment);
        return tripOrderAssignmentMapper.toDto(tripOrderAssignment);
    }

    /**
     * Partially update a tripOrderAssignment.
     *
     * @param tripOrderAssignmentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<TripOrderAssignmentDTO> partialUpdate(TripOrderAssignmentDTO tripOrderAssignmentDTO) {
        LOG.debug("Request to partially update TripOrderAssignment : {}", tripOrderAssignmentDTO);

        return tripOrderAssignmentRepository
            .findById(tripOrderAssignmentDTO.getId())
            .map(existingTripOrderAssignment -> {
                tripOrderAssignmentMapper.partialUpdate(existingTripOrderAssignment, tripOrderAssignmentDTO);

                return existingTripOrderAssignment;
            })
            .map(tripOrderAssignmentRepository::save)
            .map(tripOrderAssignmentMapper::toDto);
    }

    /**
     * Get all the tripOrderAssignments.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<TripOrderAssignmentDTO> findAll() {
        LOG.debug("Request to get all TripOrderAssignments");
        return tripOrderAssignmentRepository
            .findAll()
            .stream()
            .map(tripOrderAssignmentMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the tripOrderAssignments with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<TripOrderAssignmentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return tripOrderAssignmentRepository.findAllWithEagerRelationships(pageable).map(tripOrderAssignmentMapper::toDto);
    }

    /**
     * Get one tripOrderAssignment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<TripOrderAssignmentDTO> findOne(Long id) {
        LOG.debug("Request to get TripOrderAssignment : {}", id);
        return tripOrderAssignmentRepository.findOneWithEagerRelationships(id).map(tripOrderAssignmentMapper::toDto);
    }

    /**
     * Delete the tripOrderAssignment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete TripOrderAssignment : {}", id);
        tripOrderAssignmentRepository.deleteById(id);
    }
}
