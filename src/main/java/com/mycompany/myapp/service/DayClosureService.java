package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.DayClosure;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.service.dto.DayClosureDTO;
import com.mycompany.myapp.service.mapper.DayClosureMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.DayClosure}.
 */
@Service
@Transactional
public class DayClosureService {

    private static final Logger LOG = LoggerFactory.getLogger(DayClosureService.class);

    private final DayClosureRepository dayClosureRepository;

    private final DayClosureMapper dayClosureMapper;

    public DayClosureService(DayClosureRepository dayClosureRepository, DayClosureMapper dayClosureMapper) {
        this.dayClosureRepository = dayClosureRepository;
        this.dayClosureMapper = dayClosureMapper;
    }

    /**
     * Save a dayClosure.
     *
     * @param dayClosureDTO the entity to save.
     * @return the persisted entity.
     */
    public DayClosureDTO save(DayClosureDTO dayClosureDTO) {
        LOG.debug("Request to save DayClosure : {}", dayClosureDTO);
        DayClosure dayClosure = dayClosureMapper.toEntity(dayClosureDTO);
        dayClosure = dayClosureRepository.save(dayClosure);
        return dayClosureMapper.toDto(dayClosure);
    }

    /**
     * Update a dayClosure.
     *
     * @param dayClosureDTO the entity to save.
     * @return the persisted entity.
     */
    public DayClosureDTO update(DayClosureDTO dayClosureDTO) {
        LOG.debug("Request to update DayClosure : {}", dayClosureDTO);
        DayClosure dayClosure = dayClosureMapper.toEntity(dayClosureDTO);
        dayClosure = dayClosureRepository.save(dayClosure);
        return dayClosureMapper.toDto(dayClosure);
    }

    /**
     * Partially update a dayClosure.
     *
     * @param dayClosureDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DayClosureDTO> partialUpdate(DayClosureDTO dayClosureDTO) {
        LOG.debug("Request to partially update DayClosure : {}", dayClosureDTO);

        return dayClosureRepository
            .findById(dayClosureDTO.getId())
            .map(existingDayClosure -> {
                dayClosureMapper.partialUpdate(existingDayClosure, dayClosureDTO);

                return existingDayClosure;
            })
            .map(dayClosureRepository::save)
            .map(dayClosureMapper::toDto);
    }

    /**
     * Get all the dayClosures with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<DayClosureDTO> findAllWithEagerRelationships(Pageable pageable) {
        return dayClosureRepository.findAllWithEagerRelationships(pageable).map(dayClosureMapper::toDto);
    }

    /**
     * Get one dayClosure by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DayClosureDTO> findOne(Long id) {
        LOG.debug("Request to get DayClosure : {}", id);
        return dayClosureRepository.findOneWithEagerRelationships(id).map(dayClosureMapper::toDto);
    }

    /**
     * Delete the dayClosure by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete DayClosure : {}", id);
        dayClosureRepository.deleteById(id);
    }
}
