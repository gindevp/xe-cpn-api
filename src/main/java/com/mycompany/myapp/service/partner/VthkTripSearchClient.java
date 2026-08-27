package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CRM VTHK client: login for Bearer token + {@code POST /api/be/app/get_list_trips}.
 * Token is refreshed at least daily (or earlier if JWT expires sooner).
 */
@Component
public class VthkTripSearchClient {

    private static final Logger LOG = LoggerFactory.getLogger(VthkTripSearchClient.class);
    private static final String ENTITY = "vthkTrip";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CRM_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Duration TOKEN_REFRESH = Duration.ofHours(24);
    private static final Duration TOKEN_SKEW = Duration.ofMinutes(5);

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String baseUrl;
    private final String appIdAccess;
    private final String loginMobile;
    private final String loginPassword;
    private final boolean insecureSsl;
    private final HttpClient httpClient;

    private final Object tokenLock = new Object();
    private String cachedToken;
    private Instant tokenValidUntil = Instant.EPOCH;

    public VthkTripSearchClient(
        ObjectMapper objectMapper,
        @Value("${cpn.vthk.enabled:true}") boolean enabled,
        @Value("${cpn.vthk.base-url:https://crmapi.xevietnam.com}") String baseUrl,
        @Value("${cpn.vthk.app-id-access:CRM.XEVN}") String appIdAccess,
        @Value("${cpn.vthk.login-mobile:}") String loginMobile,
        @Value("${cpn.vthk.login-password:}") String loginPassword,
        @Value("${cpn.vthk.insecure-ssl:false}") boolean insecureSsl
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.appIdAccess = appIdAccess;
        this.loginMobile = loginMobile == null ? "" : loginMobile.trim();
        this.loginPassword = loginPassword == null ? "" : loginPassword;
        this.insecureSsl = insecureSsl;
        this.httpClient = buildHttpClient();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Search trips for itinerary labels ({@code MaHanhTrinhs}), window {@code [now, now+1h]} VN time.
     */
    public List<JsonNode> searchTripsByItineraries(List<String> maHanhTrinhs) {
        if (!enabled) {
            throw new BadRequestAlertException("VTHK integration disabled", ENTITY, "vthkDisabled");
        }
        if (maHanhTrinhs == null || maHanhTrinhs.isEmpty()) {
            throw new BadRequestAlertException("MaHanhTrinhs is required", ENTITY, "maHanhTrinhRequired");
        }
        LocalDateTime now = LocalDateTime.now(VN);
        String ngayDiTu = CRM_DT.format(now);
        String ngayDiDen = CRM_DT.format(now.plusHours(1));
        return searchTripsByItineraries(maHanhTrinhs, ngayDiTu, ngayDiDen);
    }

    public List<JsonNode> searchTripsByItineraries(List<String> maHanhTrinhs, String ngayDiTu, String ngayDiDen) {
        if (!enabled) {
            throw new BadRequestAlertException("VTHK integration disabled", ENTITY, "vthkDisabled");
        }
        String token = resolveToken();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("NgayDiTu", ngayDiTu);
            body.put("NgayDiDen", ngayDiDen);
            ArrayNode arr = body.putArray("MaHanhTrinhs");
            for (String m : maHanhTrinhs) {
                if (m != null && !m.isBlank()) {
                    arr.add(m.trim());
                }
            }
            if (arr.isEmpty()) {
                throw new BadRequestAlertException("MaHanhTrinhs is required", ENTITY, "maHanhTrinhRequired");
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/be/app/get_list_trips"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("AppIdAccess", appIdAccess)
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                invalidateToken();
                token = resolveToken();
                request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/be/app/get_list_trips"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("AppIdAccess", appIdAccess)
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn("VTHK get_list_trips HTTP {} body={}", response.statusCode(), truncate(response.body()));
                throw new BadRequestAlertException("VTHK get_list_trips failed: HTTP " + response.statusCode(), ENTITY, "vthkHttpError");
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("IsSuccess").isBoolean() && !root.path("IsSuccess").asBoolean()) {
                throw new BadRequestAlertException(
                    "VTHK get_list_trips: " + root.path("Message").asText("error"),
                    ENTITY,
                    "vthkBusinessError"
                );
            }
            JsonNode info = root.path("ObjectInfo");
            List<JsonNode> out = new ArrayList<>();
            if (info.isArray()) {
                info.forEach(out::add);
            }
            return out;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("VTHK get_list_trips call failed", e);
            throw new BadRequestAlertException("VTHK get_list_trips call failed: " + e.getMessage(), ENTITY, "vthkCallFailed");
        }
    }

    private String resolveToken() {
        synchronized (tokenLock) {
            Instant now = Instant.now();
            if (cachedToken != null && now.isBefore(tokenValidUntil)) {
                return cachedToken;
            }
            return loginAndCache();
        }
    }

    private void invalidateToken() {
        synchronized (tokenLock) {
            cachedToken = null;
            tokenValidUntil = Instant.EPOCH;
        }
    }

    private String loginAndCache() {
        if (loginMobile.isBlank() || loginPassword.isBlank()) {
            throw new BadRequestAlertException(
                "VTHK CRM login not configured (cpn.vthk.login-mobile / login-password)",
                ENTITY,
                "vthkLoginConfig"
            );
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("Mobile", loginMobile);
            body.put("Password", loginPassword);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/v1/login"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("AppIdAccess", appIdAccess)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn("VTHK login HTTP {} body={}", response.statusCode(), truncate(response.body()));
                throw new BadRequestAlertException("VTHK login failed: HTTP " + response.statusCode(), ENTITY, "vthkLoginHttp");
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("IsSuccess").isBoolean() && !root.path("IsSuccess").asBoolean()) {
                throw new BadRequestAlertException("VTHK login: " + root.path("Message").asText("error"), ENTITY, "vthkLoginBusiness");
            }
            String token = root.path("ObjectInfo").path("Token").asText(null);
            if (token == null || token.isBlank()) {
                throw new BadRequestAlertException("VTHK login: missing Token", ENTITY, "vthkLoginToken");
            }
            Instant jwtExp = parseJwtExp(token);
            Instant dailyCap = Instant.now().plus(TOKEN_REFRESH);
            Instant until = jwtExp != null && jwtExp.isBefore(dailyCap) ? jwtExp.minus(TOKEN_SKEW) : dailyCap;
            if (until.isBefore(Instant.now().plusSeconds(60))) {
                until = Instant.now().plus(Duration.ofHours(1));
            }
            cachedToken = token.trim();
            tokenValidUntil = until;
            LOG.info("VTHK CRM token refreshed; validUntil={}", tokenValidUntil);
            return cachedToken;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("VTHK login call failed", e);
            throw new BadRequestAlertException("VTHK login call failed: " + e.getMessage(), ENTITY, "vthkLoginFailed");
        }
    }

    /** Best-effort JWT {@code exp} (seconds); null if not parseable. */
    Instant parseJwtExp(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(decoded);
            if (!node.has("exp")) {
                return null;
            }
            return Instant.ofEpochSecond(node.get("exp").asLong());
        } catch (Exception e) {
            return null;
        }
    }

    private HttpClient buildHttpClient() {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15));
            if (insecureSsl) {
                TrustManager[] trustAll = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    },
                };
                SSLContext ssl = SSLContext.getInstance("TLS");
                ssl.init(null, trustAll, new SecureRandom());
                builder.sslContext(ssl);
            }
            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build VTHK HttpClient", e);
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 400 ? s : s.substring(0, 400) + "...";
    }
}
