package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.dto.RouteDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Route} and its DTO {@link RouteDTO}.
 */
@Mapper(componentModel = "spring")
public interface RouteMapper extends EntityMapper<RouteDTO, Route> {
    @Mapping(target = "fromOffice", source = "fromOffice", qualifiedByName = "officeCode")
    @Mapping(target = "toOffice", source = "toOffice", qualifiedByName = "officeCode")
    RouteDTO toDto(Route s);

    @Named("officeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    OfficeDTO toDtoOfficeCode(Office office);
}
