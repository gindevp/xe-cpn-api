package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.PricingRuleAsserts.*;
import static com.mycompany.myapp.domain.PricingRuleTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingRuleMapperTest {

    private PricingRuleMapper pricingRuleMapper;

    @BeforeEach
    void setUp() {
        pricingRuleMapper = new PricingRuleMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPricingRuleSample1();
        var actual = pricingRuleMapper.toEntity(pricingRuleMapper.toDto(expected));
        assertPricingRuleAllPropertiesEquals(expected, actual);
    }
}
