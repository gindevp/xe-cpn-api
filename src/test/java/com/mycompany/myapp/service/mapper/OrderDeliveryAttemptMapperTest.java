package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.OrderDeliveryAttemptAsserts.*;
import static com.mycompany.myapp.domain.OrderDeliveryAttemptTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderDeliveryAttemptMapperTest {

    private OrderDeliveryAttemptMapper orderDeliveryAttemptMapper;

    @BeforeEach
    void setUp() {
        orderDeliveryAttemptMapper = new OrderDeliveryAttemptMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOrderDeliveryAttemptSample1();
        var actual = orderDeliveryAttemptMapper.toEntity(orderDeliveryAttemptMapper.toDto(expected));
        assertOrderDeliveryAttemptAllPropertiesEquals(expected, actual);
    }
}
