package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FinanceFacadeServiceReceiptCandidateTest {

    @Test
    void delivered_alwaysCandidateWhenCollectable() {
        ShipmentOrder o = base(PaymentTerm.NHAN_TRA, OrderStatus.DELIVERED, null);
        assertThat(FinanceFacadeService.isReceiptCandidate(o)).isTrue();
    }

    @Test
    void guiTra_afterSenderWarehouse_isCandidate() {
        ShipmentOrder o = base(PaymentTerm.GUI_TRA, OrderStatus.CONFIRMED, ForwardStage.WH_IN);
        assertThat(FinanceFacadeService.isSenderPayEarlyCandidate(o)).isTrue();
        assertThat(FinanceFacadeService.isReceiptCandidate(o)).isTrue();
    }

    @Test
    void guiTra_beforeWarehouse_unpaid_notCandidate() {
        ShipmentOrder o = base(PaymentTerm.GUI_TRA, OrderStatus.CONFIRMED, null);
        assertThat(FinanceFacadeService.isSenderPayEarlyCandidate(o)).isFalse();
        assertThat(FinanceFacadeService.isReceiptCandidate(o)).isFalse();
    }

    @Test
    void guiTra_prepaidTruoc_candidateEvenBeforeWarehouse() {
        ShipmentOrder o = base(PaymentTerm.GUI_TRA, OrderStatus.CONFIRMED, null);
        o.setPaidAmount(new BigDecimal("30000"));
        BigDecimal truoc = new BigDecimal("30000");
        assertThat(FinanceFacadeService.receiptSettleAmount(o, truoc)).isEqualByComparingTo("30000");
        assertThat(FinanceFacadeService.isReceiptCandidate(o, truoc)).isTrue();
    }

    @Test
    void nhanTra_inTransit_notCandidate() {
        ShipmentOrder o = base(PaymentTerm.NHAN_TRA, OrderStatus.IN_TRANSIT, ForwardStage.WH_IN);
        assertThat(FinanceFacadeService.isReceiptCandidate(o)).isFalse();
    }

    @Test
    void zeroCollectable_andNoTruoc_notCandidate() {
        ShipmentOrder o = base(PaymentTerm.GUI_TRA, OrderStatus.CONFIRMED, ForwardStage.WH_IN);
        o.setFareAmount(new BigDecimal("10000"));
        o.setPaidAmount(new BigDecimal("10000"));
        assertThat(FinanceFacadeService.isReceiptCandidate(o)).isFalse();
    }

    private static ShipmentOrder base(PaymentTerm term, OrderStatus status, ForwardStage stage) {
        ShipmentOrder o = new ShipmentOrder();
        o.setPaymentTerm(term);
        o.setStatus(status);
        o.setForwardStage(stage);
        o.setFareAmount(new BigDecimal("30000"));
        o.setPaidAmount(BigDecimal.ZERO);
        o.setCodAmount(BigDecimal.ZERO);
        return o;
    }
}
