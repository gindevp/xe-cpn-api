package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.DayClosure;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.service.dto.DayClosureDTO;
import com.mycompany.myapp.service.dto.OfficeDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DayClosure} and its DTO {@link DayClosureDTO}.
 */
@Mapper(componentModel = "spring")
public interface DayClosureMapper extends EntityMapper<DayClosureDTO, DayClosure> {
    @Mapping(target = "office", source = "office", qualifiedByName = "officeCode")
    DayClosureDTO toDto(DayClosure s);

    @Named("officeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    OfficeDTO toDtoOfficeCode(Office office);
}
