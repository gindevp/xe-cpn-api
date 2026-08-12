package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.OrderPodPhotoAsserts.*;
import static com.mycompany.myapp.domain.OrderPodPhotoTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderPodPhotoMapperTest {

    private OrderPodPhotoMapper orderPodPhotoMapper;

    @BeforeEach
    void setUp() {
        orderPodPhotoMapper = new OrderPodPhotoMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOrderPodPhotoSample1();
        var actual = orderPodPhotoMapper.toEntity(orderPodPhotoMapper.toDto(expected));
        assertOrderPodPhotoAllPropertiesEquals(expected, actual);
    }
}
