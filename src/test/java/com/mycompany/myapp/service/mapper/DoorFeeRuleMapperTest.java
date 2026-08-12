package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.DoorFeeRuleAsserts.*;
import static com.mycompany.myapp.domain.DoorFeeRuleTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DoorFeeRuleMapperTest {

    private DoorFeeRuleMapper doorFeeRuleMapper;

    @BeforeEach
    void setUp() {
        doorFeeRuleMapper = new DoorFeeRuleMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDoorFeeRuleSample1();
        var actual = doorFeeRuleMapper.toEntity(doorFeeRuleMapper.toDto(expected));
        assertDoorFeeRuleAllPropertiesEquals(expected, actual);
    }
}
