package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Driver;
import com.mycompany.myapp.service.dto.DriverDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Driver} and its DTO {@link DriverDTO}.
 */
@Mapper(componentModel = "spring")
public interface DriverMapper extends EntityMapper<DriverDTO, Driver> {}
