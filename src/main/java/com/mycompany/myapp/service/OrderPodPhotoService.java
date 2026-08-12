package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.OrderPodPhoto;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.service.dto.OrderPodPhotoDTO;
import com.mycompany.myapp.service.mapper.OrderPodPhotoMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.OrderPodPhoto}.
 */
@Service
@Transactional
public class OrderPodPhotoService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderPodPhotoService.class);

    private final OrderPodPhotoRepository orderPodPhotoRepository;

    private final OrderPodPhotoMapper orderPodPhotoMapper;

    public OrderPodPhotoService(OrderPodPhotoRepository orderPodPhotoRepository, OrderPodPhotoMapper orderPodPhotoMapper) {
        this.orderPodPhotoRepository = orderPodPhotoRepository;
        this.orderPodPhotoMapper = orderPodPhotoMapper;
    }

    /**
     * Save a orderPodPhoto.
     *
     * @param orderPodPhotoDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderPodPhotoDTO save(OrderPodPhotoDTO orderPodPhotoDTO) {
        LOG.debug("Request to save OrderPodPhoto : {}", orderPodPhotoDTO);
        OrderPodPhoto orderPodPhoto = orderPodPhotoMapper.toEntity(orderPodPhotoDTO);
        orderPodPhoto = orderPodPhotoRepository.save(orderPodPhoto);
        return orderPodPhotoMapper.toDto(orderPodPhoto);
    }

    /**
     * Update a orderPodPhoto.
     *
     * @param orderPodPhotoDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderPodPhotoDTO update(OrderPodPhotoDTO orderPodPhotoDTO) {
        LOG.debug("Request to update OrderPodPhoto : {}", orderPodPhotoDTO);
        OrderPodPhoto orderPodPhoto = orderPodPhotoMapper.toEntity(orderPodPhotoDTO);
        orderPodPhoto = orderPodPhotoRepository.save(orderPodPhoto);
        return orderPodPhotoMapper.toDto(orderPodPhoto);
    }

    /**
     * Partially update a orderPodPhoto.
     *
     * @param orderPodPhotoDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderPodPhotoDTO> partialUpdate(OrderPodPhotoDTO orderPodPhotoDTO) {
        LOG.debug("Request to partially update OrderPodPhoto : {}", orderPodPhotoDTO);

        return orderPodPhotoRepository
            .findById(orderPodPhotoDTO.getId())
            .map(existingOrderPodPhoto -> {
                orderPodPhotoMapper.partialUpdate(existingOrderPodPhoto, orderPodPhotoDTO);

                return existingOrderPodPhoto;
            })
            .map(orderPodPhotoRepository::save)
            .map(orderPodPhotoMapper::toDto);
    }

    /**
     * Get all the orderPodPhotos.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrderPodPhotoDTO> findAll() {
        LOG.debug("Request to get all OrderPodPhotos");
        return orderPodPhotoRepository.findAll().stream().map(orderPodPhotoMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the orderPodPhotos with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OrderPodPhotoDTO> findAllWithEagerRelationships(Pageable pageable) {
        return orderPodPhotoRepository.findAllWithEagerRelationships(pageable).map(orderPodPhotoMapper::toDto);
    }

    /**
     * Get one orderPodPhoto by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderPodPhotoDTO> findOne(Long id) {
        LOG.debug("Request to get OrderPodPhoto : {}", id);
        return orderPodPhotoRepository.findOneWithEagerRelationships(id).map(orderPodPhotoMapper::toDto);
    }

    /**
     * Delete the orderPodPhoto by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete OrderPodPhoto : {}", id);
        orderPodPhotoRepository.deleteById(id);
    }
}
