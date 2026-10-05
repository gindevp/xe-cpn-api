package com.mycompany.myapp.service.report;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.ReceiptRepository;
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
 *   <li>DT phiếu thu = tổng phiếu thu đã xác nhận, lọc theo ngày thu tiền, theo VP lập phiếu;</li>
 *   <li>Đơn giao thành công = đơn DELIVERED có lần POD cuối trong khoảng, theo VP giao (VP nhận);</li>
 *   <li>Đơn tồn = đơn tạo trong khoảng, chưa kết thúc (giao / huỷ / hoàn xong) tại cuối khoảng, theo VP gửi; DT đơn tồn = tổng phải thu;</li>
 *   <li>Tổng DT = DT phiếu thu + DT đơn tồn; Tổng số đơn = giao thành công + đơn tồn.</li>
 * </ul>
 * Kỳ so sánh = cùng khoảng ngày lùi 1 tháng.
 */
@Service
@Transactional(readOnly = true)
public class BusinessReportService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_RANGE_DAYS = 366;
    private static final List<OrderStatus> TERMINAL = List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.RETURNED);
    private static final List<String> END_ACTIONS;

    static {
        List<String> end = new ArrayList<>(InvoicePolicy.DONE_ACTIONS);
        end.add("CANCEL");
        end.add("AUTO_CANCEL");
        END_ACTIONS = List.copyOf(end);
    }

    private final EntityManager em;
    private final ReceiptRepository receiptRepository;
    private final OfficeRepository officeRepository;
    private final StaffAccessService staffAccessService;

    public BusinessReportService(
        EntityManager em,
        ReceiptRepository receiptRepository,
        OfficeRepository officeRepository,
        StaffAccessService staffAccessService
    ) {
        this.em = em;
        this.receiptRepository = receiptRepository;
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

    public record Report(
        LocalDate from,
        LocalDate to,
        String officeCode,
        Totals current,
        LocalDate previousFrom,
        LocalDate previousTo,
        Totals previous,
        List<OfficeRow> offices
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
        Totals current = totals(cur, office, delivered, backlog);
        Totals previous = totals(prev, office, deliveredByOffice(prev), backlogByOffice(prev));

        return new Report(f, t, office, current, prev.from(), prev.to(), previous, officeRows(office, delivered, backlog));
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

    private Totals totals(Period p, String office, Map<String, long[]> delivered, Map<String, Object[]> backlog) {
        BigDecimal receipt = receiptRepository.sumListTotal(office, null, null, null, null, null, null, "CONFIRMED", p.start(), p.end());
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

    /** VP gửi → [số đơn tồn, tổng phải thu] của đơn tạo trong khoảng, chưa kết thúc tại cuối khoảng. */
    private Map<String, Object[]> backlogByOffice(Period p) {
        List<Object[]> rows = em
            .createQuery(
                "select f.code, count(o), coalesce(sum(o.fareAmount), 0) from ShipmentOrder o left join o.fromOffice f" +
                " where o.createdAt >= :start and o.createdAt < :end and o.status <> :draft" +
                " and (o.status not in :terminal or exists (select 1 from OrderEvent e where e.order = o" +
                " and upper(e.action) in :endActs and e.eventAt >= :end))" +
                " group by f.code",
                Object[].class
            )
            .setParameter("start", p.start())
            .setParameter("end", p.end())
            .setParameter("draft", OrderStatus.DRAFT)
            .setParameter("terminal", TERMINAL)
            .setParameter("endActs", END_ACTIONS)
            .getResultList();
        Map<String, Object[]> out = new LinkedHashMap<>();
        for (Object[] r : rows) {
            BigDecimal sum = r[2] instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(r[2]));
            out.put(key(r[0]), new Object[] { ((Number) r[1]).longValue(), sum });
        }
        return out;
    }

    private List<OfficeRow> officeRows(String office, Map<String, long[]> delivered, Map<String, Object[]> backlog) {
        Map<String, String> names = new TreeMap<>();
        for (Office o : officeRepository.findAll()) {
            if (o.getCode() != null) {
                names.put(o.getCode().toUpperCase(Locale.ROOT), o.getName());
            }
        }
        java.util.Set<String> codes = new java.util.LinkedHashSet<>();
        codes.addAll(delivered.keySet());
        codes.addAll(backlog.keySet());
        List<OfficeRow> out = new ArrayList<>();
        for (String code : codes) {
            if (office != null && !office.equalsIgnoreCase(code)) {
                continue;
            }
            long d = delivered.containsKey(code) ? delivered.get(code)[0] : 0;
            long b = backlog.containsKey(code) ? (Long) backlog.get(code)[0] : 0;
            if (d == 0 && b == 0) {
                continue;
            }
            out.add(new OfficeRow(code, names.getOrDefault(code, code.isEmpty() ? "Chưa rõ VP" : code), d, b));
        }
        out.sort(Comparator.comparing(OfficeRow::officeName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static String key(Object code) {
        return code == null ? "" : code.toString().trim().toUpperCase(Locale.ROOT);
    }
}
