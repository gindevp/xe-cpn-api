package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.criteria.ShipmentOrderCriteria;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import com.mycompany.myapp.service.mapper.ShipmentOrderMapper;
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
 * Service for executing complex queries for {@link ShipmentOrder} entities in the database.
 * The main input is a {@link ShipmentOrderCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ShipmentOrderDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ShipmentOrderQueryService extends QueryService<ShipmentOrder> {

    private static final Logger LOG = LoggerFactory.getLogger(ShipmentOrderQueryService.class);

    private final ShipmentOrderRepository shipmentOrderRepository;

    private final ShipmentOrderMapper shipmentOrderMapper;

    public ShipmentOrderQueryService(ShipmentOrderRepository shipmentOrderRepository, ShipmentOrderMapper shipmentOrderMapper) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.shipmentOrderMapper = shipmentOrderMapper;
    }

    /**
     * Return a {@link Page} of {@link ShipmentOrderDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ShipmentOrderDTO> findByCriteria(ShipmentOrderCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ShipmentOrder> specification = createSpecification(criteria);
        return shipmentOrderRepository.findAll(specification, page).map(shipmentOrderMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ShipmentOrderCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ShipmentOrder> specification = createSpecification(criteria);
        return shipmentOrderRepository.count(specification);
    }

    /**
     * Function to convert {@link ShipmentOrderCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ShipmentOrder> createSpecification(ShipmentOrderCriteria criteria) {
        Specification<ShipmentOrder> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), ShipmentOrder_.id));
            }
            if (criteria.getOrderCode() != null) {
                specification = specification.and(buildStringSpecification(criteria.getOrderCode(), ShipmentOrder_.orderCode));
            }
            if (criteria.getDraftCode() != null) {
                specification = specification.and(buildStringSpecification(criteria.getDraftCode(), ShipmentOrder_.draftCode));
            }
            if (criteria.getStatus() != null) {
                specification = specification.and(buildSpecification(criteria.getStatus(), ShipmentOrder_.status));
            }
            if (criteria.getForwardStage() != null) {
                specification = specification.and(buildSpecification(criteria.getForwardStage(), ShipmentOrder_.forwardStage));
            }
            if (criteria.getReturnStage() != null) {
                specification = specification.and(buildSpecification(criteria.getReturnStage(), ShipmentOrder_.returnStage));
            }
            if (criteria.getPaymentTerm() != null) {
                specification = specification.and(buildSpecification(criteria.getPaymentTerm(), ShipmentOrder_.paymentTerm));
            }
            if (criteria.getGoodsType() != null) {
                specification = specification.and(buildSpecification(criteria.getGoodsType(), ShipmentOrder_.goodsType));
            }
            if (criteria.getServiceType() != null) {
                specification = specification.and(buildSpecification(criteria.getServiceType(), ShipmentOrder_.serviceType));
            }
            if (criteria.getSenderName() != null) {
                specification = specification.and(buildStringSpecification(criteria.getSenderName(), ShipmentOrder_.senderName));
            }
            if (criteria.getSenderPhone() != null) {
                specification = specification.and(buildStringSpecification(criteria.getSenderPhone(), ShipmentOrder_.senderPhone));
            }
            if (criteria.getReceiverName() != null) {
                specification = specification.and(buildStringSpecification(criteria.getReceiverName(), ShipmentOrder_.receiverName));
            }
            if (criteria.getReceiverPhone() != null) {
                specification = specification.and(buildStringSpecification(criteria.getReceiverPhone(), ShipmentOrder_.receiverPhone));
            }
            if (criteria.getDeliveryAddress() != null) {
                specification = specification.and(buildStringSpecification(criteria.getDeliveryAddress(), ShipmentOrder_.deliveryAddress));
            }
            if (criteria.getPickupAddress() != null) {
                specification = specification.and(buildStringSpecification(criteria.getPickupAddress(), ShipmentOrder_.pickupAddress));
            }
            if (criteria.getHomePickup() != null) {
                specification = specification.and(buildSpecification(criteria.getHomePickup(), ShipmentOrder_.homePickup));
            }
            if (criteria.getHomeDelivery() != null) {
                specification = specification.and(buildSpecification(criteria.getHomeDelivery(), ShipmentOrder_.homeDelivery));
            }
            if (criteria.getQrDropOff() != null) {
                specification = specification.and(buildSpecification(criteria.getQrDropOff(), ShipmentOrder_.qrDropOff));
            }
            if (criteria.getPickupStaffUsername() != null) {
                specification = specification.and(
                    buildStringSpecification(criteria.getPickupStaffUsername(), ShipmentOrder_.pickupStaffUsername)
                );
            }
            if (criteria.getPickingAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getPickingAt(), ShipmentOrder_.pickingAt));
            }
            if (criteria.getPickedUpAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getPickedUpAt(), ShipmentOrder_.pickedUpAt));
            }
            if (criteria.getReceiverActualName() != null) {
                specification = specification.and(
                    buildStringSpecification(criteria.getReceiverActualName(), ShipmentOrder_.receiverActualName)
                );
            }
            if (criteria.getReceiverActualPhone() != null) {
                specification = specification.and(
                    buildStringSpecification(criteria.getReceiverActualPhone(), ShipmentOrder_.receiverActualPhone)
                );
            }
            if (criteria.getWeightKg() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getWeightKg(), ShipmentOrder_.weightKg));
            }
            if (criteria.getQuantity() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getQuantity(), ShipmentOrder_.quantity));
            }
            if (criteria.getDimensionsText() != null) {
                specification = specification.and(buildStringSpecification(criteria.getDimensionsText(), ShipmentOrder_.dimensionsText));
            }
            if (criteria.getFareAmount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getFareAmount(), ShipmentOrder_.fareAmount));
            }
            if (criteria.getPickupFeeAmount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getPickupFeeAmount(), ShipmentOrder_.pickupFeeAmount));
            }
            if (criteria.getDeliveryFeeAmount() != null) {
                specification = specification.and(
                    buildRangeSpecification(criteria.getDeliveryFeeAmount(), ShipmentOrder_.deliveryFeeAmount)
                );
            }
            if (criteria.getPartnerFeeAmount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getPartnerFeeAmount(), ShipmentOrder_.partnerFeeAmount));
            }
            if (criteria.getPaidAmount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getPaidAmount(), ShipmentOrder_.paidAmount));
            }
            if (criteria.getShelfNumber() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getShelfNumber(), ShipmentOrder_.shelfNumber));
            }
            if (criteria.getCancelReason() != null) {
                specification = specification.and(buildStringSpecification(criteria.getCancelReason(), ShipmentOrder_.cancelReason));
            }
            if (criteria.getLabelPrintedAt() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getLabelPrintedAt(), ShipmentOrder_.labelPrintedAt));
            }
            if (criteria.getLabelReprintCount() != null) {
                specification = specification.and(
                    buildRangeSpecification(criteria.getLabelReprintCount(), ShipmentOrder_.labelReprintCount)
                );
            }
            if (criteria.getFailCount() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getFailCount(), ShipmentOrder_.failCount));
            }
            if (criteria.getPartnerCode() != null) {
                specification = specification.and(buildStringSpecification(criteria.getPartnerCode(), ShipmentOrder_.partnerCode));
            }
            if (criteria.getPaymentPercent() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getPaymentPercent(), ShipmentOrder_.paymentPercent));
            }
            if (criteria.getPublicTrackingAllowed() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getPublicTrackingAllowed(), ShipmentOrder_.publicTrackingAllowed)
                );
            }
            if (criteria.getIssueId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getIssueId(), root -> root.join(ShipmentOrder_.issue, JoinType.LEFT).get(OrderIssue_.id))
                );
            }
            if (criteria.getReturnRequestId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getReturnRequestId(), root ->
                        root.join(ShipmentOrder_.returnRequest, JoinType.LEFT).get(OrderReturnRequest_.id)
                    )
                );
            }
            if (criteria.getFareAdjustmentRequestId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getFareAdjustmentRequestId(), root ->
                        root.join(ShipmentOrder_.fareAdjustmentRequest, JoinType.LEFT).get(OrderFareAdjustmentRequest_.id)
                    )
                );
            }
            if (criteria.getSenderCustomerId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getSenderCustomerId(), root ->
                        root.join(ShipmentOrder_.senderCustomer, JoinType.LEFT).get(Customer_.id)
                    )
                );
            }
            if (criteria.getFromOfficeId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getFromOfficeId(), root ->
                        root.join(ShipmentOrder_.fromOffice, JoinType.LEFT).get(Office_.id)
                    )
                );
            }
            if (criteria.getToOfficeId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getToOfficeId(), root -> root.join(ShipmentOrder_.toOffice, JoinType.LEFT).get(Office_.id))
                );
            }
            if (criteria.getHubOfficeId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getHubOfficeId(), root -> root.join(ShipmentOrder_.hubOffice, JoinType.LEFT).get(Office_.id)
                    )
                );
            }
            if (criteria.getFinalToOfficeId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getFinalToOfficeId(), root ->
                        root.join(ShipmentOrder_.finalToOffice, JoinType.LEFT).get(Office_.id)
                    )
                );
            }
            if (criteria.getCurrentTripId() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getCurrentTripId(), root ->
                        root.join(ShipmentOrder_.currentTrip, JoinType.LEFT).get(Trip_.id)
                    )
                );
            }
        }
        return specification;
    }
}
