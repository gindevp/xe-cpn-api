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
 * Nguồn chính: API công khai VietQR (dữ liệu Cục Thuế, địa chỉ theo đơn vị hành chính mới, có MST chi nhánh);
 * dự phòng: esgoo.net. Không cào masothue.com — trang này trả công ty ngẫu nhiên khác cho bot.
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
    private final String vietQrUrl;
    private final String baseUrl;
    private final HttpClient http;

    public TaxCodeLookupService(
        ObjectMapper objectMapper,
        @Value("${cpn.tax-lookup.vietqr-url:https://api.vietqr.io/v2/business}") String vietQrUrl,
        @Value("${cpn.tax-lookup.base-url:https://esgoo.net/api-mst}") String baseUrl,
        @Value("${cpn.tax-lookup.insecure-ssl:false}") boolean insecureSsl
    ) {
        this.objectMapper = objectMapper;
        this.vietQrUrl = trimSlash(vietQrUrl);
        this.baseUrl = trimSlash(baseUrl);
        this.http = PartnerHttpClients.build(TIMEOUT, insecureSsl);
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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
        Map<String, Object> primary = fetchFrom("vietqr", vietQrUrl + "/" + taxCode, taxCode, TaxCodeLookupService::parseVietQr);
        if (Boolean.TRUE.equals(primary.get("ok"))) {
            return primary;
        }
        Map<String, Object> fallback = fetchFrom("esgoo", baseUrl + "/" + taxCode + ".htm", taxCode, TaxCodeLookupService::parse);
        if (Boolean.TRUE.equals(fallback.get("ok"))) {
            return fallback;
        }
        return "NOT_FOUND".equals(primary.get("code")) ? primary : fallback;
    }

    Map<String, Object> fetchFrom(
        String source,
        String url,
        String taxCode,
        java.util.function.BiFunction<String, JsonNode, Map<String, Object>> parser
    ) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (compatible; CPN/1.0)")
                .GET()
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() != 200) {
                LOG.warn("Tax lookup {} via {} -> HTTP {}", taxCode, source, res.statusCode());
                return error("UPSTREAM_ERROR", "Nguồn tra cứu MST đang lỗi — nhập tay thông tin công ty");
            }
            return parser.apply(taxCode, objectMapper.readTree(res.body()));
        } catch (java.net.http.HttpTimeoutException e) {
            LOG.warn("Tax lookup {} via {} timed out", taxCode, source);
            return error("TIMEOUT", "Tra cứu MST quá lâu — thử lại hoặc nhập tay thông tin công ty");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("UPSTREAM_ERROR", "Tra cứu MST bị gián đoạn");
        } catch (Exception e) {
            LOG.warn("Tax lookup {} via {} failed: {}", taxCode, source, e.toString());
            return error("UPSTREAM_ERROR", "Không tra được MST — nhập tay thông tin công ty");
        }
    }

    /** VietQR: {@code {code:"00", data:{id, name, address}}}. */
    static Map<String, Object> parseVietQr(String taxCode, JsonNode body) {
        JsonNode data = body == null ? null : body.path("data");
        String name = data == null ? null : clean(data.path("name").asText(null));
        if (body == null || !"00".equals(body.path("code").asText()) || name == null) {
            return error("NOT_FOUND", "Không tìm thấy doanh nghiệp với MST này");
        }
        String returned = clean(data.path("id").asText(null));
        if (returned != null && !VietnamTaxCode.compact(returned).equals(VietnamTaxCode.compact(taxCode))) {
            return error("NOT_FOUND", "Nguồn tra cứu trả về MST khác — nhập tay thông tin công ty");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("taxCode", taxCode);
        out.put("companyName", name);
        out.put("address", clean(data.path("address").asText(null)));
        return out;
    }

    /** esgoo: {@code {error:0, data:{ten, mst, dc}}}. */
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
