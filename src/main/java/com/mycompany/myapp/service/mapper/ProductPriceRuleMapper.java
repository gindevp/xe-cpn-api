package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.ProductPriceRule;
import com.mycompany.myapp.service.dto.ProductPriceRuleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ProductPriceRule} and its DTO {@link ProductPriceRuleDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProductPriceRuleMapper extends EntityMapper<ProductPriceRuleDTO, ProductPriceRule> {}
