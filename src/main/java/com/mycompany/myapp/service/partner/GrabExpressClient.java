package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * GrabExpress: OAuth client_credentials rồi quote / tạo / hủy / tip.
 * Staging {@code grab-express-sandbox}, production {@code grab-express}.
 */
@Service
public class GrabExpressClient {

    private static final Logger LOG = LoggerFactory.getLogger(GrabExpressClient.class);
    private static final String ENTITY = "grab";
    static final String SCOPE = "grab_express.partner_deliveries";
    private static final String TOKEN_URL = "https://partner-api.grab.com/grabid/v1/oauth2/token";

    private final IntegrationConfigRepository integrationConfigRepository;
    private final ObjectMapper json;
    private final RestClient http = RestClient.builder().build();
    private final TransactionTemplate tokenTx;

    public GrabExpressClient(
        IntegrationConfigRepository integrationConfigRepository,
        ObjectMapper json,
        PlatformTransactionManager transactionManager
    ) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.json = json;
        this.tokenTx = new TransactionTemplate(transactionManager);
        this.tokenTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public static String vehicleOf(String bulkyTier) {
        if (bulkyTier == null || bulkyTier.isBlank()) {
            return "BIKE";
        }
        return switch (bulkyTier.trim().toUpperCase()) {
            case "TIER_2" -> "CAR";
            case "TIER_3" -> "VAN";
            case "TIER_4" -> "TRUCK";
            default -> "BIKE";
        };
    }

    public record Stop(Double lat, Double lng, String address, String name, String phone, String instruction) {}

    public record Quote(BigDecimal amount, BigDecimal distanceKm) {}

    public record Created(String deliveryId, String status, BigDecimal amount, String trackingUrl) {}

    public void refreshToken() {
        accessToken(true);
    }

    public Quote quote(Stop origin, Stop destination, String vehicle, AhamoveCargo cargo) {
        ObjectNode body = baseBody(origin, destination, vehicle, cargo, null, null);
        JsonNode res = post("/deliveries/quotes", body);
        JsonNode quotes = res.get("quotes");
        JsonNode q = quotes != null && quotes.isArray() && !quotes.isEmpty() ? quotes.get(0) : res.get("quote");
        if (q == null || q.isNull()) {
            q = res;
        }
        return new Quote(money(q.get("amount")), km(q.get("distance")));
    }

    public Created create(Stop origin, Stop destination, String vehicle, AhamoveCargo cargo, String merchantOrderId, Long codAmount) {
        ObjectNode body = baseBody(origin, destination, vehicle, cargo, merchantOrderId, codAmount);
        JsonNode res = post("/deliveries", body);
        JsonNode quote = res.get("quote");
        BigDecimal amount = quote != null ? money(quote.get("amount")) : money(res.get("amount"));
        return new Created(text(res, "deliveryID"), text(res, "status"), amount, text(res, "trackingURL"));
    }

    public void cancel(String deliveryId) {
        try {
            http
                .delete()
                .uri(apiRoot() + "/deliveries/" + deliveryId.trim())
                .header("Authorization", "Bearer " + accessToken(false))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw grabError(e);
        }
    }

    public void tip(String deliveryId, long amountVnd) {
        ObjectNode body = json.createObjectNode();
        body.put("deliveryID", deliveryId);
        body.put("amount", amountVnd);
        post("/deliveries/tip/submit", body);
    }

    public JsonNode fetch(String deliveryId) {
        try {
            String raw = http
                .get()
                .uri(apiRoot() + "/deliveries/" + deliveryId.trim())
                .header("Authorization", "Bearer " + accessToken(false))
                .retrieve()
                .body(String.class);
            return raw == null || raw.isBlank() ? null : json.readTree(raw);
        } catch (RestClientResponseException e) {
            throw grabError(e);
        } catch (Exception e) {
            throw new BadRequestAlertException("Không đọc được đơn Grab", ENTITY, "grabFetch");
        }
    }

    /** Tải ảnh proof. Link Grab hết hạn nhanh nên phải lưu nội dung, không giữ URL. */
    public String downloadDataUrl(String url) {
        if (url == null || !url.startsWith("http")) {
            return null;
        }
        try {
            byte[] bytes = http.get().uri(url).retrieve().body(byte[].class);
            if (bytes == null || bytes.length < 32 || bytes.length > 1_400_000) {
                return null;
            }
            String mime = bytes[0] == (byte) 0x89 ? "image/png" : "image/jpeg";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (RuntimeException e) {
            LOG.warn("Grab photo download failed: {}", e.getMessage());
            return null;
        }
    }

    private ObjectNode baseBody(Stop origin, Stop destination, String vehicle, AhamoveCargo cargo, String merchantOrderId, Long cod) {
        ObjectNode body = json.createObjectNode();
        if (merchantOrderId != null) {
            body.put("merchantOrderID", merchantOrderId);
        }
        body.put("serviceType", "INSTANT");
        body.put("vehicleType", vehicle == null || vehicle.isBlank() ? "BIKE" : vehicle);
        body.put("paymentMethod", "CASHLESS");
        ArrayNode packages = body.putArray("packages");
        if (cargo != null && !cargo.packages().isEmpty()) {
            int i = 1;
            for (AhamoveCargo.Pkg pkg : cargo.packages()) {
                ObjectNode p = packages.addObject();
                p.put("name", "Kiện " + i);
                p.put("description", "Hàng CPN");
                p.put("quantity", 1);
                ObjectNode dim = p.putObject("dimensions");
                dim.put("height", cm(pkg.heightCm()));
                dim.put("width", cm(pkg.widthCm()));
                dim.put("depth", cm(pkg.lengthCm()));
                dim.put("weight", grams(pkg.weightKg()));
                i++;
            }
        } else {
            ObjectNode p = packages.addObject();
            p.put("name", "Hàng CPN");
            p.put("description", "Hàng CPN");
            p.put("quantity", 1);
            ObjectNode dim = p.putObject("dimensions");
            dim.put("height", 0);
            dim.put("width", 0);
            dim.put("depth", 0);
            dim.put("weight", 0);
        }
        putStop(body.putObject("origin"), origin);
        putStop(body.putObject("destination"), destination);
        body.set("sender", person(origin));
        body.set("recipient", person(destination));
        if (cod != null && cod > 0) {
            body.put("codType", "ADVANCED");
            body.putObject("cashOnDelivery").put("amount", cod);
        }
        return body;
    }

    private void putStop(ObjectNode node, Stop stop) {
        node.put("address", stop.address() == null ? "" : stop.address());
        if (stop.lat() != null && stop.lng() != null) {
            ObjectNode c = node.putObject("coordinates");
            c.put("latitude", stop.lat());
            c.put("longitude", stop.lng());
        }
    }

    private ObjectNode person(Stop stop) {
        ObjectNode p = json.createObjectNode();
        String name = stop.name() == null || stop.name().isBlank() ? "CPN" : stop.name().trim();
        p.put("firstName", name.length() > 80 ? name.substring(0, 80) : name);
        String phone = phone(stop.phone());
        if (phone != null) {
            p.put("phone", phone);
        }
        p.put("email", "ops@xe-cpn.vn");
        p.put("smsEnabled", true);
        if (stop.instruction() != null && !stop.instruction().isBlank()) {
            String note = stop.instruction().trim();
            p.put("instruction", note.length() > 1000 ? note.substring(0, 1000) : note);
        }
        return p;
    }

    static String phone(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("84") && digits.length() >= 10) {
            return digits;
        }
        if (digits.startsWith("0") && digits.length() >= 9) {
            return "84" + digits.substring(1);
        }
        if (digits.length() >= 9 && digits.length() <= 11) {
            return "84" + digits;
        }
        return null;
    }

    private JsonNode post(String path, ObjectNode body) {
        try {
            String raw = http
                .post()
                .uri(apiRoot() + path)
                .header("Authorization", "Bearer " + accessToken(false))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .body(String.class);
            return raw == null || raw.isBlank() ? json.createObjectNode() : json.readTree(raw);
        } catch (RestClientResponseException e) {
            throw grabError(e);
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Grab {} failed: {}", path, e.getMessage());
            throw new BadRequestAlertException("Grab không phản hồi", ENTITY, "grabDown");
        }
    }

    private String accessToken(boolean force) {
        IntegrationConfig cfg = config();
        requireCreds(cfg);
        Instant exp = cfg.getGrabTokenExpiresAt();
        if (!force && cfg.getGrabAccessToken() != null && exp != null && exp.isAfter(Instant.now().plusSeconds(120))) {
            return cfg.getGrabAccessToken();
        }
        ObjectNode body = json.createObjectNode();
        body.put("client_id", cfg.getGrabClientId().trim());
        body.put("client_secret", cfg.getGrabClientSecret().trim());
        body.put("grant_type", "client_credentials");
        body.put("scope", SCOPE);
        try {
            String raw = http
                .post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .body(String.class);
            JsonNode node = json.readTree(raw == null ? "{}" : raw);
            String token = text(node, "access_token");
            if (token == null) {
                throw new BadRequestAlertException("Grab không trả token", ENTITY, "grabToken");
            }
            int seconds = node.path("expires_in").asInt(3600);
            tokenTx.executeWithoutResult(status -> {
                IntegrationConfig row = integrationConfigRepository.findAll().stream().findFirst().orElseThrow();
                row.setGrabAccessToken(token);
                row.setGrabTokenExpiresAt(Instant.now().plusSeconds(Math.max(60, seconds - 60)));
                integrationConfigRepository.save(row);
            });
            return token;
        } catch (BadRequestAlertException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw grabError(e);
        } catch (Exception e) {
            LOG.warn("Grab token failed: {}", e.getMessage());
            throw new BadRequestAlertException("Không lấy được token Grab", ENTITY, "grabToken");
        }
    }

    private String apiRoot() {
        IntegrationConfig cfg = config();
        boolean sandbox = cfg.getGrabSandbox() == null || Boolean.TRUE.equals(cfg.getGrabSandbox());
        return sandbox ? "https://partner-api.grab.com/grab-express-sandbox/v1" : "https://partner-api.grab.com/grab-express/v1";
    }

    private IntegrationConfig config() {
        return integrationConfigRepository.findAll().stream().findFirst().orElseGet(IntegrationConfig::new);
    }

    private static void requireCreds(IntegrationConfig cfg) {
        if (
            cfg.getGrabClientId() == null ||
            cfg.getGrabClientId().isBlank() ||
            cfg.getGrabClientSecret() == null ||
            cfg.getGrabClientSecret().isBlank()
        ) {
            throw new BadRequestAlertException("Chưa lưu Client ID và Client Secret Grab (Tích hợp)", ENTITY, "grabCreds");
        }
    }

    private BadRequestAlertException grabError(RestClientResponseException e) {
        String msg = "Grab từ chối (" + e.getStatusCode().value() + ")";
        try {
            JsonNode node = json.readTree(e.getResponseBodyAsString());
            String detail = text(node, "message");
            if (detail == null) {
                detail = text(node, "reason");
            }
            if (detail == null) {
                detail = text(node, "devMessage");
            }
            if (detail != null && !detail.isBlank()) {
                msg = detail.length() > 240 ? detail.substring(0, 240) : detail;
            }
        } catch (Exception ignored) {
            // giữ câu chung
        }
        LOG.warn("Grab HTTP {}: {}", e.getStatusCode().value(), msg);
        return new BadRequestAlertException(msg, ENTITY, "grabRejected");
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        String v = node.get(field).asText();
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static BigDecimal money(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.decimalValue();
    }

    private static BigDecimal km(JsonNode metres) {
        if (metres == null || metres.isNull() || !metres.isNumber()) {
            return null;
        }
        return BigDecimal.valueOf(metres.asDouble() / 1000d);
    }

    private static int cm(Double value) {
        if (value == null || value < 0) {
            return 0;
        }
        return (int) Math.round(value);
    }

    private static int grams(double kg) {
        if (kg <= 0) {
            return 0;
        }
        return (int) Math.round(kg * 1000d);
    }
}
