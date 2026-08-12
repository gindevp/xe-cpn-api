package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.OrderPaymentAsserts.*;
import static com.mycompany.myapp.domain.OrderPaymentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderPaymentMapperTest {

    private OrderPaymentMapper orderPaymentMapper;

    @BeforeEach
    void setUp() {
        orderPaymentMapper = new OrderPaymentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOrderPaymentSample1();
        var actual = orderPaymentMapper.toEntity(orderPaymentMapper.toDto(expected));
        assertOrderPaymentAllPropertiesEquals(expected, actual);
    }
}
