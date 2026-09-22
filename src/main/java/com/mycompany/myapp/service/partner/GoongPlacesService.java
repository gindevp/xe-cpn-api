package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proxy Goong Places — API key lấy từ IntegrationConfig.distanceApiToken.
 */
@Service
@Transactional(readOnly = true)
public class GoongPlacesService {

    private static final Logger LOG = LoggerFactory.getLogger(GoongPlacesService.class);
    private static final String ENTITY = "geo";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);

    private final IntegrationConfigRepository integrationConfigRepository;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final HttpClient httpClient;

    public GoongPlacesService(
        IntegrationConfigRepository integrationConfigRepository,
        ObjectMapper objectMapper,
        @Value("${cpn.goong.base-url:https://rsapi.goong.io}") String baseUrl
    ) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public List<Map<String, String>> autocomplete(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return List.of();
        }
        String apiKey = requireApiKey();
        try {
            String url =
                baseUrl +
                "/Place/AutoComplete?input=" +
                URLEncoder.encode(q, StandardCharsets.UTF_8) +
                "&api_key=" +
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            JsonNode root = getJson(url);
            List<Map<String, String>> out = new ArrayList<>();
            JsonNode predictions = root.get("predictions");
            if (predictions != null && predictions.isArray()) {
                for (JsonNode p : predictions) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("placeId", text(p, "place_id"));
                    row.put("description", text(p, "description"));
                    if (row.get("placeId") != null && row.get("description") != null) {
                        out.add(row);
                    }
                }
            }
            return out;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Goong autocomplete failed: {}", e.getMessage());
            throw new BadRequestAlertException("Goong autocomplete thất bại: " + e.getMessage(), ENTITY, "goongAutocomplete");
        }
    }

    public Map<String, Object> placeDetail(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            throw new BadRequestAlertException("placeId required", ENTITY, "placeIdRequired");
        }
        String apiKey = requireApiKey();
        try {
            String url =
                baseUrl +
                "/Place/Detail?place_id=" +
                URLEncoder.encode(placeId.trim(), StandardCharsets.UTF_8) +
                "&api_key=" +
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            JsonNode root = getJson(url);
            JsonNode result = root.get("result");
            if (result == null || result.isNull()) {
                throw new BadRequestAlertException("Goong không trả về địa điểm", ENTITY, "goongPlaceEmpty");
            }
            String name = text(result, "name");
            String formatted = text(result, "formatted_address");
            String address = joinAddress(name, formatted);
            JsonNode loc = result.path("geometry").path("location");
            BigDecimal lat = decimal(loc, "lat");
            BigDecimal lng = decimal(loc, "lng");
            if (lat == null || lng == null) {
                throw new BadRequestAlertException("Goong thiếu tọa độ", ENTITY, "goongNoCoords");
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("address", address);
            out.put("lat", lat);
            out.put("lng", lng);
            out.put("placeId", placeId.trim());
            return out;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Goong place detail failed: {}", e.getMessage());
            throw new BadRequestAlertException("Goong place detail thất bại: " + e.getMessage(), ENTITY, "goongPlace");
        }
    }

    private String requireApiKey() {
        IntegrationConfig cfg = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        String key = cfg != null ? cfg.getDistanceApiToken() : null;
        if (key == null || key.isBlank()) {
            throw new BadRequestAlertException(
                "Chưa cấu hình Goong API key trên Tích hợp (Goong / Distance Matrix)",
                ENTITY,
                "goongKeyMissing"
            );
        }
        return key.trim();
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(HTTP_TIMEOUT).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }
        return objectMapper.readTree(response.body());
    }

    static String joinAddress(String name, String formatted) {
        if (formatted == null || formatted.isBlank()) {
            return name != null ? name.trim() : "";
        }
        if (name == null || name.isBlank()) {
            return formatted.trim();
        }
        String n = name.trim();
        String f = formatted.trim();
        if (f.startsWith(n) || f.contains(n)) {
            return f;
        }
        return n + ", " + f;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return s == null || s.isBlank() ? null : s;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isNumber()) {
            return null;
        }
        return v.decimalValue();
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }
}
