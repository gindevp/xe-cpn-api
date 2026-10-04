package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import org.junit.jupiter.api.Test;

class VtechAutoCallClientTest {

    private final VtechAutoCallClient client = new VtechAutoCallClient(new ObjectMapper(), false);

    @Test
    void toResult_created_isOk_withData() {
        Result r = client.toResult(201, "{\"data\":{\"total\":1,\"imported\":1,\"skipped\":0,\"errors\":[]}}");
        assertThat(r.ok()).isTrue();
        assertThat(r.body().path("data").path("imported").asInt()).isEqualTo(1);
    }

    @Test
    void toResult_unauthorized_isInvalidApiKey_withVtechMessage() {
        Result r = client.toResult(401, "{\"error\":{\"code\":\"INTERNAL_ERROR\",\"message\":\"API key không hợp lệ\"}}");
        assertThat(r.ok()).isFalse();
        assertThat(r.httpStatus()).isEqualTo(401);
        assertThat(r.code()).isEqualTo("INVALID_API_KEY");
        assertThat(r.message()).isEqualTo("API key không hợp lệ");
    }

    @Test
    void toResult_validationAndNonJson() {
        Result v = client.toResult(400, "{\"error\":{\"code\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\"}}");
        assertThat(v.ok()).isFalse();
        assertThat(v.code()).isEqualTo("VALIDATION_ERROR");
        Result html = client.toResult(502, "<html>Bad gateway</html>");
        assertThat(html.code()).isEqualTo("HTTP_502");
        assertThat(html.message()).contains("Bad gateway");
    }

    @Test
    void normalizeBaseUrl_defaultsAndTrimsSlash() {
        assertThat(VtechAutoCallClient.normalizeBaseUrl(null)).isEqualTo("https://api.tongdai.ai/api/external/v1");
        assertThat(VtechAutoCallClient.normalizeBaseUrl(" https://x.ai/v1/ ")).isEqualTo("https://x.ai/v1");
    }

    @Test
    void normalizeBaseUrl_stripsPastedImportPath() {
        assertThat(VtechAutoCallClient.normalizeBaseUrl("https://api.tongdai.ai/api/external/v1/contacts/import")).isEqualTo(
            "https://api.tongdai.ai/api/external/v1"
        );
        assertThat(VtechAutoCallClient.normalizeBaseUrl("https://api.tongdai.ai/api/external/v1/contacts/import/")).isEqualTo(
            "https://api.tongdai.ai/api/external/v1"
        );
    }

    @Test
    void isThrottled_detectsVtechThrottlerPayload() {
        Result throttled = client.toResult(
            500,
            "{\"error\":{\"code\":\"INTERNAL_ERROR\",\"message\":\"ThrottlerException: Too Many Requests\"}}"
        );
        assertThat(VtechAutoCallClient.isThrottled(throttled)).isTrue();
        assertThat(VtechAutoCallClient.isThrottled(client.toResult(429, ""))).isTrue();
        assertThat(VtechAutoCallClient.isThrottled(client.toResult(401, "{\"error\":{\"message\":\"API key không hợp lệ\"}}"))).isFalse();
        assertThat(VtechAutoCallClient.isThrottled(client.toResult(201, "{}"))).isFalse();
    }

    @Test
    void toResult_notFound_explainsBaseUrl() {
        Result r = client.toResult(404, "");
        assertThat(r.code()).isEqualTo("HTTP_404");
        assertThat(r.message()).contains("Base URL").contains("https://api.tongdai.ai/api/external/v1");
    }
}
