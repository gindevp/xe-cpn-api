package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.service.dto.ReceiptDTO;
import com.mycompany.myapp.service.mapper.ReceiptMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Receipt}.
 */
@Service
@Transactional
public class ReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiptService.class);

    private final ReceiptRepository receiptRepository;

    private final ReceiptMapper receiptMapper;

    public ReceiptService(ReceiptRepository receiptRepository, ReceiptMapper receiptMapper) {
        this.receiptRepository = receiptRepository;
        this.receiptMapper = receiptMapper;
    }

    /**
     * Save a receipt.
     *
     * @param receiptDTO the entity to save.
     * @return the persisted entity.
     */
    public ReceiptDTO save(ReceiptDTO receiptDTO) {
        LOG.debug("Request to save Receipt : {}", receiptDTO);
        Receipt receipt = receiptMapper.toEntity(receiptDTO);
        receipt = receiptRepository.save(receipt);
        return receiptMapper.toDto(receipt);
    }

    /**
     * Update a receipt.
     *
     * @param receiptDTO the entity to save.
     * @return the persisted entity.
     */
    public ReceiptDTO update(ReceiptDTO receiptDTO) {
        LOG.debug("Request to update Receipt : {}", receiptDTO);
        Receipt receipt = receiptMapper.toEntity(receiptDTO);
        receipt = receiptRepository.save(receipt);
        return receiptMapper.toDto(receipt);
    }

    /**
     * Partially update a receipt.
     *
     * @param receiptDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ReceiptDTO> partialUpdate(ReceiptDTO receiptDTO) {
        LOG.debug("Request to partially update Receipt : {}", receiptDTO);

        return receiptRepository
            .findById(receiptDTO.getId())
            .map(existingReceipt -> {
                receiptMapper.partialUpdate(existingReceipt, receiptDTO);

                return existingReceipt;
            })
            .map(receiptRepository::save)
            .map(receiptMapper::toDto);
    }

    /**
     * Get all the receipts with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ReceiptDTO> findAllWithEagerRelationships(Pageable pageable) {
        return receiptRepository.findAllWithEagerRelationships(pageable).map(receiptMapper::toDto);
    }

    /**
     * Get one receipt by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ReceiptDTO> findOne(Long id) {
        LOG.debug("Request to get Receipt : {}", id);
        return receiptRepository.findOneWithEagerRelationships(id).map(receiptMapper::toDto);
    }

    /**
     * Delete the receipt by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Receipt : {}", id);
        receiptRepository.deleteById(id);
    }
}
