package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.AuditLog;
import com.mycompany.myapp.repository.AuditLogRepository;
import com.mycompany.myapp.service.criteria.AuditLogCriteria;
import com.mycompany.myapp.service.dto.AuditLogDTO;
import com.mycompany.myapp.service.mapper.AuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link AuditLog} entities in the database.
 * The main input is a {@link AuditLogCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link AuditLogDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogQueryService extends QueryService<AuditLog> {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogQueryService.class);

    private final AuditLogRepository auditLogRepository;

    private final AuditLogMapper auditLogMapper;

    public AuditLogQueryService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * Return a {@link Page} of {@link AuditLogDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> findByCriteria(AuditLogCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<AuditLog> specification = createSpecification(criteria);
        return auditLogRepository.findAll(specification, page).map(auditLogMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(AuditLogCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<AuditLog> specification = createSpecification(criteria);
        return auditLogRepository.count(specification);
    }

    /**
     * Function to convert {@link AuditLogCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<AuditLog> createSpecification(AuditLogCriteria criteria) {
        Specification<AuditLog> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), AuditLog_.id));
            }
            if (criteria.getAction() != null) {
                specification = specification.and(buildStringSpecification(criteria.getAction(), AuditLog_.action));
            }
            if (criteria.getEntityType() != null) {
                specification = specification.and(buildStringSpecification(criteria.getEntityType(), AuditLog_.entityType));
            }
            if (criteria.getEntityId() != null) {
                specification = specification.and(buildStringSpecification(criteria.getEntityId(), AuditLog_.entityId));
            }
            if (criteria.getDetail() != null) {
                specification = specification.and(buildStringSpecification(criteria.getDetail(), AuditLog_.detail));
            }
            if (criteria.getActedAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getActedAt(), AuditLog_.actedAt));
            }
            if (criteria.getActedByUsername() != null) {
                specification = specification.and(buildStringSpecification(criteria.getActedByUsername(), AuditLog_.actedByUsername));
            }
        }
        return specification;
    }
}
