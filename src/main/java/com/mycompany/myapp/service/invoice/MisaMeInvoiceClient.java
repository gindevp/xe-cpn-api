package com.mycompany.myapp.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MISA meInvoice All-in-One HTTP client (auth + publish + view + download).
 * Không dùng WebApp /insert.
 */
@Component
public class MisaMeInvoiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(MisaMeInvoiceClient.class);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration TOKEN_TTL = Duration.ofDays(14);
    private static final Duration TOKEN_SKEW = Duration.ofHours(12);

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String baseUrl;
    private final String appId;
    private final String taxCode;
    private final String username;
    private final String password;
    private final String invSeries;
    private final int signType;
    private final HttpClient httpClient;

    private final Object tokenLock = new Object();
    private String cachedToken;
    private Instant tokenValidUntil = Instant.EPOCH;

    public MisaMeInvoiceClient(
        ObjectMapper objectMapper,
        @Value("${cpn.misa.enabled:false}") boolean enabled,
        @Value("${cpn.misa.base-url:https://api.meinvoice.vn/api/integration}") String baseUrl,
        @Value("${cpn.misa.app-id:}") String appId,
        @Value("${cpn.misa.tax-code:0103179782}") String taxCode,
        @Value("${cpn.misa.username:}") String username,
        @Value("${cpn.misa.password:}") String password,
        @Value("${cpn.misa.inv-series:1C26MYY}") String invSeries,
        @Value("${cpn.misa.sign-type:5}") int signType
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.appId = appId == null ? "" : appId.trim();
        this.taxCode = taxCode == null ? "" : taxCode.trim();
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.invSeries = invSeries == null ? "1C26MYY" : invSeries.trim();
        this.signType = signType;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public String getInvSeries() {
        return invSeries;
    }

    public int getSignType() {
        return signType;
    }

    public String resolveToken() {
        synchronized (tokenLock) {
            if (cachedToken != null && Instant.now().isBefore(tokenValidUntil.minus(TOKEN_SKEW))) {
                return cachedToken;
            }
            return login();
        }
    }

    private String login() {
        if (appId.isBlank() || taxCode.isBlank() || username.isBlank() || password.isBlank()) {
            throw new MeInvoiceException("MISA credentials missing (app-id/tax-code/username/password)");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("appid", appId);
            body.put("taxcode", taxCode);
            body.put("username", username);
            body.put("password", password);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/token"))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MeInvoiceException("MISA auth failed HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            JsonNode root = objectMapper.readTree(response.body());
            String token = firstText(root, "data", "Data", "token", "Token", "access_token", "accessToken");
            if (token == null || token.isBlank()) {
                // Một số bản trả token ở root string / field khác
                if (root.isTextual()) {
                    token = root.asText();
                } else if (root.has("data") && root.get("data").isTextual()) {
                    token = root.get("data").asText();
                }
            }
            if (token == null || token.isBlank()) {
                throw new MeInvoiceException("MISA auth: token missing in response");
            }
            cachedToken = token.trim();
            tokenValidUntil = Instant.now().plus(TOKEN_TTL);
            LOG.info("MISA meInvoice token cached until {}", tokenValidUntil);
            return cachedToken;
        } catch (MeInvoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MeInvoiceException("MISA auth error: " + e.getMessage(), e);
        }
    }

    public PublishResult publish(ObjectNode requestBody) {
        String token = resolveToken();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/invoice"))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("CompanyTaxCode", taxCode)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String raw = response.body() == null ? "" : response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (isDuplicated(raw)) {
                    return PublishResult.duplicated(raw);
                }
                throw new MeInvoiceException("MISA publish HTTP " + response.statusCode() + ": " + truncate(raw));
            }
            return parsePublishResponse(raw);
        } catch (MeInvoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MeInvoiceException("MISA publish error: " + e.getMessage(), e);
        }
    }

    public String publishViewLink(String transactionId) {
        String token = resolveToken();
        try {
            ArrayNode body = objectMapper.createArrayNode();
            body.add(transactionId);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/invoice/publishview"))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("CompanyTaxCode", taxCode)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MeInvoiceException("MISA publishview HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            JsonNode root = objectMapper.readTree(response.body());
            String link = extractViewLink(root);
            if (link == null || link.isBlank()) {
                throw new MeInvoiceException("MISA publishview: link missing");
            }
            return link;
        } catch (MeInvoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MeInvoiceException("MISA publishview error: " + e.getMessage(), e);
        }
    }

    public byte[] downloadPdf(List<String> transactionIds) {
        String token = resolveToken();
        try {
            ArrayNode body = objectMapper.createArrayNode();
            for (String id : transactionIds) {
                if (id != null && !id.isBlank()) {
                    body.add(id.trim());
                }
            }
            String qs = "invoiceWithCode=true&invoiceCalcu=true&downloadDataType=pdf";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/invoice/download?" + qs))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("CompanyTaxCode", taxCode)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String msg = new String(response.body() == null ? new byte[0] : response.body(), StandardCharsets.UTF_8);
                throw new MeInvoiceException("MISA download HTTP " + response.statusCode() + ": " + truncate(msg));
            }
            byte[] bytes = response.body();
            if (bytes == null || bytes.length == 0) {
                throw new MeInvoiceException("MISA download: empty body");
            }
            // Một số API trả JSON chứa base64 PDF
            String asText = new String(bytes, StandardCharsets.UTF_8).trim();
            if (asText.startsWith("{") || asText.startsWith("[")) {
                JsonNode root = objectMapper.readTree(asText);
                String b64 = firstText(root, "data", "Data", "file", "File", "content", "Content");
                if (b64 != null && !b64.isBlank()) {
                    return Base64.getDecoder().decode(b64.replaceAll("\\s", ""));
                }
            }
            return bytes;
        } catch (MeInvoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MeInvoiceException("MISA download error: " + e.getMessage(), e);
        }
    }

    private PublishResult parsePublishResponse(String raw) {
        try {
            if (isDuplicated(raw)) {
                return PublishResult.duplicated(raw);
            }
            JsonNode root = objectMapper.readTree(raw);
            String errorCode = firstText(root, "ErrorCode", "errorCode", "error_code");
            if (errorCode != null && !errorCode.isBlank() && !"Success".equalsIgnoreCase(errorCode)) {
                if (errorCode.toLowerCase().contains("duplicat")) {
                    return PublishResult.duplicated(raw);
                }
                throw new MeInvoiceException("MISA ErrorCode=" + errorCode + ": " + truncate(raw));
            }

            JsonNode publishNode = root.get("publishInvoiceResult");
            if (publishNode == null) {
                publishNode = root.get("PublishInvoiceResult");
            }
            if (publishNode != null && publishNode.isTextual()) {
                publishNode = objectMapper.readTree(publishNode.asText());
            }
            if (publishNode == null) {
                publishNode = root.get("data");
            }
            if (publishNode != null && publishNode.isArray() && publishNode.size() > 0) {
                publishNode = publishNode.get(0);
            }
            if (publishNode == null || !publishNode.isObject()) {
                // Thử mảng kết quả phổ biến
                JsonNode arr = root.get("Result");
                if (arr != null && arr.isArray() && arr.size() > 0) {
                    publishNode = arr.get(0);
                }
            }

            String transactionId = firstText(publishNode, "TransactionID", "transactionID", "TransactionId");
            String invNo = firstText(publishNode, "InvNo", "invNo", "InvoiceNumber");
            String invSeries = firstText(publishNode, "InvSeries", "invSeries");
            String invCode = firstText(publishNode, "InvCode", "invCode");
            String rowError = firstText(publishNode, "ErrorCode", "errorCode");
            if (rowError != null && !rowError.isBlank() && !"Success".equalsIgnoreCase(rowError)) {
                if (rowError.toLowerCase().contains("duplicat") || "InvoiceDuplicated".equalsIgnoreCase(rowError)) {
                    return PublishResult.duplicated(raw);
                }
                throw new MeInvoiceException("MISA row ErrorCode=" + rowError + ": " + truncate(raw));
            }
            if ((transactionId == null || transactionId.isBlank()) && (invNo == null || invNo.isBlank())) {
                throw new MeInvoiceException("MISA publish: missing TransactionID/InvNo: " + truncate(raw));
            }
            return PublishResult.ok(transactionId, invNo, invSeries, invCode, raw);
        } catch (MeInvoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MeInvoiceException("MISA parse publish response: " + e.getMessage() + " · " + truncate(raw), e);
        }
    }

    private static boolean isDuplicated(String raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.toLowerCase();
        return s.contains("invoiceduplicated") || s.contains("invoice duplicated") || s.contains("\"duplicat");
    }

    private static String extractViewLink(JsonNode root) {
        if (root == null) {
            return null;
        }
        String direct = firstText(root, "data", "Data", "url", "Url", "link", "Link", "viewLink", "ViewLink");
        if (direct != null && direct.startsWith("http")) {
            return direct;
        }
        if (root.isArray() && root.size() > 0) {
            return extractViewLink(root.get(0));
        }
        if (root.has("data")) {
            return extractViewLink(root.get("data"));
        }
        return direct;
    }

    private static String firstText(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String k : keys) {
            if (node.has(k) && !node.get(k).isNull()) {
                String v = node.get(k).asText(null);
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return null;
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }

    public record PublishResult(
        boolean ok,
        boolean duplicated,
        String transactionId,
        String invNo,
        String invSeries,
        String invCode,
        String raw
    ) {
        static PublishResult ok(String tx, String no, String series, String code, String raw) {
            return new PublishResult(true, false, tx, no, series, code, raw);
        }

        static PublishResult duplicated(String raw) {
            return new PublishResult(true, true, null, null, null, null, raw);
        }
    }
}
