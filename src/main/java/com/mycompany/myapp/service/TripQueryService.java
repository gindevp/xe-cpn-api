package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.service.criteria.TripCriteria;
import com.mycompany.myapp.service.dto.TripDTO;
import com.mycompany.myapp.service.mapper.TripMapper;
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
 * Service for executing complex queries for {@link Trip} entities in the database.
 * The main input is a {@link TripCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link TripDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class TripQueryService extends QueryService<Trip> {

    private static final Logger LOG = LoggerFactory.getLogger(TripQueryService.class);

    private final TripRepository tripRepository;

    private final TripMapper tripMapper;

    public TripQueryService(TripRepository tripRepository, TripMapper tripMapper) {
        this.tripRepository = tripRepository;
        this.tripMapper = tripMapper;
    }

    /**
     * Return a {@link Page} of {@link TripDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<TripDTO> findByCriteria(TripCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Trip> specification = createSpecification(criteria);
        return tripRepository.findAll(specification, page).map(tripMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(TripCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Trip> specification = createSpecification(criteria);
        return tripRepository.count(specification);
    }

    /**
     * Function to convert {@link TripCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Trip> createSpecification(TripCriteria criteria) {
        Specification<Trip> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), Trip_.id));
            }
            if (criteria.getTripCode() != null) {
                specification = specification.and(buildStringSpecification(criteria.getTripCode(), Trip_.tripCode));
            }
            if (criteria.getStatus() != null) {
                specification = specification.and(buildSpecification(criteria.getStatus(), Trip_.status));
            }
            if (criteria.getDepartAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getDepartAt(), Trip_.departAt));
            }
            if (criteria.getLoadedCount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getLoadedCount(), Trip_.loadedCount));
            }
            if (criteria.getScannedCount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getScannedCount(), Trip_.scannedCount));
            }
            if (criteria.getClosedAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getClosedAt(), Trip_.closedAt));
            }
            if (criteria.getForceClosed() != null) {
                specification = specification.and(buildSpecification(criteria.getForceClosed(), Trip_.forceClosed));
            }
            if (criteria.getForceCloseReason() != null) {
                specification = specification.and(buildStringSpecification(criteria.getForceCloseReason(), Trip_.forceCloseReason));
            }
            if (criteria.getOfficeId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getOfficeId(), root -> root.join(Trip_.office, JoinType.LEFT).get(Office_.id))
                );
            }
            if (criteria.getRouteId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getRouteId(), root -> root.join(Trip_.route, JoinType.LEFT).get(Route_.id))
                );
            }
            if (criteria.getVehicleId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getVehicleId(), root -> root.join(Trip_.vehicle, JoinType.LEFT).get(Vehicle_.id))
                );
            }
            if (criteria.getDriverId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getDriverId(), root -> root.join(Trip_.driver, JoinType.LEFT).get(Driver_.id))
                );
            }
        }
        return specification;
    }
}
