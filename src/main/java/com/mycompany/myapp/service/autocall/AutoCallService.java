package com.mycompany.myapp.service.autocall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.AutoCall;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.AutoCallRepository;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.config.AutoCallConfigService;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import com.mycompany.myapp.service.partner.VtechAutoCallClient;
import com.mycompany.myapp.service.realtime.ServerEventService;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
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
    /** Đơn ở các trạng thái này không gọi lại nữa. */
    static final Set<OrderStatus> RETRY_STOP_STATUSES = Set.of(
        OrderStatus.DELIVERED,
        OrderStatus.CANCELLED,
        OrderStatus.RETURNING,
        OrderStatus.RETURNED
    );
    static final String TRIGGER_RETRY = "RETRY";
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter RETRY_AT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM").withZone(VN);
    private static final String ACTOR = "auto-call";
    private static final Logger LOG = LoggerFactory.getLogger(AutoCallService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AutoCallRepository autoCallRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final OrderEventRepository orderEventRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;
    private final HhvnAutoCallClient client;
    private final VtechAutoCallClient vtechClient;
    private final TransactionTemplate transactionTemplate;
    /** Kết quả gọi thử Vtech (refId CPN-TEST-…) — không có dòng auto_call, giữ trong bộ nhớ cho màn Gọi thử tra lại. */
    private final Map<String, JsonNode> vtechTestResults = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, JsonNode> eldest) {
                return size() > 200;
            }
        }
    );
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
        VtechAutoCallClient vtechClient,
        PlatformTransactionManager transactionManager
    ) {
        this.autoCallRepository = autoCallRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.orderEventRepository = orderEventRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.client = client;
        this.vtechClient = vtechClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private ServerEventService serverEventService;

    @Autowired(required = false)
    void setServerEventService(ServerEventService serverEventService) {
        this.serverEventService = serverEventService;
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
        if (cfg == null || !Boolean.TRUE.equals(cfg.getAutocallEnabled()) || !cfg.isAutocallActiveKeyConfigured()) {
            return;
        }
        createGiaoCall(order, cfg, action, "");
    }

    enum GiaoCallOutcome {
        SENT,
        SCHEDULED,
        INVALID_PHONE,
    }

    private GiaoCallOutcome createGiaoCall(ShipmentOrder order, IntegrationConfig cfg, String action, String eventPrefix) {
        autoCallRepository.skipScheduledForOrder(order.getId(), "SUPERSEDED", "Thay bằng lệnh gọi mới");
        autoCallRepository.clearRetriesForOrder(order.getId());
        AutoCall call = new AutoCall();
        call.setOrder(order);
        call.setCallType(TYPE_GIAO);
        long seq = autoCallRepository.countByOrder_IdAndCallType(order.getId(), TYPE_GIAO) + 1;
        call.setRefId(buildRefId(order.getOrderCode(), seq));
        call.setTriggerAction(action);
        call.setProvider(cfg.getAutocallActiveProvider());
        call.setSandbox(cfg.isAutocallSandbox());
        call.setCreatedAt(Instant.now());
        String phone = normalizePhone(order.getReceiverPhone());
        if (phone == null) {
            call.setPhone(truncate(order.getReceiverPhone(), 20));
            call.setStatus("SKIPPED");
            call.setErrorCode("INVALID_PHONE");
            call.setErrorMessage("SĐT người nhận không hợp lệ");
            autoCallRepository.save(call);
            appendEvent(order, "AUTO_CALL_SKIPPED", eventPrefix + "Không gọi được: SĐT người nhận không hợp lệ");
            return GiaoCallOutcome.INVALID_PHONE;
        }
        call.setPhone(phone);
        call.setStatus("PENDING");
        Instant at = fitCallWindow(cfg, call.getCreatedAt());
        if (at.isAfter(call.getCreatedAt())) {
            call.setNextRetryAt(at);
            autoCallRepository.save(call);
            appendEvent(
                order,
                "AUTO_CALL_REQUEST",
                sandboxPrefix(call) + eventPrefix + "Gọi giao → " + phone + " · ngoài khung giờ gọi, hẹn gọi lúc " + RETRY_AT_FMT.format(at)
            );
            return GiaoCallOutcome.SCHEDULED;
        }
        autoCallRepository.save(call);
        appendEvent(order, "AUTO_CALL_REQUEST", sandboxPrefix(call) + eventPrefix + "Gọi giao → " + phone);
        Long id = call.getId();
        dispatchAfterCommit(() -> {
            try {
                executor.execute(() -> sendInNewTransaction(id));
            } catch (RuntimeException e) {
                LOG.warn("Auto call {} not dispatched (stays PENDING): {}", id, e.getMessage());
            }
        });
        return GiaoCallOutcome.SENT;
    }

    static final String TRIGGER_CATCH_UP = "CATCH_UP";
    /** Lệnh PENDING chưa có giờ hẹn mà quá ngần này chưa gửi được = kẹt (vd. BE restart giữa chừng). */
    static final Duration STUCK_PENDING_AFTER = Duration.ofMinutes(2);
    /** Đã gửi tổng đài (QUEUED/…) mà quá ngần này chưa có kết quả = coi như mất, cho gọi bù. */
    static final Duration STUCK_IN_CARRIER_AFTER = Duration.ofHours(3);

    public record CatchUpSkip(String orderCode, String reason) {}

    public record CatchUpResult(List<String> eligible, int sent, int scheduled, List<CatchUpSkip> skipped) {}

    /**
     * Gọi bù cho đơn đang nhập kho giao (AT_DEST) mà khách chưa nghe máy lần nào: chưa có lệnh, lệnh gần nhất
     * lỗi gửi / lỗi tổng đài / không nghe / bị bỏ qua / tổng đài tự huỷ / kẹt. Lịch gọi lại đang chờ bị thay bằng
     * cuộc gọi mới. Không gọi bù: khách đã nghe, đang gửi, đang chờ khung giờ, người dùng đã huỷ.
     * {@code scopedOffice} khác null → chỉ đơn có VP nhận hiện tại là VP đó.
     * {@code dryRun} → chỉ trả danh sách đơn đủ điều kiện.
     */
    public CatchUpResult catchUp(List<String> orderCodes, String scopedOffice, boolean dryRun, String actor) {
        IntegrationConfig cfg = currentConfig();
        if (cfg == null || !Boolean.TRUE.equals(cfg.getAutocallEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Auto Call đang tắt");
        }
        if (!cfg.isAutocallActiveKeyConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa lưu API key Auto Call");
        }
        List<String> codes = orderCodes == null
            ? List.of()
            : orderCodes.stream().filter(c -> c != null && !c.isBlank()).map(String::trim).distinct().toList();
        if (codes.size() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa 500 đơn mỗi lần");
        }
        Map<String, ShipmentOrder> byCode = new LinkedHashMap<>();
        if (!codes.isEmpty()) {
            for (ShipmentOrder o : shipmentOrderRepository.findWithOfficesByOrderCodeIn(codes)) {
                byCode.put(o.getOrderCode(), o);
            }
        }
        Instant now = Instant.now();
        List<String> eligible = new ArrayList<>();
        List<CatchUpSkip> skipped = new ArrayList<>();
        int sent = 0;
        int scheduled = 0;
        for (String code : codes) {
            ShipmentOrder order = byCode.get(code);
            String reason = catchUpBlockReason(order, scopedOffice, now);
            if (reason != null) {
                skipped.add(new CatchUpSkip(code, reason));
                continue;
            }
            eligible.add(code);
            if (dryRun) {
                continue;
            }
            String who = actor == null || actor.isBlank() ? "" : " (" + actor + ")";
            switch (createGiaoCall(order, cfg, TRIGGER_CATCH_UP, "Gọi bù" + who + " · ")) {
                case SENT -> sent++;
                case SCHEDULED -> scheduled++;
                case INVALID_PHONE -> skipped.add(new CatchUpSkip(code, "SĐT người nhận không hợp lệ"));
            }
        }
        return new CatchUpResult(eligible, sent, scheduled, skipped);
    }

    private String catchUpBlockReason(ShipmentOrder order, String scopedOffice, Instant now) {
        if (order == null) {
            return "Không tìm thấy đơn";
        }
        if (order.getStatus() != OrderStatus.AT_DEST) {
            return "Đơn không ở nhập kho giao";
        }
        if (scopedOffice != null) {
            Office to = order.getToOffice();
            if (to == null || !scopedOffice.equalsIgnoreCase(to.getCode())) {
                return "Đơn không thuộc VP của bạn";
            }
        }
        List<AutoCall> giao = autoCallRepository
            .findByOrder_IdOrderByCreatedAtDesc(order.getId())
            .stream()
            .filter(c -> TYPE_GIAO.equals(c.getCallType()))
            .toList();
        if (giao.isEmpty()) {
            return null;
        }
        if (giao.stream().anyMatch(c -> "answered".equals(c.getResult()) || "COMPLETED".equals(c.getStatus()))) {
            return "Khách đã nghe máy";
        }
        AutoCall last = giao.get(0);
        String st = last.getStatus();
        Instant created = last.getCreatedAt() != null ? last.getCreatedAt() : now;
        if ("PENDING".equals(st) && last.getCallId() == null) {
            if (last.getNextRetryAt() != null) {
                return "Đã hẹn gọi lúc " + RETRY_AT_FMT.format(last.getNextRetryAt());
            }
            return created.isBefore(now.minus(STUCK_PENDING_AFTER)) ? null : "Đang gửi lệnh gọi";
        }
        if ("ERROR".equals(st) || "SKIPPED".equals(st) || "FAILED".equals(st)) {
            return null;
        }
        if ("CANCELLED".equals(st)) {
            boolean byCarrier = last.getCallId() != null && last.getCallId().startsWith("vtech_");
            return byCarrier ? null : "Lệnh gọi đã bị huỷ";
        }
        return created.isBefore(now.minus(STUCK_IN_CARRIER_AFTER)) ? null : "Tổng đài đang gọi";
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

    /**
     * Gửi (hoặc gửi lại cùng refId) 1 cuộc gọi đang PENDING / ERROR qua nhà cung cấp đang chọn — cuộc gọi tạo lúc
     * còn dùng HHVN mà chưa gửi được sẽ đi qua Vtech nếu admin đã chuyển.
     */
    public void send(Long id) {
        AutoCall call = autoCallRepository.findById(id).orElse(null);
        if (call == null || !("PENDING".equals(call.getStatus()) || "ERROR".equals(call.getStatus()))) {
            return;
        }
        IntegrationConfig cfg = currentConfig();
        if (cfg == null || !cfg.isAutocallActiveKeyConfigured()) {
            markError(call, "API_KEY_MISSING", "Chưa lưu API key Auto Call");
            return;
        }
        call.setProvider(cfg.getAutocallActiveProvider());
        call.setSandbox(cfg.isAutocallSandbox());
        if (cfg.isAutocallVtech()) {
            sendVtech(call, cfg);
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
            call.setNextRetryAt(null);
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

    /**
     * Import số vào chiến dịch Vtech. Vtech không trả mã cuộc gọi: thành công → QUEUED, callId để trống tới khi
     * webhook về (khớp bằng extra_data.ref_id).
     */
    private void sendVtech(AutoCall call, IntegrationConfig cfg) {
        ShipmentOrder order = call.getOrder();
        Result r = vtechClient.importContact(
            cfg.getAutocallVtechBaseUrl(),
            cfg.getAutocallVtechApiKey(),
            call.getPhone(),
            order != null ? order.getReceiverName() : null,
            vtechExtraData(call)
        );
        if (!r.ok()) {
            markError(call, r.code(), AutoCallConfigService.humanMessage(r.code(), r.message()));
            return;
        }
        JsonNode data = r.body() != null ? r.body().path("data") : null;
        if (data != null && data.path("imported").asInt(0) > 0) {
            call.setStatus("QUEUED");
            call.setErrorCode(null);
            call.setErrorMessage(null);
            call.setNextRetryAt(null);
            call.setUpdatedAt(Instant.now());
            autoCallRepository.save(call);
            return;
        }
        JsonNode errors = data != null ? data.path("errors") : null;
        if (errors != null && errors.isArray() && !errors.isEmpty()) {
            markError(call, "VTECH_REJECTED", "Vtech từ chối: " + errors.get(0).path("error").asText("không rõ lý do"));
            return;
        }
        if (data != null && data.path("skipped").asInt(0) > 0) {
            markError(call, "VTECH_SKIPPED", "Vtech bỏ qua số này (trùng trong chiến dịch)");
            return;
        }
        markError(call, "UNEXPECTED_RESPONSE", "Vtech không trả kết quả import");
    }

    /** Biến cho kịch bản callbot Vtech + ref_id để khớp webhook. */
    static Map<String, String> vtechExtraData(AutoCall call) {
        ShipmentOrder order = call.getOrder();
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("ref_id", call.getRefId());
        if (order != null) {
            extra.put("ma_don", order.getOrderCode());
            extra.put("ten_san_pham", goodsTypeLabel(order.getGoodsType()));
            extra.put("diem_nhan", officeLabel(order.getToOffice()));
        }
        return extra;
    }

    static String goodsTypeLabel(GoodsType type) {
        if (type == null) {
            return "Hàng hoá";
        }
        return switch (type) {
            case THUONG -> "Hàng thường";
            case DE_VO -> "Hàng dễ vỡ";
            case DIEN_TU -> "Hàng điện tử";
            case THUC_PHAM_KHO -> "Thực phẩm khô";
            case GIAY_TO -> "Giấy tờ";
            case CONG_KENH -> "Hàng cồng kềnh";
        };
    }

    /** "VP Mỹ Đình - 123 Phạm Hùng, Hà Nội". */
    static String officeLabel(Office office) {
        if (office == null) {
            return "";
        }
        String name = office.getName() != null ? office.getName().trim() : "";
        String addr = office.getAddress() != null ? office.getAddress().trim() : "";
        if (name.isEmpty()) return addr;
        if (addr.isEmpty()) return name;
        return name + " - " + addr;
    }

    public enum VtechWebhookOutcome {
        APPLIED,
        TEST,
        NOT_FOUND,
        IGNORED,
    }

    /**
     * Webhook {@code call.completed} của Vtech: đổi sang dạng Call object HHVN rồi áp như webhook HHVN (dùng chung
     * kết quả, gọi lại, popup lỗi). Khớp theo extra_data.ref_id; thiếu thì lấy cuộc Vtech đang chờ gần nhất của SĐT.
     */
    public VtechWebhookOutcome applyVtechWebhook(JsonNode event) {
        if (event == null || !"call.completed".equals(event.path("event").asText())) {
            return VtechWebhookOutcome.IGNORED;
        }
        JsonNode vc = event.path("call");
        String refId = text(vc.path("contact").path("extra_data"), "ref_id");
        ObjectNode data = toHhvnCall(vc, refId);
        if (refId != null && refId.startsWith("CPN-TEST-")) {
            vtechTestResults.put(refId, data);
            return VtechWebhookOutcome.TEST;
        }
        AutoCall call = refId != null ? autoCallRepository.findOneByRefId(refId).orElse(null) : null;
        if (call == null) {
            String phone = normalizePhone(text(vc.path("contact"), "phone_number"));
            if (phone != null) {
                call = autoCallRepository
                    .findFirstByProviderAndPhoneAndStatusOrderByCreatedAtDesc(IntegrationConfig.PROVIDER_VTECH, phone, "QUEUED")
                    .orElse(null);
            }
        }
        if (call == null) {
            return VtechWebhookOutcome.NOT_FOUND;
        }
        data.put("refId", call.getRefId());
        return applyCallObject(data) ? VtechWebhookOutcome.APPLIED : VtechWebhookOutcome.NOT_FOUND;
    }

    /** Kết quả gọi thử Vtech đã nhận qua webhook; null nếu chưa về. */
    public JsonNode vtechTestResult(String refId) {
        return refId == null ? null : vtechTestResults.get(refId);
    }

    /**
     * status Vtech: COMPLETED · BUSY · NO_ANSWER · FAILED · CANCELLED; outcome chi tiết hơn (ưu tiên):
     * CONNECTED → nghe máy · VOICEMAIL/BUSY/NO_ANSWER/REJECTED → không nghe · INVALID_NUMBER/NETWORK_ERROR → lỗi tổng đài.
     */
    static ObjectNode toHhvnCall(JsonNode vc, String refId) {
        String status = upper(text(vc, "status"));
        String outcome = upper(text(vc, "outcome"));
        String result;
        if ("CANCELLED".equals(status)) {
            result = "cancelled";
        } else if (outcome != null && Set.of("CONNECTED").contains(outcome)) {
            result = "answered";
        } else if (outcome != null && Set.of("VOICEMAIL", "BUSY", "NO_ANSWER", "REJECTED").contains(outcome)) {
            result = "not_answered";
        } else if (outcome != null && Set.of("INVALID_NUMBER", "NETWORK_ERROR").contains(outcome)) {
            result = "error";
        } else if ("COMPLETED".equals(status)) {
            result = "answered";
        } else if ("BUSY".equals(status) || "NO_ANSWER".equals(status)) {
            result = "not_answered";
        } else {
            result = "error";
        }
        String hhvnStatus =
            switch (result) {
                case "answered" -> "completed";
                case "cancelled" -> "cancelled";
                default -> "failed";
            };
        ObjectNode n = JsonNodeFactory.instance.objectNode();
        if (refId != null) n.put("refId", refId);
        if (vc.hasNonNull("id")) n.put("callId", truncate("vtech_" + vc.get("id").asText(), 64));
        n.put("status", hhvnStatus);
        n.put("result", result);
        n.put("type", TYPE_GIAO);
        n.put("phone", text(vc.path("contact"), "phone_number"));
        n.put("attemptCount", "cancelled".equals(result) ? 0 : 1);
        n.put("maxAttempts", 1);
        putIfText(n, "firstCallAt", text(vc, "started_at"));
        putIfText(n, "answeredAt", text(vc, "answered_at"));
        putIfText(n, "finishedAt", text(vc, "ended_at"));
        putIfText(n, "createdAt", text(vc, "started_at"));
        if (vc.hasNonNull("duration_seconds")) n.put("duration", vc.get("duration_seconds").asInt());
        putIfText(n, "recordingUrl", text(vc, "recording_url"));
        putIfText(n, "outcome", text(vc, "outcome"));
        return n;
    }

    private static void putIfText(ObjectNode n, String field, String value) {
        if (value != null && !value.isBlank()) n.put(field, value);
    }

    @Transactional(readOnly = true)
    public String vtechWebhookToken() {
        IntegrationConfig cfg = currentConfig();
        String s = cfg != null ? cfg.getAutocallVtechWebhookToken() : null;
        return s == null || s.isBlank() ? null : s.trim();
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
            if ("error".equals(call.getResult())) {
                alertAdmins(call, "carrier", "Lỗi tổng đài / nhà mạng");
            }
            scheduleRetry(call);
        }
        return true;
    }

    // ---- gọi lại do CPN cấu hình ----

    enum RetryReason {
        NO_ANSWER,
        CARRIER_ERROR,
        SEND_ERROR,
    }

    static RetryReason retryReason(AutoCall call) {
        if ("ERROR".equals(call.getStatus())) return RetryReason.SEND_ERROR;
        if (!"FAILED".equals(call.getStatus())) return null;
        return "error".equals(call.getResult()) ? RetryReason.CARRIER_ERROR : RetryReason.NO_ANSWER;
    }

    static boolean retryAllowed(IntegrationConfig cfg, RetryReason reason) {
        if (cfg == null || reason == null || !Boolean.TRUE.equals(cfg.getAutocallRetryEnabled())) return false;
        return switch (reason) {
            case NO_ANSWER -> !Boolean.FALSE.equals(cfg.getAutocallRetryNoAnswer());
            case CARRIER_ERROR -> !Boolean.FALSE.equals(cfg.getAutocallRetryCarrierError());
            case SEND_ERROR -> !Boolean.FALSE.equals(cfg.getAutocallRetrySendError());
        };
    }

    static LocalTime parseHhmm(String s, LocalTime fallback) {
        try {
            return s == null || s.isBlank() ? fallback : LocalTime.parse(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    static final List<Integer> DEFAULT_RETRY_INTERVALS = List.of(60, 120);

    /** "60,120" → [60, 120]: phút chờ trước lần gọi lại thứ 1, 2, … */
    static List<Integer> retryIntervals(IntegrationConfig cfg) {
        String raw = cfg.getAutocallRetryIntervals();
        if (raw == null) return DEFAULT_RETRY_INTERVALS;
        List<Integer> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            try {
                int v = Integer.parseInt(part.trim());
                if (v > 0) out.add(v);
            } catch (NumberFormatException ignored) {
                // bỏ phần tử hỏng
            }
        }
        return out;
    }

    /** Giữ nguyên nếu trong khung giờ gọi; trước giờ mở → giờ mở cùng ngày; sau giờ đóng → giờ mở hôm sau. */
    static Instant fitCallWindow(IntegrationConfig cfg, Instant at) {
        LocalTime from = parseHhmm(cfg.getAutocallCallFrom(), LocalTime.of(8, 0));
        LocalTime to = parseHhmm(cfg.getAutocallCallTo(), LocalTime.of(20, 0));
        ZonedDateTime z = at.atZone(VN);
        if (z.toLocalTime().isBefore(from)) {
            return z.toLocalDate().atTime(from).atZone(VN).toInstant();
        }
        if (z.toLocalTime().isAfter(to)) {
            return z.toLocalDate().plusDays(1).atTime(from).atZone(VN).toInstant();
        }
        return at;
    }

    static int retryDays(IntegrationConfig cfg) {
        Integer v = cfg.getAutocallRetryDays();
        return v == null ? 1 : Math.max(1, Math.min(7, v));
    }

    /** {@code newDay} = mở đợt gọi mới (ngày kế tiếp), cuộc đầu của đợt. */
    record RetrySlot(Instant at, boolean newDay) {}

    /**
     * Lần gọi kế tiếp sau {@code prev} (kết quả có lúc {@code now}). Còn lần gọi lại trong đợt → {@code now} +
     * khoảng cách của lần đó, rơi ngoài khung giờ thì dời sang đầu khung kế tiếp. Hết đợt mà còn ngày → đầu khung
     * giờ hôm sau, mở đợt mới. Null = hết lượt.
     */
    static RetrySlot nextRetry(IntegrationConfig cfg, AutoCall prev, Instant now) {
        List<Integer> intervals = retryIntervals(cfg);
        int done = nz(prev.getRetryNo());
        if (done < intervals.size()) {
            return new RetrySlot(fitCallWindow(cfg, now.plus(Duration.ofMinutes(intervals.get(done)))), false);
        }
        if (nz(prev.getRetryDay()) + 1 < retryDays(cfg)) {
            LocalTime from = parseHhmm(cfg.getAutocallCallFrom(), LocalTime.of(8, 0));
            return new RetrySlot(now.atZone(VN).toLocalDate().plusDays(1).atTime(from).atZone(VN).toInstant(), true);
        }
        return null;
    }

    /** "gọi lại lần 2" · "ngày 2 · cuộc 1" · "ngày 2 · gọi lại lần 1". */
    static String attemptLabel(int day, int retryNo) {
        String inDay = retryNo == 0 ? "cuộc 1" : "gọi lại lần " + retryNo;
        return day == 0 ? inDay : "ngày " + (day + 1) + " · " + inDay;
    }

    private void scheduleRetry(AutoCall call) {
        if (call.getNextRetryAt() != null) return;
        IntegrationConfig cfg = currentConfig();
        if (cfg == null || !Boolean.TRUE.equals(cfg.getAutocallEnabled())) return;
        if (!retryAllowed(cfg, retryReason(call))) return;
        ShipmentOrder order = call.getOrder();
        if (order == null || RETRY_STOP_STATUSES.contains(order.getStatus())) return;
        RetrySlot slot = nextRetry(cfg, call, Instant.now());
        if (slot == null) {
            appendEvent(order, "AUTO_CALL_RETRY", sandboxPrefix(call) + "Hết lượt gọi lại theo cấu hình");
            return;
        }
        call.setNextRetryAt(slot.at());
        autoCallRepository.save(call);
        String next = slot.newDay()
            ? attemptLabel(nz(call.getRetryDay()) + 1, 0)
            : attemptLabel(nz(call.getRetryDay()), nz(call.getRetryNo()) + 1);
        appendEvent(order, "AUTO_CALL_RETRY", sandboxPrefix(call) + "Hẹn " + next + " lúc " + RETRY_AT_FMT.format(slot.at()));
    }

    /** Mỗi phút: bỏ lịch của đơn đã giao/huỷ/hoàn, rồi tạo cuộc gọi lại cho các lịch đã đến giờ. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processDueRetries() {
        try {
            transactionTemplate.executeWithoutResult(s -> {
                autoCallRepository.skipScheduledForOrderStatuses(
                    RETRY_STOP_STATUSES,
                    "ORDER_CLOSED",
                    "Đơn đã giao / huỷ / hoàn trước giờ gọi"
                );
                autoCallRepository.clearRetriesForOrderStatuses(RETRY_STOP_STATUSES);
            });
            for (Long id : autoCallRepository.findDueRetryIds(Instant.now())) {
                Long newId = transactionTemplate.execute(s -> fireRetry(id, Instant.now()));
                if (newId != null) {
                    sendInNewTransaction(newId);
                }
            }
        } catch (Exception e) {
            LOG.warn("Auto Call retry sweep failed: {}", e.getMessage());
        }
    }

    /**
     * Cuộc {@code prevId} đã đến giờ hẹn: chưa gửi lần nào (chờ khung giờ) → trả chính nó để gửi;
     * đã có kết quả → tạo cuộc gọi lại (refId mới) và trả id cuộc mới cần gửi.
     */
    Long fireRetry(Long prevId, Instant now) {
        AutoCall prev = autoCallRepository.findById(prevId).orElse(null);
        if (prev == null || prev.getNextRetryAt() == null || prev.getNextRetryAt().isAfter(now)) {
            return null;
        }
        prev.setNextRetryAt(null);
        if (isScheduledUnsent(prev)) {
            return fireScheduled(prev, now);
        }
        if ("ERROR".equals(prev.getStatus())) {
            prev.setStatus("FAILED");
            prev.setResult("send_error");
        }
        prev.setUpdatedAt(now);
        autoCallRepository.save(prev);

        IntegrationConfig cfg = currentConfig();
        if (
            cfg == null ||
            !Boolean.TRUE.equals(cfg.getAutocallEnabled()) ||
            !Boolean.TRUE.equals(cfg.getAutocallRetryEnabled()) ||
            !cfg.isAutocallActiveKeyConfigured()
        ) {
            return null;
        }
        ShipmentOrder order = prev.getOrder();
        if (order == null || RETRY_STOP_STATUSES.contains(order.getStatus())) {
            return null;
        }
        if (prev.getCreatedAt() != null && autoCallRepository.existsByOrder_IdAndCreatedAtAfter(order.getId(), prev.getCreatedAt())) {
            return null;
        }
        String phone = normalizePhone(order.getReceiverPhone());
        if (phone == null) {
            appendEvent(order, "AUTO_CALL_SKIPPED", "Không gọi lại được: SĐT người nhận không hợp lệ");
            return null;
        }
        AutoCall call = new AutoCall();
        call.setOrder(order);
        call.setCallType(prev.getCallType());
        long seq = autoCallRepository.countByOrder_IdAndCallType(order.getId(), prev.getCallType()) + 1;
        call.setRefId(buildRefId(order.getOrderCode(), seq));
        call.setTriggerAction(TRIGGER_RETRY);
        call.setProvider(cfg.getAutocallActiveProvider());
        call.setSandbox(cfg.isAutocallSandbox());
        call.setCreatedAt(now);
        call.setPhone(phone);
        call.setStatus("PENDING");
        boolean newDay = nz(prev.getRetryNo()) >= retryIntervals(cfg).size();
        call.setRetryDay(newDay ? nz(prev.getRetryDay()) + 1 : nz(prev.getRetryDay()));
        call.setRetryNo(newDay ? 0 : nz(prev.getRetryNo()) + 1);
        autoCallRepository.save(call);
        appendEvent(
            order,
            "AUTO_CALL_REQUEST",
            sandboxPrefix(call) + "Gọi " + attemptLabel(call.getRetryDay(), call.getRetryNo()) + " → " + phone
        );
        return call.getId();
    }

    /** PENDING chưa có callId và đang có giờ hẹn = cuộc gọi chờ tới khung giờ gọi, chưa gửi HHVN. */
    static boolean isScheduledUnsent(AutoCall call) {
        return "PENDING".equals(call.getStatus()) && call.getCallId() == null;
    }

    private Long fireScheduled(AutoCall call, Instant now) {
        call.setUpdatedAt(now);
        IntegrationConfig cfg = currentConfig();
        ShipmentOrder order = call.getOrder();
        String skip = null;
        if (cfg == null || !Boolean.TRUE.equals(cfg.getAutocallEnabled())) {
            skip = "Auto Call đang tắt";
        } else if (order == null || RETRY_STOP_STATUSES.contains(order.getStatus())) {
            skip = "Đơn đã giao / huỷ / hoàn trước giờ gọi";
        }
        if (skip != null) {
            call.setStatus("SKIPPED");
            call.setErrorCode("NOT_SENT");
            call.setErrorMessage(skip);
            autoCallRepository.save(call);
            return null;
        }
        autoCallRepository.save(call);
        return call.getId();
    }

    /** Nút "Dừng gọi lại" trên đơn. */
    public List<AutoCallView> stopRetry(String orderCode, Long autoCallId) {
        ShipmentOrder order = requireOrder(orderCode);
        AutoCall call = autoCallRepository
            .findById(autoCallId)
            .filter(c -> c.getOrder() != null && order.getId().equals(c.getOrder().getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc gọi"));
        if (call.getNextRetryAt() != null) {
            boolean unsent = isScheduledUnsent(call);
            call.setNextRetryAt(null);
            if (unsent) {
                call.setStatus("CANCELLED");
                call.setResult("cancelled");
            }
            call.setUpdatedAt(Instant.now());
            autoCallRepository.save(call);
            appendEvent(order, "AUTO_CALL_RETRY", unsent ? "Đã huỷ cuộc gọi đang chờ khung giờ" : "Đã dừng gọi lại");
        }
        return list(orderCode);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** Nút "Cập nhật" trên đơn: gửi lại cuộc gọi lỗi, tra cứu HHVN cho cuộc gọi chưa có kết quả cuối. */
    public List<AutoCallView> sync(String orderCode) {
        ShipmentOrder order = requireOrder(orderCode);
        IntegrationConfig cfg = requireKeyConfigured();
        for (AutoCall call : autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(order.getId())) {
            if (FINAL_STATUSES.contains(call.getStatus()) || "SKIPPED".equals(call.getStatus())) {
                continue;
            }
            if (call.getNextRetryAt() != null && isScheduledUnsent(call)) {
                continue;
            }
            if (call.getCallId() == null) {
                send(call.getId());
                continue;
            }
            if (IntegrationConfig.PROVIDER_VTECH.equals(call.getProvider())) {
                continue;
            }
            if (!cfg.isAutocallApiKeyConfigured()) {
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
        boolean unsent = "PENDING".equals(call.getStatus()) || "ERROR".equals(call.getStatus());
        if (IntegrationConfig.PROVIDER_VTECH.equals(call.getProvider()) && !unsent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vtech không hỗ trợ huỷ cuộc gọi đã gửi sang tổng đài");
        }
        if (call.getCallId() == null) {
            if (
                "PENDING".equals(call.getStatus()) &&
                call.getNextRetryAt() == null &&
                call.getCreatedAt() != null &&
                call.getCreatedAt().isAfter(Instant.now().minusSeconds(30))
            ) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cuộc gọi đang được gửi sang tổng đài — thử lại sau vài giây");
            }
            call.setStatus("CANCELLED");
            call.setResult("cancelled");
            call.setNextRetryAt(null);
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
        IntegrationConfig cfg = currentConfig();
        if (cfg == null || !cfg.isAutocallApiKeyConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa lưu API key HHVN");
        }
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

    public static boolean tokenMatches(String expected, String given) {
        if (expected == null || given == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.trim().getBytes(StandardCharsets.UTF_8));
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
        if ("send_error".equals(call.getResult())) {
            return "Gọi giao: gửi sang tổng đài lỗi";
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
        alertAdmins(call, "send", message != null ? message : code);
        if (!"API_KEY_MISSING".equals(code)) {
            scheduleRetry(call);
        }
    }

    /** Popup cho admin đang mở web (SSE) — gửi sau commit để không báo lỗi của transaction bị rollback. */
    private void alertAdmins(AutoCall call, String kind, String message) {
        ServerEventService events = serverEventService;
        if (events == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", kind);
        payload.put("message", truncate(message, 255));
        payload.put("orderCode", call.getOrder() != null ? call.getOrder().getOrderCode() : null);
        payload.put("phone", call.getPhone());
        payload.put("refId", call.getRefId());
        payload.put("callId", call.getCallId());
        payload.put("sandbox", Boolean.TRUE.equals(call.getSandbox()));
        payload.put("provider", call.getProvider());
        payload.put("at", Instant.now().toString());
        dispatchAfterCommit(() -> events.autoCallError(payload));
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
        if (cfg == null || !cfg.isAutocallActiveKeyConfigured()) {
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
        Instant createdAt,
        Integer retryNo,
        Integer retryDay,
        Instant nextRetryAt,
        String provider
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
                c.getCreatedAt(),
                c.getRetryNo(),
                c.getRetryDay(),
                c.getNextRetryAt(),
                c.getProvider()
            );
        }
    }
}
