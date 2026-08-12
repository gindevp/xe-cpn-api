package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.service.dto.OrderPaymentDTO;
import com.mycompany.myapp.service.mapper.OrderPaymentMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.OrderPayment}.
 */
@Service
@Transactional
public class OrderPaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderPaymentService.class);

    private final OrderPaymentRepository orderPaymentRepository;

    private final OrderPaymentMapper orderPaymentMapper;

    public OrderPaymentService(OrderPaymentRepository orderPaymentRepository, OrderPaymentMapper orderPaymentMapper) {
        this.orderPaymentRepository = orderPaymentRepository;
        this.orderPaymentMapper = orderPaymentMapper;
    }

    /**
     * Save a orderPayment.
     *
     * @param orderPaymentDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderPaymentDTO save(OrderPaymentDTO orderPaymentDTO) {
        LOG.debug("Request to save OrderPayment : {}", orderPaymentDTO);
        OrderPayment orderPayment = orderPaymentMapper.toEntity(orderPaymentDTO);
        orderPayment = orderPaymentRepository.save(orderPayment);
        return orderPaymentMapper.toDto(orderPayment);
    }

    /**
     * Update a orderPayment.
     *
     * @param orderPaymentDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderPaymentDTO update(OrderPaymentDTO orderPaymentDTO) {
        LOG.debug("Request to update OrderPayment : {}", orderPaymentDTO);
        OrderPayment orderPayment = orderPaymentMapper.toEntity(orderPaymentDTO);
        orderPayment = orderPaymentRepository.save(orderPayment);
        return orderPaymentMapper.toDto(orderPayment);
    }

    /**
     * Partially update a orderPayment.
     *
     * @param orderPaymentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderPaymentDTO> partialUpdate(OrderPaymentDTO orderPaymentDTO) {
        LOG.debug("Request to partially update OrderPayment : {}", orderPaymentDTO);

        return orderPaymentRepository
            .findById(orderPaymentDTO.getId())
            .map(existingOrderPayment -> {
                orderPaymentMapper.partialUpdate(existingOrderPayment, orderPaymentDTO);

                return existingOrderPayment;
            })
            .map(orderPaymentRepository::save)
            .map(orderPaymentMapper::toDto);
    }

    /**
     * Get all the orderPayments.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderPaymentDTO> findAll() {
        LOG.debug("Request to get all OrderPayments");
        return orderPaymentRepository.findAll().stream().map(orderPaymentMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the orderPayments with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OrderPaymentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return orderPaymentRepository.findAllWithEagerRelationships(pageable).map(orderPaymentMapper::toDto);
    }

    /**
     * Get one orderPayment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderPaymentDTO> findOne(Long id) {
        LOG.debug("Request to get OrderPayment : {}", id);
        return orderPaymentRepository.findOneWithEagerRelationships(id).map(orderPaymentMapper::toDto);
    }

    /**
     * Delete the orderPayment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete OrderPayment : {}", id);
        orderPaymentRepository.deleteById(id);
    }
}
