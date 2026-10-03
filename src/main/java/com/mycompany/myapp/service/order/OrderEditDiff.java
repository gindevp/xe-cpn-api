package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chụp các trường người dùng sửa được trên đơn để ghi lịch sử "sửa cái gì: cũ → mới". */
final class OrderEditDiff {

    private static final int MAX_VALUE = 60;

    /** Tag dữ liệu kiện trong note mà người dùng sửa được; tag hệ thống (WHIN, WHOUT, DRVSIGN) không tính. */
    private static final String[][] NOTE_TAGS = {
        { "LOAI", "Loại hàng" },
        { "KIEN", "Tên hàng" },
        { "TENHANG", "Tên hàng" },
        { "CUOC", "Cước từng kiện" },
        { "SLQTY", "Số lượng từng kiện" },
        { "PKGKG", "KL từng kiện" },
        { "PKGDIM", "Kích thước kiện" },
        { "RETURN", "Người nhận hoàn" },
    };

    private static final Pattern ANY_TAG = Pattern.compile("\\[([A-Z]+)(?: [^\\]]*)?\\][\\s\\S]*?\\[/\\1\\]");

    private OrderEditDiff() {}

    static Map<String, String> snapshot(ShipmentOrder o) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Tên người gửi", o.getSenderName());
        m.put("SĐT người gửi", o.getSenderPhone());
        m.put("Tên người nhận", o.getReceiverName());
        m.put("SĐT người nhận", o.getReceiverPhone());
        m.put("VP gửi", office(o.getFromOffice()));
        m.put("VP nhận", office(o.getToOffice()));
        m.put("VP nhận cuối", office(o.getFinalToOffice()));
        m.put("Lấy tận nơi", yesNo(o.getHomePickup()));
        m.put("Địa chỉ lấy", o.getPickupAddress());
        m.put("Giao tận nơi", yesNo(o.getHomeDelivery()));
        m.put("Địa chỉ giao", o.getDeliveryAddress());
        m.put("Số kiện", o.getQuantity() == null ? null : String.valueOf(o.getQuantity()));
        m.put("Khối lượng (kg)", num(o.getWeightKg()));
        m.put("Tổng cước", money(o.getFareAmount()));
        m.put("Cước hàng", money(o.getGoodsFareAmount()));
        m.put("Phí khai giá", money(o.getDeclaredFeeAmount()));
        m.put("Giảm giá", money(o.getDiscountAmount()));
        m.put("Phí lấy tận nơi", money(o.getPickupFeeAmount()));
        m.put("Phí giao tận nơi", money(o.getDeliveryFeeAmount()));
        m.put("COD", money(o.getCodAmount()));
        m.put("Phí COD", money(o.getCodFeeAmount()));
        m.put("Ngân hàng", o.getBankName());
        m.put("STK", o.getBankAccountNo());
        m.put("Chủ TK", o.getBankAccountName());
        m.put("Đối tác", o.getPartnerCode());
        m.put("Phí đối tác", money(o.getPartnerFeeAmount()));
        m.put("Yêu cầu HĐ", yesNo(o.getInvoiceRequested()));
        m.put("MST", o.getInvoiceTaxCode());
        m.put("Tên công ty", o.getInvoiceCompanyName());
        m.put("Email HĐ", o.getInvoiceEmail());
        m.put("Địa chỉ công ty", o.getInvoiceCompanyAddress());
        String note = o.getNote() == null ? "" : o.getNote();
        for (String[] tag : NOTE_TAGS) {
            String v = tagValue(note, tag[0]);
            String prev = m.get(tag[1]);
            m.put(tag[1], prev == null ? v : (v == null ? prev : prev + " | " + v));
        }
        m.put("Ghi chú", noteBody(note));
        return m;
    }

    /** "Tên người nhận: A → B; Tổng cước: 30.000 → 40.000"; rỗng nếu không đổi gì người dùng nhìn thấy. */
    static String describe(Map<String, String> before, Map<String, String> after) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : after.entrySet()) {
            String from = blankToNull(before.get(e.getKey()));
            String to = blankToNull(e.getValue());
            if (!Objects.equals(from, to)) {
                parts.add(e.getKey() + ": " + show(from) + " → " + show(to));
            }
        }
        return String.join("; ", parts);
    }

    private static String tagValue(String note, String tag) {
        Matcher m = Pattern.compile("\\[" + tag + "\\]([\\s\\S]*?)\\[/" + tag + "\\]").matcher(note);
        return m.find() ? blankToNull(m.group(1)) : null;
    }

    private static String noteBody(String note) {
        return blankToNull(ANY_TAG.matcher(note).replaceAll(""));
    }

    private static String office(Office o) {
        if (o == null) return null;
        return o.getName() != null && !o.getName().isBlank() ? o.getName() : o.getCode();
    }

    private static String yesNo(Boolean b) {
        return Boolean.TRUE.equals(b) ? "Có" : "Không";
    }

    private static String money(BigDecimal v) {
        if (v == null || v.signum() == 0) return null;
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY));
        return f.format(v);
    }

    private static String num(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    private static String show(String v) {
        if (v == null) return "(trống)";
        String s = v.replaceAll("\\s+", " ");
        return s.length() > MAX_VALUE ? s.substring(0, MAX_VALUE) + "…" : s;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
