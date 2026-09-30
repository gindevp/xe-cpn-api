package com.mycompany.myapp.service.autocall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.AutoCallRepository;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.autocall.AutoCallService.CancelOutcome;
import com.mycompany.myapp.service.config.AutoCallConfigService;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Màn Tích hợp → Auto Call: đối soát danh sách cuộc gọi, tra cứu, huỷ, gọi thử — proxy sang HHVN bằng key đã lưu. */
@Service
public class AutoCallConsoleService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_RANGE_DAYS = 31;
    static final Set<String> CALL_TYPES = Set.of("giao", "hoan");
    static final Set<String> CALL_STATUSES = Set.of("queued", "calling", "retrying", "completed", "failed", "cancelled");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter REF_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger LOG = LoggerFactory.getLogger(AutoCallConsoleService.class);

    private final IntegrationConfigRepository integrationConfigRepository;
    private final AutoCallRepository autoCallRepository;
    private final AutoCallService autoCallService;
    private final HhvnAutoCallClient client;
    private final StaffAccessService staffAccessService;

    public AutoCallConsoleService(
        IntegrationConfigRepository integrationConfigRepository,
        AutoCallRepository autoCallRepository,
        AutoCallService autoCallService,
        HhvnAutoCallClient client,
        StaffAccessService staffAccessService
    ) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.autoCallRepository = autoCallRepository;
        this.autoCallService = autoCallService;
        this.client = client;
        this.staffAccessService = staffAccessService;
    }

    /** GET /calls của HHVN. {@code from}/{@code to} là ngày (yyyy-MM-dd, giờ VN), mặc định 7 ngày gần nhất. */
    public Map<String, Object> listCalls(String from, String to, String type, String status, Integer page, Integer limit) {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        LocalDate toDate = parseDate(to, LocalDate.now(VN));
        LocalDate fromDate = parseDate(from, toDate.minusDays(6));
        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ ngày phải trước hoặc bằng đến ngày");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) + 1 > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng ngày tối đa " + MAX_RANGE_DAYS + " ngày");
        }
        Map<String, String> query = new LinkedHashMap<>();
        query.put("from", fromDate.atStartOfDay(VN).format(ISO));
        query.put("to", LocalDateTime.of(toDate, LocalTime.of(23, 59, 59)).atZone(VN).format(ISO));
        query.put("type", optionalOf(type, CALL_TYPES, "type phải là giao hoặc hoan"));
        query.put("status", optionalOf(status, CALL_STATUSES, "Trạng thái không hợp lệ"));
        query.put("page", String.valueOf(page == null || page < 1 ? 1 : page));
        query.put("limit", String.valueOf(limit == null ? 50 : Math.max(1, Math.min(200, limit))));

        IntegrationConfig cfg = requireConfigured();
        Result r = client.listCalls(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), query);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", fromDate.toString());
        out.put("to", toDate.toString());
        if (!putError(out, r)) {
            return out;
        }
        JsonNode data = r.body() != null ? r.body().path("data") : null;
        List<JsonNode> calls = new ArrayList<>();
        if (data != null && data.isArray()) {
            data.forEach(calls::add);
        }
        out.put("data", withOrderCodes(calls));
        out.put("pagination", r.body().path("pagination"));
        return out;
    }

    /** Tra 1 cuộc gọi theo callId ({@code call_…}) hoặc refId. */
    public Map<String, Object> getCall(String idOrRef) {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        String key = idOrRef == null ? "" : idOrRef.trim();
        if (key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhập callId hoặc refId");
        }
        IntegrationConfig cfg = requireConfigured();
        Result r = key.startsWith("call_")
            ? client.getCall(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), key)
            : client.getCallByRefId(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), key);
        Map<String, Object> out = new LinkedHashMap<>();
        if (!putError(out, r)) {
            return out;
        }
        JsonNode call = unwrap(r.body());
        if (call == null) {
            out.put("ok", false);
            out.put("code", "NOT_FOUND");
            out.put("message", "Không tìm thấy cuộc gọi");
            return out;
        }
        out.put("call", withOrderCodes(List.of(call)).get(0));
        return out;
    }

    /** Huỷ 1 cuộc gọi đang chờ bên HHVN; nếu là cuộc gọi của đơn thì cập nhật luôn phía CPN. */
    public Map<String, Object> cancel(String callId) {
        String id = callId == null ? "" : callId.trim();
        if (id.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu callId");
        }
        CancelOutcome outcome = autoCallService.cancelAtHhvn(id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", outcome.ok());
        if (!outcome.ok()) {
            out.put("code", outcome.code());
            out.put("message", outcome.message());
        } else {
            LOG.info("Auto call {} cancelled by {}", id, SecurityUtils.getCurrentUserLogin().orElse("system"));
        }
        if (outcome.call() != null) {
            out.put("call", withOrderCodes(List.of(outcome.call())).get(0));
        }
        return out;
    }

    /**
     * Gọi thử 1 số (không gắn đơn). Key live gọi thật, tính phí — bắt buộc {@code confirmLive = true}.
     */
    public Map<String, Object> testCall(String phone, String type, boolean confirmLive) {
        String normalized = AutoCallService.normalizePhone(phone);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SĐT không hợp lệ (cần 10 số, bắt đầu bằng 0)");
        }
        String t = type == null || type.isBlank() ? "giao" : type.trim().toLowerCase(Locale.ROOT);
        if (!CALL_TYPES.contains(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type phải là giao hoặc hoan");
        }
        IntegrationConfig cfg = requireConfigured();
        boolean sandbox = cfg.getAutocallApiKey().startsWith("xk_test_");
        if (!sandbox && !confirmLive) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key LIVE sẽ gọi thật và tính phí — cần xác nhận trước khi gọi");
        }
        String actor = SecurityUtils.getCurrentUserLogin().orElse("system");
        String refId = buildTestRefId();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "cpn-test");
        metadata.put("by", actor);
        Result r = client.createCall(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), t, normalized, refId, metadata);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sandbox", sandbox);
        out.put("refId", refId);
        out.put("phone", normalized);
        out.put("type", t);
        if (!putError(out, r)) {
            return out;
        }
        JsonNode body = r.body();
        JsonNode accepted = body != null ? body.path("accepted") : null;
        if (accepted != null && accepted.isArray() && !accepted.isEmpty()) {
            out.put("callId", accepted.get(0).path("callId").asText(null));
            out.put("status", accepted.get(0).path("status").asText(null));
            LOG.info("Auto call test {} ({}) → {} by {}", refId, sandbox ? "sandbox" : "LIVE", normalized, actor);
            return out;
        }
        JsonNode rejected = body != null ? body.path("rejected") : null;
        out.put("ok", false);
        if (rejected != null && rejected.isArray() && !rejected.isEmpty()) {
            out.put("code", rejected.get(0).path("code").asText(null));
            out.put("message", rejected.get(0).path("message").asText(null));
        } else {
            out.put("code", "UNEXPECTED_RESPONSE");
            out.put("message", "HHVN không trả accepted/rejected");
        }
        return out;
    }

    static String buildTestRefId() {
        return "CPN-TEST-" + LocalDateTime.now(VN).format(REF_TIME) + "-" + Long.toString(RANDOM.nextInt(Integer.MAX_VALUE), 36);
    }

    /** Gắn {@code orderCode}: ưu tiên dòng auto_call phía CPN, sau đó metadata.orderCode HHVN trả lại. */
    private List<JsonNode> withOrderCodes(List<JsonNode> calls) {
        List<String> refIds = calls.stream().map(c -> c.path("refId").asText(null)).filter(s -> s != null && !s.isBlank()).toList();
        Map<String, String> codes = new HashMap<>();
        if (!refIds.isEmpty()) {
            autoCallRepository.findOrderCodesByRefIds(refIds).forEach(x -> codes.put(x.getRefId(), x.getOrderCode()));
        }
        List<JsonNode> out = new ArrayList<>(calls.size());
        for (JsonNode c : calls) {
            if (!(c instanceof ObjectNode)) {
                out.add(c);
                continue;
            }
            ObjectNode copy = ((ObjectNode) c).deepCopy();
            String code = codes.get(c.path("refId").asText(""));
            if (code == null) {
                code = c.path("metadata").path("orderCode").asText(null);
            }
            if (code != null) {
                copy.put("orderCode", code);
            }
            out.add(copy);
        }
        return out;
    }

    /** Ghi ok/code/message vào out; trả true nếu HHVN trả thành công. */
    private static boolean putError(Map<String, Object> out, Result r) {
        out.put("ok", r.ok());
        out.put("httpStatus", r.httpStatus());
        if (r.ok()) {
            return true;
        }
        out.put("code", r.code());
        out.put("message", AutoCallConfigService.humanMessage(r.code(), r.message()));
        return false;
    }

    private static JsonNode unwrap(JsonNode body) {
        if (body == null) {
            return null;
        }
        if (body.has("callId")) {
            return body;
        }
        for (String key : List.of("call", "data")) {
            JsonNode n = body.get(key);
            if (n != null && n.isObject()) return n;
            if (n instanceof ArrayNode arr && !arr.isEmpty()) return arr.get(0);
        }
        return null;
    }

    private IntegrationConfig requireConfigured() {
        IntegrationConfig cfg = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        if (cfg == null || !cfg.isAutocallApiKeyConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa lưu API key Auto Call");
        }
        return cfg;
    }

    private static LocalDate parseDate(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày không hợp lệ: " + s);
        }
    }

    private static String optionalOf(String value, Set<String> allowed, String error) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(v)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        return v;
    }
}
