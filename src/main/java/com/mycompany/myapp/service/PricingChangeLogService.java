package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.PricingChangeLog;
import com.mycompany.myapp.repository.PricingChangeLogRepository;
import com.mycompany.myapp.service.dto.PricingChangeLogDTO;
import com.mycompany.myapp.service.mapper.PricingChangeLogMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.PricingChangeLog}.
 */
@Service
@Transactional
public class PricingChangeLogService {

    private static final Logger LOG = LoggerFactory.getLogger(PricingChangeLogService.class);

    private final PricingChangeLogRepository pricingChangeLogRepository;

    private final PricingChangeLogMapper pricingChangeLogMapper;

    public PricingChangeLogService(PricingChangeLogRepository pricingChangeLogRepository, PricingChangeLogMapper pricingChangeLogMapper) {
        this.pricingChangeLogRepository = pricingChangeLogRepository;
        this.pricingChangeLogMapper = pricingChangeLogMapper;
    }

    /**
     * Save a pricingChangeLog.
     *
     * @param pricingChangeLogDTO the entity to save.
     * @return the persisted entity.
     */
    public PricingChangeLogDTO save(PricingChangeLogDTO pricingChangeLogDTO) {
        LOG.debug("Request to save PricingChangeLog : {}", pricingChangeLogDTO);
        PricingChangeLog pricingChangeLog = pricingChangeLogMapper.toEntity(pricingChangeLogDTO);
        pricingChangeLog = pricingChangeLogRepository.save(pricingChangeLog);
        return pricingChangeLogMapper.toDto(pricingChangeLog);
    }

    /**
     * Update a pricingChangeLog.
     *
     * @param pricingChangeLogDTO the entity to save.
     * @return the persisted entity.
     */
    public PricingChangeLogDTO update(PricingChangeLogDTO pricingChangeLogDTO) {
        LOG.debug("Request to update PricingChangeLog : {}", pricingChangeLogDTO);
        PricingChangeLog pricingChangeLog = pricingChangeLogMapper.toEntity(pricingChangeLogDTO);
        pricingChangeLog = pricingChangeLogRepository.save(pricingChangeLog);
        return pricingChangeLogMapper.toDto(pricingChangeLog);
    }

    /**
     * Partially update a pricingChangeLog.
     *
     * @param pricingChangeLogDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PricingChangeLogDTO> partialUpdate(PricingChangeLogDTO pricingChangeLogDTO) {
        LOG.debug("Request to partially update PricingChangeLog : {}", pricingChangeLogDTO);

        return pricingChangeLogRepository
            .findById(pricingChangeLogDTO.getId())
            .map(existingPricingChangeLog -> {
                pricingChangeLogMapper.partialUpdate(existingPricingChangeLog, pricingChangeLogDTO);

                return existingPricingChangeLog;
            })
            .map(pricingChangeLogRepository::save)
            .map(pricingChangeLogMapper::toDto);
    }

    /**
     * Get all the pricingChangeLogs.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<PricingChangeLogDTO> findAll() {
        LOG.debug("Request to get all PricingChangeLogs");
        return pricingChangeLogRepository
            .findAll()
            .stream()
            .map(pricingChangeLogMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the pricingChangeLogs with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<PricingChangeLogDTO> findAllWithEagerRelationships(Pageable pageable) {
        return pricingChangeLogRepository.findAllWithEagerRelationships(pageable).map(pricingChangeLogMapper::toDto);
    }

    /**
     * Get one pricingChangeLog by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PricingChangeLogDTO> findOne(Long id) {
        LOG.debug("Request to get PricingChangeLog : {}", id);
        return pricingChangeLogRepository.findOneWithEagerRelationships(id).map(pricingChangeLogMapper::toDto);
    }

    /**
     * Delete the pricingChangeLog by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete PricingChangeLog : {}", id);
        pricingChangeLogRepository.deleteById(id);
    }
}
