package com.mycompany.myapp.service.payment.onepay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chữ ký OWS1 của OnePay (kiểu AWS SigV4): region {@code onepay}, service {@code paycollect}.
 *
 * <p>IPN OnePay gọi về được ký với "uri" là toàn bộ URL IPN (kể cả scheme, host và {@code ?id=}) và không có query
 * riêng — theo code mẫu OnePay.
 */
public final class OnePaySigner {

    public static final String ALGORITHM = "OWS1-HMAC-SHA256";
    public static final String HDR_AUTHORIZATION = "X-OP-Authorization";
    public static final String HDR_DATE = "X-OP-Date";
    public static final String HDR_EXPIRES = "X-OP-Expires";
    static final String EMPTY_BODY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    private static final String SCHEME = "OWS1";
    private static final String REGION = "onepay";
    private static final String SERVICE = "paycollect";
    private static final String TERMINATOR = "ows1_request";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final Pattern AUTH = Pattern.compile(
        "^OWS1-HMAC-SHA256\\s+Credential=([^/,\\s]+)/(\\d{8})/[^,]*,\\s*SignedHeaders=([^,]+),\\s*Signature=([0-9a-fA-F]+)\\s*$"
    );

    private OnePaySigner() {}

    public static String timestamp(Instant t) {
        return TS.format(t);
    }

    /** Giá trị header {@code X-OP-Authorization}. */
    public static String authorization(
        String partnerId,
        String partnerKey,
        String method,
        String uri,
        Map<String, String> query,
        Map<String, String> signedHeaders,
        byte[] payload,
        String timestamp
    ) {
        String day = timestamp.substring(0, 8);
        String scope = day + "/" + REGION + "/" + SERVICE + "/" + TERMINATOR;
        Map<String, String> headers = new TreeMap<>();
        signedHeaders.forEach((k, v) -> headers.put(k.toLowerCase(Locale.ROOT), v == null ? "" : v.trim()));
        String signedNames = String.join(";", headers.keySet());
        String signature = signature(partnerKey, method, uri, query, headers, payload, timestamp, scope);
        return ALGORITHM + " Credential=" + partnerId + "/" + scope + ",SignedHeaders=" + signedNames + ",Signature=" + signature;
    }

    static String signature(
        String partnerKey,
        String method,
        String uri,
        Map<String, String> query,
        Map<String, String> lowerHeaders,
        byte[] payload,
        String timestamp,
        String scope
    ) {
        StringBuilder canonicalQuery = new StringBuilder();
        if (query != null) {
            new TreeMap<>(query).forEach((k, v) -> {
                if (!canonicalQuery.isEmpty()) {
                    canonicalQuery.append('&');
                }
                canonicalQuery.append(uriEncode(k, true)).append('=').append(uriEncode(v == null ? "" : v, true));
            });
        }
        StringBuilder canonicalHeaders = new StringBuilder();
        lowerHeaders.forEach((k, v) -> canonicalHeaders.append(k).append(':').append(v).append('\n'));
        String hashedPayload = payload == null ? UNSIGNED_PAYLOAD : payload.length == 0 ? EMPTY_BODY_SHA256 : hex(sha256(payload));
        String canonicalRequest =
            method +
            "\n" +
            uriEncode(uri, false) +
            "\n" +
            canonicalQuery +
            "\n" +
            canonicalHeaders +
            "\n" +
            String.join(";", lowerHeaders.keySet()) +
            "\n" +
            hashedPayload;
        String stringToSign =
            ALGORITHM + "\n" + timestamp + "\n" + scope + "\n" + hex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        byte[] key = hmac((SCHEME + partnerKey).getBytes(StandardCharsets.UTF_8), timestamp.substring(0, 8));
        key = hmac(key, REGION);
        key = hmac(key, SERVICE);
        key = hmac(key, TERMINATOR);
        return hex(hmac(key, stringToSign));
    }

    public record Parsed(String partnerId, String day, String signedHeaders, String signature) {}

    public static Parsed parse(String authorization) {
        if (authorization == null) {
            return null;
        }
        Matcher m = AUTH.matcher(authorization.trim());
        return m.matches() ? new Parsed(m.group(1), m.group(2), m.group(3).trim(), m.group(4)) : null;
    }

    /**
     * Kiểm chữ ký IPN. {@code headers} là header request (không phân biệt hoa thường) — chỉ lấy các header có trong
     * SignedHeaders.
     */
    public static boolean verifyIpn(String partnerKey, String method, String fullUrl, Map<String, String> headers, byte[] body) {
        Parsed p = parse(headers.get(HDR_AUTHORIZATION.toLowerCase(Locale.ROOT)));
        String ts = headers.get(HDR_DATE.toLowerCase(Locale.ROOT));
        if (p == null || ts == null || ts.length() < 8 || partnerKey == null || partnerKey.isBlank()) {
            return false;
        }
        Map<String, String> signed = new TreeMap<>();
        for (String name : p.signedHeaders().split(";")) {
            String n = name.trim().toLowerCase(Locale.ROOT);
            if (n.isEmpty()) {
                continue;
            }
            String v = headers.get(n);
            if (v == null) {
                return false;
            }
            signed.put(n, v.trim());
        }
        String scope = ts.trim().substring(0, 8) + "/" + REGION + "/" + SERVICE + "/" + TERMINATOR;
        String expected = signature(partnerKey.trim(), method, fullUrl, Map.of(), signed, body, ts.trim(), scope);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.US_ASCII),
            p.signature().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)
        );
    }

    static String uriEncode(String data, boolean encodeSlash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            int ch = b & 0xff;
            boolean plain =
                (ch >= 'A' && ch <= 'Z') ||
                (ch >= 'a' && ch <= 'z') ||
                (ch >= '0' && ch <= '9') ||
                ch == '_' ||
                ch == '-' ||
                ch == '~' ||
                ch == '.' ||
                (ch == '/' && !encodeSlash);
            if (plain) {
                sb.append((char) ch);
            } else {
                sb.append('%').append(String.format("%02X", ch));
            }
        }
        return sb.toString();
    }

    static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String hex(byte[] data) {
        return HexFormat.of().formatHex(data);
    }
}
