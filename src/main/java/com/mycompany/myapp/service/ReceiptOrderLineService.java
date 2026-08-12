package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.ReceiptOrderLine;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.service.dto.ReceiptOrderLineDTO;
import com.mycompany.myapp.service.mapper.ReceiptOrderLineMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.ReceiptOrderLine}.
 */
@Service
@Transactional
public class ReceiptOrderLineService {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiptOrderLineService.class);

    private final ReceiptOrderLineRepository receiptOrderLineRepository;

    private final ReceiptOrderLineMapper receiptOrderLineMapper;

    public ReceiptOrderLineService(ReceiptOrderLineRepository receiptOrderLineRepository, ReceiptOrderLineMapper receiptOrderLineMapper) {
        this.receiptOrderLineRepository = receiptOrderLineRepository;
        this.receiptOrderLineMapper = receiptOrderLineMapper;
    }

    /**
     * Save a receiptOrderLine.
     *
     * @param receiptOrderLineDTO the entity to save.
     * @return the persisted entity.
     */
    public ReceiptOrderLineDTO save(ReceiptOrderLineDTO receiptOrderLineDTO) {
        LOG.debug("Request to save ReceiptOrderLine : {}", receiptOrderLineDTO);
        ReceiptOrderLine receiptOrderLine = receiptOrderLineMapper.toEntity(receiptOrderLineDTO);
        receiptOrderLine = receiptOrderLineRepository.save(receiptOrderLine);
        return receiptOrderLineMapper.toDto(receiptOrderLine);
    }

    /**
     * Update a receiptOrderLine.
     *
     * @param receiptOrderLineDTO the entity to save.
     * @return the persisted entity.
     */
    public ReceiptOrderLineDTO update(ReceiptOrderLineDTO receiptOrderLineDTO) {
        LOG.debug("Request to update ReceiptOrderLine : {}", receiptOrderLineDTO);
        ReceiptOrderLine receiptOrderLine = receiptOrderLineMapper.toEntity(receiptOrderLineDTO);
        receiptOrderLine = receiptOrderLineRepository.save(receiptOrderLine);
        return receiptOrderLineMapper.toDto(receiptOrderLine);
    }

    /**
     * Partially update a receiptOrderLine.
     *
     * @param receiptOrderLineDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ReceiptOrderLineDTO> partialUpdate(ReceiptOrderLineDTO receiptOrderLineDTO) {
        LOG.debug("Request to partially update ReceiptOrderLine : {}", receiptOrderLineDTO);

        return receiptOrderLineRepository
            .findById(receiptOrderLineDTO.getId())
            .map(existingReceiptOrderLine -> {
                receiptOrderLineMapper.partialUpdate(existingReceiptOrderLine, receiptOrderLineDTO);

                return existingReceiptOrderLine;
            })
            .map(receiptOrderLineRepository::save)
            .map(receiptOrderLineMapper::toDto);
    }

    /**
     * Get all the receiptOrderLines.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ReceiptOrderLineDTO> findAll() {
        LOG.debug("Request to get all ReceiptOrderLines");
        return receiptOrderLineRepository
            .findAll()
            .stream()
            .map(receiptOrderLineMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the receiptOrderLines with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ReceiptOrderLineDTO> findAllWithEagerRelationships(Pageable pageable) {
        return receiptOrderLineRepository.findAllWithEagerRelationships(pageable).map(receiptOrderLineMapper::toDto);
    }

    /**
     * Get one receiptOrderLine by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ReceiptOrderLineDTO> findOne(Long id) {
        LOG.debug("Request to get ReceiptOrderLine : {}", id);
        return receiptOrderLineRepository.findOneWithEagerRelationships(id).map(receiptOrderLineMapper::toDto);
    }

    /**
     * Delete the receiptOrderLine by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete ReceiptOrderLine : {}", id);
        receiptOrderLineRepository.deleteById(id);
    }
}
