package com.mycompany.myapp.service.invoice;

/**
 * Kiểm tra MST Việt Nam (10 số hoặc 13 số chi nhánh) theo checksum Thông tư 105/2020/TT-BTC.
 * MST điền bừa / sai checksum → không hợp lệ (MISA cũng từ chối).
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
        if (number.length() != 10 && number.length() != 13) {
            return false;
        }
        if (!number.chars().allMatch(Character::isDigit)) {
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

    /** Check digit của 9 số đầu; trả về 10 nếu cấu trúc không hợp lệ. */
    static int calcCheckDigit(String digits) {
        int total = 0;
        for (int i = 0; i < 9; i++) {
            total += WEIGHTS[i] * (digits.charAt(i) - '0');
        }
        return 10 - (total % 11);
    }

    /** Chuẩn hoá lưu DB: 10 số hoặc 10-3. */
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
