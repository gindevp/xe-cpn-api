package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.TripOrderAssignmentAsserts.*;
import static com.mycompany.myapp.domain.TripOrderAssignmentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripOrderAssignmentMapperTest {

    private TripOrderAssignmentMapper tripOrderAssignmentMapper;

    @BeforeEach
    void setUp() {
        tripOrderAssignmentMapper = new TripOrderAssignmentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getTripOrderAssignmentSample1();
        var actual = tripOrderAssignmentMapper.toEntity(tripOrderAssignmentMapper.toDto(expected));
        assertTripOrderAssignmentAllPropertiesEquals(expected, actual);
    }
}
