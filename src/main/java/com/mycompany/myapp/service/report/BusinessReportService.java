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
 *   <li>DT phiếu thu = phần cước (bỏ COD thu hộ) của phiếu thu đã xác nhận, lọc theo ngày thu tiền, theo VP lập phiếu;</li>
 *   <li>Đơn giao thành công = đơn DELIVERED có lần POD cuối trong khoảng, theo VP giao (VP nhận);</li>
 *   <li>Đơn tồn = đơn tạo trong khoảng, chưa kết thúc (giao / huỷ / hoàn xong) tại cuối khoảng, theo VP gửi;
 *       DT đơn tồn = cước còn lại chưa lên phiếu thu của kỳ (xem {@link RevenueLedger});</li>
 *   <li>Tổng DT = DT phiếu thu + DT đơn tồn; Tổng số đơn = giao thành công + đơn tồn.</li>
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

    /** Một ngày trên biểu đồ: đơn gửi (tạo trong ngày), đơn nhận (giao trong ngày), doanh thu = tổng cước đơn gửi. */
    public record DayRow(LocalDate date, long sent, long received, BigDecimal revenue) {}

    public record Report(
        LocalDate from,
        LocalDate to,
        String officeCode,
        Totals current,
        LocalDate previousFrom,
        LocalDate previousTo,
        Totals previous,
        List<OfficeRow> offices,
        List<DayRow> days
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
        List<RevenueLedger.ReceiptLine> curLines = revenueLedger.receiptLines(cur.start(), cur.end());
        Map<String, Object[]> backlog = backlogByOffice(cur, curLines);
        Totals current = totals(office, curLines, delivered, backlog);
        List<RevenueLedger.ReceiptLine> prevLines = revenueLedger.receiptLines(prev.start(), prev.end());
        Totals previous = totals(office, prevLines, deliveredByOffice(prev), backlogByOffice(prev, prevLines));

        return new Report(
            f,
            t,
            office,
            current,
            prev.from(),
            prev.to(),
            previous,
            officeRows(office, delivered, backlog),
            dayRows(office, cur)
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
        List<RevenueLedger.ReceiptLine> lines,
        Map<String, long[]> delivered,
        Map<String, Object[]> backlog
    ) {
        BigDecimal receipt = BigDecimal.ZERO;
        for (RevenueLedger.ReceiptLine l : lines) {
            if (office == null || office.equalsIgnoreCase(l.officeCode())) {
                receipt = receipt.add(l.fare());
            }
        }
        long d = 0;
        long b = 0;
        BigDecimal bRev = BigDecimal.ZERO;
        for (var e : delivered.entrySet()) {
            if (office == null || office.equalsIgnoreCase(e.getKey())) {
                d += e.getValue()[0];
            }
        }
        for (var e : backlog.entrySet()) {
            if (office == null || office.equalsIgnoreCase(e.getKey())) {
                b += (Long) e.getValue()[0];
                bRev = bRev.add((BigDecimal) e.getValue()[1]);
            }
        }
        return Totals.of(receipt, bRev, d, b);
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

    /** VP gửi → [số đơn tồn, cước còn lại chưa lên phiếu thu của kỳ]. */
    private Map<String, Object[]> backlogByOffice(Period p, List<RevenueLedger.ReceiptLine> periodLines) {
        Map<String, Object[]> out = new LinkedHashMap<>();
        for (RevenueLedger.BacklogOrder b : revenueLedger.backlog(p.start(), p.end(), periodLines)) {
            Object[] agg = out.computeIfAbsent(b.officeCode(), x -> new Object[] { 0L, BigDecimal.ZERO });
            agg[0] = (Long) agg[0] + 1;
            agg[1] = ((BigDecimal) agg[1]).add(b.remaining());
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

    private List<DayRow> dayRows(String office, Period p) {
        Map<LocalDate, long[]> sent = new TreeMap<>();
        Map<LocalDate, BigDecimal> revenue = new TreeMap<>();
        String officeKey = office == null ? "" : office;
        List<Object[]> created = em
            .createQuery(
                "select o.createdAt, o.fareAmount from ShipmentOrder o left join o.fromOffice fr" +
                " where o.createdAt >= :start and o.createdAt < :end" +
                " and (:office = '' or upper(fr.code) = :office)",
                Object[].class
            )
            .setParameter("start", p.start())
            .setParameter("end", p.end())
            .setParameter("office", officeKey)
            .getResultList();
        for (Object[] r : created) {
            if (r[0] == null) {
                continue;
            }
            LocalDate day = ((Instant) r[0]).atZone(VN).toLocalDate();
            sent.computeIfAbsent(day, d -> new long[] { 0 })[0]++;
            BigDecimal fare = r[1] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            revenue.merge(day, fare, BigDecimal::add);
        }

        Map<LocalDate, Long> received = new TreeMap<>();
        List<Object[]> pods = em
            .createQuery(
                "select o.id, e.eventAt from OrderEvent e join e.order o left join o.finalToOffice ft left join o.toOffice t" +
                " where o.status = :delivered and upper(e.action) in :acts" +
                " and e.eventAt >= :start and e.eventAt < :end" +
                " and (:office = '' or upper(coalesce(ft.code, t.code)) = :office)" +
                " and not exists (select 1 from OrderEvent e2 where e2.order = o and upper(e2.action) in :acts and e2.eventAt > e.eventAt)",
                Object[].class
            )
            .setParameter("delivered", OrderStatus.DELIVERED)
            .setParameter("acts", InvoicePolicy.DELIVERED_ACTIONS)
            .setParameter("start", p.start())
            .setParameter("end", p.end())
            .setParameter("office", officeKey)
            .getResultList();
        java.util.Set<Object> seen = new java.util.HashSet<>();
        for (Object[] r : pods) {
            if (r[0] == null || r[1] == null || !seen.add(r[0])) {
                continue;
            }
            LocalDate day = ((Instant) r[1]).atZone(VN).toLocalDate();
            received.merge(day, 1L, Long::sum);
        }

        List<DayRow> out = new ArrayList<>();
        for (LocalDate d = p.from(); !d.isAfter(p.to()); d = d.plusDays(1)) {
            long s = sent.containsKey(d) ? sent.get(d)[0] : 0;
            long rc = received.getOrDefault(d, 0L);
            out.add(new DayRow(d, s, rc, revenue.getOrDefault(d, BigDecimal.ZERO)));
        }
        return out;
    }

    private static String key(Object code) {
        return code == null ? "" : code.toString().trim().toUpperCase(Locale.ROOT);
    }
}
