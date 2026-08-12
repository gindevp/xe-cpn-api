package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.OrderReturnRequestAsserts.*;
import static com.mycompany.myapp.domain.OrderReturnRequestTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderReturnRequestMapperTest {

    private OrderReturnRequestMapper orderReturnRequestMapper;

    @BeforeEach
    void setUp() {
        orderReturnRequestMapper = new OrderReturnRequestMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOrderReturnRequestSample1();
        var actual = orderReturnRequestMapper.toEntity(orderReturnRequestMapper.toDto(expected));
        assertOrderReturnRequestAllPropertiesEquals(expected, actual);
    }
}
