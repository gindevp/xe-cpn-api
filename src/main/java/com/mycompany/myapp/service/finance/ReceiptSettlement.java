package com.mycompany.myapp.service.finance;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.service.order.OrderMoney;
import com.mycompany.myapp.service.order.PartnerAdvance;
import java.math.BigDecimal;

/**
 * Chia số tiền phải nộp phiếu thu của một đơn thành 2 phần, mỗi phần một người chịu trách nhiệm:
 * <ul>
 *   <li>{@link #SENDER}: tiền VP gửi đang giữ (thu đầu gửi / thu tay) + cước gửi trả còn nợ sau nhập kho gửi.</li>
 *   <li>{@link #DELIVERY}: tiền thu lúc giao + cước nhận trả còn nợ + COD — chỉ sau DELIVERED.</li>
 * </ul>
 * Phiếu đã ghi phần (SENDER / DELIVERY) trừ đúng phần đó. Phiếu cũ không ghi phần vẫn trừ SENDER trước, phần dư vào DELIVERY.
 */
public final class ReceiptSettlement {

    static final String SENDER = "SENDER";
    static final String DELIVERY = "DELIVERY";

    /** Ghi chú payment khi phiếu thu phía VP gửi thu nốt cước gửi trả. */
    static final String NOTE_RECEIPT_SENDER = "RECEIPT_SENDER";
    /** Ghi chú payment khi phiếu thu phía giao thu nốt cước (giữ giá trị cũ). */
    static final String NOTE_RECEIPT_DELIVERY = "RECEIPT";
    static final String NOTE_RECEIPT_COD = "RECEIPT_COD";

    private ReceiptSettlement() {}

    /**
     * @param deliverySidePaid tổng payment SAU thu phía giao (POD… / RECEIPT)
     * @param codPaid tổng payment COD đã ghi
     * @param receiptedSender phiếu đã ghi rõ phần VP gửi
     * @param receiptedDelivery phiếu đã ghi rõ phần giao
     * @param receiptedLegacy phiếu cũ không ghi phần — trừ VP gửi trước
     */
    record Totals(
        BigDecimal deliverySidePaid,
        BigDecimal codPaid,
        BigDecimal receiptedSender,
        BigDecimal receiptedDelivery,
        BigDecimal receiptedLegacy
    ) {
        /** {@code receiptedLegacy} là tổng phiếu chưa tách phần. */
        Totals(BigDecimal deliverySidePaid, BigDecimal codPaid, BigDecimal receiptedLegacy) {
            this(deliverySidePaid, codPaid, BigDecimal.ZERO, BigDecimal.ZERO, receiptedLegacy);
        }

        static final Totals ZERO = new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * @param senderOut còn phải nộp phía VP gửi
     * @param senderHeldOut phần senderOut là tiền đã thu (không ghi thêm payment)
     * @param senderFareDue cước gửi trả còn nợ, lập phiếu sẽ ghi payment
     * @param deliveryOut còn phải nộp phía giao
     * @param deliveryHeldOut phần deliveryOut là tiền đã thu lúc giao
     * @param deliveryFareDue cước nhận trả còn nợ sau giao
     * @param codDue COD chưa ghi payment
     */
    record Split(
        BigDecimal senderOut,
        BigDecimal senderHeldOut,
        BigDecimal senderFareDue,
        BigDecimal deliveryOut,
        BigDecimal deliveryHeldOut,
        BigDecimal deliveryFareDue,
        BigDecimal codDue
    ) {
        static final Split NONE = new Split(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );

        BigDecimal totalOut() {
            return senderOut.add(deliveryOut);
        }
    }

    public static boolean isDeliverySidePayment(PaymentKind kind, String note) {
        if (kind != PaymentKind.SAU) {
            return false;
        }
        String n = note == null ? "" : note.trim().toUpperCase();
        return n.startsWith("POD") || n.equals(NOTE_RECEIPT_DELIVERY);
    }

    static boolean pastSenderWarehouse(ShipmentOrder order) {
        if (order.getForwardStage() != null) {
            return true;
        }
        OrderStatus st = order.getStatus();
        return (
            st == OrderStatus.IN_TRANSIT ||
            st == OrderStatus.WAITING ||
            st == OrderStatus.AT_DEST ||
            st == OrderStatus.OUT_FOR_DELIVERY ||
            st == OrderStatus.FAILED_DELIVERY
        );
    }

    static Split split(ShipmentOrder order, Totals totals) {
        OrderStatus st = order == null ? null : order.getStatus();
        if (st == null || st == OrderStatus.DRAFT || st == OrderStatus.CANCELLED) {
            return Split.NONE;
        }
        Totals t = totals == null ? Totals.ZERO : totals;
        BigDecimal paid = OrderMoney.nz(order.getPaidAmount());
        BigDecimal due = OrderMoney.collectDue(order);
        BigDecimal cod = OrderMoney.nz(order.getCodAmount());
        boolean delivered = st == OrderStatus.DELIVERED;
        boolean returned = st == OrderStatus.RETURNING || st == OrderStatus.RETURNED;
        boolean guiTra = order.getPaymentTerm() == PaymentTerm.GUI_TRA;

        // Đơn hoàn: mọi tiền đã thu quy về VP gửi.
        BigDecimal deliveryPaid = returned ? BigDecimal.ZERO : OrderMoney.nz(t.deliverySidePaid()).min(paid);
        BigDecimal senderHeld = paid.subtract(deliveryPaid);
        // Tài xế Ahamove đã ứng cước nhưng NV chưa ghi nhận: số đó là nợ người book ship (phần giao), không phải VP gửi.
        BigDecimal pendingAdvance = !returned && PartnerAdvance.pending(order) ? PartnerAdvance.amount(order).min(due) : BigDecimal.ZERO;
        BigDecimal senderFareDue = guiTra && !returned && (delivered || pastSenderWarehouse(order))
            ? nonNegative(due.subtract(pendingAdvance))
            : BigDecimal.ZERO;
        BigDecimal deliveryFareDue = delivered && !guiTra ? nonNegative(due.subtract(pendingAdvance)) : BigDecimal.ZERO;
        BigDecimal deliveryCod = delivered ? cod : BigDecimal.ZERO;

        BigDecimal senderExpected = senderHeld.add(senderFareDue);
        BigDecimal deliveryExpected = deliveryPaid.add(deliveryFareDue).add(deliveryCod).add(pendingAdvance);

        BigDecimal rSender = OrderMoney.nz(t.receiptedSender()).max(BigDecimal.ZERO).min(senderExpected);
        BigDecimal rDelivery = OrderMoney.nz(t.receiptedDelivery()).max(BigDecimal.ZERO).min(deliveryExpected);
        BigDecimal legacy = OrderMoney.nz(t.receiptedLegacy()).max(BigDecimal.ZERO);
        BigDecimal takeSender = legacy.min(senderExpected.subtract(rSender));
        rSender = rSender.add(takeSender);
        rDelivery = rDelivery.add(legacy.subtract(takeSender).min(deliveryExpected.subtract(rDelivery)));

        return new Split(
            senderExpected.subtract(rSender),
            nonNegative(senderHeld.subtract(rSender)),
            senderFareDue,
            nonNegative(deliveryExpected.subtract(rDelivery)),
            nonNegative(deliveryPaid.subtract(rDelivery)),
            deliveryFareDue,
            delivered ? nonNegative(cod.subtract(OrderMoney.nz(t.codPaid()))) : BigDecimal.ZERO
        );
    }

    private static BigDecimal nonNegative(BigDecimal v) {
        return v.signum() < 0 ? BigDecimal.ZERO : v;
    }
}
