package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ahamove Partner API — đổi api_key + mobile thành Bearer token.
 * POST {@code /accounts/token} với {@code Content-Type: application/json}.
 */
@Component
public class AhamoveAuthClient {

    private static final Logger LOG = LoggerFactory.getLogger(AhamoveAuthClient.class);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final HttpClient httpClient;

    public AhamoveAuthClient(
        ObjectMapper objectMapper,
        @Value("${cpn.ahamove.base-url:https://partner-api.ahamove.com/v3}") String baseUrl,
        @Value("${cpn.ahamove.insecure-ssl:false}") boolean insecureSsl
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = PartnerHttpClients.build(Duration.ofSeconds(15), insecureSsl);
        LOG.info("Ahamove AuthClient baseUrl={} insecureSsl={}", this.baseUrl, insecureSsl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * POST /accounts/token → Bearer JWT (JSON body: mobile + api_key).
     */
    public String fetchAccessToken(String apiKey, String mobile) {
        String key = sanitizeApiKey(apiKey);
        if (key == null) {
            throw new AhamoveAuthException("Ahamove api_key trống");
        }
        List<String> candidates = mobileCandidates(mobile);
        if (candidates.isEmpty()) {
            throw new AhamoveAuthException(
                "Ahamove mobile không hợp lệ — nhập SĐT VN dạng 0xxxxxxxxx hoặc 84xxxxxxxxx (9 số sau mã quốc gia)"
            );
        }
        AhamoveAuthException last = null;
        for (int i = 0; i < candidates.size(); i++) {
            String mob = candidates.get(i);
            try {
                return fetchAccessTokenOnce(key, mob);
            } catch (AhamoveAuthException e) {
                last = e;
                // Chỉ xoay format khi Ahamove bảo sai SĐT; lỗi khác (partner/auth) dừng ngay.
                if (!messageHas(e, "INVALID_PHONE_NUMBER") || i == candidates.size() - 1) {
                    throw e;
                }
                LOG.warn("Ahamove INVALID_PHONE_NUMBER với mobile={} — thử dạng khác", mob);
            }
        }
        throw last != null ? last : new AhamoveAuthException("Ahamove lấy token thất bại");
    }

    private String fetchAccessTokenOnce(String key, String mob) {
        try {
            String json = objectMapper.createObjectNode().put("mobile", mob).put("api_key", key).toString();
            String url = baseUrl + "/accounts/token";
            LOG.info(
                "Ahamove POST {} mobile={} apiKeyLen={} apiKeySuffix=…{}",
                url,
                mob,
                key.length(),
                key.length() <= 4 ? "****" : key.substring(key.length() - 4)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn("Ahamove token HTTP {}: {}", response.statusCode(), truncate(response.body()));
                throw new AhamoveAuthException(formatTokenHttpError(response.statusCode(), response.body(), mob, key));
            }
            JsonNode root = objectMapper.readTree(response.body());
            String token = text(root, "token");
            if (token == null || token.isBlank()) {
                throw new AhamoveAuthException("Ahamove response thiếu token");
            }
            return token.trim();
        } catch (AhamoveAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AhamoveAuthException("Ahamove lấy token lỗi: " + e.getMessage(), e);
        }
    }

    /** Bỏ quote/BOM/Bearer khi user dán nhầm từ email/portal. */
    public static String sanitizeApiKey(String apiKey) {
        if (apiKey == null) {
            return null;
        }
        String k = apiKey.trim().replace("\uFEFF", "");
        if ((k.startsWith("\"") && k.endsWith("\"")) || (k.startsWith("'") && k.endsWith("'"))) {
            k = k.substring(1, k.length() - 1).trim();
        }
        if (k.regionMatches(true, 0, "Bearer ", 0, 7)) {
            k = k.substring(7).trim();
        }
        return k.isBlank() ? null : k;
    }

    /**
     * Chuẩn hóa SĐT VN → 84xxxxxxxxx (docs Ahamove).
     * Sửa lỗi hay gặp: 8409… (thừa 0), chỉ còn 9 số nội địa, khoảng trắng.
     */
    public static String normalizeMobile(String mobile) {
        List<String> c = mobileCandidates(mobile);
        return c.isEmpty() ? null : c.get(0);
    }

    /**
     * Ưu tiên dạng docs {@code 84xxxxxxxxx}, rồi {@code 0xxxxxxxxx} nếu Ahamove từ chối.
     */
    static List<String> mobileCandidates(String mobile) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (mobile == null) {
            return List.copyOf(out);
        }
        String digits = mobile.trim().replace("\uFEFF", "").replaceAll("[\\s\\-().]", "");
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        digits = digits.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return List.copyOf(out);
        }
        // 8409xxxxxxxx → 849xxxxxxxx
        if (digits.startsWith("840") && digits.length() == 12) {
            digits = "84" + digits.substring(3);
        }
        String intl;
        if (digits.startsWith("84") && digits.length() == 11) {
            intl = digits;
        } else if (digits.startsWith("0") && digits.length() == 10) {
            intl = "84" + digits.substring(1);
        } else if (digits.length() == 9 && digits.matches("[35789].*")) {
            intl = "84" + digits;
        } else {
            intl = digits;
        }
        if (intl.matches("84[35789]\\d{8}")) {
            out.add(intl);
            out.add("0" + intl.substring(2));
        } else if (intl.startsWith("0") && intl.length() == 10) {
            out.add("84" + intl.substring(1));
            out.add(intl);
        } else if (!intl.isBlank()) {
            out.add(intl);
        }
        return List.copyOf(out);
    }

    private static boolean messageHas(Throwable e, String code) {
        String m = e.getMessage();
        return m != null && m.contains(code);
    }

    private String formatTokenHttpError(int status, String body, String mobile, String apiKey) {
        String code = null;
        try {
            if (body != null && !body.isBlank()) {
                JsonNode root = objectMapper.readTree(body);
                code = text(root, "code");
                if (code == null) {
                    code = text(root, "internal");
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        String keyHint =
            "host=" +
            baseUrl +
            " mobile=" +
            mobile +
            " apiKeyLen=" +
            apiKey.length() +
            " suffix=…" +
            (apiKey.length() <= 4 ? "****" : apiKey.substring(apiKey.length() - 4));
        if ("PARTNER_NOT_FOUND".equals(code)) {
            return (
                "PARTNER_NOT_FOUND — Ahamove không nhận ra API key (" +
                keyHint +
                "). Kiểm tra key đúng PROD và dán lại nguyên api_key (không dán JWT)."
            );
        }
        if ("INVALID_PHONE_NUMBER".equals(code)) {
            return (
                "INVALID_PHONE_NUMBER — SĐT không hợp lệ (" +
                keyHint +
                "). Dùng đúng SĐT đã gắn partner trên portal Ahamove, dạng 84xxxxxxxxx (vd 84901234567 từ 0901234567). Không dùng SĐT lạ."
            );
        }
        if ("USER_NOT_FOUND".equals(code)) {
            return "USER_NOT_FOUND — SĐT chưa đăng ký với partner (" + keyHint + ").";
        }
        if ("NOT_AUTHORIZED".equals(code)) {
            return "NOT_AUTHORIZED — SĐT không thuộc partner của API key này (" + keyHint + ").";
        }
        return "Ahamove lấy token thất bại (HTTP " + status + "): " + truncate(body) + " [" + keyHint + "]";
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 300 ? s : s.substring(0, 300) + "…";
    }

    public static class AhamoveAuthException extends RuntimeException {

        public AhamoveAuthException(String message) {
            super(message);
        }

        public AhamoveAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
