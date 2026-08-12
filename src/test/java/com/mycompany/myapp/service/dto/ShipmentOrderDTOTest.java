package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ShipmentOrderDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShipmentOrderDTO.class);
        ShipmentOrderDTO shipmentOrderDTO1 = new ShipmentOrderDTO();
        shipmentOrderDTO1.setId(1L);
        ShipmentOrderDTO shipmentOrderDTO2 = new ShipmentOrderDTO();
        assertThat(shipmentOrderDTO1).isNotEqualTo(shipmentOrderDTO2);
        shipmentOrderDTO2.setId(shipmentOrderDTO1.getId());
        assertThat(shipmentOrderDTO1).isEqualTo(shipmentOrderDTO2);
        shipmentOrderDTO2.setId(2L);
        assertThat(shipmentOrderDTO1).isNotEqualTo(shipmentOrderDTO2);
        shipmentOrderDTO1.setId(null);
        assertThat(shipmentOrderDTO1).isNotEqualTo(shipmentOrderDTO2);
    }
}
