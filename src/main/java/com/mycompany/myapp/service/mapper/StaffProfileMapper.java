package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.dto.StaffProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link StaffProfile} and its DTO {@link StaffProfileDTO}.
 */
@Mapper(componentModel = "spring")
public interface StaffProfileMapper extends EntityMapper<StaffProfileDTO, StaffProfile> {
    @Mapping(target = "office", source = "office", qualifiedByName = "officeCode")
    StaffProfileDTO toDto(StaffProfile s);

    @Named("officeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    OfficeDTO toDtoOfficeCode(Office office);
}
