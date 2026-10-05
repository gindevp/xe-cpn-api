package com.mycompany.myapp.service.autocall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.AutoCall;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.AutoCallRepository;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import com.mycompany.myapp.service.partner.VtechAutoCallClient;
import com.mycompany.myapp.service.realtime.ServerEventService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class AutoCallServiceTest {

    private static final String KEY = "xk_test_abcdefgh1234";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private AutoCallRepository autoCallRepository;

    @Mock
    private IntegrationConfigRepository integrationConfigRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private ShipmentOrderRepository shipmentOrderRepository;

    @Mock
    private HhvnAutoCallClient client;

    @Mock
    private VtechAutoCallClient vtechClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    private final List<Runnable> dispatched = new ArrayList<>();
    private AutoCallService service;

    @BeforeEach
    void setUp() {
        service = new AutoCallService(
            autoCallRepository,
            integrationConfigRepository,
            orderEventRepository,
            shipmentOrderRepository,
            client,
            vtechClient,
            transactionManager
        ) {
            @Override
            protected void dispatchAfterCommit(Runnable task) {
                dispatched.add(task);
            }
        };
    }

    private static IntegrationConfig config(boolean enabled, String key) {
        IntegrationConfig c = new IntegrationConfig();
        c.setAutocallEnabled(enabled);
        c.setAutocallApiKey(key);
        c.setAutocallCallFrom("00:00");
        c.setAutocallCallTo("23:59");
        return c;
    }

    private static ShipmentOrder order(String phone) {
        ShipmentOrder o = new ShipmentOrder();
        o.setId(10L);
        o.setOrderCode("HN260930-0001");
        o.setReceiverPhone(phone);
        return o;
    }

    private void stubSaveAssignsId() {
        when(autoCallRepository.save(any(AutoCall.class))).thenAnswer(inv -> {
            AutoCall c = inv.getArgument(0);
            if (c.getId() == null) c.setId(99L);
            return c;
        });
    }

    private List<String> eventActions() {
        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository, org.mockito.Mockito.atLeast(0)).save(captor.capture());
        return captor.getAllValues().stream().map(OrderEvent::getAction).toList();
    }

    // ---- trigger ----

    @Test
    void arrivedAtDest_disabled_doesNothing() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(false, KEY)));
        service.onArrivedAtDest(order("0912345678"), "SCAN_IN");
        verify(autoCallRepository, never()).save(any());
        assertThat(dispatched).isEmpty();
    }

    @Test
    void arrivedAtDest_enabledWithoutKey_doesNothing() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, null)));
        service.onArrivedAtDest(order("0912345678"), "SCAN_IN");
        verify(autoCallRepository, never()).save(any());
    }

    @Test
    void arrivedAtDest_nonTriggerAction_doesNothing() {
        service.onArrivedAtDest(order("0912345678"), "RETURN_UNDO");
        verify(integrationConfigRepository, never()).findAll();
        verify(autoCallRepository, never()).save(any());
    }

    @Test
    void arrivedAtDest_scanIn_createsPendingCall_andDispatchesAfterCommit() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        when(autoCallRepository.countByOrder_IdAndCallType(10L, "giao")).thenReturn(0L);
        stubSaveAssignsId();

        service.onArrivedAtDest(order("+84 912 345 678"), "SCAN_IN");

        ArgumentCaptor<AutoCall> captor = ArgumentCaptor.forClass(AutoCall.class);
        verify(autoCallRepository).save(captor.capture());
        AutoCall c = captor.getValue();
        assertThat(c.getStatus()).isEqualTo("PENDING");
        assertThat(c.getPhone()).isEqualTo("0912345678");
        assertThat(c.getCallType()).isEqualTo("giao");
        assertThat(c.getSandbox()).isTrue();
        assertThat(c.getRefId()).startsWith("CPN-GIAO-HN260930-0001-1-");
        assertThat(eventActions()).containsExactly("AUTO_CALL_REQUEST");
        assertThat(dispatched).hasSize(1);
        verify(client, never()).createCall(any(), any(), any(), any(), any(), anyMap());
    }

    @Test
    void arrivedAtDest_failBackToBranch_isSecondCall_withNewRefId() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        when(autoCallRepository.countByOrder_IdAndCallType(10L, "giao")).thenReturn(1L);
        stubSaveAssignsId();

        service.onArrivedAtDest(order("0912345678"), "FAIL_MAX");

        ArgumentCaptor<AutoCall> captor = ArgumentCaptor.forClass(AutoCall.class);
        verify(autoCallRepository).save(captor.capture());
        assertThat(captor.getValue().getRefId()).startsWith("CPN-GIAO-HN260930-0001-2-");
        assertThat(captor.getValue().getTriggerAction()).isEqualTo("FAIL_MAX");
    }

    @Test
    void arrivedAtDest_invalidPhone_isSkipped_noDispatch() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        when(autoCallRepository.countByOrder_IdAndCallType(10L, "giao")).thenReturn(0L);
        stubSaveAssignsId();

        service.onArrivedAtDest(order("12345"), "SCAN_IN");

        ArgumentCaptor<AutoCall> captor = ArgumentCaptor.forClass(AutoCall.class);
        verify(autoCallRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SKIPPED");
        assertThat(captor.getValue().getErrorCode()).isEqualTo("INVALID_PHONE");
        assertThat(eventActions()).containsExactly("AUTO_CALL_SKIPPED");
        assertThat(dispatched).isEmpty();
    }

    // ---- gọi bù ----

    private ShipmentOrder atDest(long id, String code, String phone, String toOffice) {
        ShipmentOrder o = new ShipmentOrder();
        o.setId(id);
        o.setOrderCode(code);
        o.setReceiverPhone(phone);
        o.setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.AT_DEST);
        com.mycompany.myapp.domain.Office off = new com.mycompany.myapp.domain.Office();
        off.setCode(toOffice);
        o.setToOffice(off);
        return o;
    }

    private AutoCall lastCall(String status, String callId, java.time.Instant nextRetryAt, java.time.Instant createdAt) {
        AutoCall c = new AutoCall();
        c.setCallType("giao");
        c.setStatus(status);
        c.setCallId(callId);
        c.setNextRetryAt(nextRetryAt);
        c.setCreatedAt(createdAt);
        return c;
    }

    private AutoCall withResult(AutoCall c, String result) {
        c.setResult(result);
        return c;
    }

    @Test
    void catchUp_picksOrdersCustomerNeverAnswered() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        java.time.Instant now = java.time.Instant.now();
        ShipmentOrder none = atDest(1L, "A1", "0912345671", "VP_A");
        ShipmentOrder error = atDest(2L, "A2", "0912345672", "VP_A");
        ShipmentOrder skipped = atDest(3L, "A3", "0912345673", "VP_A");
        ShipmentOrder stuck = atDest(4L, "A4", "0912345674", "VP_A");
        ShipmentOrder inFlight = atDest(5L, "A5", "0912345675", "VP_A");
        ShipmentOrder scheduled = atDest(6L, "A6", "0912345676", "VP_A");
        ShipmentOrder answered = atDest(7L, "A7", "0912345677", "VP_A");
        ShipmentOrder queued = atDest(8L, "A8", "0912345678", "VP_A");
        ShipmentOrder retrying = atDest(9L, "A9", "0912345679", "VP_A");
        ShipmentOrder cancelled = atDest(10L, "A10", "0912345670", "VP_A");
        ShipmentOrder errorWithRetry = atDest(11L, "A11", "0912345611", "VP_A");
        ShipmentOrder delivering = atDest(12L, "A12", "0912345612", "VP_A");
        delivering.setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.OUT_FOR_DELIVERY);
        ShipmentOrder otherOffice = atDest(13L, "A13", "0912345613", "VP_B");
        ShipmentOrder carrierError = atDest(14L, "A14", "0912345614", "VP_A");
        ShipmentOrder vtechCancelled = atDest(15L, "A15", "0912345615", "VP_A");
        ShipmentOrder queuedLost = atDest(16L, "A16", "0912345616", "VP_A");
        ShipmentOrder answeredEarlier = atDest(17L, "A17", "0912345617", "VP_A");
        when(shipmentOrderRepository.findWithOfficesByOrderCodeIn(any())).thenReturn(
            List.of(
                carrierError,
                vtechCancelled,
                queuedLost,
                answeredEarlier,
                none,
                error,
                skipped,
                stuck,
                inFlight,
                scheduled,
                answered,
                queued,
                retrying,
                cancelled,
                errorWithRetry,
                delivering,
                otherOffice
            )
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(2L)).thenReturn(List.of(lastCall("ERROR", null, null, now)));
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(3L)).thenReturn(List.of(lastCall("SKIPPED", null, null, now)));
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(4L)).thenReturn(
            List.of(lastCall("PENDING", null, null, now.minusSeconds(600)))
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(5L)).thenReturn(List.of(lastCall("PENDING", null, null, now)));
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(6L)).thenReturn(
            List.of(lastCall("PENDING", null, now.plusSeconds(3600), now))
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(7L)).thenReturn(List.of(lastCall("COMPLETED", "c7", null, now)));
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(8L)).thenReturn(List.of(lastCall("QUEUED", null, null, now)));
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(9L)).thenReturn(
            List.of(withResult(lastCall("FAILED", "c9", now.plusSeconds(600), now), "not_answered"))
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(10L)).thenReturn(List.of(lastCall("CANCELLED", null, null, now)));
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(11L)).thenReturn(
            List.of(lastCall("ERROR", null, now.plusSeconds(600), now))
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(14L)).thenReturn(
            List.of(withResult(lastCall("FAILED", "c14", null, now), "error"), withResult(lastCall("FAILED", "c14a", null, now), "error"))
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(15L)).thenReturn(
            List.of(withResult(lastCall("CANCELLED", "vtech_15", null, now), "cancelled"))
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(16L)).thenReturn(
            List.of(lastCall("QUEUED", null, null, now.minus(java.time.Duration.ofHours(4))))
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(17L)).thenReturn(
            List.of(
                withResult(lastCall("FAILED", "c17b", null, now), "not_answered"),
                withResult(lastCall("COMPLETED", "c17a", null, now.minusSeconds(3600)), "answered")
            )
        );

        List<String> codes = List.of(
            "A1",
            "A2",
            "A3",
            "A4",
            "A5",
            "A6",
            "A7",
            "A8",
            "A9",
            "A10",
            "A11",
            "A12",
            "A13",
            "A14",
            "A15",
            "A16",
            "A17",
            "NOPE"
        );
        AutoCallService.CatchUpResult r = service.catchUp(codes, "VP_A", true, "dh1");

        assertThat(r.eligible()).containsExactly("A1", "A2", "A3", "A4", "A9", "A11", "A14", "A15", "A16");
        assertThat(r.sent()).isZero();
        assertThat(r.skipped())
            .extracting(AutoCallService.CatchUpSkip::orderCode)
            .containsExactly("A5", "A6", "A7", "A8", "A10", "A12", "A13", "A17", "NOPE");
        assertThat(r.skipped())
            .filteredOn(s -> s.orderCode().equals("A17"))
            .extracting(AutoCallService.CatchUpSkip::reason)
            .containsExactly("Khách đã nghe máy");
        verify(autoCallRepository, never()).save(any());
        assertThat(dispatched).isEmpty();
    }

    @Test
    void catchUp_createsNewCalls_respectingWindow_andInvalidPhone() {
        IntegrationConfig cfg = config(true, KEY);
        when(integrationConfigRepository.findAll()).thenReturn(List.of(cfg));
        when(shipmentOrderRepository.findWithOfficesByOrderCodeIn(any())).thenReturn(
            List.of(atDest(1L, "A1", "0912345671", "VP_A"), atDest(2L, "A2", "abc", "VP_A"))
        );
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        stubSaveAssignsId();

        AutoCallService.CatchUpResult r = service.catchUp(List.of("A1", "A2"), null, false, "dh1");

        assertThat(r.eligible()).containsExactly("A1", "A2");
        assertThat(r.sent()).isEqualTo(1);
        assertThat(r.skipped()).extracting(AutoCallService.CatchUpSkip::reason).containsExactly("SĐT người nhận không hợp lệ");
        ArgumentCaptor<AutoCall> saved = ArgumentCaptor.forClass(AutoCall.class);
        verify(autoCallRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getTriggerAction()).isEqualTo("CATCH_UP");
        assertThat(saved.getAllValues().get(0).getStatus()).isEqualTo("PENDING");
        assertThat(dispatched).hasSize(1);
        ArgumentCaptor<OrderEvent> ev = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository, times(2)).save(ev.capture());
        assertThat(ev.getAllValues().get(0).getDetail()).contains("Gọi bù (dh1) · Gọi giao → 0912345671");
    }

    @Test
    void catchUp_outsideWindow_schedules() {
        IntegrationConfig cfg = config(true, KEY);
        java.time.ZonedDateTime vn = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        String from = vn.plusHours(2).toLocalTime().withSecond(0).withNano(0).toString().substring(0, 5);
        String to = vn.plusHours(3).toLocalTime().withSecond(0).withNano(0).toString().substring(0, 5);
        cfg.setAutocallCallFrom(from);
        cfg.setAutocallCallTo(to);
        when(integrationConfigRepository.findAll()).thenReturn(List.of(cfg));
        when(shipmentOrderRepository.findWithOfficesByOrderCodeIn(any())).thenReturn(List.of(atDest(1L, "A1", "0912345671", "VP_A")));
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        stubSaveAssignsId();

        AutoCallService.CatchUpResult r = service.catchUp(List.of("A1"), null, false, null);

        assertThat(r.scheduled()).isEqualTo(1);
        assertThat(r.sent()).isZero();
        assertThat(dispatched).isEmpty();
    }

    @Test
    void catchUp_disabledOrNoKey_rejected() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(false, KEY)));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.catchUp(List.of("A1"), null, true, null)).hasMessageContaining(
            "Auto Call đang tắt"
        );
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, null)));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.catchUp(List.of("A1"), null, true, null)).hasMessageContaining(
            "API key"
        );
    }

    @Test
    void normalizePhone_acceptsVietnamFormats() {
        assertThat(AutoCallService.normalizePhone("0912345678")).isEqualTo("0912345678");
        assertThat(AutoCallService.normalizePhone("84912345678")).isEqualTo("0912345678");
        assertThat(AutoCallService.normalizePhone("+84 912 345 678")).isEqualTo("0912345678");
        assertThat(AutoCallService.normalizePhone("0912.345.678")).isEqualTo("0912345678");
        assertThat(AutoCallService.normalizePhone("12345")).isNull();
        assertThat(AutoCallService.normalizePhone("")).isNull();
        assertThat(AutoCallService.normalizePhone(null)).isNull();
    }

    @Test
    void buildRefId_isHhvnSafe_andAtMost64Chars() {
        String ref = AutoCallService.buildRefId("HN/260930 #0001-" + "X".repeat(80), 3);
        assertThat(ref).matches("[A-Za-z0-9_.:-]{1,64}");
    }

    // ---- send ----

    private AutoCall pendingCall() {
        AutoCall c = new AutoCall();
        c.setId(99L);
        c.setOrder(order("0912345678"));
        c.setCallType("giao");
        c.setRefId("CPN-GIAO-HN260930-0001-1-abc");
        c.setPhone("0912345678");
        c.setStatus("PENDING");
        return c;
    }

    @Test
    void send_accepted_storesCallIdAndQueued() throws Exception {
        AutoCall c = pendingCall();
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        JsonNode body = JSON.readTree(
            "{\"success\":true,\"requestId\":\"req_1\",\"accepted\":[{\"refId\":\"CPN-GIAO-HN260930-0001-1-abc\",\"callId\":\"call_1\",\"status\":\"queued\",\"duplicated\":false}],\"rejected\":[]}"
        );
        when(client.createCall(any(), eq(KEY), eq("giao"), eq("0912345678"), eq(c.getRefId()), anyMap())).thenReturn(
            new Result(true, 202, null, null, body)
        );

        service.send(99L);

        assertThat(c.getStatus()).isEqualTo("QUEUED");
        assertThat(c.getCallId()).isEqualTo("call_1");
        assertThat(c.getRequestId()).isEqualTo("req_1");
        verify(orderEventRepository, never()).save(any());
    }

    @Test
    void send_rejected_marksError_withEvent() throws Exception {
        AutoCall c = pendingCall();
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        JsonNode body = JSON.readTree(
            "{\"success\":true,\"accepted\":[],\"rejected\":[{\"code\":\"INVALID_PHONE\",\"message\":\"Số điện thoại không hợp lệ\"}]}"
        );
        when(client.createCall(any(), anyString(), anyString(), anyString(), anyString(), anyMap())).thenReturn(
            new Result(true, 202, null, null, body)
        );

        service.send(99L);

        assertThat(c.getStatus()).isEqualTo("ERROR");
        assertThat(c.getErrorCode()).isEqualTo("INVALID_PHONE");
        assertThat(eventActions()).containsExactly("AUTO_CALL_ERROR");
    }

    @Test
    void send_networkError_marksError() {
        AutoCall c = pendingCall();
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        when(client.createCall(any(), anyString(), anyString(), anyString(), anyString(), anyMap())).thenReturn(
            new Result(false, 0, "NETWORK_ERROR", "timeout", null)
        );

        service.send(99L);

        assertThat(c.getStatus()).isEqualTo("ERROR");
        assertThat(c.getErrorCode()).isEqualTo("NETWORK_ERROR");
    }

    @Test
    void send_skipsCallAlreadyQueued() {
        AutoCall c = pendingCall();
        c.setStatus("QUEUED");
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));

        service.send(99L);

        verify(client, never()).createCall(any(), any(), any(), any(), any(), anyMap());
    }

    // ---- webhook result ----

    private static final String FINISHED =
        "{\"callId\":\"call_1\",\"refId\":\"CPN-GIAO-HN260930-0001-1-abc\",\"status\":\"completed\",\"result\":\"answered\"," +
        "\"attemptCount\":2,\"firstCallAt\":\"2026-09-24T14:00:05+07:00\",\"answeredAt\":\"2026-09-24T14:01:22+07:00\"," +
        "\"finishedAt\":\"2026-09-24T14:01:51+07:00\",\"duration\":29,\"recordingUrl\":\"https://api.quanlydon.vn/partner/v1/recordings/x.wav\"}";

    @Test
    void applyCallObject_completed_updatesAndLogsOnce() throws Exception {
        AutoCall c = pendingCall();
        c.setStatus("QUEUED");
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));

        assertThat(service.applyCallObject(JSON.readTree(FINISHED))).isTrue();
        assertThat(service.applyCallObject(JSON.readTree(FINISHED))).isTrue();

        assertThat(c.getStatus()).isEqualTo("COMPLETED");
        assertThat(c.getResult()).isEqualTo("answered");
        assertThat(c.getAttemptCount()).isEqualTo(2);
        assertThat(c.getDurationSec()).isEqualTo(29);
        assertThat(c.getRecordingUrl()).endsWith("x.wav");
        assertThat(c.getAnsweredAt()).isNotNull();
        verify(orderEventRepository, times(1)).save(any());
        assertThat(AutoCallService.resultText(c)).isEqualTo("Gọi giao: khách nghe máy · 29s · 2 lần gọi");
    }

    @Test
    void applyCallObject_unknownCall_returnsFalse() throws Exception {
        when(autoCallRepository.findOneByRefId(anyString())).thenReturn(Optional.empty());
        when(autoCallRepository.findFirstByCallId("call_1")).thenReturn(Optional.empty());
        assertThat(service.applyCallObject(JSON.readTree(FINISHED))).isFalse();
    }

    @Test
    void resultText_failedNotAnswered_andError() {
        AutoCall c = pendingCall();
        c.setStatus("FAILED");
        c.setResult("not_answered");
        c.setAttemptCount(3);
        assertThat(AutoCallService.resultText(c)).isEqualTo("Gọi giao: khách không nghe máy sau 3 lần gọi");
        c.setResult("error");
        assertThat(AutoCallService.resultText(c)).isEqualTo("Gọi giao: lỗi tổng đài / nhà mạng");
    }

    // ---- cancel ----

    private AutoCall stubOrderCall(String status, String callId) {
        AutoCall c = pendingCall();
        c.setStatus(status);
        c.setCallId(callId);
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("HN260930-0001")).thenReturn(Optional.of(c.getOrder()));
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void cancel_withoutCallId_cancelsLocallyWithoutHhvn() {
        AutoCall c = stubOrderCall("ERROR", null);

        service.cancel("HN260930-0001", 99L);

        assertThat(c.getStatus()).isEqualTo("CANCELLED");
        assertThat(eventActions()).containsExactly("AUTO_CALL_RESULT");
        verify(client, never()).cancelCall(any(), any(), any());
    }

    @Test
    void cancel_justCreatedPending_rejectedWhileDispatching() {
        AutoCall c = stubOrderCall("PENDING", null);
        c.setCreatedAt(java.time.Instant.now());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancel("HN260930-0001", 99L)).hasMessageContaining("thử lại");
        assertThat(c.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void cancel_queued_cancelsAtHhvnThenAppliesCancelledStatus() throws Exception {
        AutoCall c = stubOrderCall("QUEUED", "call_1");
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        String body =
            "{\"success\":true,\"cancelled\":1,\"notCancellable\":0,\"call\":{\"callId\":\"call_1\",\"refId\":\"" +
            c.getRefId() +
            "\",\"status\":\"cancelled\",\"result\":\"cancelled\"}}";
        when(client.cancelCall(any(), eq(KEY), eq("call_1"))).thenReturn(new Result(true, 200, null, null, JSON.readTree(body)));
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));

        service.cancel("HN260930-0001", 99L);

        assertThat(c.getStatus()).isEqualTo("CANCELLED");
        assertThat(eventActions()).containsExactly("AUTO_CALL_RESULT");
        assertThat(AutoCallService.resultText(c)).isEqualTo("Gọi giao: đã huỷ");
        verify(client, never()).getCall(any(), any(), any());
    }

    @Test
    void cancel_hhvnNotCancellable_throwsAndSyncsRealStatus() throws Exception {
        AutoCall c = stubOrderCall("QUEUED", "call_1");
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        String body =
            "{\"success\":true,\"cancelled\":0,\"notCancellable\":1,\"call\":{\"callId\":\"call_1\",\"refId\":\"" +
            c.getRefId() +
            "\",\"status\":\"calling\"}}";
        when(client.cancelCall(any(), eq(KEY), eq("call_1"))).thenReturn(new Result(true, 200, null, null, JSON.readTree(body)));
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancel("HN260930-0001", 99L)).hasMessageContaining(
            "đang đổ chuông"
        );
        assertThat(c.getStatus()).isEqualTo("CALLING");
        assertThat(eventActions()).isEmpty();
    }

    @Test
    void cancel_hhvnRefuses_throwsAndKeepsStatus() {
        AutoCall c = stubOrderCall("CALLING", "call_1");
        when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));
        when(client.cancelCall(any(), eq(KEY), eq("call_1"))).thenReturn(
            new Result(false, 400, "VALIDATION_ERROR", "Không huỷ được", null)
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancel("HN260930-0001", 99L)).hasMessageContaining(
            "Không huỷ được"
        );
        assertThat(c.getStatus()).isEqualTo("CALLING");
    }

    @Test
    void cancel_finishedOrOtherOrder_rejected() {
        stubOrderCall("COMPLETED", "call_1");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancel("HN260930-0001", 99L)).hasMessageContaining("đã kết thúc");

        ShipmentOrder other = order("0912345678");
        other.setId(11L);
        other.setOrderCode("HN260930-0002");
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("HN260930-0002")).thenReturn(Optional.of(other));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancel("HN260930-0002", 99L)).hasMessageContaining(
            "Không tìm thấy"
        );
    }

    // ---- retry ----

    private static IntegrationConfig retryConfig() {
        IntegrationConfig c = config(true, KEY);
        c.setAutocallRetryEnabled(true);
        c.setAutocallRetryIntervals("60,120");
        c.setAutocallCallFrom("07:30");
        c.setAutocallCallTo("20:00");
        return c;
    }

    private static java.time.Instant vn(String localDateTime) {
        return java.time.LocalDateTime.parse(localDateTime).atZone(AutoCallService.VN).toInstant();
    }

    private AutoCall failedCallAt(String createdLocal, int retryNo) {
        AutoCall c = pendingCall();
        c.setStatus("FAILED");
        c.setResult("not_answered");
        c.setCreatedAt(vn(createdLocal));
        c.setRetryNo(retryNo);
        return c;
    }

    private static java.time.Instant nextAt(IntegrationConfig cfg, AutoCall prev, java.time.Instant now) {
        AutoCallService.RetrySlot s = AutoCallService.nextRetry(cfg, prev, now);
        return s == null ? null : s.at();
    }

    @Test
    void nextRetry_daysRepeat_opensNewRoundNextMorning_untilMaxDays() {
        IntegrationConfig cfg = retryConfig();
        cfg.setAutocallRetryDays(2);
        AutoCall lastOfDay1 = failedCallAt("2026-10-02T12:10:00", 2);
        AutoCallService.RetrySlot s = AutoCallService.nextRetry(cfg, lastOfDay1, vn("2026-10-02T12:15:00"));
        assertThat(s.at()).isEqualTo(vn("2026-10-03T07:30:00"));
        assertThat(s.newDay()).isTrue();

        AutoCall firstOfDay2 = failedCallAt("2026-10-03T07:30:00", 0);
        firstOfDay2.setRetryDay(1);
        AutoCallService.RetrySlot s2 = AutoCallService.nextRetry(cfg, firstOfDay2, vn("2026-10-03T07:35:00"));
        assertThat(s2.at()).isEqualTo(vn("2026-10-03T08:35:00"));
        assertThat(s2.newDay()).isFalse();

        AutoCall lastOfDay2 = failedCallAt("2026-10-03T10:40:00", 2);
        lastOfDay2.setRetryDay(1);
        assertThat(AutoCallService.nextRetry(cfg, lastOfDay2, vn("2026-10-03T10:45:00"))).isNull();
    }

    @Test
    void attemptLabel_describesDayAndAttempt() {
        assertThat(AutoCallService.attemptLabel(0, 2)).isEqualTo("gọi lại lần 2");
        assertThat(AutoCallService.attemptLabel(1, 0)).isEqualTo("ngày 2 · cuộc 1");
        assertThat(AutoCallService.attemptLabel(1, 1)).isEqualTo("ngày 2 · gọi lại lần 1");
    }

    @Test
    void fireRetry_afterLastAttemptOfDay_startsNewDayRound() {
        java.time.Instant now = vn("2026-10-03T07:30:30");
        AutoCall prev = failedCallAt("2026-10-02T12:10:00", 2);
        prev.setNextRetryAt(vn("2026-10-03T07:30:00"));
        prev.getOrder().setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.AT_DEST);
        IntegrationConfig cfg = retryConfig();
        cfg.setAutocallRetryDays(3);
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(prev));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(cfg));
        when(autoCallRepository.existsByOrder_IdAndCreatedAtAfter(10L, prev.getCreatedAt())).thenReturn(false);
        when(autoCallRepository.countByOrder_IdAndCallType(10L, "giao")).thenReturn(3L);
        when(autoCallRepository.save(any(AutoCall.class))).thenAnswer(inv -> {
            AutoCall a = inv.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });

        assertThat(service.fireRetry(99L, now)).isEqualTo(100L);

        ArgumentCaptor<AutoCall> captor = ArgumentCaptor.forClass(AutoCall.class);
        verify(autoCallRepository, times(2)).save(captor.capture());
        AutoCall created = captor.getAllValues().get(1);
        assertThat(created.getRetryDay()).isEqualTo(1);
        assertThat(created.getRetryNo()).isZero();
    }

    @Test
    void nextRetryAt_usesIntervalOfEachAttempt_thenStops() {
        IntegrationConfig cfg = retryConfig();
        assertThat(nextAt(cfg, failedCallAt("2026-10-02T09:00:00", 0), vn("2026-10-02T09:05:00"))).isEqualTo(vn("2026-10-02T10:05:00"));
        assertThat(nextAt(cfg, failedCallAt("2026-10-02T10:05:00", 1), vn("2026-10-02T10:10:00"))).isEqualTo(vn("2026-10-02T12:10:00"));
        assertThat(nextAt(cfg, failedCallAt("2026-10-02T12:10:00", 2), vn("2026-10-02T12:15:00"))).isNull();
    }

    @Test
    void nextRetryAt_afterWindow_carriesOverToNextMorning_withoutExtraAttempts() {
        IntegrationConfig cfg = retryConfig();
        assertThat(nextAt(cfg, failedCallAt("2026-10-02T19:20:00", 0), vn("2026-10-02T19:30:00"))).isEqualTo(vn("2026-10-03T07:30:00"));
        assertThat(nextAt(cfg, failedCallAt("2026-10-03T07:30:00", 1), vn("2026-10-03T07:35:00"))).isEqualTo(vn("2026-10-03T09:35:00"));
        assertThat(nextAt(cfg, failedCallAt("2026-10-02T19:00:00", 0), vn("2026-10-02T19:00:00"))).isEqualTo(vn("2026-10-02T20:00:00"));
    }

    @Test
    void nextRetryAt_beforeWindow_waitsUntilStart_andEmptyListMeansNoRetry() {
        IntegrationConfig cfg = retryConfig();
        assertThat(nextAt(cfg, failedCallAt("2026-10-02T05:00:00", 0), vn("2026-10-02T05:10:00"))).isEqualTo(vn("2026-10-02T07:30:00"));
        cfg.setAutocallRetryIntervals("");
        assertThat(nextAt(cfg, failedCallAt("2026-10-02T09:00:00", 0), vn("2026-10-02T09:05:00"))).isNull();
    }

    @Test
    void arrivedAtDest_outsideWindow_schedulesFirstCallForMorning_withoutSending() {
        org.junit.jupiter.api.Assumptions.assumeTrue(java.time.LocalTime.now(AutoCallService.VN).isBefore(java.time.LocalTime.of(23, 57)));
        IntegrationConfig cfg = config(true, KEY);
        cfg.setAutocallCallFrom("23:58");
        cfg.setAutocallCallTo("23:59");
        when(integrationConfigRepository.findAll()).thenReturn(List.of(cfg));
        when(autoCallRepository.countByOrder_IdAndCallType(10L, "giao")).thenReturn(0L);
        stubSaveAssignsId();

        service.onArrivedAtDest(order("0912345678"), "SCAN_IN");

        ArgumentCaptor<AutoCall> captor = ArgumentCaptor.forClass(AutoCall.class);
        verify(autoCallRepository).save(captor.capture());
        AutoCall saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getNextRetryAt()).isAfter(saved.getCreatedAt());
        assertThat(dispatched).isEmpty();
        verify(autoCallRepository).skipScheduledForOrder(eq(10L), eq("SUPERSEDED"), anyString());
    }

    @Test
    void fitCallWindow_movesLateTriggerToNextMorning() {
        IntegrationConfig cfg = retryConfig();
        assertThat(AutoCallService.fitCallWindow(cfg, vn("2026-10-02T21:15:00"))).isEqualTo(vn("2026-10-03T07:30:00"));
        assertThat(AutoCallService.fitCallWindow(cfg, vn("2026-10-02T06:00:00"))).isEqualTo(vn("2026-10-02T07:30:00"));
        assertThat(AutoCallService.fitCallWindow(cfg, vn("2026-10-02T12:00:00"))).isEqualTo(vn("2026-10-02T12:00:00"));
    }

    @Test
    void fireRetry_scheduledFirstCall_returnsSameCallToSend() {
        java.time.Instant now = vn("2026-10-03T07:30:30");
        AutoCall c = pendingCall();
        c.setCreatedAt(vn("2026-10-02T21:00:00"));
        c.setNextRetryAt(vn("2026-10-03T07:30:00"));
        c.getOrder().setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.AT_DEST);
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(retryConfig()));

        assertThat(service.fireRetry(99L, now)).isEqualTo(99L);
        assertThat(c.getNextRetryAt()).isNull();
        assertThat(c.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void fireRetry_scheduledFirstCall_deliveredOrder_isSkipped() {
        java.time.Instant now = vn("2026-10-03T07:30:30");
        AutoCall c = pendingCall();
        c.setCreatedAt(vn("2026-10-02T21:00:00"));
        c.setNextRetryAt(vn("2026-10-03T07:30:00"));
        c.getOrder().setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.DELIVERED);
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(retryConfig()));

        assertThat(service.fireRetry(99L, now)).isNull();
        assertThat(c.getStatus()).isEqualTo("SKIPPED");
    }

    @Test
    void retryAllowed_respectsReasonToggles() {
        IntegrationConfig cfg = retryConfig();
        assertThat(AutoCallService.retryAllowed(cfg, AutoCallService.RetryReason.NO_ANSWER)).isTrue();
        cfg.setAutocallRetryCarrierError(false);
        assertThat(AutoCallService.retryAllowed(cfg, AutoCallService.RetryReason.CARRIER_ERROR)).isFalse();
        cfg.setAutocallRetryEnabled(false);
        assertThat(AutoCallService.retryAllowed(cfg, AutoCallService.RetryReason.NO_ANSWER)).isFalse();
    }

    @Test
    void applyCallObject_failed_schedulesRetry() throws Exception {
        AutoCall c = pendingCall();
        c.setStatus("QUEUED");
        c.setCreatedAt(java.time.Instant.now());
        c.getOrder().setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.AT_DEST);
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));
        IntegrationConfig cfg = retryConfig();
        cfg.setAutocallCallFrom("00:00");
        cfg.setAutocallCallTo("23:59");
        when(integrationConfigRepository.findAll()).thenReturn(List.of(cfg));

        service.applyCallObject(
            JSON.readTree("{\"callId\":\"call_1\",\"refId\":\"" + c.getRefId() + "\",\"status\":\"failed\",\"result\":\"not_answered\"}")
        );

        assertThat(c.getNextRetryAt()).isNotNull();
        assertThat(eventActions()).containsExactly("AUTO_CALL_RESULT", "AUTO_CALL_RETRY");
    }

    @SuppressWarnings("unchecked")
    @Test
    void applyCallObject_carrierError_alertsAdminsAfterCommit_notAnsweredDoesNot() throws Exception {
        ServerEventService events = org.mockito.Mockito.mock(ServerEventService.class);
        service.setServerEventService(events);
        org.mockito.Mockito.lenient().when(integrationConfigRepository.findAll()).thenReturn(List.of(config(true, KEY)));

        AutoCall err = pendingCall();
        err.setStatus("QUEUED");
        when(autoCallRepository.findOneByRefId(err.getRefId())).thenReturn(Optional.of(err));
        service.applyCallObject(
            JSON.readTree("{\"callId\":\"call_1\",\"refId\":\"" + err.getRefId() + "\",\"status\":\"failed\",\"result\":\"error\"}")
        );
        verify(events, never()).autoCallError(anyMap());
        dispatched.forEach(Runnable::run);
        ArgumentCaptor<java.util.Map<String, Object>> payload = ArgumentCaptor.forClass(java.util.Map.class);
        verify(events).autoCallError(payload.capture());
        assertThat(payload.getValue())
            .containsEntry("kind", "carrier")
            .containsEntry("phone", err.getPhone())
            .containsEntry("refId", err.getRefId())
            .containsEntry("callId", "call_1");

        dispatched.clear();
        AutoCall noAnswer = pendingCall();
        noAnswer.setRefId("CPN-GIAO-OTHER");
        noAnswer.setStatus("QUEUED");
        when(autoCallRepository.findOneByRefId("CPN-GIAO-OTHER")).thenReturn(Optional.of(noAnswer));
        service.applyCallObject(JSON.readTree("{\"refId\":\"CPN-GIAO-OTHER\",\"status\":\"failed\",\"result\":\"not_answered\"}"));
        dispatched.forEach(Runnable::run);
        verify(events, times(1)).autoCallError(anyMap());
    }

    @Test
    void applyCallObject_failed_deliveredOrder_noRetry() throws Exception {
        AutoCall c = pendingCall();
        c.setStatus("QUEUED");
        c.getOrder().setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.DELIVERED);
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(retryConfig()));

        service.applyCallObject(
            JSON.readTree("{\"callId\":\"call_1\",\"refId\":\"" + c.getRefId() + "\",\"status\":\"failed\",\"result\":\"not_answered\"}")
        );

        assertThat(c.getNextRetryAt()).isNull();
        assertThat(eventActions()).containsExactly("AUTO_CALL_RESULT");
    }

    @Test
    void fireRetry_createsNewPendingCall_andClearsSchedule() {
        java.time.Instant now = vn("2026-10-02T10:00:00");
        AutoCall prev = failedCallAt("2026-10-02T09:00:00", 0);
        prev.setNextRetryAt(now.minusSeconds(5));
        prev.getOrder().setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.AT_DEST);
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(prev));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(retryConfig()));
        when(autoCallRepository.existsByOrder_IdAndCreatedAtAfter(10L, prev.getCreatedAt())).thenReturn(false);
        when(autoCallRepository.countByOrder_IdAndCallType(10L, "giao")).thenReturn(1L);
        when(autoCallRepository.save(any(AutoCall.class))).thenAnswer(inv -> {
            AutoCall a = inv.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });

        assertThat(service.fireRetry(99L, now)).isEqualTo(100L);

        assertThat(prev.getNextRetryAt()).isNull();
        ArgumentCaptor<AutoCall> captor = ArgumentCaptor.forClass(AutoCall.class);
        verify(autoCallRepository, times(2)).save(captor.capture());
        AutoCall created = captor.getAllValues().get(1);
        assertThat(created.getStatus()).isEqualTo("PENDING");
        assertThat(created.getTriggerAction()).isEqualTo("RETRY");
        assertThat(created.getRetryNo()).isEqualTo(1);
        assertThat(created.getRefId()).startsWith("CPN-GIAO-HN260930-0001-2-");
    }

    @Test
    void fireRetry_deliveredOrder_doesNotCall() {
        java.time.Instant now = vn("2026-10-02T10:00:00");
        AutoCall prev = failedCallAt("2026-10-02T09:00:00", 0);
        prev.setNextRetryAt(now.minusSeconds(5));
        prev.getOrder().setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.DELIVERED);
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(prev));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(retryConfig()));

        assertThat(service.fireRetry(99L, now)).isNull();
        assertThat(prev.getNextRetryAt()).isNull();
        verify(autoCallRepository, times(1)).save(any());
    }

    // ---- signature ----

    private static String sign(String secret, String ts, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((ts + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void verifySignature_acceptsValid_rejectsTamperedWrongSecretAndStale() throws Exception {
        String secret = "whsec_test";
        String body = "{\"event\":\"call.finished\",\"data\":{\"refId\":\"Ă-x\"}}";
        long now = 1790233313L;
        String ts = String.valueOf(now);
        String sig = sign(secret, ts, body);
        byte[] raw = body.getBytes(StandardCharsets.UTF_8);

        assertThat(AutoCallService.verifySignature(secret, ts, raw, "sha256=" + sig, now)).isTrue();
        assertThat(AutoCallService.verifySignature(secret, ts, raw, sig, now + 299)).isTrue();
        assertThat(AutoCallService.verifySignature("whsec_other", ts, raw, "sha256=" + sig, now)).isFalse();
        assertThat(
            AutoCallService.verifySignature(secret, ts, (body + " ").getBytes(StandardCharsets.UTF_8), "sha256=" + sig, now)
        ).isFalse();
        assertThat(AutoCallService.verifySignature(secret, ts, raw, "sha256=" + sig, now + 301)).isFalse();
        assertThat(AutoCallService.verifySignature(secret, null, raw, "sha256=" + sig, now)).isFalse();
        assertThat(AutoCallService.verifySignature(secret, ts, raw, null, now)).isFalse();
    }

    // ---- Vtech ----

    private static final String VTECH_KEY = "tdai_live_secretkey9876";

    private static IntegrationConfig vtechConfig() {
        IntegrationConfig c = config(true, KEY);
        c.setAutocallProvider(IntegrationConfig.PROVIDER_VTECH);
        c.setAutocallVtechApiKey(VTECH_KEY);
        return c;
    }

    private static String vtechEvent(String refId, String phone, String status, String outcome) {
        String extra = refId == null ? "{}" : "{\"ref_id\":\"" + refId + "\",\"ma_don\":\"HN260930-0001\"}";
        return (
            "{\"event\":\"call.completed\",\"timestamp\":\"2026-05-20T07:30:00.000Z\",\"campaign_id\":123,\"call\":{" +
            "\"id\":456789,\"external_call_id\":\"abc\",\"contact\":{\"id\":1,\"phone_number\":\"" +
            phone +
            "\",\"name\":\"A\",\"extra_data\":" +
            extra +
            "},\"status\":\"" +
            status +
            "\",\"outcome\":" +
            (outcome == null ? "null" : "\"" + outcome + "\"") +
            ",\"duration_seconds\":125,\"recording_url\":\"https://example.com/r.wav\"," +
            "\"started_at\":\"2026-05-20T07:28:00.000Z\",\"answered_at\":\"2026-05-20T07:28:05.000Z\"," +
            "\"ended_at\":\"2026-05-20T07:30:00.000Z\",\"hangup_by\":\"user\"}}"
        );
    }

    @Test
    void vtech_arrivedAtDest_marksProviderVtech_notSandbox_evenWithHhvnTestKeySaved() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(vtechConfig()));
        when(autoCallRepository.countByOrder_IdAndCallType(10L, "giao")).thenReturn(0L);
        stubSaveAssignsId();

        service.onArrivedAtDest(order("0912345678"), "SCAN_IN");

        ArgumentCaptor<AutoCall> captor = ArgumentCaptor.forClass(AutoCall.class);
        verify(autoCallRepository).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo("VTECH");
        assertThat(captor.getValue().getSandbox()).isFalse();
        assertThat(dispatched).hasSize(1);
    }

    @Test
    void vtech_selectedWithoutVtechKey_doesNotCall_evenIfHhvnKeySaved() {
        IntegrationConfig cfg = vtechConfig();
        cfg.setAutocallVtechApiKey(null);
        when(integrationConfigRepository.findAll()).thenReturn(List.of(cfg));
        service.onArrivedAtDest(order("0912345678"), "SCAN_IN");
        verify(autoCallRepository, never()).save(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void vtech_send_importsContactWithScriptVariables_andQueuesWithoutCallId() throws Exception {
        AutoCall c = pendingCall();
        c.getOrder().setReceiverName("Nguyễn Văn B");
        c.getOrder().setGoodsType(com.mycompany.myapp.domain.enumeration.GoodsType.THUONG);
        c.getOrder().setNote("[LOAI]Khác|Khác[/LOAI]\n[TENHANG]VALI MỸ PHẨM|Máy làm tóc[/TENHANG]\n[CUOC]30000,20000[/CUOC]");
        com.mycompany.myapp.domain.Office office = new com.mycompany.myapp.domain.Office();
        office.setName("VP Mỹ Đình");
        office.setAddress("123 Phạm Hùng, Hà Nội");
        c.getOrder().setToOffice(office);
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(vtechConfig()));
        when(vtechClient.importContact(any(), eq(VTECH_KEY), eq("0912345678"), anyMap())).thenReturn(
            new Result(true, 201, null, null, JSON.readTree("{\"data\":{\"total\":1,\"imported\":1,\"skipped\":0,\"errors\":[]}}"))
        );

        service.send(99L);

        ArgumentCaptor<java.util.Map<String, String>> extra = ArgumentCaptor.forClass(java.util.Map.class);
        verify(vtechClient).importContact(any(), eq(VTECH_KEY), eq("0912345678"), extra.capture());
        assertThat(extra.getValue())
            .containsEntry("ref_id", c.getRefId())
            .containsEntry("ma_don", "HN260930-0001")
            .containsEntry("ten_san_pham", "VALI MỸ PHẨM, Máy làm tóc")
            .containsEntry("diem_nhan", "VP Mỹ Đình");
        assertThat(c.getStatus()).isEqualTo("QUEUED");
        assertThat(c.getCallId()).isNull();
        assertThat(c.getProvider()).isEqualTo("VTECH");
        verify(client, never()).createCall(any(), any(), any(), any(), any(), anyMap());
    }

    @Test
    void goodsNameLabel_usesPackageGoodsNames() {
        assertThat(AutoCallService.goodsNameLabel("[LOAI]Khác[/LOAI]\n[TENHANG]RĂNG[/TENHANG]")).isEqualTo("RĂNG");
        assertThat(AutoCallService.goodsNameLabel("[LOAI]Phong bì( giấy tờ bản a5)[/LOAI]\nghi chú")).isEqualTo(
            "Phong bì( giấy tờ bản a5)"
        );
        assertThat(AutoCallService.goodsNameLabel("[LOAI]Xe máy|Khác|Xe máy[/LOAI][TENHANG]||[/TENHANG]")).isEqualTo("Xe máy");
        assertThat(AutoCallService.goodsNameLabel("[LOAI]Khác|Xe máy[/LOAI][TENHANG]mẫu|[/TENHANG]")).isEqualTo("mẫu, Xe máy");
        assertThat(AutoCallService.goodsNameLabel("[KIEN]Áo, Quần[/KIEN]")).isEqualTo("Áo, Quần");
        assertThat(AutoCallService.goodsNameLabel("[LOAI]Khác[/LOAI]")).isEqualTo("Hàng hoá");
        assertThat(AutoCallService.goodsNameLabel(null)).isEqualTo("Hàng hoá");
    }

    @Test
    void vtech_send_rowError_marksError() throws Exception {
        AutoCall c = pendingCall();
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(vtechConfig()));
        when(vtechClient.importContact(any(), any(), any(), anyMap())).thenReturn(
            new Result(
                true,
                201,
                null,
                null,
                JSON.readTree(
                    "{\"data\":{\"total\":1,\"imported\":0,\"skipped\":0,\"errors\":[{\"row\":1,\"phone_number\":\"x\",\"error\":\"Số điện thoại không hợp lệ\"}]}}"
                )
            )
        );

        service.send(99L);

        assertThat(c.getStatus()).isEqualTo("ERROR");
        assertThat(c.getErrorCode()).isEqualTo("VTECH_REJECTED");
        assertThat(c.getErrorMessage()).contains("Số điện thoại không hợp lệ");
        assertThat(eventActions()).containsExactly("AUTO_CALL_ERROR");
    }

    @Test
    void vtech_send_invalidKey_marksError() {
        AutoCall c = pendingCall();
        when(autoCallRepository.findById(99L)).thenReturn(Optional.of(c));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(vtechConfig()));
        when(vtechClient.importContact(any(), any(), any(), anyMap())).thenReturn(
            new Result(false, 401, "INVALID_API_KEY", "API key không hợp lệ", null)
        );

        service.send(99L);

        assertThat(c.getStatus()).isEqualTo("ERROR");
        assertThat(c.getErrorCode()).isEqualTo("INVALID_API_KEY");
    }

    @Test
    void vtech_webhook_connected_completesByRefId_once() throws Exception {
        AutoCall c = pendingCall();
        c.setProvider("VTECH");
        c.setStatus("QUEUED");
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));
        String ev = vtechEvent(c.getRefId(), "0912345678", "COMPLETED", "CONNECTED");

        assertThat(service.applyVtechWebhook(JSON.readTree(ev))).isEqualTo(AutoCallService.VtechWebhookOutcome.APPLIED);
        assertThat(service.applyVtechWebhook(JSON.readTree(ev))).isEqualTo(AutoCallService.VtechWebhookOutcome.APPLIED);

        assertThat(c.getStatus()).isEqualTo("COMPLETED");
        assertThat(c.getResult()).isEqualTo("answered");
        assertThat(c.getCallId()).isEqualTo("vtech_456789");
        assertThat(c.getDurationSec()).isEqualTo(125);
        assertThat(c.getRecordingUrl()).isEqualTo("https://example.com/r.wav");
        assertThat(c.getAnsweredAt()).isEqualTo(java.time.Instant.parse("2026-05-20T07:28:05Z"));
        verify(orderEventRepository, times(1)).save(any());
    }

    @Test
    void vtech_webhook_mapsStatusAndOutcome() throws Exception {
        String[][] cases = {
            { "COMPLETED", "CONNECTED", "completed", "answered" },
            { "COMPLETED", "VOICEMAIL", "failed", "not_answered" },
            { "COMPLETED", null, "completed", "answered" },
            { "NO_ANSWER", "NO_ANSWER", "failed", "not_answered" },
            { "BUSY", "BUSY", "failed", "not_answered" },
            { "BUSY", null, "failed", "not_answered" },
            { "NO_ANSWER", "VOICEMAIL", "failed", "not_answered" },
            { "NO_ANSWER", "REJECTED", "failed", "not_answered" },
            { "NO_ANSWER", null, "failed", "not_answered" },
            { "FAILED", "REJECTED", "failed", "not_answered" },
            { "FAILED", "NETWORK_ERROR", "failed", "error" },
            { "FAILED", "INVALID_NUMBER", "failed", "error" },
            { "FAILED", null, "failed", "error" },
            { "CANCELLED", null, "cancelled", "cancelled" },
        };
        for (String[] k : cases) {
            JsonNode vc = JSON.readTree(vtechEvent("R", "0912345678", k[0], k[1])).path("call");
            JsonNode n = AutoCallService.toHhvnCall(vc, "R");
            assertThat(n.path("status").asText()).as(k[0] + "/" + k[1]).isEqualTo(k[2]);
            assertThat(n.path("result").asText()).as(k[0] + "/" + k[1]).isEqualTo(k[3]);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void vtech_webhook_networkError_alertsAdmins_andSchedulesRetry() throws Exception {
        ServerEventService events = org.mockito.Mockito.mock(ServerEventService.class);
        service.setServerEventService(events);
        IntegrationConfig cfg = vtechConfig();
        cfg.setAutocallRetryEnabled(true);
        cfg.setAutocallRetryIntervals("60");
        when(integrationConfigRepository.findAll()).thenReturn(List.of(cfg));
        AutoCall c = pendingCall();
        c.setProvider("VTECH");
        c.setStatus("QUEUED");
        c.getOrder().setStatus(com.mycompany.myapp.domain.enumeration.OrderStatus.AT_DEST);
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));

        service.applyVtechWebhook(JSON.readTree(vtechEvent(c.getRefId(), "0912345678", "FAILED", "NETWORK_ERROR")));
        dispatched.forEach(Runnable::run);

        assertThat(c.getStatus()).isEqualTo("FAILED");
        assertThat(c.getResult()).isEqualTo("error");
        assertThat(c.getNextRetryAt()).isNotNull();
        ArgumentCaptor<java.util.Map<String, Object>> payload = ArgumentCaptor.forClass(java.util.Map.class);
        verify(events).autoCallError(payload.capture());
        assertThat(payload.getValue()).containsEntry("kind", "carrier").containsEntry("provider", "VTECH");
    }

    @Test
    void vtech_webhook_withoutRefId_fallsBackToLatestQueuedCallOfPhone() throws Exception {
        AutoCall c = pendingCall();
        c.setProvider("VTECH");
        c.setStatus("QUEUED");
        when(autoCallRepository.findFirstByProviderAndPhoneAndStatusOrderByCreatedAtDesc("VTECH", "0912345678", "QUEUED")).thenReturn(
            Optional.of(c)
        );
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));

        assertThat(service.applyVtechWebhook(JSON.readTree(vtechEvent(null, "84912345678", "NO_ANSWER", "NO_ANSWER")))).isEqualTo(
            AutoCallService.VtechWebhookOutcome.APPLIED
        );
        assertThat(c.getStatus()).isEqualTo("FAILED");
        assertThat(c.getResult()).isEqualTo("not_answered");
    }

    @Test
    void vtech_webhook_laterAttemptOfSameContact_doesNotOverwriteAnswered() throws Exception {
        AutoCall c = pendingCall();
        c.setProvider("VTECH");
        c.setStatus("QUEUED");
        when(autoCallRepository.findOneByRefId(c.getRefId())).thenReturn(Optional.of(c));

        service.applyVtechWebhook(JSON.readTree(vtechEvent(c.getRefId(), "0912345678", "COMPLETED", "CONNECTED")));
        service.applyVtechWebhook(JSON.readTree(vtechEvent(c.getRefId(), "0912345678", "CANCELLED", "REJECTED")));
        service.applyVtechWebhook(JSON.readTree(vtechEvent(c.getRefId(), "0912345678", "NO_ANSWER", "NO_ANSWER")));

        assertThat(c.getStatus()).isEqualTo("COMPLETED");
        assertThat(c.getResult()).isEqualTo("answered");
        assertThat(c.getNextRetryAt()).isNull();
    }

    @Test
    void vtech_webhook_unknownCall_andOtherEvents() throws Exception {
        when(autoCallRepository.findOneByRefId("CPN-GIAO-NOPE")).thenReturn(Optional.empty());
        when(autoCallRepository.findFirstByProviderAndPhoneAndStatusOrderByCreatedAtDesc(any(), any(), any())).thenReturn(Optional.empty());
        assertThat(service.applyVtechWebhook(JSON.readTree(vtechEvent("CPN-GIAO-NOPE", "0912345678", "COMPLETED", "CONNECTED")))).isEqualTo(
            AutoCallService.VtechWebhookOutcome.NOT_FOUND
        );
        assertThat(service.applyVtechWebhook(JSON.readTree("{\"event\":\"call.started\"}"))).isEqualTo(
            AutoCallService.VtechWebhookOutcome.IGNORED
        );
    }

    @Test
    void vtech_webhook_docSamplePayload_withNullsAndBodyExtra() throws Exception {
        String ev =
            "{\"event\":\"call.completed\",\"timestamp\":\"2026-05-20T07:30:00.000Z\",\"campaign_id\":123,\"cpn_token\":\"t\"," +
            "\"call\":{\"id\":456789,\"external_call_id\":null,\"contact\":{\"id\":88888,\"phone_number\":\"0901234567\",\"name\":null," +
            "\"extra_data\":{\"ref_id\":\"CPN-TEST-DOC\",\"ten_san_pham\":\"x\"}},\"status\":\"NO_ANSWER\",\"outcome\":null," +
            "\"duration_seconds\":null,\"recording_url\":null,\"started_at\":\"2026-05-20T07:28:00.000Z\",\"answered_at\":null," +
            "\"ended_at\":\"2026-05-20T07:28:30.000Z\",\"hangup_by\":null}}";
        assertThat(service.applyVtechWebhook(JSON.readTree(ev))).isEqualTo(AutoCallService.VtechWebhookOutcome.TEST);
        JsonNode r = service.vtechTestResult("CPN-TEST-DOC");
        assertThat(r.path("status").asText()).isEqualTo("failed");
        assertThat(r.path("result").asText()).isEqualTo("not_answered");
        assertThat(r.path("callId").asText()).isEqualTo("vtech_456789");
    }

    @Test
    void vtech_webhook_testCall_keptInMemory_notInDb() throws Exception {
        assertThat(service.applyVtechWebhook(JSON.readTree(vtechEvent("CPN-TEST-1", "0912345678", "COMPLETED", "CONNECTED")))).isEqualTo(
            AutoCallService.VtechWebhookOutcome.TEST
        );
        assertThat(service.vtechTestResult("CPN-TEST-1").path("status").asText()).isEqualTo("completed");
        assertThat(service.vtechTestResult("CPN-TEST-2")).isNull();
        verify(autoCallRepository, never()).findOneByRefId(any());
    }

    @Test
    void vtech_cancel_sentCall_rejected_unsentCancelsLocally() {
        AutoCall c = stubOrderCall("QUEUED", null);
        c.setProvider("VTECH");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancel("HN260930-0001", 99L)).hasMessageContaining(
            "Vtech không hỗ trợ huỷ"
        );
        assertThat(c.getStatus()).isEqualTo("QUEUED");

        c.setStatus("ERROR");
        service.cancel("HN260930-0001", 99L);
        assertThat(c.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void vtech_sync_doesNotResendQueuedOrLookupHhvn() {
        AutoCall c = pendingCall();
        c.setStatus("QUEUED");
        c.setProvider("VTECH");
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("HN260930-0001")).thenReturn(Optional.of(c.getOrder()));
        when(integrationConfigRepository.findAll()).thenReturn(List.of(vtechConfig()));
        when(autoCallRepository.findByOrder_IdOrderByCreatedAtDesc(10L)).thenReturn(List.of(c));

        service.sync("HN260930-0001");

        verify(vtechClient, never()).importContact(any(), any(), any(), anyMap());
        verify(client, never()).getCall(any(), any(), any());
        assertThat(c.getStatus()).isEqualTo("QUEUED");
    }

    @Test
    void tokenMatches_constantTimeExact() {
        assertThat(AutoCallService.tokenMatches("abc123", "abc123")).isTrue();
        assertThat(AutoCallService.tokenMatches("abc123", " abc123 ")).isTrue();
        assertThat(AutoCallService.tokenMatches("abc123", "abc124")).isFalse();
        assertThat(AutoCallService.tokenMatches("abc123", null)).isFalse();
        assertThat(AutoCallService.tokenMatches(null, "abc123")).isFalse();
    }
}
