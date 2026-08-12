package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.DayClosure;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.service.criteria.DayClosureCriteria;
import com.mycompany.myapp.service.dto.DayClosureDTO;
import com.mycompany.myapp.service.mapper.DayClosureMapper;
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
 * Service for executing complex queries for {@link DayClosure} entities in the database.
 * The main input is a {@link DayClosureCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DayClosureDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DayClosureQueryService extends QueryService<DayClosure> {

    private static final Logger LOG = LoggerFactory.getLogger(DayClosureQueryService.class);

    private final DayClosureRepository dayClosureRepository;

    private final DayClosureMapper dayClosureMapper;

    public DayClosureQueryService(DayClosureRepository dayClosureRepository, DayClosureMapper dayClosureMapper) {
        this.dayClosureRepository = dayClosureRepository;
        this.dayClosureMapper = dayClosureMapper;
    }

    /**
     * Return a {@link Page} of {@link DayClosureDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DayClosureDTO> findByCriteria(DayClosureCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<DayClosure> specification = createSpecification(criteria);
        return dayClosureRepository.findAll(specification, page).map(dayClosureMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DayClosureCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<DayClosure> specification = createSpecification(criteria);
        return dayClosureRepository.count(specification);
    }

    /**
     * Function to convert {@link DayClosureCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<DayClosure> createSpecification(DayClosureCriteria criteria) {
        Specification<DayClosure> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), DayClosure_.id));
            }
            if (criteria.getBusinessDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getBusinessDate(), DayClosure_.businessDate));
            }
            if (criteria.getStatus() != null) {
                specification = specification.and(buildSpecification(criteria.getStatus(), DayClosure_.status));
            }
            if (criteria.getConfirmedByUsername() != null) {
                specification = specification.and(
                    buildStringSpecification(criteria.getConfirmedByUsername(), DayClosure_.confirmedByUsername)
                );
            }
            if (criteria.getConfirmedAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getConfirmedAt(), DayClosure_.confirmedAt));
            }
            if (criteria.getReopenedByUsername() != null) {
                specification = specification.and(
                    buildStringSpecification(criteria.getReopenedByUsername(), DayClosure_.reopenedByUsername)
                );
            }
            if (criteria.getReopenedAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getReopenedAt(), DayClosure_.reopenedAt));
            }
            if (criteria.getOfficeId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getOfficeId(), root -> root.join(DayClosure_.office, JoinType.LEFT).get(Office_.id))
                );
            }
        }
        return specification;
    }
}
