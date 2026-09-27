package com.mycompany.myapp.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * IP công cộng của client. Prod bật {@code server.forward-headers-strategy: native} nên Tomcat đã thay remoteAddr
 * bằng IP thật từ X-Forwarded-For (duyệt từ phải sang, bỏ qua proxy nội bộ). Fallback: nếu remoteAddr vẫn là IP nội bộ
 * thì lấy IP công cộng ngoài cùng bên phải trong X-Forwarded-For — phần bên trái do client tự gửi nên không tin.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String remote = normalize(request.getRemoteAddr());
        if (remote != null && !isInternal(remote)) {
            return remote;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String ip = normalize(parts[i]);
                if (ip != null && !isInternal(ip)) {
                    return ip;
                }
            }
        }
        return remote;
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String ip = raw.trim().toLowerCase(Locale.ROOT);
        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf(']'));
        }
        if (ip.startsWith("::ffff:") && ip.indexOf('.') > 0) {
            ip = ip.substring("::ffff:".length());
        }
        if (ip.equals("0:0:0:0:0:0:0:1")) {
            ip = "::1";
        }
        return ip.isEmpty() ? null : ip;
    }

    public static boolean isInternal(String ip) {
        if (ip.contains(":")) {
            return ip.equals("::1") || ip.startsWith("fe80:") || ip.startsWith("fc") || ip.startsWith("fd");
        }
        String[] o = ip.split("\\.");
        if (o.length != 4) {
            return false;
        }
        try {
            int a = Integer.parseInt(o[0]);
            int b = Integer.parseInt(o[1]);
            return (
                a == 10 ||
                a == 127 ||
                (a == 172 && b >= 16 && b <= 31) ||
                (a == 192 && b == 168) ||
                (a == 169 && b == 254) ||
                (a == 100 && b >= 64 && b <= 127)
            );
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
