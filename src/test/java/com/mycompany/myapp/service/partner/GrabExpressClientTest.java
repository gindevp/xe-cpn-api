package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GrabExpressClientTest {

    @Test
    void phoneAndVehicle() {
        assertThat(GrabExpressClient.phone("0901234567")).isEqualTo("84901234567");
        assertThat(GrabExpressClient.phone("84901234567")).isEqualTo("84901234567");
        assertThat(GrabExpressClient.vehicleOf("TIER_3")).isEqualTo("VAN");
        assertThat(GrabExpressClient.vehicleOf("")).isEqualTo("BIKE");
        assertThat(GrabDispatchService.normalize("CANCELED")).isEqualTo("CANCELLED");
        assertThat(GrabDispatchService.pickedUp("IN_DELIVERY")).isTrue();
        assertThat(GrabDispatchService.pickedUp("ALLOCATING")).isFalse();
    }
}
