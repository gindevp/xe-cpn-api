package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.service.criteria.OrderIssueCriteria;
import com.mycompany.myapp.service.dto.OrderIssueDTO;
import com.mycompany.myapp.service.mapper.OrderIssueMapper;
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
 * Service for executing complex queries for {@link OrderIssue} entities in the database.
 * The main input is a {@link OrderIssueCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link OrderIssueDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class OrderIssueQueryService extends QueryService<OrderIssue> {

    private static final Logger LOG = LoggerFactory.getLogger(OrderIssueQueryService.class);

    private final OrderIssueRepository orderIssueRepository;

    private final OrderIssueMapper orderIssueMapper;

    public OrderIssueQueryService(OrderIssueRepository orderIssueRepository, OrderIssueMapper orderIssueMapper) {
        this.orderIssueRepository = orderIssueRepository;
        this.orderIssueMapper = orderIssueMapper;
    }

    /**
     * Return a {@link Page} of {@link OrderIssueDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<OrderIssueDTO> findByCriteria(OrderIssueCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<OrderIssue> specification = createSpecification(criteria);
        return orderIssueRepository.findAll(specification, page).map(orderIssueMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(OrderIssueCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<OrderIssue> specification = createSpecification(criteria);
        return orderIssueRepository.count(specification);
    }

    /**
     * Function to convert {@link OrderIssueCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<OrderIssue> createSpecification(OrderIssueCriteria criteria) {
        Specification<OrderIssue> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), OrderIssue_.id));
            }
            if (criteria.getIssueType() != null) {
                specification = specification.and(buildSpecification(criteria.getIssueType(), OrderIssue_.issueType));
            }
            if (criteria.getIssueStatus() != null) {
                specification = specification.and(buildSpecification(criteria.getIssueStatus(), OrderIssue_.issueStatus));
            }
            if (criteria.getReason() != null) {
                specification = specification.and(buildStringSpecification(criteria.getReason(), OrderIssue_.reason));
            }
            if (criteria.getOpenedAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getOpenedAt(), OrderIssue_.openedAt));
            }
            if (criteria.getOpenedByUsername() != null) {
                specification = specification.and(buildStringSpecification(criteria.getOpenedByUsername(), OrderIssue_.openedByUsername));
            }
            if (criteria.getResolvedAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getResolvedAt(), OrderIssue_.resolvedAt));
            }
            if (criteria.getResolvedByUsername() != null) {
                specification = specification.and(
                    buildStringSpecification(criteria.getResolvedByUsername(), OrderIssue_.resolvedByUsername)
                );
            }
            if (criteria.getResolutionNote() != null) {
                specification = specification.and(buildStringSpecification(criteria.getResolutionNote(), OrderIssue_.resolutionNote));
            }
            if (criteria.getOrderId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getOrderId(), root -> root.join(OrderIssue_.order, JoinType.LEFT).get(ShipmentOrder_.id))
                );
            }
        }
        return specification;
    }
}
