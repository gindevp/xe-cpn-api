package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderEditDiffTest {

    private static ShipmentOrder order() {
        ShipmentOrder o = new ShipmentOrder();
        o.setSenderName("HẢO");
        o.setReceiverName("THUỶ");
        o.setReceiverPhone("0902118245");
        o.setFareAmount(new BigDecimal("30000.00"));
        o.setCodAmount(BigDecimal.ZERO);
        o.setWeightKg(new BigDecimal("3.00"));
        o.setNote("[LOAI]Khác[/LOAI]\n[TENHANG]thuốc[/TENHANG]\n[WHOUT]1[/WHOUT]\nhàng dễ vỡ");
        return o;
    }

    @Test
    void describe_listsEachChangedField_withOldAndNewValues() {
        ShipmentOrder o = order();
        Map<String, String> before = OrderEditDiff.snapshot(o);
        o.setReceiverName("THUỶ NGUYỄN");
        o.setFareAmount(new BigDecimal("40000"));
        o.setNote("[LOAI]Khác[/LOAI]\n[TENHANG]thuốc bổ[/TENHANG]\n[WHOUT]1[/WHOUT]\nhàng dễ vỡ");

        String d = OrderEditDiff.describe(before, OrderEditDiff.snapshot(o));

        assertThat(d).isEqualTo("Tên người nhận: THUỶ → THUỶ NGUYỄN; Tổng cước: 30.000 → 40.000; Tên hàng: thuốc → thuốc bổ");
    }

    @Test
    void describe_ignoresSystemNoteTags_zeroVsNullMoney_andScaleOnlyChanges() {
        ShipmentOrder o = order();
        Map<String, String> before = OrderEditDiff.snapshot(o);
        o.setNote("[WHIN]1[/WHIN]\n" + o.getNote() + "\n[DRVSIGN t=1]data:x[/DRVSIGN]");
        o.setCodAmount(null);
        o.setWeightKg(new BigDecimal("3"));
        o.setFareAmount(new BigDecimal("30000"));

        assertThat(OrderEditDiff.describe(before, OrderEditDiff.snapshot(o))).isEmpty();
    }

    @Test
    void describe_noteBodyAndClearedValue() {
        ShipmentOrder o = order();
        Map<String, String> before = OrderEditDiff.snapshot(o);
        o.setNote("[LOAI]Khác[/LOAI]\n[TENHANG]thuốc[/TENHANG]\n[WHOUT]1[/WHOUT]");
        o.setReceiverPhone("");

        assertThat(OrderEditDiff.describe(before, OrderEditDiff.snapshot(o))).isEqualTo(
            "SĐT người nhận: 0902118245 → (trống); Ghi chú: hàng dễ vỡ → (trống)"
        );
    }
}
