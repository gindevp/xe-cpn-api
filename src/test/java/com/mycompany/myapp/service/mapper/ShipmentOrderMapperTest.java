package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ShipmentOrderAsserts.*;
import static com.mycompany.myapp.domain.ShipmentOrderTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShipmentOrderMapperTest {

    private ShipmentOrderMapper shipmentOrderMapper;

    @BeforeEach
    void setUp() {
        shipmentOrderMapper = new ShipmentOrderMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getShipmentOrderSample1();
        var actual = shipmentOrderMapper.toEntity(shipmentOrderMapper.toDto(expected));
        assertShipmentOrderAllPropertiesEquals(expected, actual);
    }
}
