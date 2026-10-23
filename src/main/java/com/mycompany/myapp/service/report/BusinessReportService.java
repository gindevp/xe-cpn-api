package com.mycompany.myapp.service.report;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.invoice.InvoicePolicy;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo kinh doanh màn Tổng quan, theo khoảng ngày (giờ VN):
 * <ul>
 *   <li>Tổng DT = cước các đơn tạo trong khoảng (trừ nháp, huỷ), theo VP gửi, đã trừ phí Ahamove
 *       (xem {@link RevenueLedger#createdOrders});</li>
 *   <li>DT đã thu / DT còn phải thu = phần tổng DT đã thu tiền / chưa thu;</li>
 *   <li>Đơn giao thành công = đơn DELIVERED có lần POD cuối trong khoảng, theo VP giao (VP nhận);</li>
 *   <li>Đơn tồn = đơn tạo trong khoảng, chưa kết thúc (giao / huỷ / hoàn xong) tại cuối khoảng, theo VP gửi;
 *       Tổng số đơn = giao thành công + đơn tồn.</li>
 * </ul>
 * Kỳ so sánh = cùng khoảng ngày lùi 1 tháng.
 */
@Service
@Transactional(readOnly = true)
public class BusinessReportService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_RANGE_DAYS = 366;

    private final EntityManager em;
    private final RevenueLedger revenueLedger;
    private final OfficeRepository officeRepository;
    private final StaffAccessService staffAccessService;

    public BusinessReportService(
        EntityManager em,
        RevenueLedger revenueLedger,
        OfficeRepository officeRepository,
        StaffAccessService staffAccessService
    ) {
        this.em = em;
        this.revenueLedger = revenueLedger;
        this.officeRepository = officeRepository;
        this.staffAccessService = staffAccessService;
    }

    public record Totals(
        BigDecimal receiptRevenue,
        BigDecimal backlogRevenue,
        BigDecimal totalRevenue,
        long deliveredCount,
        long backlogCount,
        long totalOrders
    ) {
        static Totals of(BigDecimal receipt, BigDecimal backlog, long delivered, long backlogCount) {
            BigDecimal r = receipt == null ? BigDecimal.ZERO : receipt;
            BigDecimal b = backlog == null ? BigDecimal.ZERO : backlog;
            return new Totals(r, b, r.add(b), delivered, backlogCount, delivered + backlogCount);
        }
    }

    public record OfficeRow(String officeCode, String officeName, long delivered, long backlog) {}

    /**
     * Một ngày trên biểu đồ:
     * <ul>
     *   <li>Đơn gửi = lần nhập kho gửi trong ngày (WAREHOUSE_RECEIVE), theo VP gửi;</li>
     *   <li>Đơn nhận = lần nhập kho giao trong ngày (SCAN_IN / DEST_WH_IN / HUB_IN), theo VP nhận;</li>
     *   <li>Doanh thu trên cột = cước đơn <b>tạo</b> trong ngày (đã trừ phí giao đối tác), theo VP gửi
     *       — cùng {@link RevenueLedger#createdOrders}, độc lập với số gửi/nhận.</li>
     * </ul>
     */
    public record DayRow(LocalDate date, long sent, long received, BigDecimal revenue) {}

    /**
     * Khung 2 giờ (giờ VN): đơn gửi (giờ nhập kho gửi) + đơn nhận (giờ nhập kho giao).
     * {@code slot} 0 = 00–02h … 11 = 22–24h. {@code perOffice} = mã VP (gửi hoặc nhận tùy sự kiện).
     */
    public record HourBucket(String label, int slot, long count, Map<String, Long> perOffice) {}

    /** Sự kiện nhập kho gửi trong khoảng (để xếp khung giờ / kiểm thử). */
    record WhInHit(Instant at, String officeCode, BigDecimal revenue) {}

    /** Bấm «Nhập kho gửi» → OrderEvent WAREHOUSE_RECEIVE (không lấy CONFIRM từ nháp). */
    static final List<String> SENT_WH_ACTIONS = List.of("WAREHOUSE_RECEIVE");
    /** Bấm «Nhập kho giao» / quét xuống xe tại VP nhận. */
    static final List<String> RECEIVED_WH_ACTIONS = List.of("SCAN_IN", "DEST_WH_IN", "HUB_IN");

    public record Report(
        LocalDate from,
        LocalDate to,
        String officeCode,
        Totals current,
        LocalDate previousFrom,
        LocalDate previousTo,
        Totals previous,
        List<OfficeRow> offices,
        List<DayRow> days,
        List<HourBucket> hours
    ) {}

    record Period(LocalDate from, LocalDate to, Instant start, Instant end) {
        static Period of(LocalDate from, LocalDate to) {
            return new Period(from, to, from.atStartOfDay(VN).toInstant(), to.plusDays(1).atStartOfDay(VN).toInstant());
        }

        Period previousMonth() {
            return of(from.minusMonths(1), to.minusMonths(1));
        }
    }

    public Report business(LocalDate from, LocalDate to, String officeCode) {
        LocalDate today = LocalDate.now(VN);
        LocalDate f = from != null ? from : today;
        LocalDate t = to != null ? to : f;
        if (t.isBefore(f)) {
            LocalDate x = f;
            f = t;
            t = x;
        }
        if (ChronoUnit.DAYS.between(f, t) + 1 > MAX_RANGE_DAYS) {
            throw new BadRequestAlertException("Khoảng ngày tối đa " + MAX_RANGE_DAYS + " ngày", "report", "rangeTooLong");
        }
        String office = resolveOffice(officeCode);
        Period cur = Period.of(f, t);
        Period prev = cur.previousMonth();

        Map<String, long[]> delivered = deliveredByOffice(cur);
        Map<String, Object[]> backlog = backlogByOffice(cur);
        List<RevenueLedger.CreatedOrder> created = revenueLedger.createdOrders(cur.start(), cur.end());
        Totals current = totals(office, created, delivered, backlog);
        Totals previous = totals(
            office,
            revenueLedger.createdOrders(prev.start(), prev.end()),
            deliveredByOffice(prev),
            backlogByOffice(prev)
        );

        List<WhInHit> sentHits = warehouseInHits(office, cur, SENT_WH_ACTIONS, true);
        List<WhInHit> receivedHits = warehouseInHits(office, cur, RECEIVED_WH_ACTIONS, false);
        List<WhInHit> hourHits = new ArrayList<>(sentHits.size() + receivedHits.size());
        hourHits.addAll(sentHits);
        hourHits.addAll(receivedHits);
        return new Report(
            f,
            t,
            office,
            current,
            prev.from(),
            prev.to(),
            previous,
            officeRows(office, delivered, backlog),
            dayRows(office, cur, sentHits, receivedHits, created),
            hourBuckets(hourHits)
        );
    }

    private String resolveOffice(String requested) {
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        if (scoped != null && !scoped.isBlank()) {
            return scoped.trim().toUpperCase(Locale.ROOT);
        }
        if (requested == null || requested.isBlank() || "ALL".equalsIgnoreCase(requested.trim())) {
            return null;
        }
        return requested.trim().toUpperCase(Locale.ROOT);
    }

    private Totals totals(
        String office,
        List<RevenueLedger.CreatedOrder> created,
        Map<String, long[]> delivered,
        Map<String, Object[]> backlog
    ) {
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal due = BigDecimal.ZERO;
        for (RevenueLedger.CreatedOrder c : created) {
            if (office == null || office.equalsIgnoreCase(c.officeCode())) {
                collected = collected.add(c.collected());
                due = due.add(c.revenue().subtract(c.collected()));
            }
        }
        long d = 0;
        long b = 0;
        for (var e : delivered.entrySet()) {
            if (office == null || office.equalsIgnoreCase(e.getKey())) {
                d += e.getValue()[0];
            }
        }
        for (var e : backlog.entrySet()) {
            if (office == null || office.equalsIgnoreCase(e.getKey())) {
                b += (Long) e.getValue()[0];
            }
        }
        return Totals.of(collected, due, d, b);
    }

    /** VP giao (finalTo → to) → số đơn có lần POD cuối trong khoảng. */
    private Map<String, long[]> deliveredByOffice(Period p) {
        List<Object[]> rows = em
            .createQuery(
                "select coalesce(ft.code, t.code), count(o) from ShipmentOrder o left join o.finalToOffice ft left join o.toOffice t" +
                " where o.status = :delivered" +
                " and exists (select 1 from OrderEvent e where e.order = o and upper(e.action) in :acts and e.eventAt >= :start and e.eventAt < :end)" +
                " and not exists (select 1 from OrderEvent e2 where e2.order = o and upper(e2.action) in :acts and e2.eventAt >= :end)" +
                " group by coalesce(ft.code, t.code)",
                Object[].class
            )
            .setParameter("delivered", OrderStatus.DELIVERED)
            .setParameter("acts", InvoicePolicy.DELIVERED_ACTIONS)
            .setParameter("start", p.start())
            .setParameter("end", p.end())
            .getResultList();
        Map<String, long[]> out = new LinkedHashMap<>();
        for (Object[] r : rows) {
            out.put(key(r[0]), new long[] { ((Number) r[1]).longValue() });
        }
        return out;
    }

    /** VP gửi → [số đơn tồn]. */
    private Map<String, Object[]> backlogByOffice(Period p) {
        Map<String, Object[]> out = new LinkedHashMap<>();
        for (RevenueLedger.BacklogOrder b : revenueLedger.backlog(p.start(), p.end(), List.of())) {
            Object[] agg = out.computeIfAbsent(b.officeCode(), x -> new Object[] { 0L });
            agg[0] = (Long) agg[0] + 1;
        }
        return out;
    }

    private List<OfficeRow> officeRows(String office, Map<String, long[]> delivered, Map<String, Object[]> backlog) {
        Map<String, String> names = new TreeMap<>();
        java.util.Set<String> active = new java.util.HashSet<>();
        for (Office o : officeRepository.findAll()) {
            if (o.getCode() != null) {
                String code = o.getCode().toUpperCase(Locale.ROOT);
                names.put(code, o.getName());
                if (!Boolean.FALSE.equals(o.getActive())) {
                    active.add(code);
                }
            }
        }
        // VP đang hoạt động luôn hiện (kể cả 0 đơn); VP ngừng chỉ hiện khi còn số liệu.
        java.util.Set<String> codes = new java.util.LinkedHashSet<>(active);
        codes.addAll(delivered.keySet());
        codes.addAll(backlog.keySet());
        List<OfficeRow> out = new ArrayList<>();
        for (String code : codes) {
            if (office != null && !office.equalsIgnoreCase(code)) {
                continue;
            }
            long d = delivered.containsKey(code) ? delivered.get(code)[0] : 0;
            long b = backlog.containsKey(code) ? (Long) backlog.get(code)[0] : 0;
            if (d == 0 && b == 0 && !active.contains(code)) {
                continue;
            }
            out.add(new OfficeRow(code, names.getOrDefault(code, code.isEmpty() ? "Chưa rõ VP" : code), d, b));
        }
        out.sort(Comparator.comparing(OfficeRow::officeName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private List<DayRow> dayRows(
        String office,
        Period p,
        List<WhInHit> sentHits,
        List<WhInHit> receivedHits,
        List<RevenueLedger.CreatedOrder> created
    ) {
        Map<LocalDate, long[]> sent = new TreeMap<>();
        for (WhInHit h : sentHits) {
            if (h.at() == null) {
                continue;
            }
            LocalDate day = h.at().atZone(VN).toLocalDate();
            sent.computeIfAbsent(day, d -> new long[] { 0 })[0]++;
        }

        Map<LocalDate, Long> received = new TreeMap<>();
        for (WhInHit h : receivedHits) {
            if (h.at() == null) {
                continue;
            }
            LocalDate day = h.at().atZone(VN).toLocalDate();
            received.merge(day, 1L, Long::sum);
        }

        // Tiền trên cột: giữ logic cũ — theo ngày tạo đơn / VP gửi (createdOrders).
        Map<LocalDate, BigDecimal> revenue = new TreeMap<>();
        for (RevenueLedger.CreatedOrder c : created) {
            Instant createdAt = c.order().getCreatedAt();
            if (createdAt == null || (office != null && !office.equalsIgnoreCase(c.officeCode()))) {
                continue;
            }
            LocalDate day = createdAt.atZone(VN).toLocalDate();
            revenue.merge(day, c.revenue() == null ? BigDecimal.ZERO : c.revenue(), BigDecimal::add);
        }

        List<DayRow> out = new ArrayList<>();
        for (LocalDate d = p.from(); !d.isAfter(p.to()); d = d.plusDays(1)) {
            long s = sent.containsKey(d) ? sent.get(d)[0] : 0;
            long rc = received.getOrDefault(d, 0L);
            out.add(new DayRow(d, s, rc, revenue.getOrDefault(d, BigDecimal.ZERO)));
        }
        return out;
    }

    /**
     * Mỗi đơn một lần trong khoảng: lấy min(eventAt) của các action nhập kho.
     * {@code byFromOffice=true} → VP gửi; false → VP nhận (finalTo / to).
     */
    private List<WhInHit> warehouseInHits(String office, Period p, List<String> actions, boolean byFromOffice) {
        String officeKey = office == null ? "" : office.trim().toUpperCase(Locale.ROOT);
        String jpql = byFromOffice
            ? "select o.id, min(e.eventAt), coalesce(f.code, ''), o.fareAmount, o.partnerCode, o.partnerStatus, o.partnerFeeAmount" +
            " from OrderEvent e join e.order o left join o.fromOffice f" +
            " where upper(e.action) in :acts and e.eventAt >= :start and e.eventAt < :end" +
            " and o.status <> :draft" +
            " and (:office = '' or upper(coalesce(f.code, '')) = :office)" +
            " group by o.id, f.code, o.fareAmount, o.partnerCode, o.partnerStatus, o.partnerFeeAmount"
            : "select o.id, min(e.eventAt), coalesce(ft.code, t.code, ''), o.fareAmount, o.partnerCode, o.partnerStatus, o.partnerFeeAmount" +
            " from OrderEvent e join e.order o left join o.finalToOffice ft left join o.toOffice t" +
            " where upper(e.action) in :acts and e.eventAt >= :start and e.eventAt < :end" +
            " and o.status <> :draft" +
            " and (:office = '' or upper(coalesce(ft.code, t.code, '')) = :office)" +
            " group by o.id, ft.code, t.code, o.fareAmount, o.partnerCode, o.partnerStatus, o.partnerFeeAmount";
        List<Object[]> rows = em
            .createQuery(jpql, Object[].class)
            .setParameter("acts", actions)
            .setParameter("start", p.start())
            .setParameter("end", p.end())
            .setParameter("draft", OrderStatus.DRAFT)
            .setParameter("office", officeKey)
            .getResultList();
        List<WhInHit> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            if (r[1] == null) {
                continue;
            }
            Instant at = (Instant) r[1];
            String code = key(r[2]);
            BigDecimal rev = RevenueLedger.netFare((BigDecimal) r[3], (String) r[4], (String) r[5], (BigDecimal) r[6]);
            out.add(new WhInHit(at, code, rev));
        }
        return out;
    }

    /** 12 khung 2 giờ (giờ VN) — nhập kho gửi + nhập kho giao. */
    static List<HourBucket> hourBuckets(List<WhInHit> hits) {
        @SuppressWarnings("unchecked")
        Map<String, Long>[] perSlot = new Map[12];
        long[] totals = new long[12];
        for (int i = 0; i < 12; i++) {
            perSlot[i] = new LinkedHashMap<>();
        }
        for (WhInHit h : hits) {
            if (h.at() == null) {
                continue;
            }
            int hour = h.at().atZone(VN).getHour();
            int slot = Math.min(11, Math.max(0, hour / 2));
            String code = h.officeCode() == null ? "" : h.officeCode().trim().toUpperCase(Locale.ROOT);
            perSlot[slot].merge(code, 1L, Long::sum);
            totals[slot]++;
        }
        List<HourBucket> out = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            String label = String.format("%02d-%02dh", i * 2, i * 2 + 2);
            out.add(new HourBucket(label, i, totals[i], Map.copyOf(perSlot[i])));
        }
        return out;
    }

    private static String key(Object code) {
        return code == null ? "" : code.toString().trim().toUpperCase(Locale.ROOT);
    }
}
