package com.mycompany.myapp.service.report;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregations shaped like FE /ton-kho and /bao-cao-gio tables.
 */
@Service
@Transactional(readOnly = true)
public class InventoryHourlyReportService {

    private static final int[] BUCKET_MAX = { 6, 12, 24, 36, 48, 72, 96, 120, 192, Integer.MAX_VALUE };
    private static final String[] INV_KINDS = { "LAY", "GIAO", "TRA", "TON_LC_GIAO", "TON_LC_TRA" };
    private static final String[] HOURLY_KINDS = { "LAY", "GIAO_TRA", "LUAN_CHUYEN" };

    private final ShipmentOrderRepository shipmentOrderRepository;

    public InventoryHourlyReportService(ShipmentOrderRepository shipmentOrderRepository) {
        this.shipmentOrderRepository = shipmentOrderRepository;
    }

    public Map<String, Object> inventory(String officeCode) {
        Map<String, Map<String, int[]>> byOffice = new LinkedHashMap<>();
        for (ShipmentOrder o : shipmentOrderRepository.findAll()) {
            Classified c = classify(o);
            if (c == null) continue;
            if (
                officeCode != null && !officeCode.isBlank() && !"ALL".equalsIgnoreCase(officeCode) && !officeCode.equalsIgnoreCase(c.office)
            ) {
                continue;
            }
            Instant base = ageBase(o);
            double hours = Duration.between(base, Instant.now()).toMinutes() / 60.0;
            int idx = bucketIndex(hours);
            Map<String, int[]> row = byOffice.computeIfAbsent(c.office, k -> emptyInv());
            row.get(c.kind)[idx]++;
        }
        List<Map<String, Object>> offices = new ArrayList<>();
        for (var e : byOffice.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", e.getKey());
            item.put("rows", e.getValue());
            offices.add(item);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(
            "buckets",
            List.of("00 - 06", "06 - 12", "12 - 24", "24 - 36", "36 - 48", "48 - 72", "72 - 96", "96 - 120", "120 - 192", "192+")
        );
        out.put("kinds", INV_KINDS);
        out.put("offices", offices);
        return out;
    }

    public Map<String, Object> hourly(String officeCode) {
        Map<String, int[]> series = new LinkedHashMap<>();
        for (String k : HOURLY_KINDS) {
            series.put(k, new int[24]);
        }
        for (ShipmentOrder o : shipmentOrderRepository.findAll()) {
            for (Point p : pointsOf(o)) {
                if (
                    officeCode != null &&
                    !officeCode.isBlank() &&
                    !"ALL".equalsIgnoreCase(officeCode) &&
                    !officeCode.equalsIgnoreCase(p.office)
                ) {
                    continue;
                }
                if (p.at == null) continue;
                int h = p.at.atZone(ZoneId.systemDefault()).getHour();
                series.get(p.kind)[h]++;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("hours", 24);
        out.put("kinds", HOURLY_KINDS);
        out.put("series", series);
        return out;
    }

    private static int bucketIndex(double hours) {
        for (int i = 0; i < BUCKET_MAX.length; i++) {
            int min = i == 0 ? 0 : BUCKET_MAX[i - 1];
            if (hours >= min && hours < BUCKET_MAX[i]) return i;
        }
        return BUCKET_MAX.length - 1;
    }

    private static Map<String, int[]> emptyInv() {
        Map<String, int[]> m = new LinkedHashMap<>();
        for (String k : INV_KINDS) m.put(k, new int[BUCKET_MAX.length]);
        return m;
    }

    private record Classified(String kind, String office) {}

    private static Classified classify(ShipmentOrder o) {
        ReturnStage rs = o.getReturnStage();
        if (rs != null) {
            String office = o.getFromOffice() != null ? o.getFromOffice().getCode() : null;
            if (rs == ReturnStage.RETURN_PENDING || rs == ReturnStage.RT_DELIVERING || rs == ReturnStage.RT_WH_IN) {
                return office == null ? null : new Classified("TRA", office);
            }
            String to = o.getToOffice() != null ? o.getToOffice().getCode() : office;
            return to == null ? null : new Classified("TON_LC_TRA", to);
        }
        ForwardStage st = o.getForwardStage();
        if (st == null) {
            if (Boolean.TRUE.equals(o.getHomePickup()) && o.getPickedUpAt() == null) {
                String office = o.getFromOffice() != null ? o.getFromOffice().getCode() : null;
                return office == null ? null : new Classified("LAY", office);
            }
            return null;
        }
        String from = o.getFromOffice() != null ? o.getFromOffice().getCode() : null;
        String to = o.getToOffice() != null ? o.getToOffice().getCode() : from;
        return switch (st) {
            case PICKED -> from == null ? null : new Classified("LAY", from);
            case WH_IN, TRANSFER_PENDING, TRANSFERRING -> from == null ? null : new Classified("TON_LC_GIAO", from);
            case DEST_WH_IN, DELIVERING, FAILED -> to == null ? null : new Classified("GIAO", to);
            default -> null;
        };
    }

    private record Point(String kind, Instant at, String office) {}

    private static List<Point> pointsOf(ShipmentOrder o) {
        List<Point> out = new ArrayList<>();
        String from = o.getFromOffice() != null ? o.getFromOffice().getCode() : null;
        String to = o.getToOffice() != null ? o.getToOffice().getCode() : from;
        if (o.getStatus() != OrderStatus.DRAFT && from != null) {
            Instant created = ageBase(o);
            out.add(new Point("LAY", created, from));
        }
        ForwardStage st = o.getForwardStage();
        ReturnStage rs = o.getReturnStage();
        boolean wh =
            (st == ForwardStage.WH_IN || st == ForwardStage.DEST_WH_IN || st == ForwardStage.DELIVERING || st == ForwardStage.FAILED) ||
            (rs == ReturnStage.RT_WH_IN || rs == ReturnStage.RT_DELIVERING || rs == ReturnStage.RT_FAILED || rs == ReturnStage.RT_DONE);
        if (wh) {
            Instant at = ageBase(o);
            String office = rs != null ? from : to;
            if (office != null) out.add(new Point("GIAO_TRA", at, office));
        }
        boolean lc =
            (st == ForwardStage.TRANSFER_PENDING || st == ForwardStage.TRANSFERRING || st == ForwardStage.DEST_WH_IN) ||
            (rs == ReturnStage.RT_TRANSFER_PENDING || rs == ReturnStage.RT_TRANSFERRING);
        if (lc && from != null) {
            out.add(new Point("LUAN_CHUYEN", ageBase(o), from));
        }
        return out;
    }

    private static Instant ageBase(ShipmentOrder o) {
        if (o.getPickedUpAt() != null) return o.getPickedUpAt();
        if (o.getPickingAt() != null) return o.getPickingAt();
        if (o.getLabelPrintedAt() != null) return o.getLabelPrintedAt();
        return Instant.now();
    }
}
