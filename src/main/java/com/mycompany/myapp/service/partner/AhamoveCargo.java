package com.mycompany.myapp.service.partner;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kiện gửi Ahamove. Giá xe máy không đổi theo số đo; chỉ cộng phí khi khai request BULKY đúng bậc
 * (HAN-BIKE: tiêu chuẩn 50×40×50 cm / 30 kg miễn phí, rồi TIER_2/3/4).
 */
public final class AhamoveCargo {

    public record Pkg(double weightKg, Double lengthCm, Double widthCm, Double heightCm) {}

    /** Cạnh đã sắp giảm dần + cân nặng tối đa. Bậc 0 là tiêu chuẩn, không gửi request. */
    private static final double[][] BOX = { { 50, 50, 40, 30 }, { 60, 60, 50, 40 }, { 70, 70, 60, 60 }, { 90, 90, 70, 80 } };
    private static final String[] TIER = { null, "TIER_2", "TIER_3", "TIER_4" };

    private static final Pattern KG = Pattern.compile("\\[PKGKG]([^\\[]*)\\[/PKGKG]");
    private static final Pattern DIM = Pattern.compile("\\[PKGDIM]([^\\[]*)\\[/PKGDIM]");

    private final List<Pkg> packages;
    private final String tier;
    private final boolean oversize;

    private AhamoveCargo(List<Pkg> packages, String tier, boolean oversize) {
        this.packages = packages;
        this.tier = tier;
        this.oversize = oversize;
    }

    public static AhamoveCargo from(ShipmentOrder order) {
        String note = order != null ? order.getNote() : null;
        List<Double> weights = numbers(note, KG);
        List<double[]> dims = dims(note);
        int n = Math.max(weights.size(), dims.size());
        if (n == 0 && order != null && order.getWeightKg() != null && order.getWeightKg().signum() > 0) {
            weights.add(order.getWeightKg().doubleValue());
            n = 1;
        }
        List<Pkg> pkgs = new ArrayList<>();
        int need = 0;
        boolean over = false;
        for (int i = 0; i < n; i++) {
            double w = i < weights.size() ? weights.get(i) : 0;
            double[] d = i < dims.size() ? dims.get(i) : null;
            Pkg pkg = new Pkg(w, d == null ? null : d[0], d == null ? null : d[1], d == null ? null : d[2]);
            pkgs.add(pkg);
            int t = tierIndex(pkg);
            if (t < 0) {
                over = true;
            } else {
                need = Math.max(need, t);
            }
        }
        return new AhamoveCargo(pkgs, over ? null : TIER[need], over);
    }

    public List<Pkg> packages() {
        return packages;
    }

    /** {@code TIER_2}..{@code TIER_4}, hoặc null khi nằm trong khổ tiêu chuẩn. */
    public String tier() {
        return tier;
    }

    /**
     * Bậc NV chọn lúc bàn giao. Rỗng / {@code STANDARD} = tiêu chuẩn, không gửi BULKY.
     * {@code TIER_2}..{@code TIER_4} gửi đúng bậc Ahamove.
     */
    public AhamoveCargo withTierChoice(String raw) {
        return new AhamoveCargo(packages, parseTierChoice(raw), oversize);
    }

    public static String parseTierChoice(String raw) {
        if (raw == null || raw.isBlank() || "STANDARD".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        String t = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if ("TIER_2".equals(t) || "TIER_3".equals(t) || "TIER_4".equals(t)) {
            return t;
        }
        throw new BadRequestAlertException("Phụ phí hàng cồng kềnh không hợp lệ", "ahamove", "ahamoveBulkyTier");
    }

    public void assertFitsBike() {
        if (oversize) {
            throw new BadRequestAlertException("Kiện vượt xe máy Ahamove (tối đa 90×70×90 cm, 80 kg/kiện)", "ahamove", "ahamoveBulkyOver");
        }
    }

    /** -1 khi vượt TIER_4. */
    static int tierIndex(Pkg pkg) {
        double[] sides = sides(pkg);
        boolean hasDim = sides[0] > 0;
        for (int i = 0; i < BOX.length; i++) {
            boolean dimOk = !hasDim || (sides[0] <= BOX[i][0] && sides[1] <= BOX[i][1] && sides[2] <= BOX[i][2]);
            if (dimOk && pkg.weightKg() <= BOX[i][3] + 0.001) {
                return i;
            }
        }
        return -1;
    }

    private static double[] sides(Pkg pkg) {
        double[] s = {
            pkg.lengthCm() == null ? 0 : pkg.lengthCm(),
            pkg.widthCm() == null ? 0 : pkg.widthCm(),
            pkg.heightCm() == null ? 0 : pkg.heightCm(),
        };
        Arrays.sort(s);
        return new double[] { s[2], s[1], s[0] };
    }

    private static List<Double> numbers(String note, Pattern pattern) {
        List<Double> out = new ArrayList<>();
        if (note == null) {
            return out;
        }
        Matcher m = pattern.matcher(note);
        if (!m.find()) {
            return out;
        }
        for (String part : m.group(1).split(",")) {
            try {
                double v = Double.parseDouble(part.trim());
                if (v >= 0) {
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
            String[] p = part.trim().split("x");
            if (p.length != 3) {
                out.add(null);
                continue;
            }
            try {
                double d = Double.parseDouble(p[0].trim());
                double r = Double.parseDouble(p[1].trim());
                double c = Double.parseDouble(p[2].trim());
                out.add(d > 0 || r > 0 || c > 0 ? new double[] { d, r, c } : null);
            } catch (NumberFormatException e) {
                out.add(null);
            }
        }
        return out;
    }
}
