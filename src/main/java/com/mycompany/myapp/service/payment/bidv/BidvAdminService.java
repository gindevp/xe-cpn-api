package com.mycompany.myapp.service.payment.bidv;

import com.mycompany.myapp.domain.BankTransaction;
import com.mycompany.myapp.domain.BidvCallLog;
import com.mycompany.myapp.domain.BidvConfig;
import com.mycompany.myapp.domain.BidvTestBill;
import com.mycompany.myapp.repository.BankTransactionRepository;
import com.mycompany.myapp.repository.BidvCallLogRepository;
import com.mycompany.myapp.repository.BidvConfigRepository;
import com.mycompany.myapp.repository.BidvTestBillRepository;
import com.mycompany.myapp.service.audit.AuditRecorder;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Tab "Thu hộ" màn Tích hợp: cấu hình BIDV, hóa đơn test, nhật ký cuộc gọi. */
@Service
@Transactional
public class BidvAdminService {

    static final String AUDIT_ENTITY = "BidvConfig";
    static final int MAX_CUSTOMER_ID = 13;
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BidvConfigRepository configRepository;
    private final BidvTestBillRepository testBillRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final BidvCallLogRepository callLogRepository;
    private final AuditRecorder auditRecorder;

    public BidvAdminService(
        BidvConfigRepository configRepository,
        BidvTestBillRepository testBillRepository,
        BankTransactionRepository bankTransactionRepository,
        BidvCallLogRepository callLogRepository,
        AuditRecorder auditRecorder
    ) {
        this.configRepository = configRepository;
        this.testBillRepository = testBillRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.callLogRepository = callLogRepository;
        this.auditRecorder = auditRecorder;
    }

    // ---------------------------------------------------------------- cấu hình

    public record ConfigView(
        boolean enabled,
        String mode,
        String serviceId,
        String vaPrefix,
        String allowedIps,
        /** CPN cấp cho BIDV — admin cần xem để gửi BIDV. */
        String apiKey,
        boolean apiKeyRequired,
        boolean checksumRequired,
        boolean secretConfigured,
        Instant updatedAt
    ) {}

    /** null = giữ nguyên. secretCode trống = giữ secret cũ. */
    public record ConfigUpdate(
        Boolean enabled,
        String serviceId,
        String secretCode,
        String vaPrefix,
        String allowedIps,
        Boolean apiKeyRequired,
        Boolean checksumRequired
    ) {}

    public ConfigView getConfig() {
        return toView(ensureConfig());
    }

    public ConfigView updateConfig(ConfigUpdate in) {
        if (in == null) {
            throw bad("Thiếu dữ liệu cấu hình");
        }
        BidvConfig c = ensureConfig();
        if (in.serviceId() != null) {
            String v = trimToNull(in.serviceId());
            if (v != null && !v.matches("[A-Za-z0-9_.-]{1,50}")) {
                throw bad("Mã dịch vụ chỉ gồm chữ, số, . _ - (tối đa 50 ký tự)");
            }
            c.setServiceId(v);
        }
        if (in.vaPrefix() != null) {
            String v = trimToNull(in.vaPrefix());
            if (v != null && !v.matches("[A-Za-z0-9]{1,20}")) {
                throw bad("Tiền tố TK định danh chỉ gồm chữ và số (tối đa 20 ký tự)");
            }
            c.setVaPrefix(v == null ? null : v.toUpperCase(Locale.ROOT));
        }
        if (in.allowedIps() != null) {
            String v = trimToNull(in.allowedIps());
            if (v != null && v.length() > 1000) {
                throw bad("Danh sách IP quá dài");
            }
            c.setAllowedIps(v);
        }
        if (notBlank(in.secretCode())) {
            if (in.secretCode().trim().length() > 255) {
                throw bad("Secret code quá dài");
            }
            c.setSecretCode(in.secretCode().trim());
        }
        if (in.apiKeyRequired() != null) {
            c.setApiKeyRequired(in.apiKeyRequired());
        }
        if (in.checksumRequired() != null) {
            c.setChecksumRequired(in.checksumRequired());
        }
        if (in.enabled() != null) {
            c.setEnabled(in.enabled());
        }
        c.setMode(BidvConfig.MODE_TEST);
        c.setUpdatedAt(Instant.now());
        c = configRepository.save(c);
        auditRecorder.record(
            "BIDV_CONFIG_UPDATE",
            AUDIT_ENTITY,
            "bidv",
            (Boolean.TRUE.equals(c.getEnabled()) ? "Bật" : "Tắt") +
            " · " +
            c.getMode() +
            " · serviceId " +
            nz(c.getServiceId()) +
            " · tiền tố " +
            nz(c.getVaPrefix()) +
            (notBlank(in.secretCode()) ? " · đổi secret" : "")
        );
        return toView(c);
    }

    public ConfigView regenerateApiKey() {
        BidvConfig c = ensureConfig();
        c.setApiKey(newApiKey());
        c.setUpdatedAt(Instant.now());
        c = configRepository.save(c);
        auditRecorder.record("BIDV_API_KEY_REGENERATE", AUDIT_ENTITY, "bidv", "Tạo lại API key cấp cho BIDV");
        return toView(c);
    }

    private BidvConfig ensureConfig() {
        BidvConfig c = configRepository.findFirstByOrderByIdAsc().orElseGet(BidvConfig::new);
        if (c.getId() == null || !notBlank(c.getApiKey())) {
            c.setApiKey(newApiKey());
            c.setUpdatedAt(Instant.now());
            c = configRepository.save(c);
        }
        return c;
    }

    static String newApiKey() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return "cpnbidv_" + HexFormat.of().formatHex(b);
    }

    private static ConfigView toView(BidvConfig c) {
        return new ConfigView(
            Boolean.TRUE.equals(c.getEnabled()),
            c.getMode(),
            c.getServiceId(),
            c.getVaPrefix(),
            c.getAllowedIps(),
            c.getApiKey(),
            !Boolean.FALSE.equals(c.getApiKeyRequired()),
            !Boolean.FALSE.equals(c.getChecksumRequired()),
            notBlank(c.getSecretCode()),
            c.getUpdatedAt()
        );
    }

    // ---------------------------------------------------------------- hóa đơn test

    public record TestBillView(
        Long id,
        String customerId,
        /** Số TK định danh hiển thị trên QR = tiền tố + mã KH. */
        String virtualAccount,
        String customerName,
        BigDecimal amount,
        String remark,
        Instant createdAt,
        String paidTransId,
        Instant paidAt
    ) {}

    public record TestBillCreate(String customerId, String customerName, BigDecimal amount, String remark) {}

    @Transactional(readOnly = true)
    public List<TestBillView> listTestBills() {
        String prefix = configRepository.findFirstByOrderByIdAsc().map(BidvConfig::getVaPrefix).orElse(null);
        List<BidvTestBill> bills = testBillRepository.findAllByOrderByIdDesc();
        Map<Long, BankTransaction> paid = bills.isEmpty()
            ? Map.of()
            : bankTransactionRepository
                .findByTargetTypeAndTargetIdIn(BankTransaction.TARGET_TEST, bills.stream().map(BidvTestBill::getId).toList())
                .stream()
                .collect(Collectors.toMap(BankTransaction::getTargetId, Function.identity(), (a, b) -> a.getId() > b.getId() ? a : b));
        return bills
            .stream()
            .map(b -> {
                BankTransaction t = paid.get(b.getId());
                return new TestBillView(
                    b.getId(),
                    b.getCustomerId(),
                    nz(prefix) + b.getCustomerId(),
                    b.getCustomerName(),
                    b.getAmount(),
                    b.getRemark(),
                    b.getCreatedAt(),
                    t != null ? t.getTransId() : null,
                    t != null ? t.getCreatedAt() : null
                );
            })
            .toList();
    }

    public List<TestBillView> addTestBill(TestBillCreate in) {
        if (in == null) {
            throw bad("Thiếu dữ liệu hóa đơn test");
        }
        String code = trimToNull(in.customerId());
        if (code == null || !code.matches("[A-Za-z0-9]{1," + MAX_CUSTOMER_ID + "}")) {
            throw bad("Mã khách hàng chỉ gồm chữ và số, tối đa " + MAX_CUSTOMER_ID + " ký tự");
        }
        code = code.toUpperCase(Locale.ROOT);
        if (testBillRepository.findOneByCustomerIdIgnoreCase(code).isPresent()) {
            throw bad("Mã khách hàng " + code + " đã có");
        }
        BigDecimal amount = in.amount();
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            throw bad("Số tiền phải là số nguyên dương (VND)");
        }
        String name = trimToNull(in.customerName());
        if (name == null || name.length() > 100) {
            throw bad("Tên khách hàng bắt buộc, tối đa 100 ký tự");
        }
        String remark = trimToNull(in.remark());
        if (remark != null && remark.length() > 200) {
            throw bad("Nội dung tối đa 200 ký tự");
        }
        BidvTestBill b = new BidvTestBill();
        b.setCustomerId(code);
        b.setCustomerName(name);
        b.setAmount(amount);
        b.setRemark(remark);
        b.setCreatedAt(Instant.now());
        testBillRepository.save(b);
        auditRecorder.record("BIDV_TEST_BILL_ADD", AUDIT_ENTITY, code, "Hóa đơn test " + code + " · " + amount.toPlainString() + "đ");
        return listTestBills();
    }

    public List<TestBillView> deleteTestBill(Long id) {
        BidvTestBill b = testBillRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn test"));
        bankTransactionRepository.deleteByTarget(BankTransaction.TARGET_TEST, b.getId());
        testBillRepository.delete(b);
        auditRecorder.record("BIDV_TEST_BILL_DELETE", AUDIT_ENTITY, b.getCustomerId(), "Xoá hóa đơn test " + b.getCustomerId());
        return listTestBills();
    }

    /** Xoá các lần gạch nợ test để BIDV chạy lại bộ test từ đầu. Không đụng giao dịch thật. */
    public List<TestBillView> resetTestPayments() {
        int n = bankTransactionRepository.deleteByTargetType(BankTransaction.TARGET_TEST);
        auditRecorder.record("BIDV_TEST_RESET", AUDIT_ENTITY, "bidv", "Xoá " + n + " lần gạch nợ test");
        return listTestBills();
    }

    // ---------------------------------------------------------------- nhật ký

    public record LogView(
        Long id,
        Instant calledAt,
        String endpoint,
        String clientIp,
        String customerId,
        String transId,
        BigDecimal amount,
        String resultCode,
        String resultDesc,
        String checksumVariant,
        Integer durationMs,
        String requestBody,
        String responseBody
    ) {}

    @Transactional(readOnly = true)
    public List<LogView> logs(Integer limit) {
        int size = limit == null ? 100 : Math.max(1, Math.min(500, limit));
        return callLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, size)).stream().map(BidvAdminService::toLogView).toList();
    }

    private static LogView toLogView(BidvCallLog l) {
        return new LogView(
            l.getId(),
            l.getCalledAt(),
            l.getEndpoint(),
            l.getClientIp(),
            l.getCustomerId(),
            l.getTransId(),
            l.getAmount(),
            l.getResultCode(),
            l.getResultDesc(),
            l.getChecksumVariant(),
            l.getDurationMs(),
            l.getRequestBody(),
            l.getResponseBody()
        );
    }

    // ---------------------------------------------------------------- request mẫu

    public record SampleRequest(String type, String customerId, String transId, BigDecimal amount) {}

    /**
     * Request mẫu kèm checksum theo kiểu đặc tả BIDV — gửi BIDV đối chiếu công thức, hoặc tự gọi thử bằng curl/Postman.
     * Chuỗi đầu vào trả ra đã che secret.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> sample(SampleRequest in) {
        BidvConfig c = configRepository.findFirstByOrderByIdAsc().orElseThrow(() -> bad("Chưa lưu cấu hình BIDV"));
        if (!notBlank(c.getSecretCode())) {
            throw bad("Nhập secret code BIDV trước khi tạo request mẫu");
        }
        boolean pay = in != null && "paybill".equalsIgnoreCase(nz(in.type()).trim());
        String code = in != null ? trimToNull(in.customerId()) : null;
        if (code == null) {
            throw bad("Chọn mã khách hàng");
        }
        String customerId = nz(c.getVaPrefix()) + BidvCollectionService.normalizeCustomerId(c.getVaPrefix(), code);
        String serviceId = nz(c.getServiceId());
        String secretMask = "{SECRET}";
        Map<String, Object> body = new LinkedHashMap<>();
        String input;
        if (!pay) {
            input = BidvChecksum.getBillInputs(c.getSecretCode(), serviceId, customerId).get(BidvChecksum.GETBILL_PLUS);
            body.put("serviceId", serviceId);
            body.put("customerId", customerId);
            body.put("checksum", BidvChecksum.hash(input));
        } else {
            BigDecimal amount = in.amount();
            if (amount == null) {
                amount = testBillRepository
                    .findOneByCustomerIdIgnoreCase(BidvCollectionService.normalizeCustomerId(c.getVaPrefix(), code))
                    .map(BidvTestBill::getAmount)
                    .orElseThrow(() -> bad("Nhập số tiền"));
            }
            String amt = BidvCollectionService.plainAmount(amount);
            String transId = trimToNull(in.transId());
            if (transId == null) {
                transId = "TEST" + DateTimeFormatter.ofPattern("yyMMddHHmmss").format(Instant.now().atZone(VN));
            }
            input = BidvChecksum.payBillInputs(c.getSecretCode(), transId, amt).get(BidvChecksum.PAYBILL_PLUS_PIPE);
            body.put("transId", transId);
            body.put("transDate", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(Instant.now().atZone(VN)));
            body.put("customerId", customerId);
            body.put("billId", BidvCollectionService.normalizeCustomerId(c.getVaPrefix(), code));
            body.put("amount", amt);
            body.put("checksum", BidvChecksum.hash(input));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("path", pay ? "/api/public/bidv/paybill" : "/api/public/bidv/getbill");
        out.put("body", body);
        out.put("checksumInput", secretMask + input.substring(c.getSecretCode().length()));
        return out;
    }

    // ---------------------------------------------------------------- helpers

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static String trimToNull(String s) {
        return notBlank(s) ? s.trim() : null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
