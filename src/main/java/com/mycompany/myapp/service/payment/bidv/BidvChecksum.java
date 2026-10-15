package com.mycompany.myapp.service.payment.bidv;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Checksum BIDV thu hộ: {@code Base64(SHA256_raw(chuỗi))}.
 *
 * <p>Đặc tả BIDV ghi {@code {secret}+{serviceId}+{customerId}} (getbill) và {@code {secret}+|{transId}+{amount}} (paybill)
 * nhưng không nói rõ "+" là ký tự hay phép nối. Khi UAT chấp nhận mọi kiểu đã gặp và trả lại tên kiểu khớp
 * để chốt một kiểu duy nhất trước khi chạy thật.
 */
public final class BidvChecksum {

    public static final String GETBILL_PLUS = "PLUS";
    public static final String GETBILL_CONCAT = "CONCAT";
    public static final String PAYBILL_PLUS_PIPE = "PLUS_PIPE";
    public static final String PAYBILL_PIPE = "PIPE";
    public static final String PAYBILL_PLUS = "PLUS";
    public static final String PAYBILL_CONCAT = "CONCAT";

    private BidvChecksum() {}

    public static String hash(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Chuỗi đầu vào getbill theo từng kiểu nối, thứ tự ưu tiên = kiểu đặc tả BIDV trước. */
    public static Map<String, String> getBillInputs(String secret, String serviceId, String customerId) {
        String s = nz(secret);
        String sid = nz(serviceId);
        String cid = nz(customerId);
        Map<String, String> out = new LinkedHashMap<>();
        out.put(GETBILL_PLUS, s + "+" + sid + "+" + cid);
        out.put(GETBILL_CONCAT, s + sid + cid);
        return out;
    }

    /** Chuỗi đầu vào paybill theo từng kiểu nối; {@code amount} thử cả dạng BIDV gửi lẫn dạng số nguyên. */
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

    public static Optional<String> matchGetBill(String secret, String serviceId, String customerId, String checksum) {
        return match(getBillInputs(secret, serviceId, customerId), checksum);
    }

    public static Optional<String> matchPayBill(String secret, String transId, String amountRaw, String checksum) {
        return match(payBillInputs(secret, transId, amountRaw), checksum);
    }

    private static Optional<String> match(Map<String, String> inputs, String checksum) {
        if (checksum == null || checksum.isBlank()) {
            return Optional.empty();
        }
        byte[] given = checksum.trim().getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : inputs.entrySet()) {
            if (MessageDigest.isEqual(given, hash(e.getValue()).getBytes(StandardCharsets.UTF_8))) {
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
