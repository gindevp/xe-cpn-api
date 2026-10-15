package com.mycompany.myapp.service.payment.onepay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.OnePayConfig;
import com.mycompany.myapp.service.partner.PartnerHttpClients;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Gọi API PayCollect, ký OWS1 trên x-op-date + x-op-expires. OnePay báo lỗi nghiệp vụ bằng {@code errorCode} trong body
 * dù HTTP 200, nên kết quả luôn xét cả hai.
 */
@Component
public class OnePayClient {

    public static final String API_PREFIX = "/paycollect/api/v1";
    static final String SANDBOX_BASE = "https://mtf.onepay.vn";
    static final String PRODUCTION_BASE = "https://onepay.vn";
    private static final String EXPIRES = "3600";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final ObjectMapper objectMapper;
    private final OnePayCallLogService callLogService;
    private volatile HttpClient strict;
    private volatile HttpClient insecure;

    public OnePayClient(ObjectMapper objectMapper, OnePayCallLogService callLogService) {
        this.objectMapper = objectMapper;
        this.callLogService = callLogService;
    }

    /** ok = HTTP 2xx, không có errorCode, mọi khối batch đều 2xx. */
    public record Result(boolean ok, Integer httpStatus, JsonNode json, String raw, String error) {}

    public static String baseUrl(OnePayConfig cfg) {
        return OnePayConfig.ENV_PRODUCTION.equals(cfg.getEnvironment()) ? PRODUCTION_BASE : SANDBOX_BASE;
    }

    public Result call(OnePayConfig cfg, String action, String reference, String method, String path, String body) {
        long started = System.currentTimeMillis();
        String uri = API_PREFIX + path;
        byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        Result r;
        try {
            String ts = OnePaySigner.timestamp(Instant.now());
            String auth = OnePaySigner.authorization(
                cfg.getPartnerId().trim(),
                cfg.getPartnerKey().trim(),
                method,
                uri,
                Map.of(),
                Map.of(OnePaySigner.HDR_DATE, ts, OnePaySigner.HDR_EXPIRES, EXPIRES),
                payload,
                ts
            );
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl(cfg) + uri))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header(OnePaySigner.HDR_DATE, ts)
                .header(OnePaySigner.HDR_EXPIRES, EXPIRES)
                .header(OnePaySigner.HDR_AUTHORIZATION, auth)
                .method(method, HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
            HttpResponse<String> res = http(Boolean.TRUE.equals(cfg.getInsecureSsl())).send(req, HttpResponse.BodyHandlers.ofString());
            r = interpret(res.statusCode(), res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            r = new Result(false, null, null, null, "Bị ngắt khi gọi OnePay");
        } catch (Exception e) {
            r = new Result(false, null, null, null, "Không gọi được OnePay: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
        callLogService.record(
            new OnePayCallLogService.Entry(
                "OUT",
                action,
                r.httpStatus(),
                r.ok() ? "OK" : "ERROR",
                r.error(),
                reference,
                null,
                null,
                null,
                System.currentTimeMillis() - started,
                method + " " + uri + (body == null ? "" : "\n" + body),
                r.raw()
            )
        );
        return r;
    }

    Result interpret(int status, String raw) {
        JsonNode json = null;
        try {
            json = raw == null || raw.isBlank() ? null : objectMapper.readTree(raw);
        } catch (Exception ignored) {
            // body không phải JSON: giữ raw để xem nhật ký
        }
        String error = errorOf(json);
        if (error == null && json != null && json.isArray()) {
            for (JsonNode block : json) {
                int s = block.path("status").asInt(0);
                String inner = errorOf(block.path("body"));
                if (s < 200 || s >= 300 || inner != null) {
                    error = inner != null ? inner : "Khối batch trả status " + block.path("status").asText();
                    break;
                }
            }
        }
        if (error == null && (status < 200 || status >= 300)) {
            error = "HTTP " + status;
        }
        return new Result(error == null, status, json, raw, error);
    }

    private static String errorOf(JsonNode node) {
        if (node == null || !node.isObject() || !node.hasNonNull("errorCode") || "0".equals(node.get("errorCode").asText())) {
            return null;
        }
        return "Mã lỗi OnePay " + node.get("errorCode").asText() + ": " + node.path("message").asText("");
    }

    private HttpClient http(boolean insecureSsl) {
        if (insecureSsl) {
            if (insecure == null) {
                insecure = PartnerHttpClients.build(Duration.ofSeconds(10), true);
            }
            return insecure;
        }
        if (strict == null) {
            strict = PartnerHttpClients.build(Duration.ofSeconds(10), false);
        }
        return strict;
    }
}
