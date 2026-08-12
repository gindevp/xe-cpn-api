package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.service.dto.VehicleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Vehicle} and its DTO {@link VehicleDTO}.
 */
@Mapper(componentModel = "spring")
public interface VehicleMapper extends EntityMapper<VehicleDTO, Vehicle> {}
