package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.PricingRule;
import com.mycompany.myapp.repository.PricingRuleRepository;
import com.mycompany.myapp.service.criteria.PricingRuleCriteria;
import com.mycompany.myapp.service.dto.PricingRuleDTO;
import com.mycompany.myapp.service.mapper.PricingRuleMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link PricingRule} entities in the database.
 * The main input is a {@link PricingRuleCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PricingRuleDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PricingRuleQueryService extends QueryService<PricingRule> {

    private static final Logger LOG = LoggerFactory.getLogger(PricingRuleQueryService.class);

    private final PricingRuleRepository pricingRuleRepository;

    private final PricingRuleMapper pricingRuleMapper;

    public PricingRuleQueryService(PricingRuleRepository pricingRuleRepository, PricingRuleMapper pricingRuleMapper) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.pricingRuleMapper = pricingRuleMapper;
    }

    /**
     * Return a {@link Page} of {@link PricingRuleDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<PricingRuleDTO> findByCriteria(PricingRuleCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PricingRule> specification = createSpecification(criteria);
        return pricingRuleRepository.findAll(specification, page).map(pricingRuleMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PricingRuleCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<PricingRule> specification = createSpecification(criteria);
        return pricingRuleRepository.count(specification);
    }

    /**
     * Function to convert {@link PricingRuleCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<PricingRule> createSpecification(PricingRuleCriteria criteria) {
        Specification<PricingRule> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            specification = specification.and((root, query, cb) -> {
                if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                    root.fetch(PricingRule_.branch, JoinType.LEFT);
                    root.fetch(PricingRule_.route, JoinType.LEFT);
                    query.distinct(true);
                }
                return cb.conjunction();
            });
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), PricingRule_.id));
            }
            if (criteria.getRuleCode() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRuleCode(), PricingRule_.ruleCode));
            }
            if (criteria.getTierLabel() != null) {
                specification = specification.and(buildStringSpecification(criteria.getTierLabel(), PricingRule_.tierLabel));
            }
            if (criteria.getMinKg() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getMinKg(), PricingRule_.minKg));
            }
            if (criteria.getMaxKg() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getMaxKg(), PricingRule_.maxKg));
            }
            if (criteria.getUnitPrice() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getUnitPrice(), PricingRule_.unitPrice));
            }
            if (criteria.getSurchargeAmount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getSurchargeAmount(), PricingRule_.surchargeAmount));
            }
            if (criteria.getDimDivisor() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getDimDivisor(), PricingRule_.dimDivisor));
            }
            if (criteria.getKmMin() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getKmMin(), PricingRule_.kmMin));
            }
            if (criteria.getKmRate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getKmRate(), PricingRule_.kmRate));
            }
            if (criteria.getStepGram() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getStepGram(), PricingRule_.stepGram));
            }
            if (criteria.getAddFeeAmount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getAddFeeAmount(), PricingRule_.addFeeAmount));
            }
            if (criteria.getEffectiveFrom() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getEffectiveFrom(), PricingRule_.effectiveFrom));
            }
            if (criteria.getEffectiveTo() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getEffectiveTo(), PricingRule_.effectiveTo));
            }
            if (criteria.getActive() != null) {
                specification = specification.and(buildSpecification(criteria.getActive(), PricingRule_.active));
            }
            if (criteria.getRouteId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getRouteId(), root -> root.join(PricingRule_.route, JoinType.LEFT).get(Route_.id))
                );
            }
        }
        return specification;
    }
}
