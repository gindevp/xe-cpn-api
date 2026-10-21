package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cân tính phí lấy/giao tận nơi: từng kiện max(cân thật, D×R×C/5000), rồi cộng.
 * Kiện lấy từ [PKGKG]/[PKGDIM] trong ghi chú đơn; không có thì dùng weightKg / dimensionsText.
 */
public final class OrderPackageChargeWeight {

    public static final BigDecimal DIM_DIVISOR = new BigDecimal("5000");

    private static final Pattern KG = Pattern.compile("\\[PKGKG]([^\\[]*)\\[/PKGKG]");
    private static final Pattern DIM = Pattern.compile("\\[PKGDIM]([^\\[]*)\\[/PKGDIM]");

    private OrderPackageChargeWeight() {}

    /** Tổng cân (kg) để tra bảng phí tận nơi. */
    public static BigDecimal doorChargeKg(ShipmentOrder order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> weights = numbers(order.getNote(), KG);
        List<double[]> dims = dims(order.getNote());
        int n = Math.max(weights.size(), dims.size());
        if (n == 0) {
            return chargeKg(OrderMoney.nz(order.getWeightKg()), parseDimTriple(order.getDimensionsText()));
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            BigDecimal w = i < weights.size() ? weights.get(i) : BigDecimal.ZERO;
            double[] d = i < dims.size() ? dims.get(i) : null;
            sum = sum.add(chargeKg(w, d));
        }
        return sum;
    }

    /** max(thật, quy đổi), làm tròn lên 0,1 kg — cùng quy tắc FE {@code calcChargeWeight}. */
    static BigDecimal chargeKg(BigDecimal realKg, double[] dimCm) {
        BigDecimal real = OrderMoney.nz(realKg).max(BigDecimal.ZERO);
        BigDecimal dim = dimWeight(dimCm);
        BigDecimal raw = real.max(dim);
        if (raw.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return raw.multiply(BigDecimal.TEN).setScale(0, RoundingMode.UP).divide(BigDecimal.TEN, 1, RoundingMode.UNNECESSARY);
    }

    static BigDecimal dimWeight(double[] dimCm) {
        if (dimCm == null || dimCm.length < 3) {
            return BigDecimal.ZERO;
        }
        double d = dimCm[0];
        double r = dimCm[1];
        double c = dimCm[2];
        if (d <= 0 || r <= 0 || c <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(d)
            .multiply(BigDecimal.valueOf(r))
            .multiply(BigDecimal.valueOf(c))
            .divide(DIM_DIVISOR, 6, RoundingMode.HALF_UP);
    }

    private static List<BigDecimal> numbers(String note, Pattern pattern) {
        List<BigDecimal> out = new ArrayList<>();
        if (note == null) {
            return out;
        }
        Matcher m = pattern.matcher(note);
        if (!m.find()) {
            return out;
        }
        for (String part : m.group(1).split(",")) {
            try {
                BigDecimal v = new BigDecimal(part.trim());
                if (v.signum() >= 0) {
                    out.add(v);
                }
            } catch (NumberFormatException ignored) {
                // bỏ đoạn không phải số
            }
        }
        return out;
    }

    private static List<double[]> dims(String note) {
        List<double[]> out = new ArrayList<>();
        if (note == null) {
            return out;
        }
        Matcher m = DIM.matcher(note);
        if (!m.find()) {
            return out;
        }
        for (String part : m.group(1).split("\\|")) {
            out.add(parseDimTriple(part));
        }
        return out;
    }

    private static double[] parseDimTriple(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] p = raw.trim().toLowerCase(Locale.ROOT).split("x");
        if (p.length != 3) {
            return null;
        }
        try {
            double d = Double.parseDouble(p[0].trim());
            double r = Double.parseDouble(p[1].trim());
            double c = Double.parseDouble(p[2].trim());
            return d > 0 || r > 0 || c > 0 ? new double[] { d, r, c } : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
