package com.mycompany.myapp.service.storage;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Ảnh/file nặng lên MinIO; cột DB chỉ giữ {@code minio:{key}}.
 * Chưa cấu hình thì giữ nguyên data-URL để màn hình vẫn xem được.
 * Link trả cho trình duyệt là URL HTTPS của API (MinIO HTTP bị chặn mixed-content trên web).
 */
@Service
public class StoredMedia {

    static final String PREFIX = "minio:";
    private static final long LINK_TTL_SECONDS = 7L * 24 * 3600;

    private final IntegrationConfigRepository integrationConfigRepository;
    private final String envEndpoint;
    private final String envBucket;
    private final String envRegion;
    private final String envAccessKey;
    private final String envSecretKey;
    private final byte[] linkKey;

    public StoredMedia(
        IntegrationConfigRepository integrationConfigRepository,
        @Value("${cpn.minio.endpoint:}") String envEndpoint,
        @Value("${cpn.minio.bucket:cpn}") String envBucket,
        @Value("${cpn.minio.region:us-east-1}") String envRegion,
        @Value("${cpn.minio.access-key:}") String envAccessKey,
        @Value("${cpn.minio.secret-key:}") String envSecretKey,
        @Value("${jhipster.security.authentication.jwt.base64-secret}") String jwtSecret
    ) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.envEndpoint = envEndpoint;
        this.envBucket = envBucket;
        this.envRegion = envRegion;
        this.envAccessKey = envAccessKey;
        this.envSecretKey = envSecretKey;
        this.linkKey = jwtSecret == null ? new byte[0] : jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    /** Ghi data-URL lên MinIO. Giá trị khác (link, key, text) giữ nguyên. */
    public String store(String value, String folder) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith(PREFIX) || trimmed.startsWith("http:") || trimmed.startsWith("https:")) {
            return trimmed;
        }
        int marker = trimmed.indexOf(";base64,");
        if (!trimmed.startsWith("data:") || marker < 0) {
            return trimmed;
        }
        Settings settings = settings();
        if (!settings.complete()) {
            return trimmed;
        }
        String contentType = trimmed.substring("data:".length(), marker);
        byte[] body;
        try {
            body = Base64.getMimeDecoder().decode(trimmed.substring(marker + ";base64,".length()));
        } catch (IllegalArgumentException e) {
            return trimmed;
        }
        String key = safeFolder(folder) + "/" + UUID.randomUUID() + "." + extension(contentType);
        open(settings).put(key, body, contentType.isBlank() ? "application/octet-stream" : contentType);
        return PREFIX + key;
    }

    /** Nhiều data-URL cách nhau bởi xuống dòng (ảnh xác nhận phiếu thu). */
    public String storeLines(String value, String folder) {
        if (value == null || value.isBlank() || !value.contains("data:")) {
            return value;
        }
        String[] lines = value.split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(store(lines[i], folder));
        }
        return out.toString();
    }

    /** Link xem được trên web/app. data-URL và http(s) giữ nguyên. */
    public String expose(String stored) {
        if (stored == null || stored.isBlank() || !stored.startsWith(PREFIX)) {
            return stored;
        }
        String key = stored.substring(PREFIX.length());
        long exp = Instant.now().getEpochSecond() + LINK_TTL_SECONDS;
        String sig = sign(key, exp);
        return publicBase() + "/api/media/" + key + "?e=" + exp + "&s=" + sig;
    }

    public String exposeLines(String stored) {
        if (stored == null || stored.isBlank() || !stored.contains(PREFIX)) {
            return stored;
        }
        String[] lines = stored.split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(expose(lines[i].trim()));
        }
        return out.toString();
    }

    public byte[] readChecked(String key, String expRaw, String sig) {
        if (key == null || !key.matches("[a-z0-9][a-z0-9/_\\.-]{0,180}") || key.contains("..")) {
            throw new IllegalArgumentException("invalid media key");
        }
        long exp;
        try {
            exp = Long.parseLong(expRaw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid media expiry");
        }
        if (exp < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("media link expired");
        }
        String expected = sign(key, exp);
        if (sig == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("invalid media signature");
        }
        Settings settings = settings();
        if (!settings.complete()) {
            throw new IllegalStateException("minio is not configured");
        }
        return open(settings).get(key);
    }

    public boolean configured() {
        return settings().complete();
    }

    /** Ghi/đọc/xóa một file thử. Secret trống thì dùng secret đã lưu. */
    public String probe(String endpoint, String bucket, String region, String accessKey, String secretKey) {
        Settings saved = settings();
        String secret = notBlank(secretKey) ? secretKey.trim() : saved.secretKey;
        Settings settings = new Settings(
            notBlank(endpoint) ? endpoint.trim() : saved.endpoint,
            notBlank(bucket) ? bucket.trim() : saved.bucket,
            notBlank(region) ? region.trim() : saved.region,
            notBlank(accessKey) ? accessKey.trim() : saved.accessKey,
            secret
        );
        if (!settings.complete()) {
            throw new IllegalArgumentException("Điền đủ endpoint, bucket, access key và secret");
        }
        MinioObjectStore store = open(settings);
        if (!store.bucketExists()) {
            throw new IllegalStateException("Không thấy bucket " + settings.bucket);
        }
        String key = "probe/connectivity-" + UUID.randomUUID() + ".txt";
        byte[] body = "cpn-minio-ok".getBytes(StandardCharsets.UTF_8);
        try {
            store.put(key, body, "text/plain");
            byte[] back = store.get(key);
            if (!MessageDigest.isEqual(body, back)) {
                throw new IllegalStateException("Đọc lại file thử không khớp");
            }
        } finally {
            try {
                store.delete(key);
            } catch (RuntimeException ignored) {
                // file thử đã xóa hoặc chưa ghi được
            }
        }
        return "Kết nối MinIO OK, bucket " + settings.bucket;
    }

    private Settings settings() {
        IntegrationConfig cfg = integrationConfigRepository.findAll().stream().findFirst().orElse(null);
        String endpoint = cfg != null && notBlank(cfg.getMinioEndpoint()) ? cfg.getMinioEndpoint().trim() : blankToEmpty(envEndpoint);
        String bucket = cfg != null && notBlank(cfg.getMinioBucket()) ? cfg.getMinioBucket().trim() : blankToEmpty(envBucket);
        String region = cfg != null && notBlank(cfg.getMinioRegion()) ? cfg.getMinioRegion().trim() : blankToEmpty(envRegion);
        String access = cfg != null && notBlank(cfg.getMinioAccessKey()) ? cfg.getMinioAccessKey().trim() : blankToEmpty(envAccessKey);
        String secret = cfg != null && notBlank(cfg.getMinioSecretKey()) ? cfg.getMinioSecretKey() : blankToEmpty(envSecretKey);
        if (!notBlank(region)) {
            region = "us-east-1";
        }
        if (!notBlank(bucket)) {
            bucket = "cpn";
        }
        return new Settings(endpoint, bucket, region, access, secret);
    }

    private static MinioObjectStore open(Settings settings) {
        return new MinioObjectStore(settings.endpoint, settings.bucket, settings.region, settings.accessKey, settings.secretKey);
    }

    private String sign(String key, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(linkKey, "HmacSHA256"));
            byte[] raw = mac.doFinal((key + "\n" + exp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("media signature failed", e);
        }
    }

    private static String publicBase() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpServletRequest request = attrs.getRequest();
                return ServletUriComponentsBuilder.fromRequest(request).replacePath(null).replaceQuery(null).build().toUriString();
            }
        } catch (RuntimeException ignored) {
            // ngoài request HTTP
        }
        return "";
    }

    private static String safeFolder(String folder) {
        String raw = folder == null ? "file" : folder.trim().toLowerCase(Locale.ROOT);
        if (!raw.matches("[a-z0-9][a-z0-9-]{0,40}")) {
            return "file";
        }
        return raw;
    }

    private static String extension(String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("png")) return "png";
        if (type.contains("webp")) return "webp";
        if (type.contains("gif")) return "gif";
        if (type.contains("svg")) return "svg";
        if (type.contains("pdf")) return "pdf";
        if (type.contains("jpeg") || type.contains("jpg")) return "jpg";
        return "bin";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record Settings(String endpoint, String bucket, String region, String accessKey, String secretKey) {
        boolean complete() {
            return notBlank(endpoint) && notBlank(bucket) && notBlank(accessKey) && notBlank(secretKey);
        }
    }
}
