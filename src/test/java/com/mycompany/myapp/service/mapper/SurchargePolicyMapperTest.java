package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.SurchargePolicyAsserts.*;
import static com.mycompany.myapp.domain.SurchargePolicyTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SurchargePolicyMapperTest {

    private SurchargePolicyMapper surchargePolicyMapper;

    @BeforeEach
    void setUp() {
        surchargePolicyMapper = new SurchargePolicyMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSurchargePolicySample1();
        var actual = surchargePolicyMapper.toEntity(surchargePolicyMapper.toDto(expected));
        assertSurchargePolicyAllPropertiesEquals(expected, actual);
    }
}
