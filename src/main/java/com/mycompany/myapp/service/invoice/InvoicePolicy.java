package com.mycompany.myapp.service.invoice;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Quy tắc xuất HĐĐT theo người trả cước.
 * <ul>
 *   <li>Gửi trả (GUI_TRA, kể cả công nợ) và trả chia tỉ lệ (P30_70…): người mua = người gửi.</li>
 *   <li>Nhận trả / COD: người mua = người nhận.</li>
 *   <li>Mốc thanh toán: gửi trả = lúc nhập kho gửi ({@code pickedUpAt}); còn lại = lúc giao thành công.</li>
 *   <li>Hạn = mốc + 3 tiếng: khách phải yêu cầu HĐ công ty trước hạn, hết hạn hệ thống tự xuất HĐ cá nhân.</li>
 * </ul>
 */
public final class InvoicePolicy {

    public static final Duration WINDOW = Duration.ofHours(3);

    public static final String TYPE_COMPANY = "COMPANY";
    public static final String TYPE_PERSONAL = "PERSONAL";

    /** Sự kiện giao thành công (lấy max eventAt). */
    public static final List<String> DELIVERED_ACTIONS = List.of("POD", "POD_QUAY", "DELIVERED", "TRANSITION_DELIVERED");

    private InvoicePolicy() {}

    /** Mốc thanh toán = lúc nhập kho gửi chỉ với gửi trả; các hình thức khác thanh toán đủ khi giao. */
    public static boolean paidAtWarehouseIn(ShipmentOrder order) {
        return order.getPaymentTerm() == null || order.getPaymentTerm() == PaymentTerm.GUI_TRA;
    }

    public static boolean senderPays(ShipmentOrder order) {
        PaymentTerm t = order.getPaymentTerm();
        return t != PaymentTerm.NHAN_TRA && t != PaymentTerm.COD;
    }

    public static String payerName(ShipmentOrder order) {
        return senderPays(order) ? order.getSenderName() : order.getReceiverName();
    }

    public static String payerPhone(ShipmentOrder order) {
        return senderPays(order) ? order.getSenderPhone() : order.getReceiverPhone();
    }

    /** Mốc thanh toán; null = chưa tới (chưa nhập kho gửi / chưa giao). */
    public static Instant paidAt(ShipmentOrder order, Instant deliveredAt) {
        return paidAtWarehouseIn(order) ? order.getPickedUpAt() : deliveredAt;
    }

    public static Instant deadline(Instant paidAt) {
        return paidAt == null ? null : paidAt.plus(WINDOW);
    }

    /** Loại HĐ: cột invoice_type; đơn xuất trước khi có cột thì suy từ MST. */
    public static String typeOf(ShipmentOrder order) {
        if (order.getInvoiceType() != null && !order.getInvoiceType().isBlank()) {
            return order.getInvoiceType();
        }
        String st = order.getInvoiceStatus();
        if (st == null || MeInvoiceIssueService.STATUS_SKIPPED.equals(st)) {
            return null;
        }
        return order.getInvoiceTaxCode() != null && !order.getInvoiceTaxCode().isBlank() ? TYPE_COMPANY : TYPE_PERSONAL;
    }

    /** Có yêu cầu HĐ công ty kèm MST → xuất DN; còn lại xuất cá nhân. */
    public static String typeToIssue(ShipmentOrder order) {
        boolean company =
            Boolean.TRUE.equals(order.getInvoiceRequested()) && order.getInvoiceTaxCode() != null && !order.getInvoiceTaxCode().isBlank();
        return company ? TYPE_COMPANY : TYPE_PERSONAL;
    }
}
