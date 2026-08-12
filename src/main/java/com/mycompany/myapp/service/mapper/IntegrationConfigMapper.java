package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.service.dto.IntegrationConfigDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link IntegrationConfig} and its DTO {@link IntegrationConfigDTO}.
 */
@Mapper(componentModel = "spring")
public interface IntegrationConfigMapper extends EntityMapper<IntegrationConfigDTO, IntegrationConfig> {}
