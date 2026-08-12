package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.DoorFeeRule;
import com.mycompany.myapp.repository.DoorFeeRuleRepository;
import com.mycompany.myapp.service.dto.DoorFeeRuleDTO;
import com.mycompany.myapp.service.mapper.DoorFeeRuleMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.DoorFeeRule}.
 */
@Service
@Transactional
public class DoorFeeRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(DoorFeeRuleService.class);

    private final DoorFeeRuleRepository doorFeeRuleRepository;

    private final DoorFeeRuleMapper doorFeeRuleMapper;

    public DoorFeeRuleService(DoorFeeRuleRepository doorFeeRuleRepository, DoorFeeRuleMapper doorFeeRuleMapper) {
        this.doorFeeRuleRepository = doorFeeRuleRepository;
        this.doorFeeRuleMapper = doorFeeRuleMapper;
    }

    /**
     * Save a doorFeeRule.
     *
     * @param doorFeeRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public DoorFeeRuleDTO save(DoorFeeRuleDTO doorFeeRuleDTO) {
        LOG.debug("Request to save DoorFeeRule : {}", doorFeeRuleDTO);
        DoorFeeRule doorFeeRule = doorFeeRuleMapper.toEntity(doorFeeRuleDTO);
        doorFeeRule = doorFeeRuleRepository.save(doorFeeRule);
        return doorFeeRuleMapper.toDto(doorFeeRule);
    }

    /**
     * Update a doorFeeRule.
     *
     * @param doorFeeRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public DoorFeeRuleDTO update(DoorFeeRuleDTO doorFeeRuleDTO) {
        LOG.debug("Request to update DoorFeeRule : {}", doorFeeRuleDTO);
        DoorFeeRule doorFeeRule = doorFeeRuleMapper.toEntity(doorFeeRuleDTO);
        doorFeeRule = doorFeeRuleRepository.save(doorFeeRule);
        return doorFeeRuleMapper.toDto(doorFeeRule);
    }

    /**
     * Partially update a doorFeeRule.
     *
     * @param doorFeeRuleDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DoorFeeRuleDTO> partialUpdate(DoorFeeRuleDTO doorFeeRuleDTO) {
        LOG.debug("Request to partially update DoorFeeRule : {}", doorFeeRuleDTO);

        return doorFeeRuleRepository
            .findById(doorFeeRuleDTO.getId())
            .map(existingDoorFeeRule -> {
                doorFeeRuleMapper.partialUpdate(existingDoorFeeRule, doorFeeRuleDTO);

                return existingDoorFeeRule;
            })
            .map(doorFeeRuleRepository::save)
            .map(doorFeeRuleMapper::toDto);
    }

    /**
     * Get all the doorFeeRules.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<DoorFeeRuleDTO> findAll() {
        LOG.debug("Request to get all DoorFeeRules");
        return doorFeeRuleRepository.findAll().stream().map(doorFeeRuleMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one doorFeeRule by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DoorFeeRuleDTO> findOne(Long id) {
        LOG.debug("Request to get DoorFeeRule : {}", id);
        return doorFeeRuleRepository.findById(id).map(doorFeeRuleMapper::toDto);
    }

    /**
     * Delete the doorFeeRule by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete DoorFeeRule : {}", id);
        doorFeeRuleRepository.deleteById(id);
    }
}
