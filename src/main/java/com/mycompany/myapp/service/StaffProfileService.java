package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.service.dto.StaffProfileDTO;
import com.mycompany.myapp.service.mapper.StaffProfileMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.StaffProfile}.
 */
@Service
@Transactional
public class StaffProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(StaffProfileService.class);

    private final StaffProfileRepository staffProfileRepository;

    private final StaffProfileMapper staffProfileMapper;

    public StaffProfileService(StaffProfileRepository staffProfileRepository, StaffProfileMapper staffProfileMapper) {
        this.staffProfileRepository = staffProfileRepository;
        this.staffProfileMapper = staffProfileMapper;
    }

    /**
     * Save a staffProfile.
     *
     * @param staffProfileDTO the entity to save.
     * @return the persisted entity.
     */
    public StaffProfileDTO save(StaffProfileDTO staffProfileDTO) {
        LOG.debug("Request to save StaffProfile : {}", staffProfileDTO);
        StaffProfile staffProfile = staffProfileMapper.toEntity(staffProfileDTO);
        staffProfile = staffProfileRepository.save(staffProfile);
        return staffProfileMapper.toDto(staffProfile);
    }

    /**
     * Update a staffProfile.
     *
     * @param staffProfileDTO the entity to save.
     * @return the persisted entity.
     */
    public StaffProfileDTO update(StaffProfileDTO staffProfileDTO) {
        LOG.debug("Request to update StaffProfile : {}", staffProfileDTO);
        StaffProfile staffProfile = staffProfileMapper.toEntity(staffProfileDTO);
        staffProfile = staffProfileRepository.save(staffProfile);
        return staffProfileMapper.toDto(staffProfile);
    }

    /**
     * Partially update a staffProfile.
     *
     * @param staffProfileDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<StaffProfileDTO> partialUpdate(StaffProfileDTO staffProfileDTO) {
        LOG.debug("Request to partially update StaffProfile : {}", staffProfileDTO);

        return staffProfileRepository
            .findById(staffProfileDTO.getId())
            .map(existingStaffProfile -> {
                staffProfileMapper.partialUpdate(existingStaffProfile, staffProfileDTO);

                return existingStaffProfile;
            })
            .map(staffProfileRepository::save)
            .map(staffProfileMapper::toDto);
    }

    /**
     * Get all the staffProfiles.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<StaffProfileDTO> findAll() {
        LOG.debug("Request to get all StaffProfiles");
        return staffProfileRepository.findAll().stream().map(staffProfileMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the staffProfiles with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<StaffProfileDTO> findAllWithEagerRelationships(Pageable pageable) {
        return staffProfileRepository.findAllWithEagerRelationships(pageable).map(staffProfileMapper::toDto);
    }

    /**
     * Get one staffProfile by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<StaffProfileDTO> findOne(Long id) {
        LOG.debug("Request to get StaffProfile : {}", id);
        return staffProfileRepository.findOneWithEagerRelationships(id).map(staffProfileMapper::toDto);
    }

    /**
     * Delete the staffProfile by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete StaffProfile : {}", id);
        staffProfileRepository.deleteById(id);
    }
}
