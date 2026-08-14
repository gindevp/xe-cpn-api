package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
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
 * Outbound client for VTHK passenger catalog {@code POST /api/fe/cata/search_trips}.
 */
@Component
public class VthkTripSearchClient {

    private static final Logger LOG = LoggerFactory.getLogger(VthkTripSearchClient.class);
    private static final String ENTITY = "vthkTrip";

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String baseUrl;
    private final String appIdAccess;
    private final boolean insecureSsl;
    private final int pageSize;
    private final HttpClient httpClient;

    public VthkTripSearchClient(
        ObjectMapper objectMapper,
        @Value("${cpn.vthk.enabled:true}") boolean enabled,
        @Value("${cpn.vthk.base-url:https://113.20.107.133:8991}") String baseUrl,
        @Value("${cpn.vthk.app-id-access:APP.XEVN}") String appIdAccess,
        @Value("${cpn.vthk.insecure-ssl:true}") boolean insecureSsl,
        @Value("${cpn.vthk.page-size:50}") int pageSize
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.appIdAccess = appIdAccess;
        this.insecureSsl = insecureSsl;
        this.pageSize = Math.max(1, pageSize);
        this.httpClient = buildHttpClient();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<JsonNode> searchTrips(String lfid, String ltid, String date) {
        if (!enabled) {
            throw new BadRequestAlertException("VTHK integration disabled", ENTITY, "vthkDisabled");
        }
        try {
            String body = objectMapper.writeValueAsString(
                java.util.Map.of(
                    "query",
                    java.util.Map.of("lfid", lfid, "ltid", ltid, "dt", date),
                    "command",
                    java.util.Map.of("PageNumber", 1, "PageSize", pageSize)
                )
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/fe/cata/search_trips"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("AppIdAccess", appIdAccess)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn("VTHK search_trips HTTP {} body={}", response.statusCode(), truncate(response.body()));
                throw new BadRequestAlertException("VTHK search_trips failed: HTTP " + response.statusCode(), ENTITY, "vthkHttpError");
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("IsSuccess").isBoolean() && !root.path("IsSuccess").asBoolean()) {
                throw new BadRequestAlertException(
                    "VTHK search_trips: " + root.path("Message").asText("error"),
                    ENTITY,
                    "vthkBusinessError"
                );
            }
            JsonNode trips = root.path("ObjectInfo").path("CatalogProductsModel").path("Trips").path("Data");
            List<JsonNode> out = new ArrayList<>();
            if (trips.isArray()) {
                trips.forEach(out::add);
            }
            return out;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("VTHK search_trips call failed", e);
            throw new BadRequestAlertException("VTHK search_trips call failed: " + e.getMessage(), ENTITY, "vthkCallFailed");
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
