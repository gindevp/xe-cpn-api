package com.mycompany.myapp.service.config;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** jdbc:mysql://host:port/database — không nhận mật khẩu nằm trong URL. */
public final class DatabaseJdbc {

    private static final Pattern URL = Pattern.compile("^jdbc:mysql://([^/:?#]+)(?::(\\d+))?/([^?]+)", Pattern.CASE_INSENSITIVE);

    public record Endpoint(String host, int port, String database, String jdbcUrl) {
        public String key() {
            return host.toLowerCase(Locale.ROOT) + ":" + port + "/" + database.toLowerCase(Locale.ROOT);
        }
    }

    private DatabaseJdbc() {}

    public static Endpoint parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Thiếu địa chỉ JDBC");
        }
        String url = raw.trim();
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("password=")) {
            throw new IllegalArgumentException("Đừng để mật khẩu trong URL. Điền vào ô mật khẩu.");
        }
        Matcher m = URL.matcher(url);
        if (!m.find()) {
            throw new IllegalArgumentException("URL phải dạng jdbc:mysql://host:cổng/tên_database");
        }
        int port = m.group(2) == null ? 3306 : Integer.parseInt(m.group(2));
        String database = m.group(3);
        if (database.isBlank() || database.contains("/")) {
            throw new IllegalArgumentException("Tên database không hợp lệ");
        }
        return new Endpoint(m.group(1), port, database, url);
    }
}
