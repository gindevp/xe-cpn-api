package com.mycompany.myapp.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Đọc số tiền VNĐ thành chữ: 2440000 → "Hai triệu bốn trăm bốn mươi nghìn đồng". */
final class VietnameseMoneyWords {

    private static final String[] DIGITS = { "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín" };
    private static final String[] UNITS = { "", " nghìn", " triệu", " tỷ" };

    private VietnameseMoneyWords() {}

    static String of(BigDecimal amount) {
        long n = amount == null ? 0 : amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (n == 0) {
            return "Không đồng";
        }
        String words = (n < 0 ? "âm " : "") + read(Math.abs(n)) + " đồng";
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /** Đọc theo nhóm 3 chữ số; quá nghìn tỷ thì lặp "tỷ". */
    private static String read(long n) {
        if (n >= 1_000_000_000_000L) {
            String high = read(n / 1_000_000_000L);
            long rest = n % 1_000_000_000L;
            return rest == 0 ? high + " tỷ" : high + " tỷ " + readGroups(rest, true);
        }
        return readGroups(n, false);
    }

    private static String readGroups(long n, boolean padFirst) {
        int[] groups = new int[4];
        int top = -1;
        for (int i = 0; i < 4 && n > 0; i++) {
            groups[i] = (int) (n % 1000);
            n /= 1000;
            if (groups[i] > 0) {
                top = Math.max(top, i);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = top; i >= 0; i--) {
            if (groups[i] == 0) {
                continue;
            }
            boolean full = padFirst || i < top;
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(readTriple(groups[i], full)).append(UNITS[i]);
        }
        return sb.toString();
    }

    /** full = nhóm không đứng đầu → đọc đủ "không trăm", "linh". */
    private static String readTriple(int n, boolean full) {
        int h = n / 100;
        int t = (n / 10) % 10;
        int u = n % 10;
        StringBuilder sb = new StringBuilder();
        if (h > 0 || full) {
            sb.append(DIGITS[h]).append(" trăm");
        }
        if (t == 0) {
            if (u > 0) {
                if (!sb.isEmpty()) {
                    sb.append(" linh");
                }
                sb.append(sb.isEmpty() ? "" : " ").append(DIGITS[u]);
            }
            return sb.toString();
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(t == 1 ? "mười" : DIGITS[t] + " mươi");
        if (u == 0) {
            return sb.toString();
        }
        sb.append(' ');
        if (u == 1 && t > 1) {
            sb.append("mốt");
        } else if (u == 5) {
            sb.append("lăm");
        } else if (u == 4 && t > 1) {
            sb.append("tư");
        } else {
            sb.append(DIGITS[u]);
        }
        return sb.toString();
    }
}
