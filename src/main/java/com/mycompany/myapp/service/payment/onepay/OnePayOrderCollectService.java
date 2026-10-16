package com.mycompany.myapp.service.payment.onepay;

import com.mycompany.myapp.domain.OnePayInvoice;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.OnePayInvoiceRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.order.OrderFacadeService;
import com.mycompany.myapp.service.order.OrderMoney;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thu cước đơn app qua tài khoản ảo OnePay. Cấu hình thu hộ tắt thì không tạo VA.
 * VA sống theo hạn hoá đơn (mặc định 24 giờ). Hết hạn hoặc OnePay đã đóng thì mới cho tạo mã mới.
 */
@Service
public class OnePayOrderCollectService {

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
        BigDecimal due = OrderMoney.due(order);
        Map<String, Object> out = base(order, due);
        if (!onepay.collectEnabled()) {
            out.put("enabled", false);
            return out;
        }
        out.put("enabled", true);
        if (due.signum() <= 0) {
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

    private void closeQuietly(OnePayInvoice inv) {
        if (OnePayInvoice.STATE_UNPAID.equals(inv.getState())) {
            inv.setState(OnePayInvoice.STATE_EXPIRED);
            inv.setUpdatedAt(Instant.now());
            invoices.save(inv);
        }
        if (inv.getVaState() != null && "inactive".equalsIgnoreCase(inv.getVaState())) {
            return;
        }
        try {
            onepay.setAccountState(inv.getId(), false);
        } catch (RuntimeException ignored) {
            inv.setVaState("inactive");
            invoices.save(inv);
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
