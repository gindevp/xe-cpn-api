package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Điểm lộ trình của VP: một hoặc nhiều mã (vế tỉnh hoặc vế Hà Nội), lưu "BC,HD" theo thứ tự ưu tiên.
 * VP kiêm nhiều điểm → lộ trình chọn theo điểm đầu tiên có lộ trình đang bật tới đầu kia.
 */
public final class OfficeItineraryPoints {

    public static final String ENTITY = "office";

    /** Giới hạn cột office.itinerary_point. */
    private static final int MAX_STORED_LENGTH = 16;

    /** Mã lưu DB → nhãn hiển thị. */
    public static final Map<String, String> PROVINCE = Map.of(
        "ND",
        "NĐ",
        "TB",
        "TB",
        "YB",
        "YB",
        "PT",
        "PT",
        "TC",
        "TC",
        "NB",
        "NB",
        "VT",
        "VT"
    );

    public static final Map<String, String> HANOI = Map.of("BC", "BC", "GA", "GA", "HD", "HĐ", "PHOCO", "PHOCO");

    private OfficeItineraryPoints() {}

    /** Bắt buộc, chuẩn hóa về danh sách mã không dấu ("ND", "BC,HD"…). */
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
        Set<String> codes = new LinkedHashSet<>();
        for (String part : raw.split("[,;/\\s]+")) {
            if (part.isBlank()) {
                continue;
            }
            String folded = fold(part);
            if (!PROVINCE.containsKey(folded) && !HANOI.containsKey(folded)) {
                throw new BadRequestAlertException("Điểm lộ trình không hợp lệ: " + part.trim(), ENTITY, "itineraryPointInvalid");
            }
            codes.add(folded);
        }
        if (codes.isEmpty()) {
            return null;
        }
        String joined = String.join(",", codes);
        if (joined.length() > MAX_STORED_LENGTH) {
            throw new BadRequestAlertException("Văn phòng gắn quá nhiều điểm lộ trình", ENTITY, "itineraryPointTooMany");
        }
        return joined;
    }

    /** Mã điểm của VP theo thứ tự ưu tiên; bỏ qua mã lạ thay vì báo lỗi (dữ liệu cũ). */
    public static List<String> pointsOf(Office office) {
        return office == null ? List.of() : split(office.getItineraryPoint());
    }

    public static List<String> split(String stored) {
        List<String> out = new ArrayList<>();
        if (stored == null || stored.isBlank()) {
            return out;
        }
        for (String part : stored.split("[,;/\\s]+")) {
            if (part.isBlank()) {
                continue;
            }
            String folded = fold(part);
            if (!out.contains(folded)) {
                out.add(folded);
            }
        }
        return out;
    }

    private static String fold(String raw) {
        return Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('Đ', 'D')
            .replace('đ', 'd')
            .toUpperCase();
    }
}
