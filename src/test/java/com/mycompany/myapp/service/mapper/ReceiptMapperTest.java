package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ReceiptAsserts.*;
import static com.mycompany.myapp.domain.ReceiptTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReceiptMapperTest {

    private ReceiptMapper receiptMapper;

    @BeforeEach
    void setUp() {
        receiptMapper = new ReceiptMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReceiptSample1();
        var actual = receiptMapper.toEntity(receiptMapper.toDto(expected));
        assertReceiptAllPropertiesEquals(expected, actual);
    }
}
