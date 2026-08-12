package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.mapper.OrderEventMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.OrderEvent}.
 */
@Service
@Transactional
public class OrderEventService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderEventService.class);

    private final OrderEventRepository orderEventRepository;

    private final OrderEventMapper orderEventMapper;

    public OrderEventService(OrderEventRepository orderEventRepository, OrderEventMapper orderEventMapper) {
        this.orderEventRepository = orderEventRepository;
        this.orderEventMapper = orderEventMapper;
    }

    /**
     * Save a orderEvent.
     *
     * @param orderEventDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderEventDTO save(OrderEventDTO orderEventDTO) {
        LOG.debug("Request to save OrderEvent : {}", orderEventDTO);
        OrderEvent orderEvent = orderEventMapper.toEntity(orderEventDTO);
        orderEvent = orderEventRepository.save(orderEvent);
        return orderEventMapper.toDto(orderEvent);
    }

    /**
     * Update a orderEvent.
     *
     * @param orderEventDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderEventDTO update(OrderEventDTO orderEventDTO) {
        LOG.debug("Request to update OrderEvent : {}", orderEventDTO);
        OrderEvent orderEvent = orderEventMapper.toEntity(orderEventDTO);
        orderEvent = orderEventRepository.save(orderEvent);
        return orderEventMapper.toDto(orderEvent);
    }

    /**
     * Partially update a orderEvent.
     *
     * @param orderEventDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderEventDTO> partialUpdate(OrderEventDTO orderEventDTO) {
        LOG.debug("Request to partially update OrderEvent : {}", orderEventDTO);

        return orderEventRepository
            .findById(orderEventDTO.getId())
            .map(existingOrderEvent -> {
                orderEventMapper.partialUpdate(existingOrderEvent, orderEventDTO);

                return existingOrderEvent;
            })
            .map(orderEventRepository::save)
            .map(orderEventMapper::toDto);
    }

    /**
     * Get all the orderEvents.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderEventDTO> findAll() {
        LOG.debug("Request to get all OrderEvents");
        return orderEventRepository.findAll().stream().map(orderEventMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the orderEvents with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OrderEventDTO> findAllWithEagerRelationships(Pageable pageable) {
        return orderEventRepository.findAllWithEagerRelationships(pageable).map(orderEventMapper::toDto);
    }

    /**
     * Get one orderEvent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderEventDTO> findOne(Long id) {
        LOG.debug("Request to get OrderEvent : {}", id);
        return orderEventRepository.findOneWithEagerRelationships(id).map(orderEventMapper::toDto);
    }

    /**
     * Delete the orderEvent by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete OrderEvent : {}", id);
        orderEventRepository.deleteById(id);
    }
}
