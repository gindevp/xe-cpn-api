package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Branch;
import com.mycompany.myapp.domain.PricingRule;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.service.dto.BranchDTO;
import com.mycompany.myapp.service.dto.PricingRuleDTO;
import com.mycompany.myapp.service.dto.RouteDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PricingRule} and its DTO {@link PricingRuleDTO}.
 */
@Mapper(componentModel = "spring")
public interface PricingRuleMapper extends EntityMapper<PricingRuleDTO, PricingRule> {
    @Mapping(target = "route", source = "route", qualifiedByName = "routeCode")
    @Mapping(target = "branch", source = "branch", qualifiedByName = "branchId")
    PricingRuleDTO toDto(PricingRule s);

    @Named("routeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    RouteDTO toDtoRouteCode(Route route);

    @Named("branchId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "active", source = "active")
    BranchDTO toDtoBranchId(Branch branch);
}
