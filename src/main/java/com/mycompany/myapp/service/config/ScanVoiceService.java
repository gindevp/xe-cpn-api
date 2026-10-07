package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.ScanVoice;
import com.mycompany.myapp.repository.ScanVoiceRepository;
import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.StaffAccessService;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ScanVoiceService {

    public static final Set<String> TYPES = Set.of("ok", "err");
    private static final int MAX_BYTES = 500 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("mp3", "wav", "m4a", "aac", "ogg");

    private final ScanVoiceRepository repository;
    private final StaffAccessService staffAccessService;

    public ScanVoiceService(ScanVoiceRepository repository, StaffAccessService staffAccessService) {
        this.repository = repository;
        this.staffAccessService = staffAccessService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        Map<String, Map<String, Object>> byType = new LinkedHashMap<>();
        for (String type : List.of("ok", "err")) {
            byType.put(type, emptyMeta(type));
        }
        for (ScanVoiceRepository.ScanVoiceMeta row : repository.listMeta()) {
            byType.put(row.getVoiceType(), toMeta(row));
        }
        return new ArrayList<>(byType.values());
    }

    @Transactional(readOnly = true)
    public Optional<ScanVoice> find(String typeRaw) {
        return repository.findById(requireType(typeRaw));
    }

    public Map<String, Object> upload(String typeRaw, MultipartFile file) {
        staffAccessService.requireScreenWrite(ScreenKey.BAO_TRI);
        String type = requireType(typeRaw);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn file giọng");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File giọng tối đa 500 KB");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "voice." + type;
        String ext = extension(name);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ nhận mp3, wav, m4a, aac, ogg");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đọc được file giọng");
        }
        if (bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File giọng trống");
        }
        String contentType = contentTypeOf(file.getContentType(), ext);
        ScanVoice row = repository.findById(type).orElseGet(ScanVoice::new);
        row.setVoiceType(type);
        row.setContentType(contentType);
        row.setFileName(safeName(name));
        row.setByteSize(bytes.length);
        row.setContent(bytes);
        row.setEtag(sha256(bytes));
        row.setUpdatedAt(Instant.now());
        repository.save(row);
        return toMeta(row);
    }

    public void delete(String typeRaw) {
        staffAccessService.requireScreenWrite(ScreenKey.BAO_TRI);
        String type = requireType(typeRaw);
        repository.deleteById(type);
    }

    private static String requireType(String raw) {
        String t = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!TYPES.contains(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại giọng phải là ok hoặc err");
        }
        return t;
    }

    private static Map<String, Object> emptyMeta(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("configured", false);
        m.put("etag", null);
        m.put("contentType", null);
        m.put("fileName", null);
        m.put("byteSize", 0);
        m.put("updatedAt", null);
        return m;
    }

    private static Map<String, Object> toMeta(ScanVoiceRepository.ScanVoiceMeta row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", row.getVoiceType());
        m.put("configured", true);
        m.put("etag", row.getEtag());
        m.put("contentType", row.getContentType());
        m.put("fileName", row.getFileName());
        m.put("byteSize", row.getByteSize());
        m.put("updatedAt", row.getUpdatedAt() != null ? row.getUpdatedAt().toString() : null);
        return m;
    }

    private static Map<String, Object> toMeta(ScanVoice row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", row.getVoiceType());
        m.put("configured", true);
        m.put("etag", row.getEtag());
        m.put("contentType", row.getContentType());
        m.put("fileName", row.getFileName());
        m.put("byteSize", row.getByteSize());
        m.put("updatedAt", row.getUpdatedAt() != null ? row.getUpdatedAt().toString() : null);
        return m;
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return "";
        return name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String contentTypeOf(String reported, String ext) {
        if (reported != null && !reported.isBlank() && !"application/octet-stream".equalsIgnoreCase(reported)) {
            return reported.trim();
        }
        return switch (ext) {
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "aac" -> "audio/aac";
            case "ogg" -> "audio/ogg";
            default -> "audio/mpeg";
        };
    }

    private static String safeName(String name) {
        String n = name.trim();
        return n.length() > 255 ? n.substring(0, 255) : n;
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("sha256 unavailable", e);
        }
    }
}
