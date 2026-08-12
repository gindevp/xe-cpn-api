package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.service.dto.OrderDeliveryAttemptDTO;
import com.mycompany.myapp.service.mapper.OrderDeliveryAttemptMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.OrderDeliveryAttempt}.
 */
@Service
@Transactional
public class OrderDeliveryAttemptService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderDeliveryAttemptService.class);

    private final OrderDeliveryAttemptRepository orderDeliveryAttemptRepository;

    private final OrderDeliveryAttemptMapper orderDeliveryAttemptMapper;

    public OrderDeliveryAttemptService(
        OrderDeliveryAttemptRepository orderDeliveryAttemptRepository,
        OrderDeliveryAttemptMapper orderDeliveryAttemptMapper
    ) {
        this.orderDeliveryAttemptRepository = orderDeliveryAttemptRepository;
        this.orderDeliveryAttemptMapper = orderDeliveryAttemptMapper;
    }

    /**
     * Save a orderDeliveryAttempt.
     *
     * @param orderDeliveryAttemptDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderDeliveryAttemptDTO save(OrderDeliveryAttemptDTO orderDeliveryAttemptDTO) {
        LOG.debug("Request to save OrderDeliveryAttempt : {}", orderDeliveryAttemptDTO);
        OrderDeliveryAttempt orderDeliveryAttempt = orderDeliveryAttemptMapper.toEntity(orderDeliveryAttemptDTO);
        orderDeliveryAttempt = orderDeliveryAttemptRepository.save(orderDeliveryAttempt);
        return orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);
    }

    /**
     * Update a orderDeliveryAttempt.
     *
     * @param orderDeliveryAttemptDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderDeliveryAttemptDTO update(OrderDeliveryAttemptDTO orderDeliveryAttemptDTO) {
        LOG.debug("Request to update OrderDeliveryAttempt : {}", orderDeliveryAttemptDTO);
        OrderDeliveryAttempt orderDeliveryAttempt = orderDeliveryAttemptMapper.toEntity(orderDeliveryAttemptDTO);
        orderDeliveryAttempt = orderDeliveryAttemptRepository.save(orderDeliveryAttempt);
        return orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);
    }

    /**
     * Partially update a orderDeliveryAttempt.
     *
     * @param orderDeliveryAttemptDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderDeliveryAttemptDTO> partialUpdate(OrderDeliveryAttemptDTO orderDeliveryAttemptDTO) {
        LOG.debug("Request to partially update OrderDeliveryAttempt : {}", orderDeliveryAttemptDTO);

        return orderDeliveryAttemptRepository
            .findById(orderDeliveryAttemptDTO.getId())
            .map(existingOrderDeliveryAttempt -> {
                orderDeliveryAttemptMapper.partialUpdate(existingOrderDeliveryAttempt, orderDeliveryAttemptDTO);

                return existingOrderDeliveryAttempt;
            })
            .map(orderDeliveryAttemptRepository::save)
            .map(orderDeliveryAttemptMapper::toDto);
    }

    /**
     * Get all the orderDeliveryAttempts.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderDeliveryAttemptDTO> findAll() {
        LOG.debug("Request to get all OrderDeliveryAttempts");
        return orderDeliveryAttemptRepository
            .findAll()
            .stream()
            .map(orderDeliveryAttemptMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the orderDeliveryAttempts with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OrderDeliveryAttemptDTO> findAllWithEagerRelationships(Pageable pageable) {
        return orderDeliveryAttemptRepository.findAllWithEagerRelationships(pageable).map(orderDeliveryAttemptMapper::toDto);
    }

    /**
     * Get one orderDeliveryAttempt by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderDeliveryAttemptDTO> findOne(Long id) {
        LOG.debug("Request to get OrderDeliveryAttempt : {}", id);
        return orderDeliveryAttemptRepository.findOneWithEagerRelationships(id).map(orderDeliveryAttemptMapper::toDto);
    }

    /**
     * Delete the orderDeliveryAttempt by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete OrderDeliveryAttempt : {}", id);
        orderDeliveryAttemptRepository.deleteById(id);
    }
}
