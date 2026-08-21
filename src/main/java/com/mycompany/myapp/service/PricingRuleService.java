package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.PricingRule;
import com.mycompany.myapp.repository.BranchRepository;
import com.mycompany.myapp.repository.PricingRuleRepository;
import com.mycompany.myapp.repository.RouteRepository;
import com.mycompany.myapp.service.dto.PricingRuleDTO;
import com.mycompany.myapp.service.mapper.PricingRuleMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.PricingRule}.
 */
@Service
@Transactional
public class PricingRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(PricingRuleService.class);

    private final PricingRuleRepository pricingRuleRepository;

    private final PricingRuleMapper pricingRuleMapper;

    private final BranchRepository branchRepository;

    private final RouteRepository routeRepository;

    public PricingRuleService(
        PricingRuleRepository pricingRuleRepository,
        PricingRuleMapper pricingRuleMapper,
        BranchRepository branchRepository,
        RouteRepository routeRepository
    ) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.pricingRuleMapper = pricingRuleMapper;
        this.branchRepository = branchRepository;
        this.routeRepository = routeRepository;
    }

    /**
     * Save a pricingRule.
     *
     * @param pricingRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public PricingRuleDTO save(PricingRuleDTO pricingRuleDTO) {
        LOG.debug("Request to save PricingRule : {}", pricingRuleDTO);
        PricingRule pricingRule = pricingRuleMapper.toEntity(pricingRuleDTO);
        attachRefs(pricingRuleDTO, pricingRule);
        pricingRule = pricingRuleRepository.save(pricingRule);
        return toDtoEager(pricingRule);
    }

    /**
     * Update a pricingRule.
     *
     * @param pricingRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public PricingRuleDTO update(PricingRuleDTO pricingRuleDTO) {
        LOG.debug("Request to update PricingRule : {}", pricingRuleDTO);
        PricingRule pricingRule = pricingRuleMapper.toEntity(pricingRuleDTO);
        attachRefs(pricingRuleDTO, pricingRule);
        pricingRule = pricingRuleRepository.save(pricingRule);
        return toDtoEager(pricingRule);
    }

    /**
     * Partially update a pricingRule.
     *
     * @param pricingRuleDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PricingRuleDTO> partialUpdate(PricingRuleDTO pricingRuleDTO) {
        LOG.debug("Request to partially update PricingRule : {}", pricingRuleDTO);

        return pricingRuleRepository
            .findById(pricingRuleDTO.getId())
            .map(existingPricingRule -> {
                pricingRuleMapper.partialUpdate(existingPricingRule, pricingRuleDTO);

                return existingPricingRule;
            })
            .map(pricingRuleRepository::save)
            .map(pricingRuleMapper::toDto);
    }

    /**
     * Get all the pricingRules with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<PricingRuleDTO> findAllWithEagerRelationships(Pageable pageable) {
        return pricingRuleRepository.findAllWithEagerRelationships(pageable).map(pricingRuleMapper::toDto);
    }

    /**
     * Get one pricingRule by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PricingRuleDTO> findOne(Long id) {
        LOG.debug("Request to get PricingRule : {}", id);
        return pricingRuleRepository.findOneWithEagerRelationships(id).map(pricingRuleMapper::toDto);
    }

    /**
     * Delete the pricingRule by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete PricingRule : {}", id);
        pricingRuleRepository.deleteById(id);
    }

    private void attachRefs(PricingRuleDTO dto, PricingRule entity) {
        if (dto.getBranch() != null && dto.getBranch().getId() != null) {
            entity.setBranch(branchRepository.getReferenceById(dto.getBranch().getId()));
        } else {
            entity.setBranch(null);
        }
        if (dto.getRoute() != null && dto.getRoute().getId() != null) {
            entity.setRoute(routeRepository.getReferenceById(dto.getRoute().getId()));
        } else {
            entity.setRoute(null);
        }
    }

    private PricingRuleDTO toDtoEager(PricingRule saved) {
        return pricingRuleRepository
            .findOneWithEagerRelationships(saved.getId())
            .map(pricingRuleMapper::toDto)
            .orElseGet(() -> pricingRuleMapper.toDto(saved));
    }
}
