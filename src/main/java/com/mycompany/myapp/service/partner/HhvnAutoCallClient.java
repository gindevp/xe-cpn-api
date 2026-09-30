package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HHVN Tech Auto Call partner API (header {@code X-API-Key}).
 * Lỗi HTTP phía HHVN trả về {@link Result} thay vì ném — FE không được nhận 401 của đối tác như 401 phiên đăng nhập.
 */
@Component
public class HhvnAutoCallClient {

    public static final String DEFAULT_BASE_URL = "https://api.quanlydon.vn/partner/v1";

    private static final Logger LOG = LoggerFactory.getLogger(HhvnAutoCallClient.class);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HhvnAutoCallClient(ObjectMapper objectMapper, @Value("${cpn.hhvn.insecure-ssl:false}") boolean insecureSsl) {
        this.objectMapper = objectMapper;
        this.httpClient = PartnerHttpClients.build(Duration.ofSeconds(15), insecureSsl);
    }

    public Result getAudios(String baseUrl, String apiKey) {
        return send(baseUrl, apiKey, "GET", "/audios", HttpRequest.BodyPublishers.noBody(), null);
    }

    public Result uploadAudio(String baseUrl, String apiKey, String type, String fileName, String contentType, byte[] content) {
        String boundary = "----cpn" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = multipartFile(boundary, fileName, contentType, content);
        return send(
            baseUrl,
            apiKey,
            "PUT",
            "/audios/" + type,
            HttpRequest.BodyPublishers.ofByteArray(body),
            "multipart/form-data; boundary=" + boundary
        );
    }

    public Result deleteAudio(String baseUrl, String apiKey, String type) {
        return send(baseUrl, apiKey, "DELETE", "/audios/" + type, HttpRequest.BodyPublishers.noBody(), null);
    }

    public FileResult downloadAudio(String baseUrl, String apiKey, String type) {
        String url = normalizeBaseUrl(baseUrl) + "/audios/" + type + "/file";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("X-API-Key", apiKey)
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("Content-Type").orElse("audio/wav");
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                Result err = toResult(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
                return new FileResult(err, null, null);
            }
            return new FileResult(new Result(true, response.statusCode(), null, null, null), response.body(), contentType);
        } catch (Exception e) {
            LOG.warn("HHVN GET {} failed: {}", url, e.getMessage());
            return new FileResult(networkError(e), null, null);
        }
    }

    public static String normalizeBaseUrl(String baseUrl) {
        String b = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    private Result send(String baseUrl, String apiKey, String method, String path, HttpRequest.BodyPublisher body, String contentType) {
        String url = normalizeBaseUrl(baseUrl) + path;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("X-API-Key", apiKey)
                .header("Accept", "application/json")
                .method(method, body);
            if (contentType != null) {
                builder.header("Content-Type", contentType);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LOG.info("HHVN {} {} -> HTTP {}", method, url, response.statusCode());
            return toResult(response.statusCode(), response.body());
        } catch (Exception e) {
            LOG.warn("HHVN {} {} failed: {}", method, url, e.getMessage());
            return networkError(e);
        }
    }

    private Result toResult(int status, String rawBody) {
        JsonNode json = null;
        try {
            if (rawBody != null && !rawBody.isBlank()) {
                json = objectMapper.readTree(rawBody);
            }
        } catch (Exception ignored) {
            // body không phải JSON
        }
        boolean ok = status >= 200 && status < 300 && (json == null || !json.has("success") || json.get("success").asBoolean());
        if (ok) {
            return new Result(true, status, null, null, json);
        }
        String code = json != null && json.hasNonNull("code") ? json.get("code").asText() : null;
        String message = json != null && json.hasNonNull("message") ? json.get("message").asText() : truncate(rawBody);
        return new Result(false, status, code, message, json);
    }

    private static Result networkError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return new Result(false, 0, "NETWORK_ERROR", "Không kết nối được HHVN: " + msg, null);
    }

    private static byte[] multipartFile(String boundary, String fileName, String contentType, byte[] content) {
        String safeName = fileName == null || fileName.isBlank() ? "audio" : fileName.replace("\"", "");
        String ct = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        ByteArrayOutputStream out = new ByteArrayOutputStream(content.length + 512);
        String head =
            "--" +
            boundary +
            "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"" +
            safeName +
            "\"\r\n" +
            "Content-Type: " +
            ct +
            "\r\n\r\n";
        out.writeBytes(head.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(content);
        out.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 300 ? s : s.substring(0, 300) + "…";
    }

    /** {@code httpStatus = 0} khi lỗi mạng (không tới được HHVN). */
    public record Result(boolean ok, int httpStatus, String code, String message, JsonNode body) {}

    public record FileResult(Result result, byte[] content, String contentType) {}
}
