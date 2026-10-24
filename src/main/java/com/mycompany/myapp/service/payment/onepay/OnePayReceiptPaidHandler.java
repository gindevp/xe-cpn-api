package com.mycompany.myapp.service.payment.onepay;

import com.mycompany.myapp.service.finance.FinanceFacadeService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Khi OnePay báo đủ tiền hoá đơn phiếu thu: tự xác nhận đã thu (không cần KT confirm). */
@Service
public class OnePayReceiptPaidHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OnePayReceiptPaidHandler.class);

    private final FinanceFacadeService financeFacadeService;

    public OnePayReceiptPaidHandler(FinanceFacadeService financeFacadeService) {
        this.financeFacadeService = financeFacadeService;
    }

    @Transactional
    public void paid(String receiptCode, BigDecimal amount, String transId) {
        if (receiptCode == null || receiptCode.isBlank() || amount == null || amount.signum() <= 0) {
            return;
        }
        String note = "Thu hộ OnePay · " + amount.toPlainString() + "đ";
        if (transId != null && !transId.isBlank()) {
            note = note + " · GD " + transId.trim();
        }
        try {
            financeFacadeService.confirmReceiptByCollection(receiptCode.trim(), note);
        } catch (RuntimeException ex) {
            LOG.warn("IPN OnePay không tự xác nhận được phiếu {}: {}", receiptCode, ex.getMessage());
        }
    }
}
