package com.mycompany.myapp.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import com.mycompany.myapp.service.partner.VtechAutoCallClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AutoCallConfigServiceTest {

    private static final String KEY = "xk_test_abcdefgh1234";

    @Mock
    private IntegrationConfigRepository repository;

    @Mock
    private HhvnAutoCallClient client;

    @Mock
    private VtechAutoCallClient vtechClient;

    private AutoCallConfigService service;

    @BeforeEach
    void setUp() {
        service = new AutoCallConfigService(repository, client, vtechClient);
    }

    private IntegrationConfig saved(String apiKey) {
        IntegrationConfig c = new IntegrationConfig();
        c.setAutocallApiKey(apiKey);
        return c;
    }

    @Test
    void test_withoutKey_returnsApiKeyMissing_andDoesNotCallHhvn() {
        when(repository.findAll()).thenReturn(List.of());
        Map<String, Object> out = service.test(null);
        assertThat(out.get("ok")).isEqualTo(false);
        assertThat(out.get("code")).isEqualTo("API_KEY_MISSING");
        verify(client, never()).getAudios(any(), anyString());
    }

    @Test
    void test_ok_returnsSandboxModeAndAudios() throws Exception {
        when(repository.findAll()).thenReturn(List.of(saved(KEY)));
        JsonNode body = new ObjectMapper().readTree("{\"success\":true,\"audios\":[{\"type\":\"giao\",\"source\":\"default\"}]}");
        when(client.getAudios(null, KEY)).thenReturn(new Result(true, 200, null, null, body));

        Map<String, Object> out = service.test(Map.of());
        assertThat(out.get("ok")).isEqualTo(true);
        assertThat(out.get("mode")).isEqualTo("SANDBOX");
        assertThat(out.get("audios")).isNotNull();
    }

    @Test
    void test_overrideKeyFromBody_isUsedInsteadOfSaved() {
        when(repository.findAll()).thenReturn(List.of(saved(KEY)));
        when(client.getAudios(null, "xk_live_other")).thenReturn(new Result(false, 401, "INVALID_API_KEY", "bad", null));

        Map<String, Object> out = service.test(Map.of("autocallApiKey", " xk_live_other "));
        assertThat(out.get("mode")).isEqualTo("LIVE");
        assertThat(out.get("ok")).isEqualTo(false);
        assertThat(out.get("httpStatus")).isEqualTo(401);
        assertThat((String) out.get("message")).contains("INVALID_API_KEY");
    }

    @Test
    void uploadAudio_rejectsInvalidTypeExtensionAndSize_beforeCallingHhvn() {
        MockMultipartFile mp3 = new MockMultipartFile("file", "a.mp3", "audio/mpeg", new byte[] { 1 });
        assertThatThrownBy(() -> service.uploadAudio("khac", mp3)).isInstanceOf(ResponseStatusException.class);

        MockMultipartFile txt = new MockMultipartFile("file", "a.txt", "text/plain", new byte[] { 1 });
        assertThatThrownBy(() -> service.uploadAudio("giao", txt)).isInstanceOf(ResponseStatusException.class);

        MockMultipartFile big = new MockMultipartFile(
            "file",
            "a.mp3",
            "audio/mpeg",
            new byte[(int) AutoCallConfigService.MAX_AUDIO_BYTES + 1]
        );
        assertThatThrownBy(() -> service.uploadAudio("giao", big)).isInstanceOf(ResponseStatusException.class);

        verify(client, never()).uploadAudio(any(), any(), any(), any(), any(), any());
    }

    @Test
    void uploadAudio_withoutSavedKey_isRejected() {
        when(repository.findAll()).thenReturn(List.of());
        MockMultipartFile mp3 = new MockMultipartFile("file", "a.mp3", "audio/mpeg", new byte[] { 1 });
        assertThatThrownBy(() -> service.uploadAudio("giao", mp3)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void uploadAudio_forwardsToHhvn() {
        when(repository.findAll()).thenReturn(List.of(saved(KEY)));
        MockMultipartFile mp3 = new MockMultipartFile("file", "tb.MP3", "audio/mpeg", new byte[] { 1, 2 });
        when(client.uploadAudio(eq(null), eq(KEY), eq("hoan"), eq("tb.MP3"), eq("audio/mpeg"), any())).thenReturn(
            new Result(true, 200, null, null, null)
        );
        assertThat(service.uploadAudio("HOAN", mp3).get("ok")).isEqualTo(true);
    }

    @Test
    void humanMessage_mapsKnownCodes() {
        assertThat(AutoCallConfigService.humanMessage("IP_NOT_ALLOWED", "x")).contains("whitelist");
        assertThat(AutoCallConfigService.humanMessage("AUDIO_DURATION_INVALID", "x")).contains("2 đến 60");
        assertThat(AutoCallConfigService.humanMessage("SOMETHING", "msg")).isEqualTo("msg (SOMETHING)");
        assertThat(AutoCallConfigService.humanMessage(null, "msg")).isEqualTo("msg");
    }

    @Test
    void mergeAutoCall_blankSecretsKeepSaved_andMissingEnabledKeepsValue() {
        IntegrationConfig current = saved(KEY);
        current.setAutocallWebhookSecret("whsec_old");
        current.setAutocallEnabled(true);

        IntegrationConfig incoming = new IntegrationConfig();
        incoming.setAutocallApiKey("   ");
        ConfigFacadeService.mergeAutoCall(current, incoming);

        assertThat(current.getAutocallApiKey()).isEqualTo(KEY);
        assertThat(current.getAutocallWebhookSecret()).isEqualTo("whsec_old");
        assertThat(current.getAutocallEnabled()).isTrue();

        incoming.setAutocallEnabled(false);
        incoming.setAutocallApiKey("\"xk_live_new\"");
        incoming.setAutocallBaseUrl("https://api.quanlydon.vn/partner/v1/");
        ConfigFacadeService.mergeAutoCall(current, incoming);
        assertThat(current.getAutocallEnabled()).isFalse();
        assertThat(current.getAutocallApiKey()).isEqualTo("xk_live_new");
        assertThat(current.getAutocallBaseUrl()).isEqualTo("https://api.quanlydon.vn/partner/v1");
    }

    @Test
    void json_neverExposesKeyOrSecret() throws Exception {
        IntegrationConfig c = saved(KEY);
        c.setAutocallWebhookSecret("whsec_secret");
        String json = new ObjectMapper().writeValueAsString(c);
        assertThat(json).doesNotContain(KEY).doesNotContain("whsec_secret");
        JsonNode node = new ObjectMapper().readTree(json);
        assertThat(node.get("autocallApiKeyConfigured").asBoolean()).isTrue();
        assertThat(node.get("autocallApiKeyMode").asText()).isEqualTo("SANDBOX");
        assertThat(node.get("autocallApiKeySuffix").asText()).isEqualTo("1234");
        assertThat(node.get("autocallWebhookSecretConfigured").asBoolean()).isTrue();

        IntegrationConfig in = new ObjectMapper().readValue("{\"autocallApiKey\":\"xk_test_z\"}", IntegrationConfig.class);
        assertThat(in.getAutocallApiKey()).isEqualTo("xk_test_z");
        assertThat(in.getAutocallEnabled()).isNull();
    }

    // ---- Vtech ----

    @Test
    void mergeAutoCall_vtech_switchKeepsBothKeys_generatesTokenOnce() {
        IntegrationConfig current = saved(KEY);
        IntegrationConfig incoming = new IntegrationConfig();
        incoming.setAutocallProvider("vtech");
        incoming.setAutocallVtechApiKey(" tdai_abc123 ");
        incoming.setAutocallVtechBaseUrl("https://api.tongdai.ai/api/external/v1/");
        ConfigFacadeService.mergeAutoCall(current, incoming);

        assertThat(current.getAutocallProvider()).isEqualTo("VTECH");
        assertThat(current.getAutocallVtechApiKey()).isEqualTo("tdai_abc123");
        assertThat(current.getAutocallApiKey()).isEqualTo(KEY);
        assertThat(current.getAutocallVtechBaseUrl()).isEqualTo("https://api.tongdai.ai/api/external/v1");
        String token = current.getAutocallVtechWebhookToken();
        assertThat(token).hasSize(32).matches("[0-9a-f]+");
        assertThat(current.isAutocallSandbox()).isFalse();
        assertThat(current.getAutocallActiveApiKey()).isEqualTo("tdai_abc123");

        IntegrationConfig back = new IntegrationConfig();
        back.setAutocallProvider("HHVN");
        ConfigFacadeService.mergeAutoCall(current, back);
        assertThat(current.getAutocallProvider()).isEqualTo("HHVN");
        assertThat(current.getAutocallVtechApiKey()).isEqualTo("tdai_abc123");
        assertThat(current.getAutocallVtechWebhookToken()).isEqualTo(token);
        assertThat(current.getAutocallActiveApiKey()).isEqualTo(KEY);
        assertThat(current.isAutocallSandbox()).isTrue();
    }

    @Test
    void mergeAutoCall_invalidProvider_rejected() {
        IntegrationConfig incoming = new IntegrationConfig();
        incoming.setAutocallProvider("OTHER");
        assertThatThrownBy(() -> ConfigFacadeService.mergeAutoCall(saved(KEY), incoming)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void json_vtech_hidesKey_showsSuffixAndToken() throws Exception {
        IntegrationConfig c = saved(KEY);
        c.setAutocallProvider("VTECH");
        c.setAutocallVtechApiKey("tdai_secret_9876");
        c.setAutocallVtechWebhookToken("tok123");
        JsonNode node = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(c));
        assertThat(node.toString()).doesNotContain("tdai_secret_9876");
        assertThat(node.get("autocallProvider").asText()).isEqualTo("VTECH");
        assertThat(node.get("autocallVtechApiKeyConfigured").asBoolean()).isTrue();
        assertThat(node.get("autocallVtechApiKeySuffix").asText()).isEqualTo("9876");
        assertThat(node.get("autocallVtechWebhookToken").asText()).isEqualTo("tok123");

        IntegrationConfig in = new ObjectMapper()
            .readValue("{\"autocallVtechApiKey\":\"k\",\"autocallVtechWebhookToken\":\"hacked\"}", IntegrationConfig.class);
        assertThat(in.getAutocallVtechApiKey()).isEqualTo("k");
        assertThat(in.getAutocallVtechWebhookToken()).isNull();
    }

    @Test
    void test_vtech_badRequestMeansKeyOk_unauthorizedMeansBadKey() {
        IntegrationConfig c = saved(KEY);
        c.setAutocallProvider("VTECH");
        c.setAutocallVtechApiKey("tdai_good");
        when(repository.findAll()).thenReturn(List.of(c));
        when(vtechClient.testConnection(null, "tdai_good")).thenReturn(new Result(true, 400, null, null, null));
        Map<String, Object> ok = service.test(Map.of());
        assertThat(ok.get("ok")).isEqualTo(true);
        assertThat(ok.get("provider")).isEqualTo("VTECH");

        when(vtechClient.testConnection(null, "tdai_bad")).thenReturn(
            new Result(false, 401, "INVALID_API_KEY", "API key không hợp lệ", null)
        );
        Map<String, Object> bad = service.test(Map.of("autocallVtechApiKey", "tdai_bad"));
        assertThat(bad.get("ok")).isEqualTo(false);
        assertThat((String) bad.get("message")).isEqualTo("API key Vtech không hợp lệ");
        verify(client, never()).getAudios(any(), anyString());
    }

    @Test
    void test_providerOverride_testsVtechBeforeSaving() {
        when(repository.findAll()).thenReturn(List.of(saved(KEY)));
        Map<String, Object> out = service.test(Map.of("autocallProvider", "VTECH"));
        assertThat(out.get("code")).isEqualTo("API_KEY_MISSING");
        verify(client, never()).getAudios(any(), anyString());
    }
}
