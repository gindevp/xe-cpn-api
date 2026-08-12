package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.OrderLegAsserts.*;
import static com.mycompany.myapp.domain.OrderLegTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderLegMapperTest {

    private OrderLegMapper orderLegMapper;

    @BeforeEach
    void setUp() {
        orderLegMapper = new OrderLegMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOrderLegSample1();
        var actual = orderLegMapper.toEntity(orderLegMapper.toDto(expected));
        assertOrderLegAllPropertiesEquals(expected, actual);
    }
}
