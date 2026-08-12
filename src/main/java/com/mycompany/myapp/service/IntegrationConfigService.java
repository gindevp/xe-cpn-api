package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.service.dto.IntegrationConfigDTO;
import com.mycompany.myapp.service.mapper.IntegrationConfigMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.IntegrationConfig}.
 */
@Service
@Transactional
public class IntegrationConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationConfigService.class);

    private final IntegrationConfigRepository integrationConfigRepository;

    private final IntegrationConfigMapper integrationConfigMapper;

    public IntegrationConfigService(
        IntegrationConfigRepository integrationConfigRepository,
        IntegrationConfigMapper integrationConfigMapper
    ) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.integrationConfigMapper = integrationConfigMapper;
    }

    /**
     * Save a integrationConfig.
     *
     * @param integrationConfigDTO the entity to save.
     * @return the persisted entity.
     */
    public IntegrationConfigDTO save(IntegrationConfigDTO integrationConfigDTO) {
        LOG.debug("Request to save IntegrationConfig : {}", integrationConfigDTO);
        IntegrationConfig integrationConfig = integrationConfigMapper.toEntity(integrationConfigDTO);
        integrationConfig = integrationConfigRepository.save(integrationConfig);
        return integrationConfigMapper.toDto(integrationConfig);
    }

    /**
     * Update a integrationConfig.
     *
     * @param integrationConfigDTO the entity to save.
     * @return the persisted entity.
     */
    public IntegrationConfigDTO update(IntegrationConfigDTO integrationConfigDTO) {
        LOG.debug("Request to update IntegrationConfig : {}", integrationConfigDTO);
        IntegrationConfig integrationConfig = integrationConfigMapper.toEntity(integrationConfigDTO);
        integrationConfig = integrationConfigRepository.save(integrationConfig);
        return integrationConfigMapper.toDto(integrationConfig);
    }

    /**
     * Partially update a integrationConfig.
     *
     * @param integrationConfigDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<IntegrationConfigDTO> partialUpdate(IntegrationConfigDTO integrationConfigDTO) {
        LOG.debug("Request to partially update IntegrationConfig : {}", integrationConfigDTO);

        return integrationConfigRepository
            .findById(integrationConfigDTO.getId())
            .map(existingIntegrationConfig -> {
                integrationConfigMapper.partialUpdate(existingIntegrationConfig, integrationConfigDTO);

                return existingIntegrationConfig;
            })
            .map(integrationConfigRepository::save)
            .map(integrationConfigMapper::toDto);
    }

    /**
     * Get all the integrationConfigs.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<IntegrationConfigDTO> findAll() {
        LOG.debug("Request to get all IntegrationConfigs");
        return integrationConfigRepository
            .findAll()
            .stream()
            .map(integrationConfigMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one integrationConfig by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<IntegrationConfigDTO> findOne(Long id) {
        LOG.debug("Request to get IntegrationConfig : {}", id);
        return integrationConfigRepository.findById(id).map(integrationConfigMapper::toDto);
    }

    /**
     * Delete the integrationConfig by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete IntegrationConfig : {}", id);
        integrationConfigRepository.deleteById(id);
    }
}
