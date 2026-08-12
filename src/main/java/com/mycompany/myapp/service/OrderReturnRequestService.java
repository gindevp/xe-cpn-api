package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.OrderReturnRequest;
import com.mycompany.myapp.repository.OrderReturnRequestRepository;
import com.mycompany.myapp.service.dto.OrderReturnRequestDTO;
import com.mycompany.myapp.service.mapper.OrderReturnRequestMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.OrderReturnRequest}.
 */
@Service
@Transactional
public class OrderReturnRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderReturnRequestService.class);

    private final OrderReturnRequestRepository orderReturnRequestRepository;

    private final OrderReturnRequestMapper orderReturnRequestMapper;

    public OrderReturnRequestService(
        OrderReturnRequestRepository orderReturnRequestRepository,
        OrderReturnRequestMapper orderReturnRequestMapper
    ) {
        this.orderReturnRequestRepository = orderReturnRequestRepository;
        this.orderReturnRequestMapper = orderReturnRequestMapper;
    }

    /**
     * Save a orderReturnRequest.
     *
     * @param orderReturnRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderReturnRequestDTO save(OrderReturnRequestDTO orderReturnRequestDTO) {
        LOG.debug("Request to save OrderReturnRequest : {}", orderReturnRequestDTO);
        OrderReturnRequest orderReturnRequest = orderReturnRequestMapper.toEntity(orderReturnRequestDTO);
        orderReturnRequest = orderReturnRequestRepository.save(orderReturnRequest);
        return orderReturnRequestMapper.toDto(orderReturnRequest);
    }

    /**
     * Update a orderReturnRequest.
     *
     * @param orderReturnRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderReturnRequestDTO update(OrderReturnRequestDTO orderReturnRequestDTO) {
        LOG.debug("Request to update OrderReturnRequest : {}", orderReturnRequestDTO);
        OrderReturnRequest orderReturnRequest = orderReturnRequestMapper.toEntity(orderReturnRequestDTO);
        orderReturnRequest = orderReturnRequestRepository.save(orderReturnRequest);
        return orderReturnRequestMapper.toDto(orderReturnRequest);
    }

    /**
     * Partially update a orderReturnRequest.
     *
     * @param orderReturnRequestDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderReturnRequestDTO> partialUpdate(OrderReturnRequestDTO orderReturnRequestDTO) {
        LOG.debug("Request to partially update OrderReturnRequest : {}", orderReturnRequestDTO);

        return orderReturnRequestRepository
            .findById(orderReturnRequestDTO.getId())
            .map(existingOrderReturnRequest -> {
                orderReturnRequestMapper.partialUpdate(existingOrderReturnRequest, orderReturnRequestDTO);

                return existingOrderReturnRequest;
            })
            .map(orderReturnRequestRepository::save)
            .map(orderReturnRequestMapper::toDto);
    }

    /**
     * Get all the orderReturnRequests.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderReturnRequestDTO> findAll() {
        LOG.debug("Request to get all OrderReturnRequests");
        return orderReturnRequestRepository
            .findAll()
            .stream()
            .map(orderReturnRequestMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the orderReturnRequests where Order is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderReturnRequestDTO> findAllWhereOrderIsNull() {
        LOG.debug("Request to get all orderReturnRequests where Order is null");
        return StreamSupport.stream(orderReturnRequestRepository.findAll().spliterator(), false)
            .filter(orderReturnRequest -> orderReturnRequest.getOrder() == null)
            .map(orderReturnRequestMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one orderReturnRequest by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderReturnRequestDTO> findOne(Long id) {
        LOG.debug("Request to get OrderReturnRequest : {}", id);
        return orderReturnRequestRepository.findById(id).map(orderReturnRequestMapper::toDto);
    }

    /**
     * Delete the orderReturnRequest by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete OrderReturnRequest : {}", id);
        orderReturnRequestRepository.deleteById(id);
    }
}
