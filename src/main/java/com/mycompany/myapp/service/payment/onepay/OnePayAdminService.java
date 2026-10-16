package com.mycompany.myapp.service.payment.onepay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.BankTransaction;
import com.mycompany.myapp.domain.OnePayCallLog;
import com.mycompany.myapp.domain.OnePayConfig;
import com.mycompany.myapp.domain.OnePayInvoice;
import com.mycompany.myapp.repository.BankTransactionRepository;
import com.mycompany.myapp.repository.OnePayCallLogRepository;
import com.mycompany.myapp.repository.OnePayConfigRepository;
import com.mycompany.myapp.repository.OnePayInvoiceRepository;
import com.mycompany.myapp.service.audit.AuditRecorder;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Mục OnePay trong tab "Thu hộ" màn Tích hợp: cấu hình, hoá đơn test, nhật ký. */
@Service
@Transactional
public class OnePayAdminService {

    static final String AUDIT_ENTITY = "OnePayConfig";
    public static final String IPN_PATH = "/api/public/onepay/ipn";
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter EXPIRED_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(VN);
    private static final DateTimeFormatter REF_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss").withZone(VN);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${cpn.collect-account-holder:DLLXEVIETNAM}")
    private String collectHolder;

    private final OnePayConfigRepository configRepository;
    private final OnePayInvoiceRepository invoiceRepository;
    private final OnePayCallLogRepository callLogRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final OnePayClient client;
    private final ObjectMapper objectMapper;
    private final AuditRecorder auditRecorder;

    public OnePayAdminService(
        OnePayConfigRepository configRepository,
        OnePayInvoiceRepository invoiceRepository,
        OnePayCallLogRepository callLogRepository,
        BankTransactionRepository bankTransactionRepository,
        OnePayClient client,
        ObjectMapper objectMapper,
        AuditRecorder auditRecorder
    ) {
        this.configRepository = configRepository;
        this.invoiceRepository = invoiceRepository;
        this.callLogRepository = callLogRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.client = client;
        this.objectMapper = objectMapper;
        this.auditRecorder = auditRecorder;
    }

    // ---------------------------------------------------------------- cấu hình

    public record ConfigView(
        boolean enabled,
        String environment,
        String baseUrl,
        String partnerId,
        boolean partnerKeyConfigured,
        String bankSwift,
        String ipnUrl,
        int invoiceTtlHours,
        boolean signatureRequired,
        boolean insecureSsl,
        Instant updatedAt
    ) {}

    /** null = giữ nguyên. partnerKey trống = giữ key cũ. */
    public record ConfigUpdate(
        Boolean enabled,
        String environment,
        String partnerId,
        String partnerKey,
        String bankSwift,
        String ipnUrl,
        Integer invoiceTtlHours,
        Boolean signatureRequired,
        Boolean insecureSsl
    ) {}

    @Transactional(readOnly = true)
    public ConfigView getConfig() {
        return toView(configRepository.findFirstByOrderByIdAsc().orElseGet(OnePayConfig::new));
    }

    public ConfigView updateConfig(ConfigUpdate in) {
        if (in == null) {
            throw bad("Thiếu dữ liệu cấu hình");
        }
        OnePayConfig c = configRepository.findFirstByOrderByIdAsc().orElseGet(OnePayConfig::new);
        if (in.environment() != null) {
            String env = in.environment().trim().toUpperCase(Locale.ROOT);
            if (!OnePayConfig.ENV_SANDBOX.equals(env) && !OnePayConfig.ENV_PRODUCTION.equals(env)) {
                throw bad("Môi trường chỉ nhận SANDBOX hoặc PRODUCTION");
            }
            c.setEnvironment(env);
        }
        if (in.partnerId() != null) {
            String v = trimToNull(in.partnerId());
            if (v != null && !v.matches("[A-Za-z0-9_.-]{1,50}")) {
                throw bad("Partner ID chỉ gồm chữ, số, . _ - (tối đa 50 ký tự)");
            }
            c.setPartnerId(v);
        }
        if (notBlank(in.partnerKey())) {
            if (in.partnerKey().trim().length() > 255) {
                throw bad("Partner key quá dài");
            }
            c.setPartnerKey(in.partnerKey().trim());
        }
        if (in.bankSwift() != null) {
            String v = trimToNull(in.bankSwift());
            if (v != null && !v.matches("[A-Za-z0-9]{4,20}")) {
                throw bad("Mã SWIFT ngân hàng chỉ gồm chữ và số");
            }
            c.setBankSwift(v == null ? null : v.toUpperCase(Locale.ROOT));
        }
        if (in.ipnUrl() != null) {
            String v = trimToNull(in.ipnUrl());
            if (v != null && (v.length() > 255 || !v.matches("https?://\\S+"))) {
                throw bad("URL IPN phải bắt đầu bằng http(s)://, tối đa 255 ký tự");
            }
            c.setIpnUrl(v);
        }
        if (in.invoiceTtlHours() != null) {
            if (in.invoiceTtlHours() < 1 || in.invoiceTtlHours() > 24 * 90) {
                throw bad("Thời hạn hoá đơn từ 1 giờ đến 90 ngày");
            }
            c.setInvoiceTtlHours(in.invoiceTtlHours());
        }
        if (in.signatureRequired() != null) {
            c.setSignatureRequired(in.signatureRequired());
        }
        if (in.insecureSsl() != null) {
            c.setInsecureSsl(in.insecureSsl());
        }
        if (in.enabled() != null) {
            if (in.enabled() && (!notBlank(c.getPartnerId()) || !notBlank(c.getPartnerKey()))) {
                throw bad("Bật OnePay cần nhập Partner ID và Partner key");
            }
            c.setEnabled(in.enabled());
        }
        c.setUpdatedAt(Instant.now());
        c = configRepository.save(c);
        auditRecorder.record(
            "ONEPAY_CONFIG_UPDATE",
            AUDIT_ENTITY,
            "onepay",
            (Boolean.TRUE.equals(c.getEnabled()) ? "Bật" : "Tắt") +
            " · " +
            c.getEnvironment() +
            " · partner " +
            nz(c.getPartnerId()) +
            (notBlank(in.partnerKey()) ? " · đổi partner key" : "")
        );
        return toView(c);
    }

    /** Tra một hoá đơn không tồn tại: OnePay trả mã 5 "không có user" nghĩa là chữ ký và partner đã đúng. */
    public Map<String, Object> testConnection() {
        OnePayConfig c = requireCredentials(false);
        String ref = "CPNPING" + REF_TIME.format(Instant.now());
        OnePayClient.Result r = client.call(
            c,
            "PING",
            ref,
            "GET",
            "/partners/" + c.getPartnerId().trim() + "/users/" + ref + "/invoices/" + ref,
            null
        );
        String code = r.json() != null ? r.json().path("errorCode").asText("") : "";
        boolean ok = "5".equals(code) || "21".equals(code);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", ok);
        out.put(
            "message",
            ok
                ? "Kết nối và chữ ký OWS hợp lệ (" + OnePayClient.baseUrl(c) + ")"
                : r.error() != null ? r.error() : "Phản hồi không như mong đợi"
        );
        out.put("httpStatus", r.httpStatus());
        out.put("response", OnePayCallLogService.stripImages(r.raw()));
        return out;
    }

    private OnePayConfig requireCredentials(boolean mustBeEnabled) {
        OnePayConfig c = configRepository.findFirstByOrderByIdAsc().orElseThrow(() -> bad("Chưa lưu cấu hình OnePay"));
        if (!notBlank(c.getPartnerId()) || !notBlank(c.getPartnerKey())) {
            throw bad("Nhập Partner ID và Partner key OnePay trước");
        }
        if (mustBeEnabled && !Boolean.TRUE.equals(c.getEnabled())) {
            throw bad("Bật thu hộ OnePay trước khi tạo hoá đơn");
        }
        return c;
    }

    private static ConfigView toView(OnePayConfig c) {
        return new ConfigView(
            Boolean.TRUE.equals(c.getEnabled()),
            c.getEnvironment(),
            OnePayClient.baseUrl(c),
            c.getPartnerId(),
            notBlank(c.getPartnerKey()),
            c.getBankSwift(),
            c.getIpnUrl(),
            c.getInvoiceTtlHours() == null ? 24 : c.getInvoiceTtlHours(),
            !Boolean.FALSE.equals(c.getSignatureRequired()),
            Boolean.TRUE.equals(c.getInsecureSsl()),
            c.getUpdatedAt()
        );
    }

    // ---------------------------------------------------------------- hoá đơn test

    public record InvoiceView(
        Long id,
        String purpose,
        String userReference,
        String invoiceReference,
        String payerName,
        BigDecimal amount,
        String description,
        String state,
        String accountNumber,
        String bankName,
        String qrData,
        String vaState,
        Instant expiresAt,
        String paidTransId,
        BigDecimal paidAmount,
        Instant paidAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record InvoiceCreate(String payerName, BigDecimal amount, String description) {}

    @Transactional(readOnly = true)
    public List<InvoiceView> listInvoices() {
        return invoiceRepository.findAllByOrderByIdDesc().stream().map(OnePayAdminService::toView).toList();
    }

    /** Tạo khách + tài khoản ảo + hoá đơn trên OnePay trong một lệnh batch; OnePay lỗi thì không lưu gì. */
    public InvoiceView createInvoice(InvoiceCreate in) {
        if (in == null) {
            throw bad("Thiếu dữ liệu hoá đơn");
        }
        BigDecimal amount = in.amount();
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            throw bad("Số tiền phải là số nguyên dương (VND)");
        }
        if (amount.compareTo(new BigDecimal("10000000000")) >= 0) {
            throw bad("Số tiền quá lớn");
        }
        String name = asciiName(in.payerName());
        if (name.isEmpty()) {
            throw bad("Tên người nộp bắt buộc (chữ không dấu / số, tối đa 70 ký tự)");
        }
        String description = trimToNull(in.description());
        if (description != null && description.length() > 200) {
            throw bad("Nội dung tối đa 200 ký tự");
        }
        OnePayConfig c = requireCredentials(true);
        String partner = c.getPartnerId().trim();
        String ref = newReference();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(c.getInvoiceTtlHours() == null ? 24 : c.getInvoiceTtlHours()));
        String desc = ascii(description != null ? description : "CPN " + ref);

        ObjectNode user = objectMapper.createObjectNode();
        user.put("name", name);
        if (notBlank(c.getBankSwift())) {
            user.put("bank_id", c.getBankSwift());
        }
        user.put("description", desc);
        user.put("expired_time", EXPIRED_TIME.format(expiresAt));
        ObjectNode invoice = objectMapper.createObjectNode();
        invoice.put("amount", amount.setScale(0, java.math.RoundingMode.DOWN).toPlainString());
        invoice.put("description", desc);
        ArrayNode batch = objectMapper.createArrayNode();
        batch
            .addObject()
            .put("name", "user")
            .put("do_on", "always")
            .put("method", "PUT")
            .put("href", "/partners/" + partner + "/users/" + ref)
            .set("body", user);
        batch
            .addObject()
            .put("name", "invoice")
            .put("do_on", "$user.response.status == 201")
            .put("method", "PUT")
            .put("href", "/partners/" + partner + "/users/" + ref + "/invoices/" + ref)
            .set("body", invoice);

        OnePayClient.Result r = client.call(c, "BATCH", ref, "POST", "/batchs", batch.toString());
        if (!r.ok()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OnePay từ chối tạo hoá đơn — " + r.error());
        }
        OnePayInvoice inv = new OnePayInvoice();
        inv.setPurpose(OnePayInvoice.PURPOSE_TEST);
        inv.setUserReference(ref);
        inv.setInvoiceReference(ref);
        inv.setPayerName(name);
        inv.setAmount(amount);
        inv.setDescription(description);
        inv.setState(OnePayInvoice.STATE_UNPAID);
        inv.setExpiresAt(expiresAt);
        applyBatchResponse(inv, r.json());
        inv.setCreatedAt(Instant.now());
        inv.setUpdatedAt(inv.getCreatedAt());
        inv = invoiceRepository.save(inv);
        auditRecorder.record(
            "ONEPAY_INVOICE_CREATE",
            AUDIT_ENTITY,
            ref,
            "Hoá đơn test OnePay " + ref + " · " + amount.toPlainString() + "đ · TK " + nz(inv.getAccountNumber())
        );
        return toView(inv);
    }

    /**
     * Tài khoản ảo thu cước một đơn. Chủ tài khoản là tên merchant, không phải tên khách.
     * OnePay từ chối thì không lưu hoá đơn.
     */
    public OnePayInvoice createForOrder(String orderCode, BigDecimal amount) {
        if (orderCode == null || orderCode.isBlank()) {
            throw bad("Thiếu mã đơn");
        }
        if (amount == null || amount.signum() <= 0) {
            throw bad("Đơn không còn số tiền cần thu");
        }
        OnePayConfig c = requireCredentials(true);
        String partner = c.getPartnerId().trim();
        String ref = newReference();
        int hours = c.getInvoiceTtlHours() == null ? 24 : c.getInvoiceTtlHours();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(hours));
        String holder = asciiName(collectHolder);
        if (holder.isEmpty()) {
            holder = "XEVIETNAM";
        }
        String desc = ascii("CPN " + orderCode.trim());
        if (desc.length() > 20) {
            desc = desc.substring(0, 20);
        }

        ObjectNode user = objectMapper.createObjectNode();
        user.put("name", holder);
        if (notBlank(c.getBankSwift())) {
            user.put("bank_id", c.getBankSwift());
        }
        user.put("description", desc);
        user.put("expired_time", EXPIRED_TIME.format(expiresAt));
        ObjectNode invoice = objectMapper.createObjectNode();
        invoice.put("amount", amount.setScale(0, java.math.RoundingMode.DOWN).toPlainString());
        invoice.put("description", desc);
        ArrayNode batch = objectMapper.createArrayNode();
        batch
            .addObject()
            .put("name", "user")
            .put("do_on", "always")
            .put("method", "PUT")
            .put("href", "/partners/" + partner + "/users/" + ref)
            .set("body", user);
        batch
            .addObject()
            .put("name", "invoice")
            .put("do_on", "$user.response.status == 201")
            .put("method", "PUT")
            .put("href", "/partners/" + partner + "/users/" + ref + "/invoices/" + ref)
            .set("body", invoice);

        OnePayClient.Result r = client.call(c, "BATCH", ref, "POST", "/batchs", batch.toString());
        if (!r.ok()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OnePay từ chối tạo tài khoản thu hộ");
        }
        OnePayInvoice inv = new OnePayInvoice();
        inv.setPurpose(OnePayInvoice.PURPOSE_ORDER);
        inv.setOrderCode(orderCode.trim());
        inv.setUserReference(ref);
        inv.setInvoiceReference(ref);
        inv.setPayerName(holder);
        inv.setAmount(amount.setScale(0, java.math.RoundingMode.DOWN));
        inv.setDescription(desc);
        inv.setState(OnePayInvoice.STATE_UNPAID);
        inv.setExpiresAt(expiresAt);
        applyBatchResponse(inv, r.json());
        inv.setCreatedAt(Instant.now());
        inv.setUpdatedAt(inv.getCreatedAt());
        return invoiceRepository.save(inv);
    }

    public boolean collectEnabled() {
        return configRepository
            .findFirstByOrderByIdAsc()
            .map(c -> Boolean.TRUE.equals(c.getEnabled()) && notBlank(c.getPartnerId()) && notBlank(c.getPartnerKey()))
            .orElse(false);
    }

    static void applyBatchResponse(OnePayInvoice inv, JsonNode json) {
        if (json == null || !json.isArray()) {
            return;
        }
        for (JsonNode block : json) {
            JsonNode body = block.path("body");
            JsonNode accounts = body.path("accounts");
            if (!accounts.isMissingNode() && !accounts.isNull()) {
                JsonNode acc = accounts.isArray() ? accounts.path(0) : accounts;
                inv.setOnepayUserId(text(body, "id"));
                inv.setAccountNumber(text(acc, "account_number"));
                inv.setBankName(text(acc, "bank_name"));
                inv.setVaState(text(acc, "state"));
                if (inv.getQrData() == null) {
                    inv.setQrData(text(acc.path("qr"), "data"));
                }
            } else if (body.has("amount")) {
                inv.setOnepayInvoiceId(text(body, "id"));
                String qr = text(body.path("qr"), "data");
                if (qr != null) {
                    inv.setQrData(qr);
                }
            }
        }
    }

    /** Tra trạng thái trên OnePay — dùng khi chưa nhận IPN (VD OnePay chưa cấu hình URL IPN). */
    public InvoiceView refreshInvoice(Long id) {
        OnePayInvoice inv = find(id);
        OnePayConfig c = requireCredentials(false);
        OnePayClient.Result r = client.call(
            c,
            "GET_INVOICE",
            inv.getUserReference(),
            "GET",
            "/partners/" + c.getPartnerId().trim() + "/users/" + inv.getUserReference() + "/invoices/" + inv.getInvoiceReference(),
            null
        );
        if (!r.ok()) {
            inv.setLastError(cut(r.error(), 255));
            inv.setUpdatedAt(Instant.now());
            return toView(invoiceRepository.save(inv));
        }
        String remote = text(r.json(), "state");
        inv.setLastError(null);
        if (inv.getOnepayInvoiceId() == null) {
            inv.setOnepayInvoiceId(text(r.json(), "id"));
        }
        if ("paid".equalsIgnoreCase(remote) && OnePayInvoice.STATE_UNPAID.equals(inv.getState())) {
            inv.setState(OnePayInvoice.STATE_PAID);
            if (inv.getPaidAt() == null) {
                inv.setPaidAt(Instant.now());
            }
            if (inv.getPaidAmount() == null) {
                inv.setLastError("OnePay báo đã thanh toán nhưng CPN chưa nhận IPN — kiểm tra URL IPN");
            }
        } else if ("expired".equalsIgnoreCase(remote) && OnePayInvoice.STATE_UNPAID.equals(inv.getState())) {
            inv.setState(OnePayInvoice.STATE_EXPIRED);
        }
        inv.setUpdatedAt(Instant.now());
        return toView(invoiceRepository.save(inv));
    }

    /** Khoá / mở tài khoản ảo bên OnePay. Khoá rồi khách không chuyển tiền vào được nữa. */
    public InvoiceView setAccountState(Long id, boolean active) {
        OnePayInvoice inv = find(id);
        OnePayConfig c = requireCredentials(false);
        String state = active ? "active" : "inactive";
        OnePayClient.Result r = client.call(
            c,
            "USER_STATE",
            inv.getUserReference(),
            "PATCH",
            "/partners/" + c.getPartnerId().trim() + "/users/" + inv.getUserReference(),
            "{\"state\":\"" + state + "\"}"
        );
        if (!r.ok()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OnePay từ chối đổi trạng thái tài khoản — " + r.error());
        }
        inv.setVaState(notBlank(text(r.json(), "state")) ? text(r.json(), "state") : state);
        inv.setUpdatedAt(Instant.now());
        inv = invoiceRepository.save(inv);
        auditRecorder.record(
            "ONEPAY_VA_STATE",
            AUDIT_ENTITY,
            inv.getUserReference(),
            (active ? "Mở" : "Khoá") + " TK ảo " + nz(inv.getAccountNumber())
        );
        return toView(inv);
    }

    /** Chỉ xoá bên CPN (kèm giao dịch test đã ghi); tài khoản ảo bên OnePay vẫn còn — khoá trước nếu cần. */
    public List<InvoiceView> deleteInvoice(Long id) {
        OnePayInvoice inv = find(id);
        if (!OnePayInvoice.PURPOSE_TEST.equals(inv.getPurpose())) {
            throw bad("Chỉ xoá được hoá đơn test");
        }
        bankTransactionRepository.deleteByTarget(BankTransaction.TARGET_ONEPAY_TEST, inv.getId());
        invoiceRepository.delete(inv);
        auditRecorder.record(
            "ONEPAY_INVOICE_DELETE",
            AUDIT_ENTITY,
            inv.getUserReference(),
            "Xoá hoá đơn test OnePay " + inv.getUserReference()
        );
        return listInvoices();
    }

    private OnePayInvoice find(Long id) {
        return invoiceRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hoá đơn OnePay"));
    }

    private static InvoiceView toView(OnePayInvoice i) {
        return new InvoiceView(
            i.getId(),
            i.getPurpose(),
            i.getUserReference(),
            i.getInvoiceReference(),
            i.getPayerName(),
            i.getAmount(),
            i.getDescription(),
            i.getState(),
            i.getAccountNumber(),
            i.getBankName(),
            i.getQrData(),
            i.getVaState(),
            i.getExpiresAt(),
            i.getPaidTransId(),
            i.getPaidAmount(),
            i.getPaidAt(),
            i.getLastError(),
            i.getCreatedAt(),
            i.getUpdatedAt()
        );
    }

    // ---------------------------------------------------------------- IPN mẫu (chỉ sandbox)

    public record SampleIpnRequest(Long invoiceId, BigDecimal amount, String apiBase) {}

    /**
     * IPN đã ký bằng partner key như OnePay gửi — để tự test endpoint IPN khi OnePay chưa cấu hình URL. Chỉ ở SANDBOX:
     * ở PRODUCTION ai có quyền màn hình cũng giả được lệnh báo có tiền.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> sampleIpn(SampleIpnRequest in) {
        OnePayConfig c = requireCredentials(false);
        if (!OnePayConfig.ENV_SANDBOX.equals(c.getEnvironment())) {
            throw bad("IPN mẫu chỉ tạo được ở môi trường SANDBOX");
        }
        if (in == null || in.invoiceId() == null) {
            throw bad("Chọn hoá đơn");
        }
        OnePayInvoice inv = find(in.invoiceId());
        BigDecimal amount = in.amount() != null ? in.amount() : inv.getAmount();
        String base = notBlank(c.getIpnUrl()) ? c.getIpnUrl().trim() : nz(trimToNull(in.apiBase())).replaceAll("/+$", "") + IPN_PATH;
        if (!base.matches("https?://\\S+")) {
            throw bad("Chưa có URL IPN — nhập URL IPN trong cấu hình");
        }
        String transId = "TCLTEST" + REF_TIME.format(Instant.now()) + (100 + RANDOM.nextInt(900));
        String url = stripQuery(base) + "?id=" + transId;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bank_txn_ref", "BIL-TEST-" + transId.substring(7));
        body.put("create_time", EXPIRED_TIME.format(Instant.now()));
        body.put("state", "Successful");
        body.put("amount", amount.setScale(0, java.math.RoundingMode.DOWN).toPlainString());
        body.put("remark", "CPN test " + inv.getInvoiceReference());
        body.put("currency", "VND");
        body.put("invoice", Map.of("id", nz(inv.getOnepayInvoiceId())));
        body.put("user", Map.of("id", nz(inv.getOnepayUserId()), "reference", inv.getUserReference(), "name", inv.getPayerName()));
        body.put(
            "account",
            Map.of(
                "id",
                "",
                "account_number",
                nz(inv.getAccountNumber()),
                "bank_name",
                nz(inv.getBankName()),
                "swift_code",
                nz(c.getBankSwift())
            )
        );
        body.put("merchant_id", c.getPartnerId().trim());
        String raw;
        try {
            raw = objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT).writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        String ts = OnePaySigner.timestamp(Instant.now());
        String expires = "3600";
        String auth = OnePaySigner.authorization(
            c.getPartnerId().trim(),
            c.getPartnerKey().trim(),
            "PUT",
            url,
            Map.of(),
            Map.of(OnePaySigner.HDR_DATE, ts, OnePaySigner.HDR_EXPIRES, expires),
            raw.getBytes(StandardCharsets.UTF_8),
            ts
        );
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put(OnePaySigner.HDR_DATE, ts);
        headers.put(OnePaySigner.HDR_EXPIRES, expires);
        headers.put(OnePaySigner.HDR_AUTHORIZATION, auth);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("method", "PUT");
        out.put("url", url);
        out.put("headers", headers);
        out.put("body", raw);
        return out;
    }

    // ---------------------------------------------------------------- nhật ký

    public record LogView(
        Long id,
        Instant calledAt,
        String direction,
        String action,
        Integer httpStatus,
        String result,
        String message,
        String reference,
        String transId,
        BigDecimal amount,
        String clientIp,
        Integer durationMs,
        String requestBody,
        String responseBody
    ) {}

    @Transactional(readOnly = true)
    public List<LogView> logs(Integer limit) {
        int size = limit == null ? 100 : Math.max(1, Math.min(500, limit));
        return callLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, size)).stream().map(OnePayAdminService::toLogView).toList();
    }

    private static LogView toLogView(OnePayCallLog l) {
        return new LogView(
            l.getId(),
            l.getCalledAt(),
            l.getDirection(),
            l.getAction(),
            l.getHttpStatus(),
            l.getResult(),
            l.getMessage(),
            l.getReference(),
            l.getTransId(),
            l.getAmount(),
            l.getClientIp(),
            l.getDurationMs(),
            l.getRequestBody(),
            l.getResponseBody()
        );
    }

    // ---------------------------------------------------------------- helpers

    /** user_reference / invoice_reference: OnePay giới hạn invoice_reference 20 ký tự [_a-zA-Z0-9.@]. */
    static String newReference() {
        return "CPN" + REF_TIME.format(Instant.now()) + (10000 + RANDOM.nextInt(90000));
    }

    static String stripQuery(String url) {
        int q = url.indexOf('?');
        return q < 0 ? url : url.substring(0, q);
    }

    /** Tên khách OnePay chỉ nhận ^[a-zA-Z0-9 ]{1,70}$. */
    static String asciiName(String s) {
        String a = ascii(s).replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        return a.length() > 70 ? a.substring(0, 70).trim() : a;
    }

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

    static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static String trimToNull(String s) {
        return notBlank(s) ? s.trim() : null;
    }

    static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }
}
