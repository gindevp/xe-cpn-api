package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.service.criteria.ReceiptCriteria;
import com.mycompany.myapp.service.dto.ReceiptDTO;
import com.mycompany.myapp.service.mapper.ReceiptMapper;
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
 * Service for executing complex queries for {@link Receipt} entities in the database.
 * The main input is a {@link ReceiptCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ReceiptDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ReceiptQueryService extends QueryService<Receipt> {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiptQueryService.class);

    private final ReceiptRepository receiptRepository;

    private final ReceiptMapper receiptMapper;

    public ReceiptQueryService(ReceiptRepository receiptRepository, ReceiptMapper receiptMapper) {
        this.receiptRepository = receiptRepository;
        this.receiptMapper = receiptMapper;
    }

    /**
     * Return a {@link Page} of {@link ReceiptDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ReceiptDTO> findByCriteria(ReceiptCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Receipt> specification = createSpecification(criteria);
        return receiptRepository.findAll(specification, page).map(receiptMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ReceiptCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Receipt> specification = createSpecification(criteria);
        return receiptRepository.count(specification);
    }

    /**
     * Function to convert {@link ReceiptCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Receipt> createSpecification(ReceiptCriteria criteria) {
        Specification<Receipt> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), Receipt_.id));
            }
            if (criteria.getReceiptCode() != null) {
                specification = specification.and(buildStringSpecification(criteria.getReceiptCode(), Receipt_.receiptCode));
            }
            if (criteria.getPayerName() != null) {
                specification = specification.and(buildStringSpecification(criteria.getPayerName(), Receipt_.payerName));
            }
            if (criteria.getPayerCode() != null) {
                specification = specification.and(buildStringSpecification(criteria.getPayerCode(), Receipt_.payerCode));
            }
            if (criteria.getTotalAmount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getTotalAmount(), Receipt_.totalAmount));
            }
            if (criteria.getCreatedAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getCreatedAt(), Receipt_.createdAt));
            }
            if (criteria.getCreatedByUsername() != null) {
                specification = specification.and(buildStringSpecification(criteria.getCreatedByUsername(), Receipt_.createdByUsername));
            }
            if (criteria.getOfficeId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getOfficeId(), root -> root.join(Receipt_.office, JoinType.LEFT).get(Office_.id))
                );
            }
        }
        return specification;
    }
}
