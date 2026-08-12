package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.PricingChangeLog;
import com.mycompany.myapp.domain.PricingRule;
import com.mycompany.myapp.service.dto.PricingChangeLogDTO;
import com.mycompany.myapp.service.dto.PricingRuleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PricingChangeLog} and its DTO {@link PricingChangeLogDTO}.
 */
@Mapper(componentModel = "spring")
public interface PricingChangeLogMapper extends EntityMapper<PricingChangeLogDTO, PricingChangeLog> {
    @Mapping(target = "pricingRule", source = "pricingRule", qualifiedByName = "pricingRuleRuleCode")
    PricingChangeLogDTO toDto(PricingChangeLog s);

    @Named("pricingRuleRuleCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "ruleCode", source = "ruleCode")
    PricingRuleDTO toDtoPricingRuleRuleCode(PricingRule pricingRule);
}
