package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Ahamove Partner: list services + estimate distance (KM).
 */
@Service
public class AhamoveOrderClient {

    private static final Logger LOG = LoggerFactory.getLogger(AhamoveOrderClient.class);
    private static final String ENTITY = "ahamove";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private final AhamoveTokenService ahamoveTokenService;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final HttpClient httpClient;

    public AhamoveOrderClient(
        AhamoveTokenService ahamoveTokenService,
        ObjectMapper objectMapper,
        @Value("${cpn.ahamove.base-url:https://partner-api.ahamove.com/v3}") String baseUrl,
        @Value("${cpn.ahamove.insecure-ssl:false}") boolean insecureSsl
    ) {
        this.ahamoveTokenService = ahamoveTokenService;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = PartnerHttpClients.build(Duration.ofSeconds(15), insecureSsl);
    }

    public List<Map<String, Object>> listServices(double lat, double lng, String deliveryType) {
        String token = requireToken();
        String type = deliveryType == null || deliveryType.isBlank() ? "INSTANT" : deliveryType.trim();
        try {
            String url =
                baseUrl +
                "/services?lat=" +
                URLEncoder.encode(Double.toString(lat), StandardCharsets.UTF_8) +
                "&lng=" +
                URLEncoder.encode(Double.toString(lng), StandardCharsets.UTF_8) +
                "&delivery_type=" +
                URLEncoder.encode(type, StandardCharsets.UTF_8);
            JsonNode root = getJson(url, token);
            List<Map<String, Object>> out = new ArrayList<>();
            if (root != null && root.isArray()) {
                for (JsonNode s : root) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", text(s, "_id"));
                    row.put("_id", text(s, "_id"));
                    row.put("name", text(s, "name") != null ? text(s, "name") : text(s, "description_vi_vn"));
                    row.put("cityId", text(s, "city_id"));
                    row.put("deliveryType", text(s, "delivery_type"));
                    if (row.get("id") != null) {
                        out.add(row);
                    }
                }
            }
            return out;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Ahamove list services failed: {}", e.getMessage());
            throw new BadRequestAlertException("Ahamove services thất bại: " + e.getMessage(), ENTITY, "ahamoveServices");
        }
    }

    /**
     * Estimate: path[0]=điểm lấy (VP), path[1]=điểm pin.
     * Payload giống curl docs: order_time, path, services/_id+requests, payment_method.
     * Response PROD: object hoặc array [{ service_id, data: { distance: km }, error }]; distance đã là km.
     */
    public Map<String, Object> estimateKm(
        String serviceId,
        double fromLat,
        double fromLng,
        String fromAddress,
        double toLat,
        double toLng,
        String toAddress
    ) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new BadRequestAlertException("serviceId required", ENTITY, "ahamoveServiceRequired");
        }
        String token = requireToken();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("order_time", 0);
            body.put("payment_method", "CASH");
            ArrayNode path = body.putArray("path");
            path.add(point(fromLat, fromLng, fromAddress));
            path.add(point(toLat, toLng, toAddress));
            ArrayNode services = body.putArray("services");
            ObjectNode svc = services.addObject();
            svc.put("_id", serviceId.trim());
            svc.putArray("requests");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/orders/estimates"))
                .timeout(HTTP_TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn("Ahamove estimate HTTP {}: {}", response.statusCode(), truncate(response.body()));
                throw new BadRequestAlertException(
                    "Ahamove estimate thất bại (HTTP " + response.statusCode() + "): " + truncate(response.body()),
                    ENTITY,
                    "ahamoveEstimate"
                );
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode item = unwrapEstimateItem(root, serviceId.trim());
            assertNoEstimateError(item);
            Double km = extractDistanceKm(item);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("serviceId", text(item, "service_id") != null ? text(item, "service_id") : serviceId.trim());
            if (km != null) {
                BigDecimal kmBd = BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
                out.put("distanceKm", kmBd);
                out.put("distanceMeters", kmBd.multiply(BigDecimal.valueOf(1000)));
            } else {
                LOG.warn("Ahamove estimate missing distance: {}", truncate(response.body()));
                out.put("distanceKm", null);
                out.put("distanceMeters", null);
            }
            return out;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Ahamove estimate failed: {}", e.getMessage());
            throw new BadRequestAlertException("Ahamove estimate thất bại: " + e.getMessage(), ENTITY, "ahamoveEstimate");
        }
    }

    /**
     * Convenience: ưu tiên group_services BIKE (Ahamove tự gắn HAN-BIKE/SGN-BIKE theo GPS),
     * fallback list /services rồi estimate theo _id.
     */
    public Map<String, Object> estimatePickupDistance(
        double officeLat,
        double officeLng,
        String officeAddress,
        double pinLat,
        double pinLng,
        String pinAddress
    ) {
        // 1) group_services BIKE — đúng docs, tránh nhầm SGN-BIKE ở Hà Nội.
        try {
            Map<String, Object> byGroup = estimateKmByGroupService("BIKE", officeLat, officeLng, officeAddress, pinLat, pinLng, pinAddress);
            if (byGroup.get("distanceKm") != null) {
                List<Map<String, Object>> services = listServices(officeLat, officeLng, "INSTANT");
                byGroup.put("services", services);
                return byGroup;
            }
        } catch (BadRequestAlertException e) {
            LOG.warn("Ahamove group_services BIKE failed, fallback listServices: {}", e.getMessage());
        } catch (Exception e) {
            LOG.warn("Ahamove group_services BIKE failed, fallback listServices: {}", e.getMessage());
        }

        List<Map<String, Object>> services = listServices(officeLat, officeLng, "INSTANT");
        if (services.isEmpty()) {
            throw new BadRequestAlertException("Không có service Ahamove tại vị trí VP", ENTITY, "ahamoveNoService");
        }
        String serviceId = pickPreferredServiceId(services);
        Map<String, Object> est = estimateKm(serviceId, officeLat, officeLng, officeAddress, pinLat, pinLng, pinAddress);
        if (est.get("distanceKm") == null) {
            throw new BadRequestAlertException(
                "Ahamove không trả distance (service=" + serviceId + ") — kiểm tra khu vực lấy hàng / service",
                ENTITY,
                "ahamoveEstimate"
            );
        }
        est.put("services", services);
        return est;
    }

    /** Ưu tiên *-BIKE, rồi *-ECO, rồi phần tử đầu. */
    static String pickPreferredServiceId(List<Map<String, Object>> services) {
        for (Map<String, Object> s : services) {
            String id = String.valueOf(s.get("id"));
            if (id.endsWith("-BIKE") || id.equals("BIKE")) {
                return id;
            }
        }
        for (Map<String, Object> s : services) {
            String id = String.valueOf(s.get("id"));
            if (id.contains("ECO")) {
                return id;
            }
        }
        return String.valueOf(services.get(0).get("id"));
    }

    private Map<String, Object> estimateKmByGroupService(
        String groupId,
        double fromLat,
        double fromLng,
        String fromAddress,
        double toLat,
        double toLng,
        String toAddress
    ) throws Exception {
        String token = requireToken();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("order_time", 0);
        body.put("payment_method", "CASH");
        ArrayNode path = body.putArray("path");
        path.add(point(fromLat, fromLng, fromAddress));
        path.add(point(toLat, toLng, toAddress));
        ArrayNode groups = body.putArray("group_services");
        ObjectNode g = groups.addObject();
        g.put("_id", groupId);
        g.putArray("group_requests");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/orders/estimates"))
            .timeout(HTTP_TIMEOUT)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BadRequestAlertException(
                "Ahamove estimate thất bại (HTTP " + response.statusCode() + "): " + truncate(response.body()),
                ENTITY,
                "ahamoveEstimate"
            );
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode item = unwrapEstimateItem(root, null);
        assertNoEstimateError(item);
        Double km = extractDistanceKm(item);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("serviceId", text(item, "service_id") != null ? text(item, "service_id") : groupId);
        if (km != null) {
            BigDecimal kmBd = BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
            out.put("distanceKm", kmBd);
            out.put("distanceMeters", kmBd.multiply(BigDecimal.valueOf(1000)));
        } else {
            out.put("distanceKm", null);
            out.put("distanceMeters", null);
        }
        return out;
    }

    private ObjectNode point(double lat, double lng, String address) {
        ObjectNode p = objectMapper.createObjectNode();
        p.put("lat", lat);
        p.put("lng", lng);
        p.put("address", address != null && !address.isBlank() ? address : (lat + "," + lng));
        return p;
    }

    private String requireToken() {
        Optional<String> token = ahamoveTokenService.resolveAccessToken();
        if (token.isEmpty()) {
            throw new BadRequestAlertException(
                "Chưa cấu hình / lấy được token Ahamove — vào Tích hợp Test API key",
                ENTITY,
                "ahamoveTokenMissing"
            );
        }
        return token.get();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(HTTP_TIMEOUT)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOG.warn("Ahamove GET HTTP {}: {}", response.statusCode(), truncate(response.body()));
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }
        return objectMapper.readTree(response.body());
    }

    /** Response có thể là 1 object hoặc array (docs). */
    static JsonNode unwrapEstimateItem(JsonNode root, String preferredServiceId) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            if (root.isEmpty()) {
                return null;
            }
            if (preferredServiceId != null) {
                for (JsonNode n : root) {
                    if (preferredServiceId.equals(text(n, "service_id"))) {
                        return n;
                    }
                }
            }
            // Ưu tiên phần tử có data.distance
            for (JsonNode n : root) {
                if (extractDistanceKm(n) != null) {
                    return n;
                }
            }
            return root.get(0);
        }
        return root;
    }

    static void assertNoEstimateError(JsonNode item) {
        if (item == null) {
            throw new BadRequestAlertException("Ahamove estimate response rỗng", ENTITY, "ahamoveEstimate");
        }
        JsonNode err = item.get("error");
        if (err != null && !err.isNull() && err.isObject() && err.size() > 0) {
            String code = text(err, "code");
            String title = text(err, "title");
            String desc = text(err, "description");
            String msg =
                (code != null ? code + " — " : "") + (title != null ? title : "Ahamove estimate lỗi") + (desc != null ? ": " + desc : "");
            throw new BadRequestAlertException(msg, ENTITY, "ahamoveEstimate");
        }
    }

    /**
     * Ahamove {@code data.distance} là km (vd 2.79, 11.98), không phải mét.
     */
    static Double extractDistanceKm(JsonNode item) {
        if (item == null) {
            return null;
        }
        JsonNode data = item.get("data");
        if (data != null && !data.isNull()) {
            Double d = number(data, "distance");
            if (d != null) {
                return d;
            }
            if (data.isArray() && !data.isEmpty()) {
                Double d0 = number(data.get(0), "distance");
                if (d0 != null) {
                    return d0;
                }
            }
        }
        Double top = number(item, "distance");
        if (top != null) {
            return top;
        }
        JsonNode est = item.get("order_estimate");
        if (est != null) {
            return number(est, "distance");
        }
        return null;
    }

    /** @deprecated dùng {@link #extractDistanceKm} — giữ tên cũ cho test cũ nếu còn tham chiếu. */
    static Double extractDistanceMeters(JsonNode root) {
        JsonNode item = unwrapEstimateItem(root, null);
        Double km = extractDistanceKm(item);
        return km == null ? null : km * 1000.0;
    }

    private static Double number(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isNumber()) {
            return v.asDouble();
        }
        if (v.isTextual()) {
            try {
                return Double.parseDouble(v.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return s == null || s.isBlank() ? null : s;
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 300 ? s : s.substring(0, 300) + "…";
    }
}
