package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.AppDatabaseSlot;
import com.mycompany.myapp.repository.AppDatabaseSlotRepository;
import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.config.DatabaseJdbc.Endpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Hai database trong cấu hình tích hợp. Chuyển = ngừng ghi, copy từ DB đang dùng sang DB kia, rồi mới đổi pool.
 * Ảnh MinIO không nằm trong bản copy — DB chỉ giữ khóa {@code minio:}.
 */
@Service
public class DatabaseCutoverService {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseCutoverService.class);

    private final AppDatabaseSlotRepository repository;
    private final DataSourceProperties dataSourceProperties;
    private final StaffAccessService staffAccessService;
    private final ExecutorService jobs = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "db-cutover");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean cuttingOver;
    private volatile String status = "IDLE";
    private volatile String message = "";

    public DatabaseCutoverService(
        AppDatabaseSlotRepository repository,
        DataSourceProperties dataSourceProperties,
        StaffAccessService staffAccessService
    ) {
        this.repository = repository;
        this.dataSourceProperties = dataSourceProperties;
        this.staffAccessService = staffAccessService;
    }

    public boolean isCuttingOver() {
        return cuttingOver;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> board() {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("runtimeUrl", publicUrl(dataSourceProperties.getUrl()));
        body.put("cuttingOver", cuttingOver);
        body.put("switchStatus", status);
        body.put("switchMessage", message);
        List<Map<String, Object>> slots = new ArrayList<>();
        for (AppDatabaseSlot slot : repository.findAll()) {
            slots.add(view(slot));
        }
        slots.sort((a, b) -> String.valueOf(a.get("slot")).compareTo(String.valueOf(b.get("slot"))));
        body.put("slots", slots);
        return body;
    }

    @Transactional
    public Map<String, Object> save(String slotId, String label, String jdbcUrl, String username, String password) {
        staffAccessService.requireScreenWrite(ScreenKey.TICH_HOP);
        AppDatabaseSlot slot = required(slotId);
        if (label != null && !label.isBlank()) {
            slot.setLabel(label.trim().length() > 40 ? label.trim().substring(0, 40) : label.trim());
        }
        if (jdbcUrl != null) {
            String url = jdbcUrl.trim();
            if (url.isEmpty()) {
                slot.setJdbcUrl(null);
            } else {
                DatabaseJdbc.parse(url);
                slot.setJdbcUrl(url);
            }
            slot.setLastTestOk(null);
            slot.setLastTestAt(null);
        }
        if (username != null) {
            slot.setDbUsername(username.isBlank() ? null : username.trim());
            slot.setLastTestOk(null);
            slot.setLastTestAt(null);
        }
        if (password != null && !password.isBlank()) {
            if (password.indexOf('\n') >= 0 || password.indexOf('\r') >= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không hợp lệ");
            }
            slot.setDbPassword(password);
            slot.setLastTestOk(null);
            slot.setLastTestAt(null);
        }
        repository.save(slot);
        return board();
    }

    @Transactional
    public Map<String, Object> test(String slotId) {
        staffAccessService.requireScreenWrite(ScreenKey.TICH_HOP);
        AppDatabaseSlot slot = required(slotId);
        if (!notBlank(slot.getJdbcUrl()) && !Boolean.TRUE.equals(slot.getActive())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                slot.getLabel() + " chưa có địa chỉ JDBC. Để trống thì Test nối vào database đang chạy, nên ô nào cũng báo thành công."
            );
        }
        Creds creds = creds(slot);
        String where = probe(creds);
        slot.setLastTestAt(Instant.now());
        slot.setLastTestOk(where.startsWith("ok:"));
        repository.save(slot);
        if (!where.startsWith("ok:")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, where);
        }
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("ok", true);
        ok.put("message", "Kết nối " + slot.getLabel() + " thành công: " + where.substring(3));
        return ok;
    }

    public synchronized Map<String, Object> startSwitch(String targetSlot) {
        staffAccessService.requireScreenWrite(ScreenKey.TICH_HOP);
        if (cuttingOver) {
            return statusBody();
        }
        String target = normalizeSlot(targetSlot);
        AppDatabaseSlot from = activeSlot();
        AppDatabaseSlot to = required(target);
        if (from.getSlot().equals(to.getSlot())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Database này đang được dùng");
        }
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Chưa chuyển. API vẫn dùng database hiện tại. Cổng database mới chưa mở thì chỉ cần lưu cấu hình và bấm Test."
        );
    }

    public Map<String, Object> statusBody() {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cuttingOver", cuttingOver);
        body.put("switchStatus", status);
        body.put("switchMessage", message);
        return body;
    }

    private void runSwitch(Creds source, Creds dest, String fromSlot, String toSlot) {
        try {
            Thread.sleep(3000);
            copy(source, dest);
            stamp(dest, toSlot);
            stamp(source, toSlot);
            status = "DONE";
            message = "Đã đồng bộ và chuyển sang database " + toSlot + ". Ảnh vẫn ở MinIO.";
            LOG.info("Đã chuyển database sang slot {}", toSlot);
        } catch (Exception e) {
            status = "FAILED";
            message = "Chưa chuyển. Vẫn dùng database cũ. " + e.getMessage();
            LOG.warn("Chuyển database thất bại: {}", e.getMessage());
        } finally {
            cuttingOver = false;
        }
    }

    private void copy(Creds source, Creds dest) throws IOException, InterruptedException {
        String dumpBin = binary("mysqldump");
        String mysqlBin = binary("mysql");
        Path sourceCnf = cnf(source);
        Path destCnf = cnf(dest);
        try {
            Process dump = new ProcessBuilder(
                dumpBin,
                "--defaults-extra-file=" + sourceCnf,
                "--single-transaction",
                "--quick",
                "--routines",
                "--triggers",
                "--events",
                "--no-tablespaces",
                "--set-gtid-purged=OFF",
                "--column-statistics=0",
                "--default-character-set=utf8mb4",
                source.endpoint.database()
            )
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();
            Process load = new ProcessBuilder(
                mysqlBin,
                "--defaults-extra-file=" + destCnf,
                "--default-character-set=utf8mb4",
                dest.endpoint.database()
            )
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();
            Thread pump = new Thread(
                () -> {
                    try {
                        dump.getInputStream().transferTo(load.getOutputStream());
                    } catch (IOException ignored) {
                        // process bên kia đã đóng
                    } finally {
                        try {
                            load.getOutputStream().close();
                        } catch (IOException ignored) {
                            // đã đóng
                        }
                    }
                },
                "db-copy-pump"
            );
            pump.setDaemon(true);
            pump.start();
            String dumpErr = new String(dump.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            String loadErr = new String(load.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int dumpCode = dump.waitFor();
            pump.join(10_000);
            int loadCode = load.waitFor();
            if (dumpCode != 0 || loadCode != 0) {
                throw new IllegalStateException(tail(dumpErr + "\n" + loadErr));
            }
        } finally {
            Files.deleteIfExists(sourceCnf);
            Files.deleteIfExists(destCnf);
        }
    }

    private static void stamp(Creds creds, String activeSlot) throws Exception {
        try (
            Connection c = DriverManager.getConnection(withTimeouts(creds.endpoint.jdbcUrl()), creds.username, creds.password);
            Statement st = c.createStatement()
        ) {
            st.executeUpdate(
                "UPDATE app_database_slot SET active = (slot = '" +
                (activeSlot.equals("B") ? "B" : "A") +
                "'), last_sync_at = CURRENT_TIMESTAMP(6)"
            );
        }
    }

    /** {@code ok:user tại host:port/db} khi vào được; còn lại là lỗi. */
    private static String probe(Creds creds) {
        try (Connection c = DriverManager.getConnection(withTimeouts(creds.endpoint.jdbcUrl()), creds.username, creds.password)) {
            if (!c.isValid(8)) {
                return "Không xác nhận được kết nối";
            }
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT CURRENT_USER(), DATABASE()")) {
                if (!rs.next()) {
                    return "Không đọc được database vừa nối";
                }
                String user = rs.getString(1) == null ? creds.username : rs.getString(1);
                String database = rs.getString(2) == null ? creds.endpoint.database() : rs.getString(2);
                return "ok:" + user + " tại " + creds.endpoint.host() + ":" + creds.endpoint.port() + "/" + database;
            }
        } catch (Exception e) {
            String text = e.getMessage() == null ? "Không kết nối được" : e.getMessage();
            return text.length() > 300 ? text.substring(0, 300) : text;
        }
    }

    private Creds creds(AppDatabaseSlot slot) {
        String url = notBlank(slot.getJdbcUrl()) ? slot.getJdbcUrl().trim() : dataSourceProperties.getUrl();
        String user = notBlank(slot.getDbUsername()) ? slot.getDbUsername().trim() : dataSourceProperties.getUsername();
        String pass = slot.getDbPassword() != null ? slot.getDbPassword() : dataSourceProperties.getPassword();
        if (!notBlank(url) || !notBlank(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, slot.getLabel() + " chưa có đủ địa chỉ và user");
        }
        try {
            return new Creds(DatabaseJdbc.parse(url), user, pass == null ? "" : pass);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private AppDatabaseSlot activeSlot() {
        ensureRows();
        return repository.findAll().stream().filter(s -> Boolean.TRUE.equals(s.getActive())).findFirst().orElseGet(() -> required("A"));
    }

    private AppDatabaseSlot required(String slotId) {
        ensureRows();
        return repository
            .findById(normalizeSlot(slotId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có database A hoặc B"));
    }

    private void ensureRows() {
        if (repository.count() >= 2) {
            return;
        }
        if (repository.findById("A").isEmpty()) {
            AppDatabaseSlot a = new AppDatabaseSlot();
            a.setSlot("A");
            a.setLabel("Database 1");
            a.setActive(true);
            repository.save(a);
        }
        if (repository.findById("B").isEmpty()) {
            AppDatabaseSlot b = new AppDatabaseSlot();
            b.setSlot("B");
            b.setLabel("Database 2");
            b.setActive(false);
            repository.save(b);
        }
    }

    private void markTest(AppDatabaseSlot slot, boolean ok) {
        slot.setLastTestAt(Instant.now());
        slot.setLastTestOk(ok);
        repository.save(slot);
    }

    private Map<String, Object> view(AppDatabaseSlot slot) {
        boolean env = !notBlank(slot.getJdbcUrl());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("slot", slot.getSlot());
        row.put("label", slot.getLabel());
        row.put("jdbcUrl", env ? "" : slot.getJdbcUrl());
        row.put("username", slot.getDbUsername() == null ? "" : slot.getDbUsername());
        row.put("passwordConfigured", notBlank(slot.getDbPassword()) || (env && notBlank(dataSourceProperties.getPassword())));
        row.put("usesEnvironment", env);
        row.put("active", Boolean.TRUE.equals(slot.getActive()));
        row.put("lastTestOk", slot.getLastTestOk());
        row.put("lastTestAt", slot.getLastTestAt());
        row.put("lastSyncAt", slot.getLastSyncAt());
        return row;
    }

    private static String normalizeSlot(String slot) {
        if (slot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn database A hoặc B");
        }
        String s = slot.trim().toUpperCase();
        if (!s.equals("A") && !s.equals("B")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn database A hoặc B");
        }
        return s;
    }

    private static String withTimeouts(String url) {
        String extra = "connectTimeout=8000&socketTimeout=8000";
        if (url.contains("connectTimeout=")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + extra;
    }

    private static String publicUrl(String url) {
        if (url == null) {
            return "";
        }
        int q = url.indexOf('?');
        return q < 0 ? url : url.substring(0, q);
    }

    private static Path cnf(Creds creds) throws IOException {
        Path file = Files.createTempFile("cpn-db-", ".cnf");
        String body =
            "[client]\nhost=" +
            creds.endpoint.host() +
            "\nport=" +
            creds.endpoint.port() +
            "\nuser=" +
            creds.username +
            "\npassword=\"" +
            creds.password.replace("\\", "\\\\").replace("\"", "\\\"") +
            "\"\n";
        Files.writeString(file, body, StandardCharsets.UTF_8);
        return file;
    }

    private static String binary(String name) {
        String env = System.getenv(name.equals("mysql") ? "CPN_MYSQL_BIN" : "CPN_MYSQLDUMP_BIN");
        List<String> candidates = new ArrayList<>();
        if (notBlank(env)) {
            candidates.add(env.trim());
        }
        candidates.add(name);
        candidates.add("/usr/bin/" + name);
        candidates.add("C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\" + name + ".exe");
        for (String candidate : candidates) {
            if (candidate.equals(name)) {
                return candidate;
            }
            if (Files.isRegularFile(Path.of(candidate))) {
                return candidate;
            }
        }
        return name;
    }

    private static String tail(String text) {
        String clean = text == null ? "" : text.replaceAll("(?i)password\\s*=\\s*\\S+", "password=***").trim();
        if (clean.isEmpty()) {
            return "mysqldump hoặc mysql trả lỗi";
        }
        return clean.length() > 400 ? clean.substring(clean.length() - 400) : clean;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record Creds(Endpoint endpoint, String username, String password) {}
}
