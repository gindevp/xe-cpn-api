package com.mycompany.myapp.service.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chuyển data-URL đang nằm trong DB lên MinIO, cột chỉ còn {@code minio:key}.
 * Mỗi lần một lô nhỏ để request không quá thời gian. Gọi lại đến khi {@code hasMore=false}.
 */
@Service
public class MinioBlobMigration {

    private static final int BATCH = 5;

    private final JdbcTemplate jdbc;
    private final StoredMedia storedMedia;
    private final ObjectMapper objectMapper;

    public MinioBlobMigration(JdbcTemplate jdbc, StoredMedia storedMedia, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.storedMedia = storedMedia;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> migrateBatch() {
        if (!storedMedia.configured()) {
            throw new IllegalStateException("Chưa cấu hình MinIO");
        }
        int moved = 0;
        boolean hasMore = false;
        moved += column("order_pod_photo", "photo_url", "pod");
        moved += column("order_goods_photo", "photo_url", "goods");
        moved += column("inventory_check_photo", "photo_url", "inventory");
        moved += column("vehicle_event_photo", "photo_url", "vehicle");
        moved += column("attendance_record", "photo", "attendance");
        moved += column("receipt", "confirm_proof_image", "receipt");
        moved += lines("receipt", "confirm_proof_extra", "receipt");
        moved += column("receipt", "transfer_proof_image", "receipt");
        moved += json("order_issue", "evidence_photos", "issue");
        moved += column("maintenance_policy", "image_url", "maintenance");
        hasMore = pending();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("moved", moved);
        out.put("hasMore", hasMore);
        return out;
    }

    private int column(String table, String column, String folder) {
        List<Row> rows = rows(table, column);
        int moved = 0;
        for (Row row : rows) {
            String next = storedMedia.store(row.value, folder);
            if (next != null && !next.equals(row.value)) {
                jdbc.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?", next, row.id);
                moved++;
            }
        }
        return moved;
    }

    private int lines(String table, String column, String folder) {
        List<Row> rows = rows(table, column);
        int moved = 0;
        for (Row row : rows) {
            String next = storedMedia.storeLines(row.value, folder);
            if (next != null && !next.equals(row.value)) {
                jdbc.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?", next, row.id);
                moved++;
            }
        }
        return moved;
    }

    private int json(String table, String column, String folder) {
        List<Row> rows = rows(table, column);
        int moved = 0;
        for (Row row : rows) {
            String next = rewriteJson(row.value, folder);
            if (next != null && !next.equals(row.value)) {
                jdbc.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?", next, row.id);
                moved++;
            }
        }
        return moved;
    }

    private String rewriteJson(String raw, String folder) {
        String trimmed = raw == null ? "" : raw.trim();
        if (!trimmed.startsWith("[")) {
            return storedMedia.store(trimmed, folder);
        }
        try {
            List<String> items = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            List<String> next = new ArrayList<>();
            for (String item : items) {
                next.add(storedMedia.store(item, folder));
            }
            return objectMapper.writeValueAsString(next);
        } catch (Exception e) {
            return storedMedia.store(trimmed, folder);
        }
    }

    private List<Row> rows(String table, String column) {
        return jdbc.query(
            "SELECT id, " + column + " FROM " + table + " WHERE " + column + " LIKE '%data:%' ORDER BY id LIMIT " + BATCH,
            (rs, n) -> new Row(rs.getLong(1), rs.getString(2))
        );
    }

    private boolean pending() {
        String[] checks = {
            "SELECT 1 FROM order_pod_photo WHERE photo_url LIKE '%data:%' LIMIT 1",
            "SELECT 1 FROM order_goods_photo WHERE photo_url LIKE '%data:%' LIMIT 1",
            "SELECT 1 FROM inventory_check_photo WHERE photo_url LIKE '%data:%' LIMIT 1",
            "SELECT 1 FROM vehicle_event_photo WHERE photo_url LIKE '%data:%' LIMIT 1",
            "SELECT 1 FROM attendance_record WHERE photo LIKE '%data:%' LIMIT 1",
            "SELECT 1 FROM receipt WHERE confirm_proof_image LIKE '%data:%' OR confirm_proof_extra LIKE '%data:%' OR transfer_proof_image LIKE '%data:%' LIMIT 1",
            "SELECT 1 FROM order_issue WHERE evidence_photos LIKE '%data:%' LIMIT 1",
            "SELECT 1 FROM maintenance_policy WHERE image_url LIKE '%data:%' LIMIT 1",
        };
        for (String sql : checks) {
            if (!jdbc.queryForList(sql).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private record Row(long id, String value) {}
}
