package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.StaffProfileAsserts.*;
import static com.mycompany.myapp.domain.StaffProfileTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffProfileMapperTest {

    private StaffProfileMapper staffProfileMapper;

    @BeforeEach
    void setUp() {
        staffProfileMapper = new StaffProfileMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getStaffProfileSample1();
        var actual = staffProfileMapper.toEntity(staffProfileMapper.toDto(expected));
        assertStaffProfileAllPropertiesEquals(expected, actual);
    }
}
