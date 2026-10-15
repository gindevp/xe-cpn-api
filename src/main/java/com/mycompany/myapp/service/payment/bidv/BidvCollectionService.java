package com.mycompany.myapp.service.payment.bidv;

import com.fasterxml.jackson.databind.JsonNode;
import com.mycompany.myapp.domain.BankTransaction;
import com.mycompany.myapp.domain.BidvConfig;
import com.mycompany.myapp.domain.BidvTestBill;
import com.mycompany.myapp.repository.BankTransactionRepository;
import com.mycompany.myapp.repository.BidvConfigRepository;
import com.mycompany.myapp.repository.BidvTestBillRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BIDV gọi vào CPN: vấn tin (getbill) và gạch nợ (paybill) — đặc tả "Thu hộ định danh KHDN" Phần I.
 *
 * <p>Mọi kết quả nghiệp vụ trả {@code result_code} theo bảng mã BIDV (không ném lỗi HTTP). Chế độ TEST chỉ trả lời
 * {@link BidvTestBill}, không đụng đơn hàng / phiếu thu.
 */
@Service
@Transactional
public class BidvCollectionService {

    public static final String OK = "000";
    public static final String MISSING_PARAM = "001";
    public static final String IP_DENIED = "002";
    public static final String AUTH_FAILED = "003";
    public static final String CHECKSUM_INVALID = "004";
    public static final String SERVICE_INVALID = "006";
    public static final String CUSTOMER_NOT_FOUND = "011";
    public static final String BILL_NOT_FOUND = "021";
    public static final String AMOUNT_MISMATCH = "022";
    public static final String ALREADY_PAID = "023";
    public static final String SYSTEM_ERROR = "031";

    private final BidvConfigRepository configRepository;
    private final BidvTestBillRepository testBillRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public BidvCollectionService(
        BidvConfigRepository configRepository,
        BidvTestBillRepository testBillRepository,
        BankTransactionRepository bankTransactionRepository
    ) {
        this.configRepository = configRepository;
        this.testBillRepository = testBillRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    /** IP đã qua ClientIpResolver; apiKey từ header X-API-KEY hoặc Authorization: Bearer. */
    public record CallContext(String clientIp, String apiKey) {}

    public record Outcome(Map<String, Object> body, String customerId, String transId, BigDecimal amount, String checksumVariant) {
        public String resultCode() {
            return String.valueOf(body.get("result_code"));
        }

        public String resultDesc() {
            Object d = body.get("result_desc");
            return d == null ? null : d.toString();
        }
    }

    // ---------------------------------------------------------------- getbill

    public Outcome getBill(JsonNode req, CallContext ctx) {
        String customerId = text(req, "customerId", "customer_id");
        String serviceId = text(req, "serviceId", "service_id");
        String checksum = text(req, "checksum");

        BidvConfig cfg = configRepository.findFirstByOrderByIdAsc().orElse(null);
        Optional<Outcome> denied = gate(cfg, ctx, serviceId, customerId, null, null);
        if (denied.isPresent()) {
            return denied.get();
        }
        if (customerId == null) {
            return fail(MISSING_PARAM, "Thieu tham so customerId", null, null, null, null);
        }
        String sidForHash = serviceId != null ? serviceId : nz(cfg.getServiceId());
        ChecksumCheck sum = checkChecksum(cfg, checksum, BidvChecksum.matchGetBill(cfg.getSecretCode(), sidForHash, customerId, checksum));
        if (!sum.ok()) {
            return fail(CHECKSUM_INVALID, sum.reason(), customerId, null, null, null);
        }
        if (!BidvConfig.MODE_TEST.equals(cfg.getMode())) {
            return fail(SYSTEM_ERROR, "Che do thu that chua mo", customerId, null, null, sum.variant());
        }

        String code = normalizeCustomerId(cfg.getVaPrefix(), customerId);
        BidvTestBill bill = testBillRepository.findOneByCustomerIdIgnoreCase(code).orElse(null);
        if (bill == null) {
            return fail(CUSTOMER_NOT_FOUND, "Ma khach hang khong dung/ khong ton tai", customerId, null, null, sum.variant());
        }
        if (paidTransaction(bill).isPresent()) {
            return fail(ALREADY_PAID, "Hoa don da gach no roi", customerId, null, bill.getAmount(), sum.variant());
        }

        String amount = plainAmount(bill.getAmount());
        String billId = bill.getCustomerId();
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("bill_id", billId);
        line.put("amount", amount);
        line.put("remark", ascii(bill.getRemark() != null ? bill.getRemark() : "CPN " + billId));
        Map<String, Object> period = new LinkedHashMap<>();
        period.put("period", "CPN " + billId);
        period.put("data", List.of(line));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result_code", OK);
        body.put("result_desc", "success");
        body.put("customer_id", customerId);
        body.put("customer_name", ascii(bill.getCustomerName()));
        body.put("customer_addr", "");
        body.put("bill_id", billId);
        body.put("type", "1");
        body.put("total_amount", amount);
        body.put("data", List.of(period));
        return new Outcome(body, customerId, null, bill.getAmount(), sum.variant());
    }

    // ---------------------------------------------------------------- paybill

    public Outcome payBill(JsonNode req, CallContext ctx) {
        String transId = text(req, "transId", "trans_id");
        String transDate = text(req, "transDate", "trans_date");
        String customerId = text(req, "customerId", "customer_id");
        String billId = text(req, "billId", "bill_id");
        String serviceId = text(req, "serviceId", "service_id");
        String amountRaw = text(req, "amount");
        String checksum = text(req, "checksum");
        BigDecimal amount = parseAmount(amountRaw);

        BidvConfig cfg = configRepository.findFirstByOrderByIdAsc().orElse(null);
        Optional<Outcome> denied = gate(cfg, ctx, serviceId, customerId, transId, amount);
        if (denied.isPresent()) {
            return denied.get();
        }
        if (transId == null || customerId == null || billId == null || amountRaw == null) {
            return fail(MISSING_PARAM, "Thieu tham so dau vao", customerId, transId, amount, null);
        }
        if (amount == null || amount.signum() <= 0) {
            return fail(MISSING_PARAM, "So tien khong hop le", customerId, transId, null, null);
        }
        ChecksumCheck sum = checkChecksum(cfg, checksum, BidvChecksum.matchPayBill(cfg.getSecretCode(), transId, amountRaw, checksum));
        if (!sum.ok()) {
            return fail(CHECKSUM_INVALID, sum.reason(), customerId, transId, amount, null);
        }

        Optional<BankTransaction> existing = bankTransactionRepository.findOneByTransId(transId);
        if (existing.isPresent()) {
            BankTransaction t = existing.get();
            if (t.getBillId().equalsIgnoreCase(billId) && t.getAmount().compareTo(amount) == 0) {
                return ok(customerId, transId, amount, sum.variant());
            }
            return fail(ALREADY_PAID, "transId da duoc dung cho hoa don khac", customerId, transId, amount, sum.variant());
        }
        if (!BidvConfig.MODE_TEST.equals(cfg.getMode())) {
            return fail(SYSTEM_ERROR, "Che do thu that chua mo", customerId, transId, amount, sum.variant());
        }

        String code = normalizeCustomerId(cfg.getVaPrefix(), customerId);
        BidvTestBill found = testBillRepository.findOneByCustomerIdIgnoreCase(code).orElse(null);
        if (found == null) {
            return fail(CUSTOMER_NOT_FOUND, "Ma khach hang khong dung/ khong ton tai", customerId, transId, amount, sum.variant());
        }
        BidvTestBill bill = testBillRepository.findByIdForUpdate(found.getId()).orElse(found);
        if (!bill.getCustomerId().equalsIgnoreCase(normalizeCustomerId(cfg.getVaPrefix(), billId))) {
            return fail(BILL_NOT_FOUND, "Ma hoa don khong ton tai", customerId, transId, amount, sum.variant());
        }
        if (paidTransaction(bill).isPresent()) {
            return fail(ALREADY_PAID, "Hoa don da gach no roi", customerId, transId, amount, sum.variant());
        }
        if (bill.getAmount().compareTo(amount) != 0) {
            return fail(
                AMOUNT_MISMATCH,
                "So tien gui len khong dung voi so tien trong hoa don",
                customerId,
                transId,
                amount,
                sum.variant()
            );
        }

        BankTransaction t = new BankTransaction();
        t.setProvider(BankTransaction.PROVIDER_BIDV);
        t.setTransId(transId);
        t.setTransDate(transDate);
        t.setCustomerId(customerId);
        t.setBillId(bill.getCustomerId());
        t.setAmount(amount);
        t.setTargetType(BankTransaction.TARGET_TEST);
        t.setTargetId(bill.getId());
        t.setCreatedAt(Instant.now());
        bankTransactionRepository.saveAndFlush(t);
        return ok(customerId, transId, amount, sum.variant());
    }

    // ---------------------------------------------------------------- kiểm tra chung

    private Optional<Outcome> gate(
        BidvConfig cfg,
        CallContext ctx,
        String serviceId,
        String customerId,
        String transId,
        BigDecimal amount
    ) {
        if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) {
            return Optional.of(fail(SYSTEM_ERROR, "Dich vu chua bat", customerId, transId, amount, null));
        }
        if (!ipAllowed(cfg.getAllowedIps(), ctx.clientIp())) {
            return Optional.of(fail(IP_DENIED, "IP khong duoc truy cap", customerId, transId, amount, null));
        }
        boolean keyGiven = ctx.apiKey() != null && !ctx.apiKey().isBlank();
        if ((Boolean.TRUE.equals(cfg.getApiKeyRequired()) || keyGiven) && !tokenEquals(cfg.getApiKey(), ctx.apiKey())) {
            return Optional.of(fail(AUTH_FAILED, "Thong tin dang nhap khong dung", customerId, transId, amount, null));
        }
        if (serviceId != null && notBlank(cfg.getServiceId()) && !cfg.getServiceId().trim().equalsIgnoreCase(serviceId)) {
            return Optional.of(fail(SERVICE_INVALID, "Service ID khong dung/ khong ton tai", customerId, transId, amount, null));
        }
        return Optional.empty();
    }

    record ChecksumCheck(boolean ok, String variant, String reason) {}

    private static ChecksumCheck checkChecksum(BidvConfig cfg, String checksum, Optional<String> matched) {
        if (checksum == null) {
            return Boolean.TRUE.equals(cfg.getChecksumRequired())
                ? new ChecksumCheck(false, null, "Thieu checksum")
                : new ChecksumCheck(true, "NONE", null);
        }
        if (!notBlank(cfg.getSecretCode())) {
            return new ChecksumCheck(false, null, "Checksum khong hop le (CPN chua cau hinh secret)");
        }
        return matched.map(v -> new ChecksumCheck(true, v, null)).orElseGet(() -> new ChecksumCheck(false, null, "Checksum khong hop le"));
    }

    private Optional<BankTransaction> paidTransaction(BidvTestBill bill) {
        return bankTransactionRepository.findFirstByTargetTypeAndTargetIdOrderByIdDesc(BankTransaction.TARGET_TEST, bill.getId());
    }

    private static Outcome ok(String customerId, String transId, BigDecimal amount, String variant) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result_code", OK);
        body.put("result_desc", "success");
        return new Outcome(body, customerId, transId, amount, variant);
    }

    public static Outcome fail(String code, String desc, String customerId, String transId, BigDecimal amount, String variant) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result_code", code);
        body.put("result_desc", desc);
        return new Outcome(body, customerId, transId, amount, variant);
    }

    // ---------------------------------------------------------------- helpers

    /** BIDV gửi mã KH có thể kèm hoặc không kèm mã đầu TK định danh. */
    static String normalizeCustomerId(String vaPrefix, String customerId) {
        String c = customerId == null ? "" : customerId.trim().toUpperCase(Locale.ROOT);
        String p = vaPrefix == null ? "" : vaPrefix.trim().toUpperCase(Locale.ROOT);
        if (!p.isEmpty() && c.startsWith(p) && c.length() > p.length()) {
            return c.substring(p.length());
        }
        return c;
    }

    static boolean ipAllowed(String allowedIps, String clientIp) {
        if (!notBlank(allowedIps)) {
            return true;
        }
        if (!notBlank(clientIp)) {
            return false;
        }
        for (String rule : allowedIps.split("[,;\\s]+")) {
            if (rule.isBlank()) {
                continue;
            }
            try {
                if (new IpAddressMatcher(rule.trim()).matches(clientIp.trim())) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // dòng IP sai định dạng: bỏ qua
            }
        }
        return false;
    }

    static boolean tokenEquals(String expected, String given) {
        if (!notBlank(expected) || !notBlank(given)) {
            return false;
        }
        return MessageDigest.isEqual(expected.trim().getBytes(StandardCharsets.UTF_8), given.trim().getBytes(StandardCharsets.UTF_8));
    }

    /** Đọc field theo tên chuẩn hoặc alias snake_case; số được đọc nguyên dạng chuỗi. */
    static String text(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String n : names) {
            JsonNode v = node.get(n);
            if (v != null && !v.isNull()) {
                String s = v.isNumber() ? v.numberValue().toString() : v.asText();
                if (s != null && !s.isBlank()) {
                    return s.trim();
                }
            }
        }
        return null;
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

    static String plainAmount(BigDecimal v) {
        return v.setScale(0, java.math.RoundingMode.DOWN).toPlainString();
    }

    /** BIDV khuyến nghị không gửi tiếng Việt có dấu / ký tự đặc biệt. */
    static String ascii(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .replaceAll("[^A-Za-z0-9 .,_-]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
