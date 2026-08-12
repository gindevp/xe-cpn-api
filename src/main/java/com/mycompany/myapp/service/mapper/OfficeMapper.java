package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.service.dto.OfficeDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Office} and its DTO {@link OfficeDTO}.
 */
@Mapper(componentModel = "spring")
public interface OfficeMapper extends EntityMapper<OfficeDTO, Office> {}
