package com.mycompany.myapp.service.payment.onepay;

import com.mycompany.myapp.domain.OnePayInvoice;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.OnePayInvoiceRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.order.CustomerTrackStatus;
import com.mycompany.myapp.service.order.OrderFacadeService;
import com.mycompany.myapp.service.order.OrderMoney;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thu cước đơn app qua tài khoản ảo OnePay. Cấu hình thu hộ tắt thì không tạo VA.
 * QR/VA chỉ khi đơn còn tab Chờ bàn giao (chờ nhận / chờ lấy / đang lấy).
 * Số VA = phần người gửi còn phải trả (GUI_TRA = cước còn thiếu; NHAN_TRA = trả trước ghi trong note).
 * Rời tab đó thì đóng VA và không trả số TK / QR.
 */
@Service
public class OnePayOrderCollectService {

    /** App ghi vào note khi thu cước 1 phần — guest create không tin paidAmount từ client. */
    private static final Pattern APP_PREPAID = Pattern.compile("\\[XVN_PREPAID:(\\d+)\\]");

    private final ShipmentOrderRepository orders;
    private final OnePayInvoiceRepository invoices;
    private final OnePayAdminService onepay;

    public OnePayOrderCollectService(ShipmentOrderRepository orders, OnePayInvoiceRepository invoices, OnePayAdminService onepay) {
        this.orders = orders;
        this.invoices = invoices;
        this.onepay = onepay;
    }

    @Transactional
    public Map<String, Object> view(String code, String phone, boolean refresh) {
        ShipmentOrder order = requireOrder(code, phone);
        BigDecimal fareDue = OrderMoney.due(order);
        BigDecimal due = senderVaDue(order, fareDue);
        Map<String, Object> out = base(order, due);
        if (!onepay.collectEnabled()) {
            out.put("enabled", false);
            out.put("qrAllowed", false);
            return out;
        }
        out.put("enabled", true);
        boolean qrAllowed = CustomerTrackStatus.pendingHandover(order);
        out.put("qrAllowed", qrAllowed);
        if (!qrAllowed) {
            closeAliveForOrder(order.getOrderCode());
            out.put("canRefresh", false);
            return out;
        }
        if (due.signum() <= 0) {
            closeAliveForOrder(order.getOrderCode());
            out.put("canRefresh", false);
            return out;
        }
        OnePayInvoice current = invoices.findFirstByOrderCodeOrderByIdDesc(order.getOrderCode()).orElse(null);
        if (current != null && alive(current)) {
            out.put("canRefresh", false);
            putAccount(out, current);
            return out;
        }
        if (current != null) {
            closeQuietly(current);
        }
        boolean closed = current != null;
        if (refresh) {
            if (!closed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã chuyển khoản vẫn còn hiệu lực");
            }
            OnePayInvoice created = onepay.createForOrder(order.getOrderCode(), due);
            out.put("canRefresh", false);
            putAccount(out, created);
            return out;
        }
        if (current == null) {
            OnePayInvoice created = onepay.createForOrder(order.getOrderCode(), due);
            out.put("canRefresh", false);
            putAccount(out, created);
            return out;
        }
        out.put("canRefresh", true);
        return out;
    }

    /** Đóng VA còn sống khi đơn đã rời Chờ bàn giao (nhập kho, lên xe, huỷ, …). */
    @Transactional
    public void closeAliveForOrder(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return;
        }
        OnePayInvoice current = invoices.findFirstByOrderCodeOrderByIdDesc(orderCode.trim()).orElse(null);
        if (current != null && alive(current)) {
            closeQuietly(current);
        }
    }

    /** Gọi sau khi lưu đơn: còn Chờ bàn giao thì giữ VA, không thì đóng. */
    @Transactional
    public void syncAfterOrderChange(ShipmentOrder order) {
        if (order == null || order.getOrderCode() == null || order.getOrderCode().isBlank()) {
            return;
        }
        if (CustomerTrackStatus.pendingHandover(order)) {
            return;
        }
        closeAliveForOrder(order.getOrderCode());
    }

    private ShipmentOrder requireOrder(String code, String phone) {
        if (code == null || code.isBlank() || phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu mã đơn hoặc số điện thoại");
        }
        ShipmentOrder order = orders
            .findOneByOrderCodeOrDraftCode(code.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn"));
        if (!OrderFacadeService.phoneMatchesSenderOrReceiver(phone.trim(), order.getSenderPhone(), order.getReceiverPhone())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn");
        }
        return order;
    }

    private Map<String, Object> base(ShipmentOrder order, BigDecimal due) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderCode", order.getOrderCode());
        out.put("amountDue", due);
        out.put("paidAmount", OrderMoney.nz(order.getPaidAmount()));
        out.put("fareAmount", OrderMoney.nz(order.getFareAmount()));
        out.put("codAmount", OrderMoney.nz(order.getCodAmount()));
        out.put("codRefundPaid", order.getCodExportedAt() != null);
        out.put("canRefresh", false);
        return out;
    }

    /**
     * Số tiền QR người gửi cần chuyển: người gửi trả = cước còn thiếu; người nhận trả = 0
     * (trừ khoản trả trước app ghi trong note); thu 1 phần = phần trả trước còn thiếu.
     */
    static BigDecimal senderVaDue(ShipmentOrder order, BigDecimal fareDue) {
        BigDecimal remaining = fareDue == null ? BigDecimal.ZERO : fareDue.max(BigDecimal.ZERO);
        if (order.getPaymentTerm() == PaymentTerm.NHAN_TRA) {
            BigDecimal prepaid = parseAppPrepaid(order.getNote());
            if (prepaid.signum() <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal still = prepaid.subtract(OrderMoney.nz(order.getPaidAmount()));
            if (still.signum() <= 0) {
                return BigDecimal.ZERO;
            }
            return still.min(remaining);
        }
        return remaining;
    }

    static BigDecimal parseAppPrepaid(String note) {
        if (note == null || note.isBlank()) {
            return BigDecimal.ZERO;
        }
        Matcher m = APP_PREPAID.matcher(note);
        if (!m.find()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(m.group(1));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean alive(OnePayInvoice inv) {
        if (!OnePayInvoice.STATE_UNPAID.equals(inv.getState())) {
            return false;
        }
        if (inv.getExpiresAt() != null && !inv.getExpiresAt().isAfter(Instant.now())) {
            return false;
        }
        String va = inv.getVaState();
        return va == null || va.isBlank() || "active".equalsIgnoreCase(va);
    }

    /**
     * Chỉ cập nhật DB trong transaction hiện tại. Gọi OnePay khoá VA sau commit —
     * nếu gọi trong cùng txn mà OnePay lỗi thì Spring đánh rollback-only dù đã catch,
     * khiến cả request thu hộ trả 500.
     */
    private void closeQuietly(OnePayInvoice inv) {
        boolean changed = false;
        if (OnePayInvoice.STATE_UNPAID.equals(inv.getState())) {
            inv.setState(OnePayInvoice.STATE_EXPIRED);
            inv.setUpdatedAt(Instant.now());
            changed = true;
        }
        if (inv.getVaState() == null || !"inactive".equalsIgnoreCase(inv.getVaState())) {
            inv.setVaState("inactive");
            inv.setUpdatedAt(Instant.now());
            changed = true;
        }
        if (changed) {
            invoices.save(inv);
        }
        Long id = inv.getId();
        if (id == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deactivateRemoteQuietly(id);
                    }
                }
            );
        } else {
            deactivateRemoteQuietly(id);
        }
    }

    private void deactivateRemoteQuietly(Long invoiceId) {
        try {
            onepay.setAccountState(invoiceId, false);
        } catch (RuntimeException ignored) {
            /* VA local đã hết hạn; khoá remote lỗi không được làm fail thu hộ */
        }
    }

    private static void putAccount(Map<String, Object> out, OnePayInvoice inv) {
        out.put("bankName", inv.getBankName());
        out.put("accountHolder", inv.getPayerName());
        out.put("accountNumber", inv.getAccountNumber());
        out.put("qrData", inv.getQrData());
        out.put("expiresAt", inv.getExpiresAt() == null ? null : inv.getExpiresAt().toString());
    }
}
