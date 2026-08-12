package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.OrderFareAdjustmentRequestAsserts.*;
import static com.mycompany.myapp.domain.OrderFareAdjustmentRequestTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderFareAdjustmentRequestMapperTest {

    private OrderFareAdjustmentRequestMapper orderFareAdjustmentRequestMapper;

    @BeforeEach
    void setUp() {
        orderFareAdjustmentRequestMapper = new OrderFareAdjustmentRequestMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOrderFareAdjustmentRequestSample1();
        var actual = orderFareAdjustmentRequestMapper.toEntity(orderFareAdjustmentRequestMapper.toDto(expected));
        assertOrderFareAdjustmentRequestAllPropertiesEquals(expected, actual);
    }
}
