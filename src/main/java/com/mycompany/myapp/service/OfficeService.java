package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.mapper.OfficeMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Office}.
 */
@Service
@Transactional
public class OfficeService {

    private static final Logger LOG = LoggerFactory.getLogger(OfficeService.class);

    private final OfficeRepository officeRepository;

    private final OfficeMapper officeMapper;

    public OfficeService(OfficeRepository officeRepository, OfficeMapper officeMapper) {
        this.officeRepository = officeRepository;
        this.officeMapper = officeMapper;
    }

    /**
     * Save a office.
     *
     * @param officeDTO the entity to save.
     * @return the persisted entity.
     */
    public OfficeDTO save(OfficeDTO officeDTO) {
        LOG.debug("Request to save Office : {}", officeDTO);
        Office office = officeMapper.toEntity(officeDTO);
        office = officeRepository.save(office);
        return officeMapper.toDto(office);
    }

    /**
     * Update a office.
     *
     * @param officeDTO the entity to save.
     * @return the persisted entity.
     */
    public OfficeDTO update(OfficeDTO officeDTO) {
        LOG.debug("Request to update Office : {}", officeDTO);
        Office office = officeMapper.toEntity(officeDTO);
        office = officeRepository.save(office);
        return officeMapper.toDto(office);
    }

    /**
     * Partially update a office.
     *
     * @param officeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OfficeDTO> partialUpdate(OfficeDTO officeDTO) {
        LOG.debug("Request to partially update Office : {}", officeDTO);

        return officeRepository
            .findById(officeDTO.getId())
            .map(existingOffice -> {
                officeMapper.partialUpdate(existingOffice, officeDTO);

                return existingOffice;
            })
            .map(officeRepository::save)
            .map(officeMapper::toDto);
    }

    /**
     * Get all the offices.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OfficeDTO> findAll() {
        LOG.debug("Request to get all Offices");
        return officeRepository.findAll().stream().map(officeMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one office by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OfficeDTO> findOne(Long id) {
        LOG.debug("Request to get Office : {}", id);
        return officeRepository.findById(id).map(officeMapper::toDto);
    }

    /**
     * Delete the office by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Office : {}", id);
        officeRepository.deleteById(id);
    }
}
