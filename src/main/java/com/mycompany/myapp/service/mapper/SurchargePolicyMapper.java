package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.service.dto.SurchargePolicyDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SurchargePolicy} and its DTO {@link SurchargePolicyDTO}.
 */
@Mapper(componentModel = "spring")
public interface SurchargePolicyMapper extends EntityMapper<SurchargePolicyDTO, SurchargePolicy> {}
