package com.mycompany.myapp.service.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/** Pool đang chạy. Đổi sau khi đồng bộ xong, không đóng pool cũ ngay. */
public class LiveDataSource extends DelegatingDataSource {

    private final DataSourceProperties boot;
    private volatile String jdbcUrl;
    private volatile String username;
    private volatile String password;

    public LiveDataSource(DataSourceProperties boot) {
        this.boot = boot;
        HikariDataSource pool = pool(boot.getUrl(), boot.getUsername(), boot.getPassword());
        setTargetDataSource(pool);
        try {
            afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("Không mở được database môi trường", e);
        }
        this.jdbcUrl = boot.getUrl();
        this.username = boot.getUsername();
        this.password = boot.getPassword();
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public synchronized void use(String url, String user, String pass) {
        HikariDataSource next = pool(url, user, pass);
        try (var c = next.getConnection()) {
            if (!c.isValid(8)) {
                next.close();
                throw new IllegalStateException("Database mới không nhận kết nối");
            }
        } catch (Exception e) {
            next.close();
            throw new IllegalStateException("Không mở được database mới: " + e.getMessage(), e);
        }
        DataSource previous = obtainTargetDataSource();
        setTargetDataSource(next);
        try {
            afterPropertiesSet();
        } catch (Exception e) {
            next.close();
            throw new IllegalStateException("Không gắn được database mới", e);
        }
        this.jdbcUrl = url;
        this.username = user;
        this.password = pass;
        if (previous instanceof HikariDataSource old && previous != next) {
            Thread closer = new Thread(
                () -> {
                    try {
                        Thread.sleep(20_000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    old.close();
                },
                "close-old-db-pool"
            );
            closer.setDaemon(true);
            closer.start();
        }
    }

    private HikariDataSource pool(String url, String user, String pass) {
        HikariDataSource ds = boot.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setPoolName("cpn");
        ds.setMaximumPoolSize(number("CPN_DB_POOL_MAX", 20));
        ds.setMinimumIdle(number("CPN_DB_POOL_MIN_IDLE", 3));
        ds.setConnectionTimeout(15_000);
        return ds;
    }

    private static int number(String name, int fallback) {
        String raw = System.getenv(name);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
