package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AhamoveCargoTest {

    private static ShipmentOrder order(String note, String weight) {
        ShipmentOrder o = new ShipmentOrder();
        o.setNote(note);
        if (weight != null) {
            o.setWeightKg(new BigDecimal(weight));
        }
        return o;
    }

    @Test
    void standardSizeNeedsNoTier() {
        AhamoveCargo cargo = AhamoveCargo.from(order("[PKGKG]5[/PKGKG]\n[PKGDIM]30x20x10[/PKGDIM]", null));
        assertThat(cargo.tier()).isNull();
        assertThat(cargo.packages()).hasSize(1);
        assertThatCode(cargo::assertFitsBike).doesNotThrowAnyException();
    }

    @Test
    void weightAloneUsesOrderTotal() {
        assertThat(AhamoveCargo.from(order(null, "8")).tier()).isNull();
        assertThat(AhamoveCargo.from(order(null, "35")).tier()).isEqualTo("TIER_2");
    }

    @Test
    void largestPackagePicksTheTier() {
        AhamoveCargo cargo = AhamoveCargo.from(order("[PKGKG]10,20[/PKGKG]\n[PKGDIM]20x20x20|60x50x40[/PKGDIM]", null));
        assertThat(cargo.tier()).isEqualTo("TIER_2");
        assertThat(cargo.packages()).hasSize(2);
        assertThat(AhamoveCargo.from(order("[PKGDIM]65x55x50[/PKGDIM]", "10")).tier()).isEqualTo("TIER_3");
        assertThat(AhamoveCargo.from(order("[PKGDIM]85x70x60[/PKGDIM]", "10")).tier()).isEqualTo("TIER_4");
    }

    @Test
    void tierChoiceOverridesPackageTier() {
        AhamoveCargo auto = AhamoveCargo.from(order("[PKGKG]35[/PKGKG]\n[PKGDIM]55x45x50[/PKGDIM]", null));
        assertThat(auto.tier()).isEqualTo("TIER_2");
        assertThat(auto.withTierChoice("").tier()).isNull();
        assertThat(auto.withTierChoice("STANDARD").tier()).isNull();
        assertThat(auto.withTierChoice("tier_3").tier()).isEqualTo("TIER_3");
        assertThatThrownBy(() -> auto.withTierChoice("TIER_9")).isInstanceOf(
            com.mycompany.myapp.web.rest.errors.BadRequestAlertException.class
        );
    }

    @Test
    void overBikeLimitIsRejected() {
        AhamoveCargo cargo = AhamoveCargo.from(order(null, "90"));
        assertThatThrownBy(cargo::assertFitsBike).isInstanceOf(com.mycompany.myapp.web.rest.errors.BadRequestAlertException.class);
    }
}
