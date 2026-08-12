package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import com.mycompany.myapp.service.dto.SurchargePolicyDTO;
import com.mycompany.myapp.service.mapper.SurchargePolicyMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.SurchargePolicy}.
 */
@Service
@Transactional
public class SurchargePolicyService {

    private static final Logger LOG = LoggerFactory.getLogger(SurchargePolicyService.class);

    private final SurchargePolicyRepository surchargePolicyRepository;

    private final SurchargePolicyMapper surchargePolicyMapper;

    public SurchargePolicyService(SurchargePolicyRepository surchargePolicyRepository, SurchargePolicyMapper surchargePolicyMapper) {
        this.surchargePolicyRepository = surchargePolicyRepository;
        this.surchargePolicyMapper = surchargePolicyMapper;
    }

    /**
     * Save a surchargePolicy.
     *
     * @param surchargePolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public SurchargePolicyDTO save(SurchargePolicyDTO surchargePolicyDTO) {
        LOG.debug("Request to save SurchargePolicy : {}", surchargePolicyDTO);
        SurchargePolicy surchargePolicy = surchargePolicyMapper.toEntity(surchargePolicyDTO);
        surchargePolicy = surchargePolicyRepository.save(surchargePolicy);
        return surchargePolicyMapper.toDto(surchargePolicy);
    }

    /**
     * Update a surchargePolicy.
     *
     * @param surchargePolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public SurchargePolicyDTO update(SurchargePolicyDTO surchargePolicyDTO) {
        LOG.debug("Request to update SurchargePolicy : {}", surchargePolicyDTO);
        SurchargePolicy surchargePolicy = surchargePolicyMapper.toEntity(surchargePolicyDTO);
        surchargePolicy = surchargePolicyRepository.save(surchargePolicy);
        return surchargePolicyMapper.toDto(surchargePolicy);
    }

    /**
     * Partially update a surchargePolicy.
     *
     * @param surchargePolicyDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<SurchargePolicyDTO> partialUpdate(SurchargePolicyDTO surchargePolicyDTO) {
        LOG.debug("Request to partially update SurchargePolicy : {}", surchargePolicyDTO);

        return surchargePolicyRepository
            .findById(surchargePolicyDTO.getId())
            .map(existingSurchargePolicy -> {
                surchargePolicyMapper.partialUpdate(existingSurchargePolicy, surchargePolicyDTO);

                return existingSurchargePolicy;
            })
            .map(surchargePolicyRepository::save)
            .map(surchargePolicyMapper::toDto);
    }

    /**
     * Get all the surchargePolicies.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<SurchargePolicyDTO> findAll() {
        LOG.debug("Request to get all SurchargePolicies");
        return surchargePolicyRepository
            .findAll()
            .stream()
            .map(surchargePolicyMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one surchargePolicy by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<SurchargePolicyDTO> findOne(Long id) {
        LOG.debug("Request to get SurchargePolicy : {}", id);
        return surchargePolicyRepository.findById(id).map(surchargePolicyMapper::toDto);
    }

    /**
     * Delete the surchargePolicy by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete SurchargePolicy : {}", id);
        surchargePolicyRepository.deleteById(id);
    }
}
