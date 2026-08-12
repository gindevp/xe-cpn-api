package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.OrderFareAdjustmentRequest;
import com.mycompany.myapp.repository.OrderFareAdjustmentRequestRepository;
import com.mycompany.myapp.service.dto.OrderFareAdjustmentRequestDTO;
import com.mycompany.myapp.service.mapper.OrderFareAdjustmentRequestMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.OrderFareAdjustmentRequest}.
 */
@Service
@Transactional
public class OrderFareAdjustmentRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderFareAdjustmentRequestService.class);

    private final OrderFareAdjustmentRequestRepository orderFareAdjustmentRequestRepository;

    private final OrderFareAdjustmentRequestMapper orderFareAdjustmentRequestMapper;

    public OrderFareAdjustmentRequestService(
        OrderFareAdjustmentRequestRepository orderFareAdjustmentRequestRepository,
        OrderFareAdjustmentRequestMapper orderFareAdjustmentRequestMapper
    ) {
        this.orderFareAdjustmentRequestRepository = orderFareAdjustmentRequestRepository;
        this.orderFareAdjustmentRequestMapper = orderFareAdjustmentRequestMapper;
    }

    /**
     * Save a orderFareAdjustmentRequest.
     *
     * @param orderFareAdjustmentRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderFareAdjustmentRequestDTO save(OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO) {
        LOG.debug("Request to save OrderFareAdjustmentRequest : {}", orderFareAdjustmentRequestDTO);
        OrderFareAdjustmentRequest orderFareAdjustmentRequest = orderFareAdjustmentRequestMapper.toEntity(orderFareAdjustmentRequestDTO);
        orderFareAdjustmentRequest = orderFareAdjustmentRequestRepository.save(orderFareAdjustmentRequest);
        return orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);
    }

    /**
     * Update a orderFareAdjustmentRequest.
     *
     * @param orderFareAdjustmentRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderFareAdjustmentRequestDTO update(OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO) {
        LOG.debug("Request to update OrderFareAdjustmentRequest : {}", orderFareAdjustmentRequestDTO);
        OrderFareAdjustmentRequest orderFareAdjustmentRequest = orderFareAdjustmentRequestMapper.toEntity(orderFareAdjustmentRequestDTO);
        orderFareAdjustmentRequest = orderFareAdjustmentRequestRepository.save(orderFareAdjustmentRequest);
        return orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);
    }

    /**
     * Partially update a orderFareAdjustmentRequest.
     *
     * @param orderFareAdjustmentRequestDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderFareAdjustmentRequestDTO> partialUpdate(OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO) {
        LOG.debug("Request to partially update OrderFareAdjustmentRequest : {}", orderFareAdjustmentRequestDTO);

        return orderFareAdjustmentRequestRepository
            .findById(orderFareAdjustmentRequestDTO.getId())
            .map(existingOrderFareAdjustmentRequest -> {
                orderFareAdjustmentRequestMapper.partialUpdate(existingOrderFareAdjustmentRequest, orderFareAdjustmentRequestDTO);

                return existingOrderFareAdjustmentRequest;
            })
            .map(orderFareAdjustmentRequestRepository::save)
            .map(orderFareAdjustmentRequestMapper::toDto);
    }

    /**
     * Get all the orderFareAdjustmentRequests.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderFareAdjustmentRequestDTO> findAll() {
        LOG.debug("Request to get all OrderFareAdjustmentRequests");
        return orderFareAdjustmentRequestRepository
            .findAll()
            .stream()
            .map(orderFareAdjustmentRequestMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the orderFareAdjustmentRequests where Order is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderFareAdjustmentRequestDTO> findAllWhereOrderIsNull() {
        LOG.debug("Request to get all orderFareAdjustmentRequests where Order is null");
        return StreamSupport.stream(orderFareAdjustmentRequestRepository.findAll().spliterator(), false)
            .filter(orderFareAdjustmentRequest -> orderFareAdjustmentRequest.getOrder() == null)
            .map(orderFareAdjustmentRequestMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one orderFareAdjustmentRequest by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderFareAdjustmentRequestDTO> findOne(Long id) {
        LOG.debug("Request to get OrderFareAdjustmentRequest : {}", id);
        return orderFareAdjustmentRequestRepository.findById(id).map(orderFareAdjustmentRequestMapper::toDto);
    }

    /**
     * Delete the orderFareAdjustmentRequest by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete OrderFareAdjustmentRequest : {}", id);
        orderFareAdjustmentRequestRepository.deleteById(id);
    }
}
