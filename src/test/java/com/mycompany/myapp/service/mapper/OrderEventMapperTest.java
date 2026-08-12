package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.OrderEventAsserts.*;
import static com.mycompany.myapp.domain.OrderEventTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderEventMapperTest {

    private OrderEventMapper orderEventMapper;

    @BeforeEach
    void setUp() {
        orderEventMapper = new OrderEventMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOrderEventSample1();
        var actual = orderEventMapper.toEntity(orderEventMapper.toDto(expected));
        assertOrderEventAllPropertiesEquals(expected, actual);
    }
}
