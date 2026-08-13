package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Branch;
import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.service.dto.BranchDTO;
import com.mycompany.myapp.service.dto.ItineraryDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Itinerary} and its DTO {@link ItineraryDTO}.
 */
@Mapper(componentModel = "spring")
public interface ItineraryMapper extends EntityMapper<ItineraryDTO, Itinerary> {
    @Mapping(target = "branch", source = "branch", qualifiedByName = "branchCode")
    ItineraryDTO toDto(Itinerary s);

    @Named("branchCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    BranchDTO toDtoBranchCode(Branch branch);
}
