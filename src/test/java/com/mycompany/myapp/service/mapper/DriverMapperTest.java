package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.DriverAsserts.*;
import static com.mycompany.myapp.domain.DriverTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DriverMapperTest {

    private DriverMapper driverMapper;

    @BeforeEach
    void setUp() {
        driverMapper = new DriverMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDriverSample1();
        var actual = driverMapper.toEntity(driverMapper.toDto(expected));
        assertDriverAllPropertiesEquals(expected, actual);
    }
}
