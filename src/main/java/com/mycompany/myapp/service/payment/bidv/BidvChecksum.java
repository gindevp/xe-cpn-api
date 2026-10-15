package com.mycompany.myapp.service.payment.bidv;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checksum BIDV thu hộ.
 *
 * <p>Đặc tả bản 04/2024 ghi {@code Base64(SHA256({secret}+{serviceId}+{customerId}))} (getbill) và
 * {@code {secret}+|{transId}+{amount}} (paybill), không rõ "+" là ký tự hay phép nối. BIDV (15/10/2026) chuyển sang
 * HMAC-SHA256 và xác nhận công thức {@link #CONFIRMED} (kiểm đầu tiên). Khi UAT vẫn chấp nhận các kiểu dưới đây và
 * trả lại tên kiểu khớp; chỉ còn {@link #CONFIRMED} khớp thực tế thì bỏ các kiểu khác trước khi chạy thật:
 * <ul>
 *   <li>{@code HMAC_<nối>_B64|HEX}: HMAC-SHA256, khoá = secret, chuỗi = các trường (bỏ secret đứng đầu);</li>
 *   <li>{@code HMAC_<nối>_FULL_B64|HEX}: như trên nhưng chuỗi giữ nguyên cả secret;</li>
 *   <li>{@code <nối>}: SHA256 thuần như đặc tả cũ.</li>
 * </ul>
 */
public final class BidvChecksum {

    public static final String GETBILL_PLUS = "PLUS";
    public static final String GETBILL_CONCAT = "CONCAT";
    public static final String GETBILL_PIPE = "PIPE";
    public static final String PAYBILL_PLUS_PIPE = "PLUS_PIPE";
    public static final String PAYBILL_PIPE = "PIPE";
    public static final String PAYBILL_PLUS = "PLUS";
    public static final String PAYBILL_CONCAT = "CONCAT";

    /**
     * Công thức BIDV xác nhận (14/10/2026): {@code Base64(HMAC_SHA256(key = secret, secret + serviceId + customerId))},
     * paybill {@code secret + transId + amount} — nối liền, không ký tự phân cách.
     */
    public static final String CONFIRMED = "HMAC_" + GETBILL_CONCAT + "_FULL_B64";

    private static final String HMAC = "HMAC_";
    private static final String HEX = "_HEX";

    private BidvChecksum() {}

    public static String hash(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] hmac(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Chuỗi đầu vào SHA256 getbill theo từng kiểu nối (đều bắt đầu bằng secret). */
    public static Map<String, String> getBillInputs(String secret, String serviceId, String customerId) {
        String s = nz(secret);
        String sid = nz(serviceId);
        String cid = nz(customerId);
        Map<String, String> out = new LinkedHashMap<>();
        out.put(GETBILL_PLUS, s + "+" + sid + "+" + cid);
        out.put(GETBILL_CONCAT, s + sid + cid);
        out.put(GETBILL_PIPE, s + "|" + sid + "|" + cid);
        return out;
    }

    /** Chuỗi đầu vào SHA256 paybill theo từng kiểu nối; {@code amount} thử cả dạng BIDV gửi lẫn dạng số nguyên. */
    public static Map<String, String> payBillInputs(String secret, String transId, String amountRaw) {
        String s = nz(secret);
        String tid = nz(transId);
        Map<String, String> out = new LinkedHashMap<>();
        boolean first = true;
        for (String amt : amountForms(amountRaw)) {
            String suffix = first ? "" : "_INT";
            first = false;
            out.putIfAbsent(PAYBILL_PLUS_PIPE + suffix, s + "+|" + tid + "+" + amt);
            out.putIfAbsent(PAYBILL_PIPE + suffix, s + "|" + tid + "|" + amt);
            out.putIfAbsent(PAYBILL_PLUS + suffix, s + "+" + tid + "+" + amt);
            out.putIfAbsent(PAYBILL_CONCAT + suffix, s + tid + amt);
        }
        return out;
    }

    /** Mọi checksum hợp lệ cho getbill: tên kiểu → giá trị. */
    public static Map<String, String> getBillChecksums(String secret, String serviceId, String customerId) {
        return expected(nz(secret), getBillInputs(secret, serviceId, customerId));
    }

    /** Mọi checksum hợp lệ cho paybill: tên kiểu → giá trị. */
    public static Map<String, String> payBillChecksums(String secret, String transId, String amountRaw) {
        return expected(nz(secret), payBillInputs(secret, transId, amountRaw));
    }

    /** Chuỗi được ký HMAC của một kiểu nối (bỏ secret và dấu nối đứng đầu). */
    public static String hmacMessage(String secret, String plainInput) {
        String rest = plainInput.substring(nz(secret).length());
        for (String sep : new String[] { "+|", "+", "|" }) {
            if (rest.startsWith(sep)) {
                return rest.substring(sep.length());
            }
        }
        return rest;
    }

    public static Optional<String> matchGetBill(String secret, String serviceId, String customerId, String checksum) {
        return match(getBillChecksums(secret, serviceId, customerId), checksum);
    }

    public static Optional<String> matchPayBill(String secret, String transId, String amountRaw, String checksum) {
        return match(payBillChecksums(secret, transId, amountRaw), checksum);
    }

    private static Map<String, String> expected(String secret, Map<String, String> inputs) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!secret.isEmpty()) {
            for (Map.Entry<String, String> e : inputs.entrySet()) {
                if (e.getKey().startsWith(GETBILL_CONCAT)) {
                    out.put(HMAC + e.getKey() + "_FULL_B64", Base64.getEncoder().encodeToString(hmac(secret, e.getValue())));
                }
            }
            for (Map.Entry<String, String> e : inputs.entrySet()) {
                byte[] mac = hmac(secret, hmacMessage(secret, e.getValue()));
                out.putIfAbsent(HMAC + e.getKey() + "_B64", Base64.getEncoder().encodeToString(mac));
                out.putIfAbsent(HMAC + e.getKey() + HEX, HexFormat.of().formatHex(mac));
            }
            for (Map.Entry<String, String> e : inputs.entrySet()) {
                byte[] mac = hmac(secret, e.getValue());
                out.putIfAbsent(HMAC + e.getKey() + "_FULL_B64", Base64.getEncoder().encodeToString(mac));
                out.putIfAbsent(HMAC + e.getKey() + "_FULL" + HEX, HexFormat.of().formatHex(mac));
            }
        }
        for (Map.Entry<String, String> e : inputs.entrySet()) {
            out.putIfAbsent(e.getKey(), hash(e.getValue()));
        }
        return out;
    }

    private static Optional<String> match(Map<String, String> expected, String checksum) {
        if (checksum == null || checksum.isBlank()) {
            return Optional.empty();
        }
        String given = checksum.trim();
        byte[] exact = given.getBytes(StandardCharsets.UTF_8);
        byte[] lower = given.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : expected.entrySet()) {
            byte[] candidate = e.getKey().endsWith(HEX) ? lower : exact;
            if (MessageDigest.isEqual(candidate, e.getValue().getBytes(StandardCharsets.UTF_8))) {
                return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }

    /** "250000", "250000.00", 250000.0 → thử chuỗi gốc và số nguyên. */
    static Set<String> amountForms(String amountRaw) {
        Set<String> forms = new LinkedHashSet<>();
        String raw = nz(amountRaw).trim();
        forms.add(raw);
        try {
            BigDecimal v = new BigDecimal(raw);
            if (v.stripTrailingZeros().scale() <= 0) {
                forms.add(v.toBigInteger().toString());
            }
        } catch (NumberFormatException ignored) {
            // để nguyên chuỗi gốc
        }
        return forms;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
