package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.IntegrationConfigAsserts.*;
import static com.mycompany.myapp.domain.IntegrationConfigTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrationConfigMapperTest {

    private IntegrationConfigMapper integrationConfigMapper;

    @BeforeEach
    void setUp() {
        integrationConfigMapper = new IntegrationConfigMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getIntegrationConfigSample1();
        var actual = integrationConfigMapper.toEntity(integrationConfigMapper.toDto(expected));
        assertIntegrationConfigAllPropertiesEquals(expected, actual);
    }
}
