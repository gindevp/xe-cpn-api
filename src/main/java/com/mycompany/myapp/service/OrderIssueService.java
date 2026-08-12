package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.service.dto.OrderIssueDTO;
import com.mycompany.myapp.service.mapper.OrderIssueMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.OrderIssue}.
 */
@Service
@Transactional
public class OrderIssueService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderIssueService.class);

    private final OrderIssueRepository orderIssueRepository;

    private final OrderIssueMapper orderIssueMapper;

    public OrderIssueService(OrderIssueRepository orderIssueRepository, OrderIssueMapper orderIssueMapper) {
        this.orderIssueRepository = orderIssueRepository;
        this.orderIssueMapper = orderIssueMapper;
    }

    /**
     * Save a orderIssue.
     *
     * @param orderIssueDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderIssueDTO save(OrderIssueDTO orderIssueDTO) {
        LOG.debug("Request to save OrderIssue : {}", orderIssueDTO);
        OrderIssue orderIssue = orderIssueMapper.toEntity(orderIssueDTO);
        orderIssue = orderIssueRepository.save(orderIssue);
        return orderIssueMapper.toDto(orderIssue);
    }

    /**
     * Update a orderIssue.
     *
     * @param orderIssueDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderIssueDTO update(OrderIssueDTO orderIssueDTO) {
        LOG.debug("Request to update OrderIssue : {}", orderIssueDTO);
        OrderIssue orderIssue = orderIssueMapper.toEntity(orderIssueDTO);
        orderIssue = orderIssueRepository.save(orderIssue);
        return orderIssueMapper.toDto(orderIssue);
    }

    /**
     * Partially update a orderIssue.
     *
     * @param orderIssueDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderIssueDTO> partialUpdate(OrderIssueDTO orderIssueDTO) {
        LOG.debug("Request to partially update OrderIssue : {}", orderIssueDTO);

        return orderIssueRepository
            .findById(orderIssueDTO.getId())
            .map(existingOrderIssue -> {
                orderIssueMapper.partialUpdate(existingOrderIssue, orderIssueDTO);

                return existingOrderIssue;
            })
            .map(orderIssueRepository::save)
            .map(orderIssueMapper::toDto);
    }

    /**
     *  Get all the orderIssues where Order is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderIssueDTO> findAllWhereOrderIsNull() {
        LOG.debug("Request to get all orderIssues where Order is null");
        return StreamSupport.stream(orderIssueRepository.findAll().spliterator(), false)
            .filter(orderIssue -> orderIssue.getOrder() == null)
            .map(orderIssueMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one orderIssue by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderIssueDTO> findOne(Long id) {
        LOG.debug("Request to get OrderIssue : {}", id);
        return orderIssueRepository.findById(id).map(orderIssueMapper::toDto);
    }

    /**
     * Delete the orderIssue by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete OrderIssue : {}", id);
        orderIssueRepository.deleteById(id);
    }
}
