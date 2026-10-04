package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Auto Call Vtech (tongdai.ai): import contact vào chiến dịch callbot (header {@code X-Api-Key}, key theo chiến dịch).
 * Vtech không trả mã cuộc gọi khi import — kết quả chỉ về qua webhook {@code call.completed}, khớp bằng extra_data.ref_id.
 */
@Component
public class VtechAutoCallClient {

    public static final String DEFAULT_BASE_URL = "https://api.tongdai.ai/api/external/v1";
    private static final String IMPORT_PATH = "/contacts/import";

    private static final Logger LOG = LoggerFactory.getLogger(VtechAutoCallClient.class);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VtechAutoCallClient(ObjectMapper objectMapper, @Value("${cpn.vtech.insecure-ssl:false}") boolean insecureSsl) {
        this.objectMapper = objectMapper;
        this.httpClient = PartnerHttpClients.build(Duration.ofSeconds(15), insecureSsl);
    }

    /**
     * Import 1 số vào chiến dịch = yêu cầu callbot gọi. {@code skip_duplicates = false}: gọi lại cùng số vẫn được gọi.
     * Thành công: HTTP 201 {@code {data:{total, imported, skipped, errors:[{row, phone_number, error}]}}}.
     */
    public Result importContact(String baseUrl, String apiKey, String phone, String name, Map<String, String> extraData) {
        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("phone_number", phone);
        if (name != null && !name.isBlank()) {
            contact.put("name", name.trim());
        }
        if (extraData != null && !extraData.isEmpty()) {
            contact.put("extra_data", extraData);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contacts", List.of(contact));
        body.put("skip_duplicates", false);
        body.put("normalize_phone", false);
        return post(baseUrl, apiKey, body);
    }

    /**
     * Kiểm tra key mà không gọi ai: import danh sách rỗng. Key đúng → 400 (validation), key sai → 401.
     */
    public Result testConnection(String baseUrl, String apiKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contacts", List.of());
        Result r = post(baseUrl, apiKey, body);
        if (r.ok() || r.httpStatus() == 400 || r.httpStatus() == 422) {
            return new Result(true, r.httpStatus(), null, null, r.body());
        }
        return r;
    }

    public static String normalizeBaseUrl(String baseUrl) {
        String b = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        if (b.toLowerCase(java.util.Locale.ROOT).endsWith(IMPORT_PATH)) {
            b = b.substring(0, b.length() - IMPORT_PATH.length());
        }
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b.isEmpty() ? DEFAULT_BASE_URL : b;
    }

    private Result post(String baseUrl, String apiKey, Map<String, Object> body) {
        String url = normalizeBaseUrl(baseUrl) + "/contacts/import";
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return new Result(false, 0, "SERIALIZE_ERROR", e.getMessage(), null);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("X-Api-Key", apiKey)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LOG.info("Vtech POST {} -> HTTP {}", url, response.statusCode());
            return toResult(response.statusCode(), response.body());
        } catch (Exception e) {
            LOG.warn("Vtech POST {} failed: {}", url, e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new Result(false, 0, "NETWORK_ERROR", "Không kết nối được Vtech: " + msg, null);
        }
    }

    Result toResult(int status, String rawBody) {
        JsonNode json = null;
        try {
            if (rawBody != null && !rawBody.isBlank()) {
                json = objectMapper.readTree(rawBody);
            }
        } catch (Exception ignored) {
            // body không phải JSON
        }
        if (status >= 200 && status < 300) {
            return new Result(true, status, null, null, json);
        }
        JsonNode err = json != null && json.has("error") && json.get("error").isObject() ? json.get("error") : json;
        String message = err != null && err.hasNonNull("message") ? err.get("message").asText() : truncate(rawBody);
        if (status == 404 && (message == null || message.isBlank())) {
            message = "Sai Base URL Vtech (HTTP 404) — dùng " + DEFAULT_BASE_URL;
        }
        String code = status == 401 || status == 403
            ? "INVALID_API_KEY"
            : err != null && err.hasNonNull("code") ? err.get("code").asText() : "HTTP_" + status;
        return new Result(false, status, code, message, json);
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 300 ? s : s.substring(0, 300) + "…";
    }
}
