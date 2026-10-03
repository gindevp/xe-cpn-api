package com.mycompany.myapp.service.invoice;

/**
 * Kiểm tra MST Việt Nam: 10 số / 13 số chi nhánh theo checksum Thông tư 105/2020/TT-BTC,
 * hoặc 12 số CCCD chủ hộ kinh doanh / cá nhân (Thông tư 86/2024, từ 01/07/2025 — không có checksum,
 * 3 số đầu là mã tỉnh 001–096). MST điền bừa / sai checksum → không hợp lệ (MISA cũng từ chối).
 */
public final class VietnamTaxCode {

    private static final int[] WEIGHTS = { 31, 29, 23, 19, 17, 13, 7, 5, 3 };

    private VietnamTaxCode() {}

    /** Bỏ khoảng trắng, dấu chấm, gạch ngang. */
    public static String compact(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("[\\s.\\-]", "");
    }

    public static boolean isValid(String raw) {
        String number = compact(raw);
        if (number.isEmpty() || !number.chars().allMatch(Character::isDigit)) {
            return false;
        }
        if (number.length() == 12) {
            return isPersonalId(number);
        }
        if (number.length() != 10 && number.length() != 13) {
            return false;
        }
        // 7 số giữa không được toàn 0
        if ("0000000".equals(number.substring(2, 9))) {
            return false;
        }
        // Chi nhánh: 3 số cuối 001–999
        if (number.length() == 13 && "000".equals(number.substring(10))) {
            return false;
        }
        int check = calcCheckDigit(number);
        if (check < 0 || check > 9) {
            return false; // tổng % 11 == 0 → check digit = 10 — không cấp
        }
        return number.charAt(9) - '0' == check;
    }

    /** 12 số CCCD: mã tỉnh 001–096. */
    static boolean isPersonalId(String number) {
        int province = Integer.parseInt(number.substring(0, 3));
        return province >= 1 && province <= 96;
    }

    /** Check digit của 9 số đầu; trả về 10 nếu cấu trúc không hợp lệ. */
    static int calcCheckDigit(String digits) {
        int total = 0;
        for (int i = 0; i < 9; i++) {
            total += WEIGHTS[i] * (digits.charAt(i) - '0');
        }
        return 10 - (total % 11);
    }

    /** Chuẩn hoá lưu DB: 10 số, 10-3, hoặc 12 số CCCD. */
    public static String normalize(String raw) {
        String number = compact(raw);
        if (!isValid(number)) {
            return number;
        }
        if (number.length() == 13) {
            return number.substring(0, 10) + "-" + number.substring(10);
        }
        return number;
    }
}
