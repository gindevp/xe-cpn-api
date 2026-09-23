package com.mycompany.myapp.service.trip;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Biển số VN: so khớp theo dạng chuẩn hóa (bỏ "-", ".", khoảng trắng; viết hoa) để
 * {@code 29H-885.24} và {@code 29H88524} là cùng một xe.
 */
public final class VehiclePlates {

    /** 2 số tỉnh + 1–2 chữ seri + 4–6 số (gồm cả seri có số, vd 29H1xxxx). */
    private static final Pattern VN_PLATE = Pattern.compile("^\\d{2}[A-Z]{1,2}\\d{4,6}$");

    private VehiclePlates() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String raw) {
        return VN_PLATE.matcher(normalize(raw)).matches();
    }
}
