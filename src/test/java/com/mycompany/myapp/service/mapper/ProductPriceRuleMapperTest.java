package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ProductPriceRuleAsserts.*;
import static com.mycompany.myapp.domain.ProductPriceRuleTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductPriceRuleMapperTest {

    private ProductPriceRuleMapper productPriceRuleMapper;

    @BeforeEach
    void setUp() {
        productPriceRuleMapper = new ProductPriceRuleMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getProductPriceRuleSample1();
        var actual = productPriceRuleMapper.toEntity(productPriceRuleMapper.toDto(expected));
        assertProductPriceRuleAllPropertiesEquals(expected, actual);
    }
}
