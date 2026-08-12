package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import com.mycompany.myapp.service.mapper.ShipmentOrderMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.ShipmentOrder}.
 */
@Service
@Transactional
public class ShipmentOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ShipmentOrderService.class);

    private final ShipmentOrderRepository shipmentOrderRepository;

    private final ShipmentOrderMapper shipmentOrderMapper;

    private final DayClosureGuard dayClosureGuard;

    public ShipmentOrderService(
        ShipmentOrderRepository shipmentOrderRepository,
        ShipmentOrderMapper shipmentOrderMapper,
        DayClosureGuard dayClosureGuard
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.shipmentOrderMapper = shipmentOrderMapper;
        this.dayClosureGuard = dayClosureGuard;
    }

    /**
     * Save a shipmentOrder.
     *
     * @param shipmentOrderDTO the entity to save.
     * @return the persisted entity.
     */
    public ShipmentOrderDTO save(ShipmentOrderDTO shipmentOrderDTO) {
        LOG.debug("Request to save ShipmentOrder : {}", shipmentOrderDTO);
        ShipmentOrder shipmentOrder = shipmentOrderMapper.toEntity(shipmentOrderDTO);
        shipmentOrder = shipmentOrderRepository.save(shipmentOrder);
        return shipmentOrderMapper.toDto(shipmentOrder);
    }

    /**
     * Update a shipmentOrder.
     *
     * @param shipmentOrderDTO the entity to save.
     * @return the persisted entity.
     */
    public ShipmentOrderDTO update(ShipmentOrderDTO shipmentOrderDTO) {
        LOG.debug("Request to update ShipmentOrder : {}", shipmentOrderDTO);
        if (shipmentOrderDTO.getId() != null) {
            shipmentOrderRepository.findById(shipmentOrderDTO.getId()).ifPresent(dayClosureGuard::assertOrderMutable);
        }
        ShipmentOrder shipmentOrder = shipmentOrderMapper.toEntity(shipmentOrderDTO);
        shipmentOrder = shipmentOrderRepository.save(shipmentOrder);
        return shipmentOrderMapper.toDto(shipmentOrder);
    }

    /**
     * Partially update a shipmentOrder.
     *
     * @param shipmentOrderDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ShipmentOrderDTO> partialUpdate(ShipmentOrderDTO shipmentOrderDTO) {
        LOG.debug("Request to partially update ShipmentOrder : {}", shipmentOrderDTO);

        return shipmentOrderRepository
            .findById(shipmentOrderDTO.getId())
            .map(existingShipmentOrder -> {
                dayClosureGuard.assertOrderMutable(existingShipmentOrder);
                shipmentOrderMapper.partialUpdate(existingShipmentOrder, shipmentOrderDTO);

                return existingShipmentOrder;
            })
            .map(shipmentOrderRepository::save)
            .map(shipmentOrderMapper::toDto);
    }

    /**
     * Get all the shipmentOrders with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ShipmentOrderDTO> findAllWithEagerRelationships(Pageable pageable) {
        return shipmentOrderRepository.findAllWithEagerRelationships(pageable).map(shipmentOrderMapper::toDto);
    }

    /**
     * Get one shipmentOrder by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ShipmentOrderDTO> findOne(Long id) {
        LOG.debug("Request to get ShipmentOrder : {}", id);
        return shipmentOrderRepository.findOneWithEagerRelationships(id).map(shipmentOrderMapper::toDto);
    }

    /**
     * Delete the shipmentOrder by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete ShipmentOrder : {}", id);
        shipmentOrderRepository.deleteById(id);
    }
}
