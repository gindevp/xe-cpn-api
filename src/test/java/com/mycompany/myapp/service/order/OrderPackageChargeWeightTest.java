package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderPackageChargeWeightTest {

    @Test
    void packageUsesMaxOfRealAndDimWeight() {
        // 10×10×10 / 5000 = 0,2 kg; cân thật 1 → lấy 1
        assertThat(OrderPackageChargeWeight.chargeKg(new BigDecimal("1"), new double[] { 10, 10, 10 })).isEqualByComparingTo("1.0");
        // 50×40×40 / 5000 = 16 kg; cân thật 5 → lấy 16
        assertThat(OrderPackageChargeWeight.chargeKg(new BigDecimal("5"), new double[] { 50, 40, 40 })).isEqualByComparingTo("16.0");
    }

    @Test
    void doorChargeKgSumsPackagesFromNote() {
        ShipmentOrder o = new ShipmentOrder();
        o.setWeightKg(new BigDecimal("6"));
        o.setNote("[PKGKG]1,5[/PKGKG][PKGDIM]10x10x10|50x40x40[/PKGDIM]");
        // kiện 1: max(1, 0.2)=1; kiện 2: max(5, 16)=16 → 17
        assertThat(OrderPackageChargeWeight.doorChargeKg(o)).isEqualByComparingTo("17.0");
    }

    @Test
    void doorChargeKgFallsBackToOrderWeight() {
        ShipmentOrder o = new ShipmentOrder();
        o.setWeightKg(new BigDecimal("3.2"));
        assertThat(OrderPackageChargeWeight.doorChargeKg(o)).isEqualByComparingTo("3.2");
    }
}
