package com.mycompany.myapp.service.payment.onepay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.BankTransaction;
import com.mycompany.myapp.domain.OnePayConfig;
import com.mycompany.myapp.domain.OnePayInvoice;
import com.mycompany.myapp.repository.BankTransactionRepository;
import com.mycompany.myapp.repository.OnePayConfigRepository;
import com.mycompany.myapp.repository.OnePayInvoiceRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OnePay báo có tiền vào tài khoản ảo (API 2 — IPN). Trả HTTP 200 + {@code error_code "0"} là OnePay ngừng gửi; khác
 * 200 thì OnePay gửi lại 3 lần, mỗi lần cách 300 giây — nên chỉ trả lỗi khi gửi lại có ích (tắt dịch vụ, sai chữ ký,
 * lỗi hệ thống).
 */
@Service
@Transactional
public class OnePayIpnService {

    public static final String OK_STATE = "successful";

    private final OnePayConfigRepository configRepository;
    private final OnePayInvoiceRepository invoiceRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ObjectMapper objectMapper;

    public OnePayIpnService(
        OnePayConfigRepository configRepository,
        OnePayInvoiceRepository invoiceRepository,
        BankTransactionRepository bankTransactionRepository,
        ObjectMapper objectMapper
    ) {
        this.configRepository = configRepository;
        this.invoiceRepository = invoiceRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * @param requestUrl URL dựng lại từ request (sau proxy có thể khác URL OnePay gọi — vì thế thử thêm URL IPN đã cấu
     *     hình)
     * @param headers tên header viết thường
     */
    public record IpnRequest(String method, String requestUrl, String queryString, String id, Map<String, String> headers, byte[] body) {}

    public record Outcome(
        int httpStatus,
        boolean accepted,
        String result,
        String message,
        String reference,
        String transId,
        BigDecimal amount
    ) {
        public Map<String, Object> body() {
            return accepted ? Map.of("error_code", "0", "message", "Success") : Map.of("error_code", "1", "message", "Fail");
        }
    }

    public Outcome handle(IpnRequest req) {
        OnePayConfig cfg = configRepository.findFirstByOrderByIdAsc().orElse(null);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) {
            return reject(503, "DISABLED", "Thu hộ OnePay đang tắt", null, req.id(), null);
        }
        JsonNode json;
        try {
            json = req.body() == null || req.body().length == 0
                ? null
                : objectMapper.readTree(new String(req.body(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            json = null;
        }
        if (json == null || !json.isObject()) {
            return reject(400, "BAD_REQUEST", "Body IPN không phải JSON", null, req.id(), null);
        }
        String reference = OnePayAdminService.text(json.path("user"), "reference");
        BigDecimal amount = parseAmount(OnePayAdminService.text(json, "amount"));
        String transId = OnePayAdminService.notBlank(req.id()) ? req.id().trim() : OnePayAdminService.text(json, "bank_txn_ref");

        boolean signed = signatureValid(cfg, req);
        if (!signed && !Boolean.FALSE.equals(cfg.getSignatureRequired())) {
            return reject(401, "BAD_SIGNATURE", "Sai chữ ký OWS", reference, transId, amount);
        }
        String sigNote = signed ? "" : " (bỏ qua chữ ký sai — đang tắt kiểm chữ ký)";

        String merchant = OnePayAdminService.text(json, "merchant_id");
        if (merchant != null && OnePayAdminService.notBlank(cfg.getPartnerId()) && !merchant.equalsIgnoreCase(cfg.getPartnerId().trim())) {
            return reject(400, "WRONG_MERCHANT", "merchant_id " + merchant + " không phải partner CPN", reference, transId, amount);
        }
        if (transId == null) {
            return reject(400, "BAD_REQUEST", "Thiếu mã giao dịch (?id=)", reference, null, amount);
        }
        String state = OnePayAdminService.text(json, "state");
        if (state == null || !OK_STATE.equals(state.toLowerCase(Locale.ROOT))) {
            return accept("IGNORED_STATE", "Trạng thái " + state + " — không ghi nhận tiền" + sigNote, reference, transId, amount);
        }
        if (amount == null || amount.signum() <= 0) {
            return reject(400, "BAD_REQUEST", "Số tiền không hợp lệ", reference, transId, null);
        }

        Optional<BankTransaction> existing = bankTransactionRepository.findOneByTransId(transId);
        if (existing.isPresent()) {
            return accept("DUPLICATE", "Giao dịch đã ghi nhận trước đó (OnePay gửi lại)" + sigNote, reference, transId, amount);
        }

        OnePayInvoice found = findInvoice(json, reference).orElse(null);
        if (found == null) {
            return accept("UNMATCHED", "Không khớp hoá đơn nào của CPN — cần đối soát tay" + sigNote, reference, transId, amount);
        }
        OnePayInvoice inv = invoiceRepository.findByIdForUpdate(found.getId()).orElse(found);

        BankTransaction t = new BankTransaction();
        t.setProvider(BankTransaction.PROVIDER_ONEPAY);
        t.setTransId(transId);
        t.setTransDate(cut(OnePayAdminService.text(json, "create_time"), 20));
        t.setCustomerId(cut(inv.getUserReference(), 40));
        t.setBillId(cut(inv.getInvoiceReference(), 40));
        t.setAmount(amount);
        t.setTargetType(BankTransaction.TARGET_ONEPAY_TEST);
        t.setTargetId(inv.getId());
        t.setCreatedAt(Instant.now());
        bankTransactionRepository.saveAndFlush(t);

        String result;
        String message;
        if (OnePayInvoice.STATE_PAID.equals(inv.getState()) || OnePayInvoice.STATE_AMOUNT_MISMATCH.equals(inv.getState())) {
            result = "EXTRA_PAYMENT";
            message = "Hoá đơn đã có tiền trước đó — ghi nhận giao dịch thừa, cần xử lý tay";
        } else {
            boolean exact = inv.getAmount().compareTo(amount) == 0;
            inv.setState(exact ? OnePayInvoice.STATE_PAID : OnePayInvoice.STATE_AMOUNT_MISMATCH);
            inv.setPaidTransId(transId);
            inv.setPaidAmount(amount);
            inv.setPaidAt(Instant.now());
            inv.setLastError(exact ? null : "Tiền về " + amount.toPlainString() + " khác số hoá đơn " + inv.getAmount().toPlainString());
            result = exact ? "PAID" : "AMOUNT_MISMATCH";
            message = exact ? "Đã ghi nhận thanh toán" : inv.getLastError();
        }
        inv.setUpdatedAt(Instant.now());
        invoiceRepository.save(inv);
        return accept(result, message + sigNote, inv.getUserReference(), transId, amount);
    }

    /** Khớp theo mã hoá đơn OnePay, rồi user_reference, rồi số tài khoản ảo — không dùng nội dung chuyển khoản. */
    private Optional<OnePayInvoice> findInvoice(JsonNode json, String reference) {
        String invoiceId = OnePayAdminService.text(json.path("invoice"), "id");
        if (invoiceId != null) {
            Optional<OnePayInvoice> byId = invoiceRepository.findOneByOnepayInvoiceId(invoiceId);
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (reference != null) {
            Optional<OnePayInvoice> byRef = invoiceRepository.findOneByUserReference(reference);
            if (byRef.isPresent()) {
                return byRef;
            }
        }
        String account = OnePayAdminService.text(json.path("account"), "account_number");
        return account != null ? invoiceRepository.findOneByAccountNumber(account) : Optional.empty();
    }

    /** OnePay ký trên toàn bộ URL IPN nó gọi; sau proxy Railway URL nhìn thấy có thể khác nên thử cả URL đã cấu hình. */
    static List<String> candidateUrls(OnePayConfig cfg, IpnRequest req) {
        Set<String> out = new LinkedHashSet<>();
        String query = OnePayAdminService.notBlank(req.queryString()) ? "?" + req.queryString() : "";
        if (OnePayAdminService.notBlank(cfg.getIpnUrl())) {
            out.add(OnePayAdminService.stripQuery(cfg.getIpnUrl().trim()) + query);
        }
        if (OnePayAdminService.notBlank(req.requestUrl())) {
            out.add(req.requestUrl() + query);
        }
        return new ArrayList<>(out);
    }

    private static boolean signatureValid(OnePayConfig cfg, IpnRequest req) {
        if (!OnePayAdminService.notBlank(cfg.getPartnerKey())) {
            return false;
        }
        for (String url : candidateUrls(cfg, req)) {
            if (OnePaySigner.verifyIpn(cfg.getPartnerKey(), req.method(), url, req.headers(), req.body())) {
                return true;
            }
        }
        return false;
    }

    private static Outcome accept(String result, String message, String reference, String transId, BigDecimal amount) {
        return new Outcome(200, true, result, message, reference, transId, amount);
    }

    private static Outcome reject(int status, String result, String message, String reference, String transId, BigDecimal amount) {
        return new Outcome(status, false, result, message, reference, transId, amount);
    }

    static BigDecimal parseAmount(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }
}
