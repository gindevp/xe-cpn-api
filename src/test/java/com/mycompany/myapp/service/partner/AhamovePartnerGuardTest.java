package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import org.junit.jupiter.api.Test;

class AhamovePartnerGuardTest {

    private static ShipmentOrder order(String partnerStatus) {
        ShipmentOrder o = new ShipmentOrder();
        o.setPartnerCode(AhamoveDispatchService.PARTNER_CODE);
        o.setPartnerOrderId("26CXG3LM");
        o.setPartnerStatus(partnerStatus);
        return o;
    }

    @Test
    void runningAhamoveBlocksManualFail() {
        assertThat(AhamoveDispatchService.partnerActive(order("ACCEPTED"))).isTrue();
        assertThat(AhamoveDispatchService.partnerActive(order("IN PROCESS"))).isTrue();
        assertThatThrownBy(() -> AhamoveDispatchService.assertNoActivePartner(order("ASSIGNING")))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Hủy Ahamove");
    }

    @Test
    void finishedOrNoAhamoveAllowsFail() {
        for (String st : new String[] { "CANCELLED", "COMPLETED", "FAILED", "cancelled", null }) {
            assertThatCode(() -> AhamoveDispatchService.assertNoActivePartner(order(st))).doesNotThrowAnyException();
        }
        ShipmentOrder plain = new ShipmentOrder();
        assertThat(AhamoveDispatchService.partnerActive(plain)).isFalse();
    }
}
