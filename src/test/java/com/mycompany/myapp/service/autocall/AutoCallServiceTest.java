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
}
