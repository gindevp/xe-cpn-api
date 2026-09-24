package com.mycompany.myapp.service;

import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.text.Normalizer;
import java.util.Map;

/** Mỗi VP gắn đúng một điểm lộ trình: vế tỉnh hoặc vế Hà Nội. */
public final class OfficeItineraryPoints {

    public static final String ENTITY = "office";

    /** Mã lưu DB → nhãn hiển thị. */
    public static final Map<String, String> PROVINCE = Map.of("ND", "NĐ", "TB", "TB", "YB", "YB", "PT", "PT", "TC", "TC", "VT", "VT");

    public static final Map<String, String> HANOI = Map.of("BC", "BC", "GA", "GA", "HD", "HĐ", "PHOCO", "PHOCO");

    private OfficeItineraryPoints() {}

    /** Bắt buộc, chuẩn hóa về mã không dấu (ND, HD, PHOCO…). */
    public static String require(String raw) {
        String code = normalize(raw);
        if (code == null) {
            throw new BadRequestAlertException("Chọn điểm lộ trình của văn phòng", ENTITY, "itineraryPointRequired");
        }
        return code;
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String folded = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase();
        if (PROVINCE.containsKey(folded) || HANOI.containsKey(folded)) {
            return folded;
        }
        throw new BadRequestAlertException("Điểm lộ trình không hợp lệ: " + raw.trim(), ENTITY, "itineraryPointInvalid");
    }
}
