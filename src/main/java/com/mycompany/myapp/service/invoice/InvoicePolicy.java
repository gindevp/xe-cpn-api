package com.mycompany.myapp.service.invoice;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
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
 *   <li>Hạn = mốc + 3 tiếng và đơn đã hoàn tất (giao thành công / hoàn xong về người gửi): khách yêu cầu HĐ công ty
 *   trước hạn, hết hạn hệ thống tự xuất HĐ cá nhân.</li>
 * </ul>
 */
public final class InvoicePolicy {

    public static final Duration WINDOW = Duration.ofHours(3);
    /** Job tự xuất chạy theo chu kỳ nên HĐ có thể ra sau hạn vài phút — trễ quá mức này mới tính là xuất muộn. */
    public static final Duration LATE_GRACE = Duration.ofMinutes(15);

    public static final String TYPE_COMPANY = "COMPANY";
    public static final String TYPE_PERSONAL = "PERSONAL";

    /** Sự kiện giao thành công (lấy max eventAt). */
    public static final List<String> DELIVERED_ACTIONS = List.of("POD", "POD_QUAY", "DELIVERED", "TRANSITION_DELIVERED");
    /** Sự kiện đơn hoàn tất: giao thành công hoặc hoàn xong về người gửi. */
    public static final List<String> DONE_ACTIONS = List.of(
        "POD",
        "POD_QUAY",
        "DELIVERED",
        "TRANSITION_DELIVERED",
        "RT_DONE",
        "TRANSITION_RETURNED"
    );

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

    /** Đơn đã hoàn tất: giao thành công, hoặc đơn hoàn đã trả xong về người gửi. */
    public static boolean isDone(ShipmentOrder order) {
        return order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.RETURNED;
    }

    /**
     * Hạn tự xuất (cũng là hạn khách yêu cầu HĐ công ty): mốc thanh toán + 3 tiếng và đơn phải đã hoàn tất.
     * Gửi trả hoàn tất sau mốc + 3 tiếng thì hạn = lúc hoàn tất; chưa hoàn tất ({@code doneAt} null) → null.
     */
    public static Instant deadline(ShipmentOrder order, Instant doneAt) {
        Instant paid = paidAt(order, doneAt);
        if (paid == null || doneAt == null) {
            return null;
        }
        Instant d = paid.plus(WINDOW);
        return doneAt.isAfter(d) ? doneAt : d;
    }

    public static boolean issuedLate(Instant deadline, Instant issuedAt) {
        return deadline != null && issuedAt != null && issuedAt.isAfter(deadline.plus(LATE_GRACE));
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
