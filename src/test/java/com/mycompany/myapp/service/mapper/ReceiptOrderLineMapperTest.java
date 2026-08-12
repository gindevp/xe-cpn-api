package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ReceiptOrderLineAsserts.*;
import static com.mycompany.myapp.domain.ReceiptOrderLineTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReceiptOrderLineMapperTest {

    private ReceiptOrderLineMapper receiptOrderLineMapper;

    @BeforeEach
    void setUp() {
        receiptOrderLineMapper = new ReceiptOrderLineMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReceiptOrderLineSample1();
        var actual = receiptOrderLineMapper.toEntity(receiptOrderLineMapper.toDto(expected));
        assertReceiptOrderLineAllPropertiesEquals(expected, actual);
    }
}
