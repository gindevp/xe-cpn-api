package com.mycompany.myapp.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.partner.PartnerHttpClients;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tra tên + địa chỉ doanh nghiệp theo MST để điền sẵn thông tin xuất hoá đơn (nhân viên vẫn kiểm tra/sửa).
 * Nguồn: API công khai esgoo.net (JSON). Không cào masothue.com — trang này trả công ty ngẫu nhiên khác cho bot.
 */
@Service
public class TaxCodeLookupService {

    private static final Logger LOG = LoggerFactory.getLogger(TaxCodeLookupService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int CACHE_MAX = 5000;

    private record Cached(Instant at, Map<String, Object> value) {}

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final HttpClient http;

    public TaxCodeLookupService(
        ObjectMapper objectMapper,
        @Value("${cpn.tax-lookup.base-url:https://esgoo.net/api-mst}") String baseUrl,
        @Value("${cpn.tax-lookup.insecure-ssl:false}") boolean insecureSsl
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = PartnerHttpClients.build(TIMEOUT, insecureSsl);
    }

    /** Luôn trả map {ok, …} — lỗi mạng / không tìm thấy không ném ra ngoài. */
    public Map<String, Object> lookup(String raw) {
        if (!VietnamTaxCode.isValid(raw)) {
            return error("INVALID", "Mã số thuế không hợp lệ (sai định dạng hoặc checksum)");
        }
        String taxCode = VietnamTaxCode.normalize(raw);
        Cached hit = cache.get(taxCode);
        if (hit != null && hit.at().plus(CACHE_TTL).isAfter(Instant.now())) {
            return hit.value();
        }
        Map<String, Object> out = fetch(taxCode);
        if (Boolean.TRUE.equals(out.get("ok"))) {
            if (cache.size() >= CACHE_MAX) {
                cache.clear();
            }
            cache.put(taxCode, new Cached(Instant.now(), out));
        }
        return out;
    }

    Map<String, Object> fetch(String taxCode) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/" + taxCode + ".htm"))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (compatible; CPN/1.0)")
                .GET()
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() != 200) {
                LOG.info("Tax lookup {} -> HTTP {}", taxCode, res.statusCode());
                return error("UPSTREAM_ERROR", "Nguồn tra cứu MST đang lỗi — nhập tay thông tin công ty");
            }
            return parse(taxCode, objectMapper.readTree(res.body()));
        } catch (java.net.http.HttpTimeoutException e) {
            return error("TIMEOUT", "Tra cứu MST quá lâu — thử lại hoặc nhập tay thông tin công ty");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("UPSTREAM_ERROR", "Tra cứu MST bị gián đoạn");
        } catch (Exception e) {
            LOG.info("Tax lookup {} failed: {}", taxCode, e.toString());
            return error("UPSTREAM_ERROR", "Không tra được MST — nhập tay thông tin công ty");
        }
    }

    static Map<String, Object> parse(String taxCode, JsonNode body) {
        JsonNode data = body == null ? null : body.path("data");
        String name = data == null ? null : clean(data.path("ten").asText(null));
        if (body == null || body.path("error").asInt(1) != 0 || name == null) {
            return error("NOT_FOUND", "Không tìm thấy doanh nghiệp với MST này");
        }
        String returned = clean(data.path("mst").asText(null));
        if (returned != null && !VietnamTaxCode.compact(returned).equals(VietnamTaxCode.compact(taxCode))) {
            return error("NOT_FOUND", "Nguồn tra cứu trả về MST khác — nhập tay thông tin công ty");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("taxCode", taxCode);
        out.put("companyName", name);
        out.put("address", clean(data.path("dc").asText(null)));
        return out;
    }

    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.isEmpty() || "null".equalsIgnoreCase(t) ? null : t;
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);
        out.put("code", code);
        out.put("message", message);
        return out;
    }
}
