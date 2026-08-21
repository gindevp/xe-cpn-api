package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Driver;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.service.dto.DriverDTO;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.dto.VehicleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Vehicle} and its DTO {@link VehicleDTO}.
 */
@Mapper(componentModel = "spring")
public interface VehicleMapper extends EntityMapper<VehicleDTO, Vehicle> {
    @Mapping(target = "office", source = "office", qualifiedByName = "officeCode")
    @Mapping(target = "defaultDriver", source = "defaultDriver", qualifiedByName = "driverName")
    VehicleDTO toDto(Vehicle s);

    @Mapping(target = "office", ignore = true)
    @Mapping(target = "defaultDriver", ignore = true)
    Vehicle toEntity(VehicleDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "office", ignore = true)
    @Mapping(target = "defaultDriver", ignore = true)
    void partialUpdate(@MappingTarget Vehicle entity, VehicleDTO dto);

    @Named("officeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    OfficeDTO toDtoOfficeCode(Office office);

    @Named("driverName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "driverCode", source = "driverCode")
    @Mapping(target = "fullName", source = "fullName")
    DriverDTO toDtoDriverName(Driver driver);
}
