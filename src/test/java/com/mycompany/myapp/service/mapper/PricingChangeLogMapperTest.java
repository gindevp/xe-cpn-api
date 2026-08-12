package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.PricingChangeLogAsserts.*;
import static com.mycompany.myapp.domain.PricingChangeLogTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingChangeLogMapperTest {

    private PricingChangeLogMapper pricingChangeLogMapper;

    @BeforeEach
    void setUp() {
        pricingChangeLogMapper = new PricingChangeLogMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPricingChangeLogSample1();
        var actual = pricingChangeLogMapper.toEntity(pricingChangeLogMapper.toDto(expected));
        assertPricingChangeLogAllPropertiesEquals(expected, actual);
    }
}
