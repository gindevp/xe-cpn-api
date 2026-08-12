package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.DoorFeeRule;
import com.mycompany.myapp.service.dto.DoorFeeRuleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DoorFeeRule} and its DTO {@link DoorFeeRuleDTO}.
 */
@Mapper(componentModel = "spring")
public interface DoorFeeRuleMapper extends EntityMapper<DoorFeeRuleDTO, DoorFeeRule> {}
