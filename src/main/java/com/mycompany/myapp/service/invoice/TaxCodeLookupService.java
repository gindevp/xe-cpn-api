package com.mycompany.myapp.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.partner.PartnerHttpClients;
import com.mycompany.myapp.service.realtime.ServerEventService;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tra tên + địa chỉ doanh nghiệp theo MST để điền sẵn thông tin xuất hoá đơn.
 * Nguồn chính: cổng X.E ({@code company_by_tax}). Không ra dữ liệu thì Xinvoice
 * (dữ liệu Tổng cục Thuế, có MST chi nhánh + trạng thái hoạt động), rồi VietQR, rồi esgoo.net.
 * Không cào masothue.com — trang này trả công ty ngẫu nhiên khác cho bot.
 */
@Service
public class TaxCodeLookupService {

    private static final Logger LOG = LoggerFactory.getLogger(TaxCodeLookupService.class);
    /** {@code TP} / {@code TP.} đứng riêng (không nuốt {@code TPHCM}). */
    private static final Pattern TP_ABBR = Pattern.compile(
        "(?iu)(?<![\\p{L}\\p{N}])TP\\.(?=\\s|$|\\p{L})|(?<![\\p{L}\\p{N}])TP(?=\\s|$|[,;])"
    );
    private static final Pattern VIETNAM = Pattern.compile("vi[eệ]t\\s*nam", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int CACHE_MAX = 5000;
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(10);

    private record Cached(Instant at, Map<String, Object> value) {}

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    /** MST → lần cuối đã báo admin, để một MST lỗi tra đi tra lại không bắn popup liên tục. */
    private final Map<String, Instant> alerted = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ObjectProvider<ServerEventService> serverEventService;
    private final String portalUrl;
    private final String portalAppId;
    private final String xinvoiceUrl;
    private final Map<String, String> xinvoiceHeaders;
    private final String vietQrUrl;
    private final String baseUrl;
    private final HttpClient http;

    public TaxCodeLookupService(
        ObjectMapper objectMapper,
        ObjectProvider<ServerEventService> serverEventService,
        @Value("${cpn.tax-lookup.portal-url:https://portal.xevietnam.com/api/fe/com/company_by_tax}") String portalUrl,
        @Value("${cpn.tax-lookup.portal-app-id:APP.XEVN}") String portalAppId,
        @Value("${cpn.tax-lookup.xinvoice-url:https://api.xinvoice.vn/gdt-api/tax-payer}") String xinvoiceUrl,
        @Value("${cpn.tax-lookup.xinvoice-client-id:}") String xinvoiceClientId,
        @Value("${cpn.tax-lookup.xinvoice-api-key:}") String xinvoiceApiKey,
        @Value("${cpn.tax-lookup.vietqr-url:https://api.vietqr.io/v2/business}") String vietQrUrl,
        @Value("${cpn.tax-lookup.base-url:https://esgoo.net/api-mst}") String baseUrl,
        @Value("${cpn.tax-lookup.insecure-ssl:false}") boolean insecureSsl
    ) {
        this.objectMapper = objectMapper;
        this.serverEventService = serverEventService;
        this.portalUrl = portalUrl == null ? "" : portalUrl.trim();
        this.portalAppId = portalAppId == null ? "" : portalAppId.trim();
        this.xinvoiceUrl = trimSlash(xinvoiceUrl);
        Map<String, String> headers = new LinkedHashMap<>();
        if (xinvoiceClientId != null && !xinvoiceClientId.isBlank()) {
            headers.put("client-id", xinvoiceClientId.trim());
        }
        if (xinvoiceApiKey != null && !xinvoiceApiKey.isBlank()) {
            headers.put("api-key", xinvoiceApiKey.trim());
        }
        this.xinvoiceHeaders = Map.copyOf(headers);
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
        } else if (isUpstreamFailure(out)) {
            alertAdmins(taxCode, out);
        }
        return out;
    }

    static boolean isUpstreamFailure(Map<String, Object> result) {
        Object code = result.get("code");
        return "UPSTREAM_ERROR".equals(code) || "TIMEOUT".equals(code);
    }

    private void alertAdmins(String taxCode, Map<String, Object> result) {
        Instant now = Instant.now();
        Instant last = alerted.get(taxCode);
        if (last != null && last.plus(ALERT_COOLDOWN).isAfter(now)) {
            return;
        }
        if (alerted.size() >= CACHE_MAX) {
            alerted.clear();
        }
        alerted.put(taxCode, now);
        ServerEventService events = serverEventService.getIfAvailable();
        if (events == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taxCode", taxCode);
        payload.put("code", result.get("code"));
        payload.put("message", result.get("message"));
        payload.put("by", SecurityUtils.getCurrentUserLogin().orElse(null));
        payload.put("at", now.toString());
        events.taxLookupError(payload);
    }

    /** Thử lần lượt các nguồn; nguồn nào báo "không tìm thấy" thì kết quả cuối là NOT_FOUND (không báo lỗi admin). */
    Map<String, Object> fetch(String taxCode) {
        Map<String, Object> portal = fetchPortal(taxCode);
        if (Boolean.TRUE.equals(portal.get("ok"))) {
            return portal;
        }
        Map<String, Object> xinvoice = fetchFrom(
            "xinvoice",
            xinvoiceUrl + "/" + taxCode,
            xinvoiceHeaders,
            taxCode,
            TaxCodeLookupService::parseXinvoice
        );
        if (Boolean.TRUE.equals(xinvoice.get("ok"))) {
            return xinvoice;
        }
        Map<String, Object> vietQr = fetchFrom("vietqr", vietQrUrl + "/" + taxCode, Map.of(), taxCode, TaxCodeLookupService::parseVietQr);
        if (Boolean.TRUE.equals(vietQr.get("ok"))) {
            return vietQr;
        }
        Map<String, Object> esgoo = fetchFrom("esgoo", baseUrl + "/" + taxCode + ".htm", Map.of(), taxCode, TaxCodeLookupService::parse);
        if (Boolean.TRUE.equals(esgoo.get("ok"))) {
            return esgoo;
        }
        for (Map<String, Object> r : java.util.List.of(portal, xinvoice, vietQr, esgoo)) {
            if ("NOT_FOUND".equals(r.get("code"))) {
                return r;
            }
        }
        return esgoo;
    }

    private Map<String, Object> fetchPortal(String taxCode) {
        if (portalUrl.isBlank()) {
            return error("UPSTREAM_ERROR", "Chưa cấu hình nguồn tra cứu MST của X.E");
        }
        String url = portalUrl + (portalUrl.contains("?") ? "&" : "?") + "c=" + URLEncoder.encode(taxCode, StandardCharsets.UTF_8);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (!portalAppId.isBlank()) {
            headers.put("AppIdAccess", portalAppId);
        }
        return fetchFrom("portal", url, headers, taxCode, TaxCodeLookupService::parsePortal);
    }

    /** Cổng X.E: {@code {status:SUCCESS, code:200, data:{companyName, taxCode, address}}}. */
    static Map<String, Object> parsePortal(String taxCode, JsonNode body) {
        JsonNode data = body == null ? null : body.path("data");
        String name = data != null && data.isObject() ? clean(data.path("companyName").asText(null)) : null;
        if (body == null || !"SUCCESS".equalsIgnoreCase(body.path("status").asText()) || name == null) {
            return error("NOT_FOUND", "Không tìm thấy doanh nghiệp với MST này");
        }
        String returned = clean(data.path("taxCode").asText(null));
        if (returned != null && !VietnamTaxCode.compact(returned).equals(VietnamTaxCode.compact(taxCode))) {
            return error("NOT_FOUND", "Nguồn tra cứu trả về MST khác — kiểm tra lại MST");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("taxCode", taxCode);
        out.put("companyName", name);
        out.put("address", normalizeAddress(data.path("address").asText(null)));
        return out;
    }

    Map<String, Object> fetchFrom(
        String source,
        String url,
        Map<String, String> headers,
        String taxCode,
        java.util.function.BiFunction<String, JsonNode, Map<String, Object>> parser
    ) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (compatible; CPN/1.0)");
            headers.forEach(builder::header);
            HttpRequest req = builder.GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() == 404) {
                return error("NOT_FOUND", "Không tìm thấy doanh nghiệp với MST này");
            }
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

    /** Xinvoice: {@code {taxID, name, address, status, orgType}}; không có MST thì HTTP 404. */
    static Map<String, Object> parseXinvoice(String taxCode, JsonNode body) {
        String name = body == null ? null : clean(body.path("name").asText(null));
        if (name == null) {
            return error("NOT_FOUND", "Không tìm thấy doanh nghiệp với MST này");
        }
        String returned = clean(body.path("taxID").asText(null));
        if (returned != null && !VietnamTaxCode.compact(returned).equals(VietnamTaxCode.compact(taxCode))) {
            return error("NOT_FOUND", "Nguồn tra cứu trả về MST khác — kiểm tra lại MST");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("taxCode", taxCode);
        out.put("companyName", name);
        out.put("address", normalizeAddress(body.path("address").asText(null)));
        String orgType = clean(body.path("orgType").asText(null));
        if (orgType != null) {
            out.put("orgType", orgType);
        }
        String status = clean(body.path("status").asText(null));
        if (status != null) {
            out.put("status", status);
            out.put("active", isActiveStatus(status));
        }
        return out;
    }

    /** "NNT đang hoạt động…" là còn hoạt động; ngừng / tạm ngừng / không hoạt động tại địa chỉ thì không. */
    static boolean isActiveStatus(String status) {
        String s = status.toLowerCase(java.util.Locale.ROOT);
        return s.contains("đang hoạt động") && !s.contains("ngừng") && !s.contains("không hoạt động");
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
        out.put("address", normalizeAddress(data.path("address").asText(null)));
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
        out.put("address", normalizeAddress(data.path("dc").asText(null)));
        return out;
    }

    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.isEmpty() || "null".equalsIgnoreCase(t) ? null : t;
    }

    /** "Thành Phố" viết bằng escape để dấu không phụ thuộc encoding lúc biên dịch. */
    private static final String THANH_PHO = "Th\u00e0nh Ph\u1ed1";

    /**
     * Địa chỉ tra theo MST: {@code TP} → {@code Thành Phố}.
     * Ghép chuỗi trực tiếp, không dùng {@code appendReplacement} (replacement của Matcher dễ làm mất dấu).
     * Thiếu "Việt Nam" thì thêm đuôi {@code , Việt Nam.}; bỏ dấu chấm / phẩy sát cuối trước khi thêm
     * để không thành {@code ., Việt Nam.}.
     */
    static String normalizeAddress(String raw) {
        String t = clean(raw);
        if (t == null) {
            return null;
        }
        t = expandTp(t);
        if (VIETNAM.matcher(t).find()) {
            return t;
        }
        t = t.replaceFirst("[\\s.,]+$", "");
        return t.isEmpty() ? "Việt Nam." : t + ", Việt Nam.";
    }

    static String expandTp(String s) {
        Matcher m = TP_ABBR.matcher(s);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(s, last, m.start());
            int next = m.end();
            boolean glued = next < s.length() && Character.isLetter(s.charAt(next));
            sb.append(THANH_PHO);
            if (glued) {
                sb.append(' ');
            }
            last = m.end();
        }
        if (last == 0) {
            return s;
        }
        sb.append(s, last, s.length());
        return sb.toString().replaceAll("\\s{2,}", " ").trim();
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);
        out.put("code", code);
        out.put("message", message);
        return out;
    }
}
