package com.mycompany.myapp.service.trip;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VehiclePlatesTest {

    @Test
    void normalizeStripsSeparatorsAndUppercases() {
        assertThat(VehiclePlates.normalize("29h-885.24")).isEqualTo("29H88524");
        assertThat(VehiclePlates.normalize(" 29H 885 95 ")).isEqualTo("29H88595");
        assertThat(VehiclePlates.normalize(null)).isEmpty();
    }

    @Test
    void acceptsRealVnPlates() {
        assertThat(VehiclePlates.isValid("29H88524")).isTrue();
        assertThat(VehiclePlates.isValid("29H-885.95")).isTrue();
        assertThat(VehiclePlates.isValid("29G00888")).isTrue();
        assertThat(VehiclePlates.isValid("51C-678.90")).isTrue();
        assertThat(VehiclePlates.isValid("29LD12345")).isTrue();
        assertThat(VehiclePlates.isValid("29F01771")).isTrue();
    }

    @Test
    void rejectsPartialOrTypoPlates() {
        assertThat(VehiclePlates.isValid("88524")).isFalse();
        assertThat(VehiclePlates.isValid("8.88")).isFalse();
        assertThat(VehiclePlates.isValid("6952")).isFalse();
        assertThat(VehiclePlates.isValid("09738")).isFalse();
        assertThat(VehiclePlates.isValid("29H")).isFalse();
        assertThat(VehiclePlates.isValid("")).isFalse();
    }
}
