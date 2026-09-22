package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Geocode qua Photon (Komoot) — ổn định hơn Nominatim public cho VN.
 * Không cần API key.
 */
@Service
public class OsmNominatimService {

    private static final Logger LOG = LoggerFactory.getLogger(OsmNominatimService.class);
    private static final String ENTITY = "geo";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(25);
    private static final Duration MIN_INTERVAL = Duration.ofMillis(400);

    private final ObjectMapper objectMapper;
    private final String photonBaseUrl;
    private final String userAgent;
    private final HttpClient httpClient;
    private final Object rateLock = new Object();
    private long lastRequestAtMs;

    public OsmNominatimService(
        ObjectMapper objectMapper,
        @Value("${cpn.osm.photon-base-url:https://photon.komoot.io}") String photonBaseUrl,
        @Value("${cpn.osm.user-agent:CPN-XE/1.0 (office-geo; contact=ops@xevietnam.local)}") String userAgent
    ) {
        this.objectMapper = objectMapper;
        this.photonBaseUrl = photonBaseUrl.endsWith("/") ? photonBaseUrl.substring(0, photonBaseUrl.length() - 1) : photonBaseUrl;
        this.userAgent = userAgent;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public List<Map<String, String>> autocomplete(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return List.of();
        }
        try {
            String qForSearch = q;
            String lower = q.toLowerCase(Locale.ROOT);
            if (!lower.contains("việt nam") && !lower.contains("vietnam")) {
                qForSearch = q + ", Việt Nam";
            }
            String url = photonBaseUrl + "/api/?limit=8&lang=vi&q=" + URLEncoder.encode(qForSearch, StandardCharsets.UTF_8);
            JsonNode root = getJson(url);
            JsonNode features = root.get("features");
            List<Map<String, String>> out = new ArrayList<>();
            if (features != null && features.isArray()) {
                for (JsonNode f : features) {
                    Map<String, String> row = mapPhotonFeature(f);
                    if (row != null) {
                        out.add(row);
                    }
                }
            }
            return out;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Photon autocomplete failed: {}", e.getMessage());
            throw new BadRequestAlertException("Tìm địa chỉ thất bại: " + e.getMessage(), ENTITY, "osmAutocomplete");
        }
    }

    public Map<String, Object> placeDetail(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            throw new BadRequestAlertException("placeId required", ENTITY, "placeIdRequired");
        }
        // Photon không có lookup theo id ổn định — search lại theo placeId encoded không hữu ích.
        // FE geocode dùng autocomplete (có lat/lng sẵn). Endpoint giữ để tương thích.
        throw new BadRequestAlertException("Dùng kết quả autocomplete (đã có lat/lng)", ENTITY, "osmPlace");
    }

    public Map<String, Object> reverse(double lat, double lng) {
        try {
            String url =
                photonBaseUrl +
                "/reverse?lat=" +
                URLEncoder.encode(Double.toString(lat), StandardCharsets.UTF_8) +
                "&lon=" +
                URLEncoder.encode(Double.toString(lng), StandardCharsets.UTF_8) +
                "&lang=vi";
            JsonNode root = getJson(url);
            JsonNode features = root.get("features");
            if (features == null || !features.isArray() || features.isEmpty()) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("address", "");
                empty.put("lat", BigDecimal.valueOf(lat));
                empty.put("lng", BigDecimal.valueOf(lng));
                return empty;
            }
            Map<String, String> row = mapPhotonFeature(features.get(0));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("address", row != null ? row.getOrDefault("description", "") : "");
            out.put("lat", row != null && row.get("lat") != null ? new BigDecimal(row.get("lat")) : BigDecimal.valueOf(lat));
            out.put("lng", row != null && row.get("lng") != null ? new BigDecimal(row.get("lng")) : BigDecimal.valueOf(lng));
            return out;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Photon reverse failed: {}", e.getMessage());
            throw new BadRequestAlertException("Reverse geocode thất bại: " + e.getMessage(), ENTITY, "osmReverse");
        }
    }

    static Map<String, String> mapPhotonFeature(JsonNode f) {
        if (f == null || f.isNull()) {
            return null;
        }
        JsonNode coords = f.path("geometry").path("coordinates");
        if (!coords.isArray() || coords.size() < 2) {
            return null;
        }
        // GeoJSON: [lng, lat]
        String lng = coords.get(0).asText();
        String lat = coords.get(1).asText();
        JsonNode props = f.path("properties");
        String osmType = text(props, "osm_type");
        String osmId = text(props, "osm_id");
        String placeId = encodePlaceId(osmTypeLetterToName(osmType), osmId);
        String description = buildPhotonDescription(props);
        if (description == null || lat == null || lng == null) {
            return null;
        }
        if (placeId == null) {
            placeId = "photon:" + lat + "," + lng;
        }
        Map<String, String> row = new LinkedHashMap<>();
        row.put("placeId", placeId);
        row.put("description", description);
        row.put("lat", lat);
        row.put("lng", lng);
        return row;
    }

    static String buildPhotonDescription(JsonNode props) {
        List<String> parts = new ArrayList<>();
        addPart(parts, text(props, "name"));
        addPart(parts, text(props, "street"));
        addPart(parts, text(props, "district"));
        addPart(parts, text(props, "city"));
        addPart(parts, text(props, "state"));
        addPart(parts, text(props, "country"));
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(", ", parts);
    }

    private static void addPart(List<String> parts, String v) {
        if (v == null || v.isBlank()) {
            return;
        }
        if (parts.stream().anyMatch(p -> p.equalsIgnoreCase(v))) {
            return;
        }
        parts.add(v.trim());
    }

    /** N/W/R hoặc node/way/relation → node/way/relation */
    static String osmTypeLetterToName(String osmType) {
        if (osmType == null || osmType.isBlank()) {
            return null;
        }
        String t = osmType.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "n", "node" -> "node";
            case "w", "way" -> "way";
            case "r", "relation" -> "relation";
            default -> t;
        };
    }

    static String encodePlaceId(String osmType, String osmId) {
        if (osmType == null || osmId == null || osmType.isBlank() || osmId.isBlank()) {
            return null;
        }
        return osmType.trim().toLowerCase(Locale.ROOT) + ":" + osmId.trim();
    }

    static String toOsmIdsParam(String placeId) {
        String[] parts = placeId.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        String type = parts[0].trim().toLowerCase(Locale.ROOT);
        String id = parts[1].trim();
        if (id.isEmpty() || !id.chars().allMatch(Character::isDigit)) {
            return null;
        }
        char prefix =
            switch (type) {
                case "node", "n" -> 'N';
                case "way", "w" -> 'W';
                case "relation", "r" -> 'R';
                default -> 0;
            };
        if (prefix == 0) {
            return null;
        }
        return prefix + id;
    }

    private JsonNode getJson(String url) throws Exception {
        throttle();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(HTTP_TIMEOUT)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }
        return objectMapper.readTree(response.body());
    }

    private void throttle() throws InterruptedException {
        synchronized (rateLock) {
            long now = System.currentTimeMillis();
            long wait = MIN_INTERVAL.toMillis() - (now - lastRequestAtMs);
            if (wait > 0) {
                Thread.sleep(wait);
            }
            lastRequestAtMs = System.currentTimeMillis();
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        // osm_id có thể là number
        String s = v.isNumber() ? v.asText() : v.asText();
        return s == null || s.isBlank() ? null : s;
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }
}
