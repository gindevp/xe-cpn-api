package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.FileResult;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.Result;
import com.mycompany.myapp.service.partner.VtechAutoCallClient;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Màn Tích hợp → Auto Call (HHVN Tech): test kết nối + quản lý file thông báo giao/hoan. */
@Service
@Transactional(readOnly = true)
public class AutoCallConfigService {

    static final Set<String> CALL_TYPES = Set.of("giao", "hoan");
    static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a");
    static final long MAX_AUDIO_BYTES = 5L * 1024 * 1024;

    private final IntegrationConfigRepository integrationConfigRepository;
    private final HhvnAutoCallClient client;
    private final VtechAutoCallClient vtechClient;

    public AutoCallConfigService(
        IntegrationConfigRepository integrationConfigRepository,
        HhvnAutoCallClient client,
        VtechAutoCallClient vtechClient
    ) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.client = client;
        this.vtechClient = vtechClient;
    }

    /** Gọi GET /audios — nhẹ nhất, xác nhận key + IP whitelist. Key/baseUrl trong body chỉ dùng để thử, không lưu. */
    public Map<String, Object> test(Map<String, String> override) {
        IntegrationConfig cfg = current();
        String provider = firstNonBlank(override != null ? override.get("autocallProvider") : null, cfg.getAutocallActiveProvider());
        if (IntegrationConfig.PROVIDER_VTECH.equalsIgnoreCase(provider)) {
            return testVtech(cfg, override);
        }
        String apiKey = firstNonBlank(override != null ? override.get("autocallApiKey") : null, cfg.getAutocallApiKey());
        String baseUrl = firstNonBlank(override != null ? override.get("autocallBaseUrl") : null, cfg.getAutocallBaseUrl());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("baseUrl", HhvnAutoCallClient.normalizeBaseUrl(baseUrl));
        if (apiKey == null) {
            out.put("ok", false);
            out.put("code", "API_KEY_MISSING");
            out.put("message", "Chưa có API key Auto Call — nhập key rồi Test");
            return stamp(out);
        }
        out.put("mode", apiKey.startsWith("xk_test_") ? "SANDBOX" : apiKey.startsWith("xk_live_") ? "LIVE" : "UNKNOWN");
        Result r = client.getAudios(baseUrl, apiKey.trim());
        putResult(out, r);
        if (r.ok()) {
            out.put("message", "Kết nối HHVN OK");
        }
        return stamp(out);
    }

    /** Import danh sách rỗng: Vtech kiểm tra key trước validation, nên 400 = key đúng, 401 = key sai; không gọi ai. */
    private Map<String, Object> testVtech(IntegrationConfig cfg, Map<String, String> override) {
        String apiKey = firstNonBlank(override != null ? override.get("autocallVtechApiKey") : null, cfg.getAutocallVtechApiKey());
        String baseUrl = firstNonBlank(override != null ? override.get("autocallVtechBaseUrl") : null, cfg.getAutocallVtechBaseUrl());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("provider", IntegrationConfig.PROVIDER_VTECH);
        out.put("baseUrl", VtechAutoCallClient.normalizeBaseUrl(baseUrl));
        if (apiKey == null) {
            out.put("ok", false);
            out.put("code", "API_KEY_MISSING");
            out.put("message", "Chưa có API key Vtech — nhập key rồi Test");
            return stamp(out);
        }
        out.put("mode", "LIVE");
        Result r = vtechClient.testConnection(baseUrl, apiKey.trim());
        out.put("ok", r.ok());
        out.put("httpStatus", r.httpStatus());
        if (r.ok()) {
            out.put("message", "Kết nối Vtech OK (key hợp lệ)");
        } else {
            out.put("code", r.code());
            out.put("message", "INVALID_API_KEY".equals(r.code()) ? "API key Vtech không hợp lệ" : humanMessage(r.code(), r.message()));
        }
        return stamp(out);
    }

    public Map<String, Object> listAudios() {
        IntegrationConfig cfg = requireConfigured();
        Map<String, Object> out = new LinkedHashMap<>();
        putResult(out, client.getAudios(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey()));
        return out;
    }

    public Map<String, Object> uploadAudio(String type, MultipartFile file) {
        String t = requireType(type);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn file audio");
        }
        if (file.getSize() > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File audio lớn hơn 5 MB");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio";
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!AUDIO_EXTENSIONS.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ nhận file MP3, WAV hoặc M4A");
        }
        IntegrationConfig cfg = requireConfigured();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đọc được file audio");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        putResult(out, client.uploadAudio(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), t, name, file.getContentType(), bytes));
        return out;
    }

    public Map<String, Object> deleteAudio(String type) {
        String t = requireType(type);
        IntegrationConfig cfg = requireConfigured();
        Map<String, Object> out = new LinkedHashMap<>();
        putResult(out, client.deleteAudio(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), t));
        return out;
    }

    public FileResult downloadAudio(String type) {
        String t = requireType(type);
        IntegrationConfig cfg = requireConfigured();
        return client.downloadAudio(cfg.getAutocallBaseUrl(), cfg.getAutocallApiKey(), t);
    }

    /** Thông báo tiếng Việt theo mã lỗi HHVN (§8 tài liệu tích hợp). */
    public static String humanMessage(String code, String fallback) {
        if (code == null) {
            return fallback;
        }
        return switch (code) {
            case "INVALID_API_KEY" -> "API key sai hoặc đã bị thu hồi (INVALID_API_KEY)";
            case "IP_NOT_ALLOWED" -> "IP máy chủ chưa được HHVN whitelist (IP_NOT_ALLOWED)";
            case "PERMISSION_DENIED" -> "Tài khoản chưa được HHVN bật loại cuộc gọi này (PERMISSION_DENIED)";
            case "RATE_LIMIT" -> "Vượt giới hạn HHVN (60 request/phút, 10 lần tải audio/giờ) — thử lại sau";
            case "UNSUPPORTED_AUDIO_FORMAT" -> "File không phải MP3/WAV/M4A hoặc bị hỏng";
            case "AUDIO_DURATION_INVALID" -> "File audio phải dài từ 2 đến 60 giây";
            case "FILE_TOO_LARGE" -> "File audio lớn hơn 5 MB";
            default -> fallback != null ? fallback + " (" + code + ")" : code;
        };
    }

    private void putResult(Map<String, Object> out, Result r) {
        out.put("ok", r.ok());
        out.put("httpStatus", r.httpStatus());
        if (r.ok()) {
            if (r.body() != null) {
                if (r.body().has("audios")) out.put("audios", r.body().get("audios"));
                if (r.body().has("audio")) out.put("audio", r.body().get("audio"));
            }
            return;
        }
        out.put("code", r.code());
        out.put("message", humanMessage(r.code(), r.message()));
        if ("IP_NOT_ALLOWED".equals(r.code())) {
            out.put("serverOutboundIp", lookupOutboundIp());
        }
    }

    private IntegrationConfig current() {
        return integrationConfigRepository.findAll().stream().findFirst().orElseGet(IntegrationConfig::new);
    }

    private IntegrationConfig requireConfigured() {
        IntegrationConfig cfg = current();
        if (!cfg.isAutocallApiKeyConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa lưu API key Auto Call");
        }
        return cfg;
    }

    private static String requireType(String type) {
        String t = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!CALL_TYPES.contains(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type phải là giao hoặc hoan");
        }
        return t;
    }

    private static Map<String, Object> stamp(Map<String, Object> out) {
        out.put("testedBy", SecurityUtils.getCurrentUserLogin().orElse("system"));
        out.put("testedAt", Instant.now().toString());
        return out;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    /** IP ra của BE — để gửi HHVN whitelist khi bị IP_NOT_ALLOWED. */
    private static String lookupOutboundIp() {
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.ipify.org")).timeout(Duration.ofSeconds(4)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            String ip = res.body() != null ? res.body().trim() : "";
            return res.statusCode() == 200 && ip.length() <= 45 ? ip : null;
        } catch (Exception e) {
            return null;
        }
    }
}
