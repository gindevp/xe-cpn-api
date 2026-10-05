package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;

/**
 * Tài xế đối tác (Ahamove) ứng cước cho VP lúc lấy hàng rồi thu lại người nhận.
 * Số ứng đã gửi đối tác không sửa được → trong lúc chờ NV xác nhận nhận tiền, khoá thu tiền / sửa cước của đơn.
 */
public final class PartnerAdvance {

    /** Ghi chú khoản thu tiền ứng — bắt đầu bằng "POD" để báo cáo xếp vào phần VP giao. */
    public static final String PAYMENT_NOTE_PREFIX = "POD AHAMOVE ỨNG";

    private PartnerAdvance() {}

    public static String money(BigDecimal v) {
        return OrderMoney.nz(v).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public static BigDecimal amount(ShipmentOrder o) {
        return o == null ? BigDecimal.ZERO : OrderMoney.nz(o.getPartnerCodAmount());
    }

    /** Đã gửi đối tác số ứng, đơn đang giao / đã giao, NV chưa xác nhận nhận tiền. */
    public static boolean pending(ShipmentOrder o) {
        return (
            amount(o).signum() > 0 &&
            o.getPartnerCodCollectedAt() == null &&
            (o.getStatus() == OrderStatus.OUT_FOR_DELIVERY || o.getStatus() == OrderStatus.DELIVERED)
        );
    }

    /** Đã nhận tiền ứng nhưng không giao được → phải trả lại tài xế trước khi giao lại. */
    public static boolean needsRefund(ShipmentOrder o) {
        return (
            amount(o).signum() > 0 &&
            o.getPartnerCodCollectedAt() != null &&
            o.getStatus() != OrderStatus.OUT_FOR_DELIVERY &&
            o.getStatus() != OrderStatus.DELIVERED
        );
    }

    public static void assertNotPending(ShipmentOrder o, String entity) {
        if (pending(o)) {
            throw new BadRequestAlertException(
                "Đơn " +
                o.getOrderCode() +
                " đang chờ tài xế Ahamove ứng " +
                money(amount(o)) +
                "đ — xác nhận tiền ứng (hoặc hủy Ahamove) trước khi thu tiền / sửa cước",
                entity,
                "partnerAdvancePending"
            );
        }
    }

    public static void assertNoRefundDue(ShipmentOrder o, String entity) {
        if (needsRefund(o)) {
            throw new BadRequestAlertException(
                "Đơn " + o.getOrderCode() + " chưa hoàn " + money(amount(o)) + "đ tiền ứng cho tài xế Ahamove",
                entity,
                "partnerAdvanceRefundDue"
            );
        }
    }
}
