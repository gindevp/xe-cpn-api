package com.mycompany.myapp.service.autocall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.AutoCall;
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
import com.mycompany.myapp.service.partner.VtechAutoCallClient;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
    static final int HHVN_PAGE_LIMIT = 200;
    /** HHVN giới hạn 60 request/phút — tối đa 2000 cuộc gọi mỗi lần tải danh sách. */
    static final int MAX_FETCH_PAGES = 10;
    static final Set<String> CALL_TYPES = Set.of("giao", "hoan");
    static final Set<String> CALL_STATUSES = Set.of("queued", "calling", "retrying", "completed", "failed", "cancelled");
    static final Set<String> CALL_RESULTS = Set.of("answered", "not_answered", "error");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter REF_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger LOG = LoggerFactory.getLogger(AutoCallConsoleService.class);

    private final IntegrationConfigRepository integrationConfigRepository;
    private final AutoCallRepository autoCallRepository;
    private final AutoCallService autoCallService;
    private final HhvnAutoCallClient client;
    private final VtechAutoCallClient vtechClient;
    private final StaffAccessService staffAccessService;

    public AutoCallConsoleService(
        IntegrationConfigRepository integrationConfigRepository,
        AutoCallRepository autoCallRepository,
        AutoCallService autoCallService,
        HhvnAutoCallClient client,
        VtechAutoCallClient vtechClient,
        StaffAccessService staffAccessService
    ) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.autoCallRepository = autoCallRepository;
        this.autoCallService = autoCallService;
        this.client = client;
        this.vtechClient = vtechClient;
        this.staffAccessService = staffAccessService;
    }

    /** GET /calls của HHVN. {@code from}/{@code to} là ngày (yyyy-MM-dd, giờ VN), mặc định 7 ngày gần nhất. */
    /**
     * {@code result} (answered / not_answered / error) và {@code phone} (chứa chuỗi số đã nhập, chấp nhận 84…/+84…):
     * HHVN không có 2 tham số này nên lọc phía CPN.
     */
    public Map<String, Object> listCalls(
        String from,
        String to,
        String type,
        String status,
        String result,
        String phone,
        Integer page,
        Integer limit
    ) {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        String resultFilter = optionalOf(result, CALL_RESULTS, "Kết quả không hợp lệ");
        String phoneQuery = phoneDigits(phone);
        if (phoneQuery != null && phoneQuery.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhập ít nhất 3 số điện thoại");
        }
        LocalDate toDate = parseDate(to, LocalDate.now(VN));
        LocalDate fromDate = parseDate(from, toDate.minusDays(6));
        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ ngày phải trước hoặc bằng đến ngày");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) + 1 > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng ngày tối đa " + MAX_RANGE_DAYS + " ngày");
        }
        String typeFilter = optionalOf(type, CALL_TYPES, "type phải là giao hoặc hoan");
        String statusFilter = optionalOf(status, CALL_STATUSES, "Trạng thái không hợp lệ");
        IntegrationConfig active = requireConfigured();
        if (active.isAutocallVtech()) {
            return listLocalCalls(fromDate, toDate, resultFilter, phoneQuery, page, limit);
        }
        Map<String, String> query = new LinkedHashMap<>();
        query.put("from", fromDate.atStartOfDay(VN).format(ISO));
        query.put("to", LocalDateTime.of(toDate, LocalTime.of(23, 59, 59)).atZone(VN).format(ISO));
        query.put("type", typeFilter);
        query.put("status", statusFilter);
        query.put("limit", String.valueOf(HHVN_PAGE_LIMIT));
        int pg = page == null || page < 1 ? 1 : page;
        int lim = limit == null ? 50 : Math.max(1, Math.min(HHVN_PAGE_LIMIT, limit));

        IntegrationConfig cfg = active;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", fromDate.toString());
        out.put("to", toDate.toString());
        // HHVN không có tham số sắp xếp — lấy cả khoảng ngày rồi tự xếp mới → cũ trước khi chia trang.
        List<JsonNode> all = new ArrayList<>();
        boolean truncated = false;
        for (int p = 1;; p++) {
            query.put("page", String.valueOf(p));
            Result r = client.listCalls(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), new LinkedHashMap<>(query));
            if (!putError(out, r)) {
                return out;
            }
            JsonNode data = r.body() != null ? r.body().path("data") : null;
            int got = 0;
            if (data != null && data.isArray()) {
                data.forEach(all::add);
                got = data.size();
            }
            int totalPages = r.body().path("pagination").path("totalPages").asInt(1);
            if (p >= totalPages || got < HHVN_PAGE_LIMIT) {
                break;
            }
            if (p >= MAX_FETCH_PAGES) {
                truncated = true;
                break;
            }
        }
        if (resultFilter != null) {
            all.removeIf(c -> !matchesResult(resultFilter, c));
        }
        if (phoneQuery != null) {
            all.removeIf(c -> {
                String p = phoneDigits(c.path("phone").asText(""));
                return p == null || !p.contains(phoneQuery);
            });
        }
        all.sort(Comparator.<JsonNode>comparingLong(AutoCallConsoleService::createdAtMillis).reversed());
        int start = Math.min(all.size(), (pg - 1) * lim);
        int end = Math.min(all.size(), start + lim);
        out.put("data", withOrderCodes(all.subList(start, end)));
        ObjectNode pagination = JsonNodeFactory.instance.objectNode();
        pagination.put("page", pg);
        pagination.put("limit", lim);
        pagination.put("total", all.size());
        pagination.put("totalPages", Math.max(1, (all.size() + lim - 1) / lim));
        out.put("pagination", pagination);
        if (truncated) {
            out.put("truncated", true);
        }
        return out;
    }

    /** Lọc "Không nghe" gồm cả cuộc đã huỷ. */
    static boolean matchesResult(String filter, JsonNode call) {
        String result = call.path("result").asText("");
        if (filter.equals(result)) {
            return true;
        }
        return (
            "not_answered".equals(filter) &&
            ("cancelled".equalsIgnoreCase(result) || "cancelled".equalsIgnoreCase(call.path("status").asText("")))
        );
    }

    /**
     * "Lần gọi" = cuộc thứ mấy tới được tổng đài của cùng đơn + loại gọi trong ngày (giờ VN). Lệnh gửi lỗi / chưa gửi
     * không tính và không có số.
     */
    static Map<AutoCall, Integer> dailyCallOrdinals(List<AutoCall> calls) {
        Map<AutoCall, Integer> out = new java.util.IdentityHashMap<>();
        Map<String, Integer> seen = new java.util.HashMap<>();
        calls
            .stream()
            .filter(c -> c.getCreatedAt() != null && reachedCarrier(c))
            .sorted(Comparator.comparing(AutoCall::getCreatedAt))
            .forEach(c -> {
                Object orderKey = c.getOrder() != null && c.getOrder().getId() != null ? c.getOrder().getId() : c.getPhone();
                String key = orderKey + "|" + c.getCallType() + "|" + c.getCreatedAt().atZone(VN).toLocalDate();
                out.put(c, seen.merge(key, 1, Integer::sum));
            });
        return out;
    }

    private static boolean reachedCarrier(AutoCall c) {
        String st = c.getStatus() == null ? "" : c.getStatus();
        if ("FAILED".equals(st)) {
            return !"send_error".equals(c.getResult());
        }
        return Set.of("QUEUED", "CALLING", "RINGING", "COMPLETED", "CANCELLED").contains(st);
    }

    /** Vtech không có API danh sách — đọc auto_call phía CPN, trả cùng dạng Call object HHVN cho FE. */
    private Map<String, Object> listLocalCalls(
        LocalDate fromDate,
        LocalDate toDate,
        String resultFilter,
        String phoneQuery,
        Integer page,
        Integer limit
    ) {
        int pg = page == null || page < 1 ? 1 : page;
        int lim = limit == null ? 50 : Math.max(1, Math.min(HHVN_PAGE_LIMIT, limit));
        List<AutoCall> calls = autoCallRepository.findForConsole(
            IntegrationConfig.PROVIDER_VTECH,
            fromDate.atStartOfDay(VN).toInstant(),
            toDate.plusDays(1).atStartOfDay(VN).toInstant()
        );
        Map<AutoCall, Integer> ordinal = dailyCallOrdinals(calls);
        List<JsonNode> all = new ArrayList<>();
        for (AutoCall c : calls) {
            ObjectNode n = toCallNode(c);
            n.put("attemptCount", ordinal.getOrDefault(c, 0));
            all.add(n);
        }
        if (resultFilter != null) {
            all.removeIf(c -> !matchesResult(resultFilter, c));
        }
        if (phoneQuery != null) {
            all.removeIf(c -> {
                String p = phoneDigits(c.path("phone").asText(""));
                return p == null || !p.contains(phoneQuery);
            });
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("provider", IntegrationConfig.PROVIDER_VTECH);
        out.put("from", fromDate.toString());
        out.put("to", toDate.toString());
        int start = Math.min(all.size(), (pg - 1) * lim);
        int end = Math.min(all.size(), start + lim);
        out.put("data", all.subList(start, end));
        ObjectNode pagination = JsonNodeFactory.instance.objectNode();
        pagination.put("page", pg);
        pagination.put("limit", lim);
        pagination.put("total", all.size());
        pagination.put("totalPages", Math.max(1, (all.size() + lim - 1) / lim));
        out.put("pagination", pagination);
        return out;
    }

    /**
     * Dòng auto_call → dạng Call object HHVN. Chưa gửi / đang chờ kết quả → queued; gửi lỗi → failed + send_error.
     * callId để trống thì dùng refId (FE cần khoá duy nhất cho mỗi dòng).
     */
    static ObjectNode toCallNode(AutoCall c) {
        ObjectNode n = JsonNodeFactory.instance.objectNode();
        n.put("callId", c.getCallId() != null ? c.getCallId() : c.getRefId());
        n.put("refId", c.getRefId());
        n.put("type", c.getCallType());
        n.put("phone", c.getPhone());
        String status = c.getStatus() == null ? "" : c.getStatus();
        switch (status) {
            case "COMPLETED" -> n.put("status", "completed");
            case "FAILED" -> n.put("status", "failed");
            case "CANCELLED" -> n.put("status", "cancelled");
            case "ERROR" -> n.put("status", "failed");
            default -> n.put("status", "queued");
        }
        String result = "ERROR".equals(status) ? "send_error" : c.getResult();
        if (result != null) n.put("result", result);
        if (c.getAttemptCount() != null) n.put("attemptCount", c.getAttemptCount());
        if (c.getDurationSec() != null) n.put("duration", c.getDurationSec());
        putInstant(n, "createdAt", c.getCreatedAt());
        putInstant(n, "firstCallAt", c.getFirstCallAt());
        putInstant(n, "answeredAt", c.getAnsweredAt());
        putInstant(n, "finishedAt", c.getFinishedAt());
        if (c.getRecordingUrl() != null) n.put("recordingUrl", c.getRecordingUrl());
        if (c.getErrorMessage() != null) n.put("errorMessage", c.getErrorMessage());
        if (c.getNextRetryAt() != null) putInstant(n, "nextRetryAt", c.getNextRetryAt());
        if (c.getOrder() != null) n.put("orderCode", c.getOrder().getOrderCode());
        n.put("provider", c.getProvider());
        return n;
    }

    private static void putInstant(ObjectNode n, String field, java.time.Instant at) {
        if (at != null) n.put(field, at.toString());
    }

    /** Chỉ giữ chữ số, đổi đầu 84 (11 số) về 0; null nếu rỗng. */
    static String phoneDigits(String s) {
        if (s == null) {
            return null;
        }
        String d = s.replaceAll("\\D", "");
        if (d.isEmpty()) {
            return null;
        }
        if (d.startsWith("84") && d.length() == 11) {
            d = "0" + d.substring(2);
        }
        return d;
    }

    static long createdAtMillis(JsonNode call) {
        String s = call.path("createdAt").asText("");
        if (s.isBlank()) {
            return Long.MIN_VALUE;
        }
        try {
            return OffsetDateTime.parse(s).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(s).atZone(VN).toInstant().toEpochMilli();
            } catch (DateTimeParseException e2) {
                return Long.MIN_VALUE;
            }
        }
    }

    /** Tra 1 cuộc gọi theo callId ({@code call_…}) hoặc refId. */
    public Map<String, Object> getCall(String idOrRef) {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        String key = idOrRef == null ? "" : idOrRef.trim();
        if (key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhập callId hoặc refId");
        }
        IntegrationConfig cfg = requireConfigured();
        if (cfg.isAutocallVtech()) {
            return getLocalCall(key);
        }
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

    /** Vtech: gọi thử (CPN-TEST-…) đọc kết quả webhook trong bộ nhớ; cuộc gọi của đơn đọc auto_call. */
    private Map<String, Object> getLocalCall(String key) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        if (key.startsWith("CPN-TEST-")) {
            JsonNode result = autoCallService.vtechTestResult(key);
            ObjectNode call;
            if (result instanceof ObjectNode r) {
                call = r.deepCopy();
            } else {
                call = JsonNodeFactory.instance.objectNode();
                call.put("status", "queued");
                call.put("attemptCount", 0);
            }
            call.put("callId", key);
            call.put("refId", key);
            out.put("call", call);
            return out;
        }
        AutoCall c = autoCallRepository.findOneByRefId(key).or(() -> autoCallRepository.findFirstByCallId(key)).orElse(null);
        if (c == null) {
            out.put("ok", false);
            out.put("code", "NOT_FOUND");
            out.put("message", "Không tìm thấy cuộc gọi");
            return out;
        }
        ObjectNode node = toCallNode(c);
        if (IntegrationConfig.PROVIDER_VTECH.equals(c.getProvider()) && c.getOrder() != null && c.getOrder().getId() != null) {
            node.put(
                "attemptCount",
                dailyCallOrdinals(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(c.getOrder().getId())).getOrDefault(c, 0)
            );
        }
        out.put("call", node);
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
        boolean sandbox = cfg.isAutocallSandbox();
        if (!sandbox && !confirmLive) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key LIVE sẽ gọi thật và tính phí — cần xác nhận trước khi gọi");
        }
        String actor = SecurityUtils.getCurrentUserLogin().orElse("system");
        String refId = buildTestRefId();
        if (cfg.isAutocallVtech()) {
            return testCallVtech(cfg, normalized, refId, actor);
        }
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

    /** callId trả FE = refId (Vtech không có mã cuộc gọi lúc import); kết quả về qua webhook. */
    private Map<String, Object> testCallVtech(IntegrationConfig cfg, String phone, String refId, String actor) {
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("ref_id", refId);
        extra.put("ma_don", "GOI-THU");
        extra.put("ten_san_pham", "Hàng hoá");
        extra.put("diem_nhan", "Văn phòng CPN");
        Result r = vtechClient.importContact(cfg.getAutocallVtechBaseUrl(), cfg.getAutocallVtechApiKey(), phone, extra);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sandbox", false);
        out.put("provider", IntegrationConfig.PROVIDER_VTECH);
        out.put("refId", refId);
        out.put("phone", phone);
        out.put("type", "giao");
        if (!putError(out, r)) {
            return out;
        }
        JsonNode data = r.body() != null ? r.body().path("data") : null;
        if (data != null && data.path("imported").asInt(0) > 0) {
            out.put("callId", refId);
            out.put("status", "queued");
            LOG.info("Auto call test {} (Vtech) → {} by {}", refId, phone, actor);
            return out;
        }
        out.put("ok", false);
        JsonNode errors = data != null ? data.path("errors") : null;
        if (errors != null && errors.isArray() && !errors.isEmpty()) {
            out.put("code", "VTECH_REJECTED");
            out.put("message", "Vtech từ chối: " + errors.get(0).path("error").asText("không rõ lý do"));
        } else {
            out.put("code", "UNEXPECTED_RESPONSE");
            out.put("message", "Vtech không trả kết quả import");
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
        if (cfg == null || !cfg.isAutocallActiveKeyConfigured()) {
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
