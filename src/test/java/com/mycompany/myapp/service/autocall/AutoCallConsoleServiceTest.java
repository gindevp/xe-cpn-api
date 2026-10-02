package com.mycompany.myapp.service.autocall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.AutoCallRepository;
import com.mycompany.myapp.repository.AutoCallRepository.RefOrderCode;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.autocall.AutoCallService.CancelOutcome;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutoCallConsoleServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private IntegrationConfigRepository integrationConfigRepository;

    @Mock
    private AutoCallRepository autoCallRepository;

    @Mock
    private AutoCallService autoCallService;

    @Mock
    private HhvnAutoCallClient client;

    @Mock
    private StaffAccessService staffAccessService;

    @InjectMocks
    private AutoCallConsoleService service;

    private void stubKey(String key) {
        IntegrationConfig c = new IntegrationConfig();
        c.setAutocallApiKey(key);
        when(integrationConfigRepository.findAll()).thenReturn(List.of(c));
    }

    private static RefOrderCode ref(String refId, String orderCode) {
        return new RefOrderCode() {
            public String getRefId() {
                return refId;
            }

            public String getOrderCode() {
                return orderCode;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    void listCalls_buildsVnDayRange_andAddsOrderCodes() throws Exception {
        stubKey("xk_test_1234");
        JsonNode body = JSON.readTree(
            "{\"success\":true,\"data\":[" +
            "{\"callId\":\"call_a\",\"refId\":\"CPN-GIAO-A-1-x\"}," +
            "{\"callId\":\"call_b\",\"refId\":\"OTHER\",\"metadata\":{\"orderCode\":\"B\"}}," +
            "{\"callId\":\"call_c\",\"refId\":\"CPN-TEST-1\"}]," +
            "\"pagination\":{\"page\":1,\"limit\":50,\"total\":3,\"totalPages\":1}}"
        );
        when(client.listCalls(any(), eq("xk_test_1234"), anyMap())).thenReturn(new Result(true, 200, null, null, body));
        when(autoCallRepository.findOrderCodesByRefIds(any())).thenReturn(List.of(ref("CPN-GIAO-A-1-x", "A")));

        Map<String, Object> out = service.listCalls("2026-09-01", "2026-09-30", "GIAO", "", null, null, 1, 500);

        ArgumentCaptor<Map<String, String>> q = ArgumentCaptor.forClass(Map.class);
        verify(client).listCalls(any(), any(), q.capture());
        assertThat(q.getValue())
            .containsEntry("from", "2026-09-01T00:00:00+07:00")
            .containsEntry("to", "2026-09-30T23:59:59+07:00")
            .containsEntry("type", "giao")
            .containsEntry("page", "1")
            .containsEntry("limit", "200");
        assertThat(q.getValue().get("status")).isNull();
        List<JsonNode> data = (List<JsonNode>) out.get("data");
        assertThat(data).extracting(n -> n.path("orderCode").asText(null)).containsExactly("A", "B", null);
        assertThat(out.get("ok")).isEqualTo(true);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listCalls_fetchesAllHhvnPages_sortsNewestFirst_thenPaginates() throws Exception {
        stubKey("xk_test_1234");
        StringBuilder page1 = new StringBuilder("{\"success\":true,\"data\":[");
        for (int i = 0; i < 200; i++) {
            if (i > 0) page1.append(',');
            page1.append("{\"callId\":\"old_").append(i).append("\",\"createdAt\":\"2026-09-01T08:00:00+07:00\"}");
        }
        page1.append("],\"pagination\":{\"page\":1,\"limit\":200,\"total\":202,\"totalPages\":2}}");
        JsonNode p1 = JSON.readTree(page1.toString());
        JsonNode p2 = JSON.readTree(
            "{\"success\":true,\"data\":[" +
            "{\"callId\":\"mid\",\"createdAt\":\"2026-09-20T01:00:00Z\"}," +
            "{\"callId\":\"newest\",\"createdAt\":\"2026-09-30T10:00:00+07:00\"}]," +
            "\"pagination\":{\"page\":2,\"limit\":200,\"total\":202,\"totalPages\":2}}"
        );
        when(client.listCalls(any(), any(), anyMap())).thenAnswer(inv -> {
            Map<String, String> q = inv.getArgument(2);
            return new Result(true, 200, null, null, "1".equals(q.get("page")) ? p1 : p2);
        });

        Map<String, Object> first = service.listCalls("2026-09-01", "2026-09-30", null, null, null, null, 1, 20);
        List<JsonNode> data = (List<JsonNode>) first.get("data");
        assertThat(data).hasSize(20);
        assertThat(data.get(0).path("callId").asText()).isEqualTo("newest");
        assertThat(data.get(1).path("callId").asText()).isEqualTo("mid");
        assertThat(((JsonNode) first.get("pagination")).path("total").asInt()).isEqualTo(202);
        assertThat(((JsonNode) first.get("pagination")).path("totalPages").asInt()).isEqualTo(11);

        Map<String, Object> last = service.listCalls("2026-09-01", "2026-09-30", null, null, null, null, 11, 20);
        assertThat((List<JsonNode>) last.get("data")).hasSize(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listCalls_filtersByPhone_partialAnd84Prefix() throws Exception {
        stubKey("xk_test_1234");
        JsonNode body = JSON.readTree(
            "{\"success\":true,\"data\":[" +
            "{\"callId\":\"a\",\"phone\":\"0912345678\"}," +
            "{\"callId\":\"b\",\"phone\":\"0987654321\"}," +
            "{\"callId\":\"c\",\"phone\":\"0912000678\"}]," +
            "\"pagination\":{\"page\":1,\"limit\":200,\"total\":3,\"totalPages\":1}}"
        );
        when(client.listCalls(any(), any(), anyMap())).thenReturn(new Result(true, 200, null, null, body));

        Map<String, Object> full = service.listCalls(null, null, null, null, null, "+84 912 345 678", 1, 20);
        assertThat((List<JsonNode>) full.get("data")).extracting(n -> n.path("callId").asText()).containsExactly("a");
        assertThat(((JsonNode) full.get("pagination")).path("total").asInt()).isEqualTo(1);

        Map<String, Object> tail = service.listCalls(null, null, null, null, null, "678", 1, 20);
        assertThat((List<JsonNode>) tail.get("data")).extracting(n -> n.path("callId").asText()).containsExactlyInAnyOrder("a", "c");

        verify(client, never()).listCalls(any(), any(), argThat(q -> q.containsKey("phone")));
    }

    @SuppressWarnings("unchecked")
    @Test
    void listCalls_filtersByResult() throws Exception {
        stubKey("xk_test_1234");
        JsonNode body = JSON.readTree(
            "{\"success\":true,\"data\":[" +
            "{\"callId\":\"a\",\"result\":\"answered\"}," +
            "{\"callId\":\"b\",\"result\":\"not_answered\"}," +
            "{\"callId\":\"c\",\"result\":\"error\"}," +
            "{\"callId\":\"d\",\"status\":\"queued\"}]," +
            "\"pagination\":{\"page\":1,\"limit\":200,\"total\":4,\"totalPages\":1}}"
        );
        when(client.listCalls(any(), any(), anyMap())).thenReturn(new Result(true, 200, null, null, body));

        Map<String, Object> err = service.listCalls(null, null, null, null, "ERROR", null, 1, 20);
        assertThat((List<JsonNode>) err.get("data")).extracting(n -> n.path("callId").asText()).containsExactly("c");
        Map<String, Object> no = service.listCalls(null, null, null, null, "not_answered", null, 1, 20);
        assertThat((List<JsonNode>) no.get("data")).extracting(n -> n.path("callId").asText()).containsExactly("b");

        assertThatThrownBy(() -> service.listCalls(null, null, null, null, "busy", null, 1, 20)).hasMessageContaining("Kết quả");
    }

    @Test
    void listCalls_rejectsRangeOver31DaysAndReversedDates() {
        assertThatThrownBy(() -> service.listCalls("2026-08-30", "2026-09-30", null, null, null, null, null, null)).hasMessageContaining(
            "31"
        );
        assertThatThrownBy(() -> service.listCalls("2026-09-30", "2026-09-01", null, null, null, null, null, null)).hasMessageContaining(
            "trước"
        );
        assertThatThrownBy(() -> service.listCalls("2026-09-01", "2026-09-30", null, "done", null, null, null, null)).hasMessageContaining(
            "Trạng thái"
        );
        assertThatThrownBy(() -> service.listCalls(null, null, null, null, null, "09", null, null)).hasMessageContaining("3 số");
        verify(client, never()).listCalls(any(), any(), anyMap());
    }

    @Test
    void listCalls_hhvnError_returnsVietnameseMessage() {
        stubKey("xk_test_1234");
        when(client.listCalls(any(), any(), anyMap())).thenReturn(new Result(false, 403, "IP_NOT_ALLOWED", "ip", null));
        Map<String, Object> out = service.listCalls(null, null, null, null, null, null, null, null);
        assertThat(out.get("ok")).isEqualTo(false);
        assertThat((String) out.get("message")).contains("whitelist");
    }

    @Test
    void getCall_routesCallIdAndRefId() throws Exception {
        stubKey("xk_test_1234");
        JsonNode call = JSON.readTree("{\"callId\":\"call_a\",\"refId\":\"R1\"}");
        when(client.getCall(any(), any(), eq("call_a"))).thenReturn(new Result(true, 200, null, null, call));
        when(client.getCallByRefId(any(), any(), eq("R1"))).thenReturn(
            new Result(true, 200, null, null, JSON.readTree("{\"success\":true,\"data\":[{\"callId\":\"call_a\",\"refId\":\"R1\"}]}"))
        );

        assertThat(((JsonNode) service.getCall("call_a").get("call")).path("refId").asText()).isEqualTo("R1");
        assertThat(((JsonNode) service.getCall(" R1 ").get("call")).path("callId").asText()).isEqualTo("call_a");
    }

    @Test
    void testCall_sandbox_sendsWithTestRefIdAndMetadata() throws Exception {
        stubKey("xk_test_1234");
        JsonNode body = JSON.readTree("{\"success\":true,\"accepted\":[{\"callId\":\"call_t\",\"status\":\"queued\"}],\"rejected\":[]}");
        when(client.createCall(any(), any(), eq("giao"), eq("0912345671"), anyString(), anyMap())).thenReturn(
            new Result(true, 202, null, null, body)
        );

        Map<String, Object> out = service.testCall("+84 912 345 671", null, false);

        assertThat(out).containsEntry("ok", true).containsEntry("callId", "call_t").containsEntry("sandbox", true);
        assertThat((String) out.get("refId")).matches("CPN-TEST-\\d{12}-[a-z0-9]+");
    }

    @Test
    void testCall_liveKey_requiresConfirm() {
        stubKey("xk_live_1234");
        assertThatThrownBy(() -> service.testCall("0912345671", "giao", false)).hasMessageContaining("LIVE");
        verify(client, never()).createCall(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCall_invalidPhoneOrType_rejectedBeforeHhvn() {
        assertThatThrownBy(() -> service.testCall("12345", "giao", false)).hasMessageContaining("SĐT");
        assertThatThrownBy(() -> service.testCall("0912345671", "xyz", false)).hasMessageContaining("type");
        verify(client, never()).createCall(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCall_rejectedByHhvn_returnsCode() throws Exception {
        stubKey("xk_test_1234");
        JsonNode body = JSON.readTree(
            "{\"success\":true,\"accepted\":[],\"rejected\":[{\"code\":\"INVALID_PHONE\",\"message\":\"Số không hợp lệ\"}]}"
        );
        when(client.createCall(any(), any(), any(), any(), any(), any())).thenReturn(new Result(true, 202, null, null, body));
        Map<String, Object> out = service.testCall("0912345671", "hoan", false);
        assertThat(out).containsEntry("ok", false).containsEntry("code", "INVALID_PHONE");
    }

    @Test
    void cancel_ok_returnsCancelledCall() throws Exception {
        JsonNode call = JSON.readTree("{\"callId\":\"call_a\",\"status\":\"cancelled\"}");
        when(autoCallService.cancelAtHhvn("call_a")).thenReturn(new CancelOutcome(true, null, null, call));

        Map<String, Object> out = service.cancel("call_a");

        assertThat(out.get("ok")).isEqualTo(true);
        assertThat(((JsonNode) out.get("call")).path("status").asText()).isEqualTo("cancelled");
    }

    @Test
    void cancel_notCancellable_returnsMessageAndRealStatus() throws Exception {
        JsonNode call = JSON.readTree("{\"callId\":\"call_a\",\"status\":\"failed\"}");
        when(autoCallService.cancelAtHhvn("call_a")).thenReturn(new CancelOutcome(false, "NOT_CANCELLABLE", "Không huỷ được", call));

        Map<String, Object> out = service.cancel("call_a");

        assertThat(out).containsEntry("ok", false).containsEntry("code", "NOT_CANCELLABLE");
        assertThat(((JsonNode) out.get("call")).path("status").asText()).isEqualTo("failed");
    }
}
