package com.mycompany.myapp.service.invoice;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tỉnh/TP sau sáp nhập 01/07/2025 (63 → 34, Nghị quyết 202/2025/QH15): nhận tên tỉnh cũ hoặc mới trong một đoạn địa chỉ
 * và trả tên tỉnh mới để ghi lên HĐĐT.
 */
public final class ProvinceMerger {

    /** Khoá = tên không dấu, chữ thường; giá trị = tên tỉnh mới có dấu. */
    private static final Map<String, String> TO_NEW = build();

    /** Khoá dài trước để "ha nam" không bắt nhầm trong chuỗi chứa tỉnh dài hơn. */
    private static final List<String> KEYS_LONGEST_FIRST = sortedKeys();

    private ProvinceMerger() {}

    /** Tên tỉnh mới nếu {@code text} đúng là một tên tỉnh (bỏ tiền tố Tỉnh/TP); không khớp → rỗng. */
    public static String exact(String text) {
        String key = stripPrefix(normalize(text));
        return key.isEmpty() ? "" : TO_NEW.getOrDefault(key, "");
    }

    /** Tên tỉnh mới xuất hiện trong {@code text} (địa chỉ/tên VP); ưu tiên đoạn cuối sau dấu phẩy. */
    public static String find(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] parts = text.split("[,\\-–]");
        for (int i = parts.length - 1; i >= 0; i--) {
            String hit = exact(parts[i]);
            if (!hit.isEmpty()) {
                return hit;
            }
        }
        String norm = " " + normalize(text).replaceAll("[^a-z0-9]+", " ") + " ";
        for (String key : KEYS_LONGEST_FIRST) {
            if (norm.contains(" " + key + " ")) {
                return TO_NEW.get(key);
            }
        }
        return "";
    }

    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        s = s.replaceAll("[.]", " ").replaceAll("\\s+", " ").trim();
        return s;
    }

    private static String stripPrefix(String norm) {
        return norm.replaceFirst("^(tinh|thanh pho|tp)\\s+", "").trim();
    }

    private static Map<String, String> build() {
        Map<String, String> m = new LinkedHashMap<>();
        // 11 tỉnh/TP giữ nguyên
        put(m, "Hà Nội", "ha noi", "hn");
        put(m, "Huế", "thua thien hue", "hue");
        put(m, "Lai Châu", "lai chau");
        put(m, "Điện Biên", "dien bien");
        put(m, "Sơn La", "son la");
        put(m, "Lạng Sơn", "lang son");
        put(m, "Quảng Ninh", "quang ninh");
        put(m, "Thanh Hóa", "thanh hoa");
        put(m, "Nghệ An", "nghe an");
        put(m, "Hà Tĩnh", "ha tinh");
        put(m, "Cao Bằng", "cao bang");
        // 23 tỉnh/TP mới sau sáp nhập
        put(m, "Tuyên Quang", "tuyen quang", "ha giang");
        put(m, "Lào Cai", "lao cai", "yen bai");
        put(m, "Thái Nguyên", "thai nguyen", "bac kan", "bac can");
        put(m, "Phú Thọ", "phu tho", "vinh phuc", "hoa binh");
        put(m, "Bắc Ninh", "bac ninh", "bac giang");
        put(m, "Hưng Yên", "hung yen", "thai binh");
        put(m, "Hải Phòng", "hai phong", "hai duong");
        put(m, "Ninh Bình", "ninh binh", "ha nam", "nam dinh");
        put(m, "Quảng Trị", "quang tri", "quang binh");
        put(m, "Đà Nẵng", "da nang", "quang nam");
        put(m, "Quảng Ngãi", "quang ngai", "kon tum");
        put(m, "Gia Lai", "gia lai", "binh dinh");
        put(m, "Khánh Hòa", "khanh hoa", "ninh thuan");
        put(m, "Lâm Đồng", "lam dong", "dak nong", "dac nong", "binh thuan");
        put(m, "Đắk Lắk", "dak lak", "dac lac", "phu yen");
        put(m, "Hồ Chí Minh", "ho chi minh", "hcm", "sai gon", "binh duong", "ba ria vung tau", "vung tau");
        put(m, "Đồng Nai", "dong nai", "binh phuoc");
        put(m, "Tây Ninh", "tay ninh", "long an");
        put(m, "Cần Thơ", "can tho", "soc trang", "hau giang");
        put(m, "Vĩnh Long", "vinh long", "ben tre", "tra vinh");
        put(m, "Đồng Tháp", "dong thap", "tien giang");
        put(m, "Cà Mau", "ca mau", "bac lieu");
        put(m, "An Giang", "an giang", "kien giang");
        return m;
    }

    private static void put(Map<String, String> m, String newName, String... aliases) {
        for (String alias : aliases) {
            m.put(normalize(alias), newName);
        }
    }

    private static List<String> sortedKeys() {
        List<String> keys = new ArrayList<>(TO_NEW.keySet());
        keys.removeIf(k -> k.length() < 3);
        keys.sort(Comparator.comparingInt(String::length).reversed());
        return keys;
    }
}
