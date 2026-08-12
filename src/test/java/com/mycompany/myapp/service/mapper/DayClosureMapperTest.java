package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.DayClosureAsserts.*;
import static com.mycompany.myapp.domain.DayClosureTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DayClosureMapperTest {

    private DayClosureMapper dayClosureMapper;

    @BeforeEach
    void setUp() {
        dayClosureMapper = new DayClosureMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDayClosureSample1();
        var actual = dayClosureMapper.toEntity(dayClosureMapper.toDto(expected));
        assertDayClosureAllPropertiesEquals(expected, actual);
    }
}
