package com.mycompany.myapp.service.autocall;

import com.fasterxml.jackson.databind.JsonNode;
import com.mycompany.myapp.domain.AutoCall;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.AutoCallRepository;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.config.AutoCallConfigService;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Auto Call HHVN Tech — gọi giao cho người nhận khi đơn vào nhập kho giao (AT_DEST).
 * Mỗi lần vào AT_DEST từ các action trong {@link #GIAO_TRIGGERS} = 1 cuộc gọi mới (refId mới);
 * gửi lại cùng refId không làm HHVN gọi lần 2.
 */
@Service
@Transactional
public class AutoCallService {

    public static final String TYPE_GIAO = "giao";
    /** Quét nhập VP nhận · chặng cuối đến · giao thất bại quay về kho. Không gồm hoàn / hoàn tác. */
    public static final Set<String> GIAO_TRIGGERS = Set.of("SCAN_IN", "LEG_ARRIVE_DEST", "FAIL_MAX", "FAIL_48H");
    static final Set<String> FINAL_STATUSES = Set.of("COMPLETED", "FAILED", "CANCELLED");
    static final long WEBHOOK_MAX_SKEW_SECONDS = 300;
    private static final String ACTOR = "auto-call";
    private static final Logger LOG = LoggerFactory.getLogger(AutoCallService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AutoCallRepository autoCallRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final OrderEventRepository orderEventRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;
    private final HhvnAutoCallClient client;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "auto-call");
        t.setDaemon(true);
        return t;
    });

    public AutoCallService(
        AutoCallRepository autoCallRepository,
        IntegrationConfigRepository integrationConfigRepository,
        OrderEventRepository orderEventRepository,
        ShipmentOrderRepository shipmentOrderRepository,
        HhvnAutoCallClient client,
        PlatformTransactionManager transactionManager
    ) {
        this.autoCallRepository = autoCallRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.orderEventRepository = orderEventRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.client = client;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    /**
     * Chạy trong transaction đổi trạng thái đơn → AT_DEST. Chỉ ghi 1 dòng PENDING;
     * gọi HHVN sau khi transaction commit, ở thread riêng — lỗi HHVN không làm hỏng thao tác quét.
     */
    public void onArrivedAtDest(ShipmentOrder order, String action) {
        if (order == null || order.getId() == null || action == null || !GIAO_TRIGGERS.contains(action)) {
            return;
        }
        IntegrationConfig cfg = currentConfig();
        if (cfg == null || !Boolean.TRUE.equals(cfg.getAutocallEnabled()) || !cfg.isAutocallApiKeyConfigured()) {
            return;
        }
        AutoCall call = new AutoCall();
        call.setOrder(order);
        call.setCallType(TYPE_GIAO);
        long seq = autoCallRepository.countByOrder_IdAndCallType(order.getId(), TYPE_GIAO) + 1;
        call.setRefId(buildRefId(order.getOrderCode(), seq));
        call.setTriggerAction(action);
        call.setSandbox(cfg.getAutocallApiKey().startsWith("xk_test_"));
        call.setCreatedAt(Instant.now());
        String phone = normalizePhone(order.getReceiverPhone());
        if (phone == null) {
            call.setPhone(truncate(order.getReceiverPhone(), 20));
            call.setStatus("SKIPPED");
            call.setErrorCode("INVALID_PHONE");
            call.setErrorMessage("SĐT người nhận không hợp lệ");
            autoCallRepository.save(call);
            appendEvent(order, "AUTO_CALL_SKIPPED", "Không gọi được: SĐT người nhận không hợp lệ");
            return;
        }
        call.setPhone(phone);
        call.setStatus("PENDING");
        autoCallRepository.save(call);
        appendEvent(order, "AUTO_CALL_REQUEST", sandboxPrefix(call) + "Gọi giao → " + phone);
        Long id = call.getId();
        dispatchAfterCommit(() -> {
            try {
                executor.execute(() -> sendInNewTransaction(id));
            } catch (RuntimeException e) {
                LOG.warn("Auto call {} not dispatched (stays PENDING): {}", id, e.getMessage());
            }
        });
    }

    protected void dispatchAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        task.run();
                    }
                }
            );
        } else {
            task.run();
        }
    }

    void sendInNewTransaction(Long id) {
        try {
            transactionTemplate.executeWithoutResult(s -> send(id));
        } catch (Exception e) {
            LOG.warn("Auto Call send {} failed: {}", id, e.getMessage());
        }
    }

    /** Gửi (hoặc gửi lại cùng refId) 1 cuộc gọi đang PENDING / ERROR sang HHVN. */
    public void send(Long id) {
        AutoCall call = autoCallRepository.findById(id).orElse(null);
        if (call == null || !("PENDING".equals(call.getStatus()) || "ERROR".equals(call.getStatus()))) {
            return;
        }
        IntegrationConfig cfg = currentConfig();
        if (cfg == null || !cfg.isAutocallApiKeyConfigured()) {
            markError(call, "API_KEY_MISSING", "Chưa lưu API key Auto Call");
            return;
        }
        ShipmentOrder order = call.getOrder();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("orderCode", order.getOrderCode());
        if (order.getToOffice() != null) {
            metadata.put("office", order.getToOffice().getCode());
        }
        Result r = client.createCall(
            cfg.getAutocallBaseUrl(),
            cfg.getAutocallApiKey(),
            call.getCallType(),
            call.getPhone(),
            call.getRefId(),
            metadata
        );
        if (!r.ok()) {
            markError(call, r.code(), AutoCallConfigService.humanMessage(r.code(), r.message()));
            return;
        }
        JsonNode body = r.body();
        JsonNode accepted = body != null ? body.path("accepted") : null;
        if (accepted != null && accepted.isArray() && !accepted.isEmpty()) {
            JsonNode a = accepted.get(0);
            call.setCallId(text(a, "callId"));
            call.setRequestId(text(body, "requestId"));
            String st = text(a, "status");
            call.setStatus(st != null ? st.toUpperCase(Locale.ROOT) : "QUEUED");
            call.setErrorCode(null);
            call.setErrorMessage(null);
            call.setUpdatedAt(Instant.now());
            autoCallRepository.save(call);
            return;
        }
        JsonNode rejected = body != null ? body.path("rejected") : null;
        if (rejected != null && rejected.isArray() && !rejected.isEmpty()) {
            JsonNode x = rejected.get(0);
            markError(call, text(x, "code"), text(x, "message"));
            return;
        }
        markError(call, "UNEXPECTED_RESPONSE", "HHVN không trả accepted/rejected");
    }

    /** Áp Call object của HHVN (webhook hoặc tra cứu). Trả false nếu không tìm thấy cuộc gọi phía CPN. */
    public boolean applyCallObject(JsonNode data) {
        if (data == null || data.isMissingNode() || data.isNull()) {
            return false;
        }
        String refId = text(data, "refId");
        String callId = text(data, "callId");
        AutoCall call = refId != null ? autoCallRepository.findOneByRefId(refId).orElse(null) : null;
        if (call == null && callId != null) {
            call = autoCallRepository.findFirstByCallId(callId).orElse(null);
        }
        if (call == null) {
            return false;
        }
        String prevStatus = call.getStatus();
        String status = upper(text(data, "status"));
        if (FINAL_STATUSES.contains(prevStatus) && prevStatus.equals(status)) {
            return true;
        }
        if (callId != null) call.setCallId(callId);
        if (text(data, "requestId") != null) call.setRequestId(text(data, "requestId"));
        if (status != null) call.setStatus(status);
        if (text(data, "result") != null) call.setResult(text(data, "result"));
        if (data.hasNonNull("attemptCount")) call.setAttemptCount(data.get("attemptCount").asInt());
        call.setFirstCallAt(firstNonNull(instant(data, "firstCallAt"), call.getFirstCallAt()));
        call.setAnsweredAt(firstNonNull(instant(data, "answeredAt"), call.getAnsweredAt()));
        call.setFinishedAt(firstNonNull(instant(data, "finishedAt"), call.getFinishedAt()));
        if (data.hasNonNull("duration")) call.setDurationSec(data.get("duration").asInt());
        if (text(data, "recordingUrl") != null) call.setRecordingUrl(truncate(text(data, "recordingUrl"), 500));
        call.setUpdatedAt(Instant.now());
        autoCallRepository.save(call);
        if (status != null && FINAL_STATUSES.contains(status) && !status.equals(prevStatus)) {
            appendEvent(call.getOrder(), "AUTO_CALL_RESULT", sandboxPrefix(call) + resultText(call));
        }
        return true;
    }

    /** Nút "Cập nhật" trên đơn: gửi lại cuộc gọi lỗi, tra cứu HHVN cho cuộc gọi chưa có kết quả cuối. */
    public List<AutoCallView> sync(String orderCode) {
        ShipmentOrder order = requireOrder(orderCode);
        IntegrationConfig cfg = requireKeyConfigured();
        for (AutoCall call : autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(order.getId())) {
            if (FINAL_STATUSES.contains(call.getStatus()) || "SKIPPED".equals(call.getStatus())) {
                continue;
            }
            if (call.getCallId() == null) {
                send(call.getId());
                continue;
            }
            Result r = client.getCall(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), call.getCallId());
            if (r.ok()) {
                applyCallObject(unwrapCall(r.body()));
            }
        }
        return list(orderCode);
    }

    /**
     * Huỷ 1 cuộc gọi chưa có kết quả của đơn. Đã gửi HHVN → huỷ bên HHVN rồi đọc lại trạng thái;
     * chưa gửi được (PENDING/ERROR, chưa có callId) → huỷ luôn phía CPN.
     */
    public List<AutoCallView> cancel(String orderCode, Long autoCallId) {
        ShipmentOrder order = requireOrder(orderCode);
        AutoCall call = autoCallRepository
            .findById(autoCallId)
            .filter(c -> c.getOrder() != null && order.getId().equals(c.getOrder().getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc gọi"));
        if (FINAL_STATUSES.contains(call.getStatus()) || "SKIPPED".equals(call.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cuộc gọi đã kết thúc, không huỷ được");
        }
        if (call.getCallId() == null) {
            if (
                "PENDING".equals(call.getStatus()) &&
                call.getCreatedAt() != null &&
                call.getCreatedAt().isAfter(Instant.now().minusSeconds(30))
            ) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cuộc gọi đang được gửi sang tổng đài — thử lại sau vài giây");
            }
            call.setStatus("CANCELLED");
            call.setResult("cancelled");
            call.setUpdatedAt(Instant.now());
            autoCallRepository.save(call);
            appendEvent(order, "AUTO_CALL_RESULT", sandboxPrefix(call) + resultText(call));
            return list(orderCode);
        }
        CancelOutcome outcome = cancelAtHhvn(call.getCallId());
        if (!outcome.ok()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, outcome.message());
        }
        return list(orderCode);
    }

    /**
     * POST /calls/{callId}/cancel rồi áp Call object HHVN trả kèm vào dòng auto_call (nếu có).
     * HHVN trả 200 cả khi không huỷ được ({@code cancelled = 0, notCancellable = 1}) — phải đọc số đã huỷ.
     */
    public CancelOutcome cancelAtHhvn(String callId) {
        IntegrationConfig cfg = requireKeyConfigured();
        Result r = client.cancelCall(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), callId);
        if (!r.ok()) {
            return new CancelOutcome(false, r.code(), AutoCallConfigService.humanMessage(r.code(), r.message()), null);
        }
        JsonNode data = unwrapCall(r.body());
        if (data == null) {
            Result got = client.getCall(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), callId);
            data = got.ok() ? unwrapCall(got.body()) : null;
        }
        applyCallObject(data);
        boolean cancelled = r.body() != null && r.body().has("cancelled")
            ? r.body().get("cancelled").asInt(0) > 0
            : data != null && "cancelled".equals(text(data, "status"));
        if (!cancelled) {
            return new CancelOutcome(false, "NOT_CANCELLABLE", "Không huỷ được: cuộc gọi đang đổ chuông hoặc đã kết thúc", data);
        }
        return new CancelOutcome(true, null, null, data);
    }

    public record CancelOutcome(boolean ok, String code, String message, JsonNode call) {}

    @Transactional(readOnly = true)
    public List<AutoCallView> list(String orderCode) {
        ShipmentOrder order = requireOrder(orderCode);
        return autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(order.getId()).stream().map(AutoCallView::of).toList();
    }

    @Transactional(readOnly = true)
    public String webhookSecret() {
        IntegrationConfig cfg = currentConfig();
        String s = cfg != null ? cfg.getAutocallWebhookSecret() : null;
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** HEX(HMAC_SHA256(secret, timestamp + "." + rawBody)), so sánh constant-time; từ chối lệch > 5 phút. */
    public static boolean verifySignature(String secret, String timestamp, byte[] rawBody, String signatureHeader, long nowEpochSeconds) {
        if (secret == null || timestamp == null || signatureHeader == null || rawBody == null) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(nowEpochSeconds - ts) > WEBHOOK_MAX_SKEW_SECONDS) {
            return false;
        }
        String sig = signatureHeader.trim();
        if (sig.startsWith("sha256=")) {
            sig = sig.substring("sha256=".length());
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp.trim() + ".").getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(rawBody)).getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, sig.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }

    /** 0912345678 · 84912345678 · +84 912 345 678 → 0912345678; null nếu không phải SĐT VN 10 số. */
    static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("84") && d.length() == 11) {
            d = "0" + d.substring(2);
        }
        return d.length() == 10 && d.startsWith("0") ? d : null;
    }

    static String buildRefId(String orderCode, long seq) {
        String code = orderCode == null ? "NA" : orderCode.replaceAll("[^A-Za-z0-9_.:-]", "-");
        if (code.length() > 36) {
            code = code.substring(code.length() - 36);
        }
        String suffix = Long.toString(RANDOM.nextInt(Integer.MAX_VALUE), 36);
        return "CPN-GIAO-" + code + "-" + seq + "-" + suffix;
    }

    static String resultText(AutoCall call) {
        String status = call.getStatus();
        int attempts = call.getAttemptCount() == null ? 0 : call.getAttemptCount();
        if ("COMPLETED".equals(status)) {
            String dur = call.getDurationSec() != null ? " · " + call.getDurationSec() + "s" : "";
            return "Gọi giao: khách nghe máy" + dur + (attempts > 0 ? " · " + attempts + " lần gọi" : "");
        }
        if ("CANCELLED".equals(status)) {
            return "Gọi giao: đã huỷ";
        }
        if ("error".equals(call.getResult())) {
            return "Gọi giao: lỗi tổng đài / nhà mạng";
        }
        return "Gọi giao: khách không nghe máy" + (attempts > 0 ? " sau " + attempts + " lần gọi" : "");
    }

    private static JsonNode unwrapCall(JsonNode body) {
        if (body == null) {
            return null;
        }
        if (body.has("callId")) {
            return body;
        }
        for (String key : List.of("call", "data")) {
            JsonNode n = body.get(key);
            if (n != null && n.isObject()) return n;
            if (n != null && n.isArray() && !n.isEmpty()) return n.get(0);
        }
        return null;
    }

    private void markError(AutoCall call, String code, String message) {
        call.setStatus("ERROR");
        call.setErrorCode(truncate(code, 50));
        call.setErrorMessage(truncate(message, 255));
        call.setUpdatedAt(Instant.now());
        autoCallRepository.save(call);
        appendEvent(call.getOrder(), "AUTO_CALL_ERROR", message != null ? message : code);
    }

    private void appendEvent(ShipmentOrder order, String action, String detail) {
        OrderEvent event = new OrderEvent();
        event.setEventAt(Instant.now());
        event.setAction(action);
        event.setDetail(truncate(detail, 255));
        event.setActorUsername(ACTOR);
        event.setOrder(order);
        orderEventRepository.save(event);
    }

    private ShipmentOrder requireOrder(String code) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(code.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + code));
    }

    private IntegrationConfig currentConfig() {
        return integrationConfigRepository.findAll().stream().findFirst().orElse(null);
    }

    private IntegrationConfig requireKeyConfigured() {
        IntegrationConfig cfg = currentConfig();
        if (cfg == null || !cfg.isAutocallApiKeyConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa lưu API key Auto Call");
        }
        return cfg;
    }

    private static String sandboxPrefix(AutoCall call) {
        return Boolean.TRUE.equals(call.getSandbox()) ? "[Sandbox] " : "";
    }

    private static String text(JsonNode n, String field) {
        return n != null && n.hasNonNull(field) ? n.get(field).asText() : null;
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT);
    }

    private static Instant instant(JsonNode n, String field) {
        String s = text(n, field);
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    public record AutoCallView(
        Long id,
        String callType,
        String status,
        String result,
        String phone,
        Integer attemptCount,
        Instant firstCallAt,
        Instant answeredAt,
        Instant finishedAt,
        Integer durationSec,
        String recordingUrl,
        String errorCode,
        String errorMessage,
        Boolean sandbox,
        String triggerAction,
        Instant createdAt
    ) {
        static AutoCallView of(AutoCall c) {
            return new AutoCallView(
                c.getId(),
                c.getCallType(),
                c.getStatus(),
                c.getResult(),
                c.getPhone(),
                c.getAttemptCount(),
                c.getFirstCallAt(),
                c.getAnsweredAt(),
                c.getFinishedAt(),
                c.getDurationSec(),
                c.getRecordingUrl(),
                c.getErrorCode(),
                c.getErrorMessage(),
                c.getSandbox(),
                c.getTriggerAction(),
                c.getCreatedAt()
            );
        }
    }
}
