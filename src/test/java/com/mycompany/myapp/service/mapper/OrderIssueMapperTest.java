package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.OrderIssueAsserts.*;
import static com.mycompany.myapp.domain.OrderIssueTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderIssueMapperTest {

    private OrderIssueMapper orderIssueMapper;

    @BeforeEach
    void setUp() {
        orderIssueMapper = new OrderIssueMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOrderIssueSample1();
        var actual = orderIssueMapper.toEntity(orderIssueMapper.toDto(expected));
        assertOrderIssueAllPropertiesEquals(expected, actual);
    }
}
