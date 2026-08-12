package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.OrderLeg;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.service.dto.OrderLegDTO;
import com.mycompany.myapp.service.mapper.OrderLegMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.OrderLeg}.
 */
@Service
@Transactional
public class OrderLegService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderLegService.class);

    private final OrderLegRepository orderLegRepository;

    private final OrderLegMapper orderLegMapper;

    public OrderLegService(OrderLegRepository orderLegRepository, OrderLegMapper orderLegMapper) {
        this.orderLegRepository = orderLegRepository;
        this.orderLegMapper = orderLegMapper;
    }

    /**
     * Save a orderLeg.
     *
     * @param orderLegDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderLegDTO save(OrderLegDTO orderLegDTO) {
        LOG.debug("Request to save OrderLeg : {}", orderLegDTO);
        OrderLeg orderLeg = orderLegMapper.toEntity(orderLegDTO);
        orderLeg = orderLegRepository.save(orderLeg);
        return orderLegMapper.toDto(orderLeg);
    }

    /**
     * Update a orderLeg.
     *
     * @param orderLegDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderLegDTO update(OrderLegDTO orderLegDTO) {
        LOG.debug("Request to update OrderLeg : {}", orderLegDTO);
        OrderLeg orderLeg = orderLegMapper.toEntity(orderLegDTO);
        orderLeg = orderLegRepository.save(orderLeg);
        return orderLegMapper.toDto(orderLeg);
    }

    /**
     * Partially update a orderLeg.
     *
     * @param orderLegDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderLegDTO> partialUpdate(OrderLegDTO orderLegDTO) {
        LOG.debug("Request to partially update OrderLeg : {}", orderLegDTO);

        return orderLegRepository
            .findById(orderLegDTO.getId())
            .map(existingOrderLeg -> {
                orderLegMapper.partialUpdate(existingOrderLeg, orderLegDTO);

                return existingOrderLeg;
            })
            .map(orderLegRepository::save)
            .map(orderLegMapper::toDto);
    }

    /**
     * Get all the orderLegs.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderLegDTO> findAll() {
        LOG.debug("Request to get all OrderLegs");
        return orderLegRepository.findAll().stream().map(orderLegMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the orderLegs with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OrderLegDTO> findAllWithEagerRelationships(Pageable pageable) {
        return orderLegRepository.findAllWithEagerRelationships(pageable).map(orderLegMapper::toDto);
    }

    /**
     * Get one orderLeg by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderLegDTO> findOne(Long id) {
        LOG.debug("Request to get OrderLeg : {}", id);
        return orderLegRepository.findOneWithEagerRelationships(id).map(orderLegMapper::toDto);
    }

    /**
     * Delete the orderLeg by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete OrderLeg : {}", id);
        orderLegRepository.deleteById(id);
    }
}
