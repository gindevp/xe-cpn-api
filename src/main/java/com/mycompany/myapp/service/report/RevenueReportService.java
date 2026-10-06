package com.mycompany.myapp.service.report;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.order.OrderMoney;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo doanh thu theo đơn — cùng logic và tổng với Báo cáo kinh doanh ({@link RevenueLedger}):
 * <ul>
 *   <li>Dòng phiếu thu: phiếu đã xác nhận có ngày thu tiền trong khoảng, tính cho VP lập phiếu, chỉ phần cước (bỏ COD).</li>
 *   <li>Dòng đơn tồn: đơn tạo trong khoảng chưa kết thúc tại cuối khoảng, tính cho VP gửi, cước còn lại chưa lên phiếu thu.</li>
 *   <li>Các khoản phí của dòng = khoản phí của đơn × (số tiền dòng / tổng các khoản), làm tròn đồng; chênh làm tròn dồn vào cước hàng.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RevenueReportService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_RANGE_DAYS = 366;
    private static final int ID_CHUNK = 1000;

    public enum Kind {
        ALL,
        BACKLOG,
        RECEIPT,
    }

    public record Row(
        String orderCode,
        Instant createdAt,
        String officeCode,
        String officeName,
        /** RECEIPT = dòng phiếu thu, BACKLOG = đơn tồn còn phải thu. */
        String source,
        String receiptCode,
        OrderStatus status,
        BigDecimal goodsFare,
        BigDecimal deliveryFee,
        BigDecimal pickupFee,
        BigDecimal codFee,
        BigDecimal declaredFee,
        BigDecimal discount,
        BigDecimal total
    ) {}

    public record Totals(
        BigDecimal goodsFare,
        BigDecimal deliveryFee,
        BigDecimal pickupFee,
        BigDecimal codFee,
        BigDecimal declaredFee,
        BigDecimal discount,
        BigDecimal total
    ) {}

    public record Report(LocalDate from, LocalDate to, String officeCode, Kind kind, List<Row> rows, Totals totals) {}

    /** Khoản phí của đơn; fare = goods + delivery + pickup + codFee + declared − discount. */
    record Fees(
        BigDecimal fare,
        BigDecimal goods,
        BigDecimal delivery,
        BigDecimal pickup,
        BigDecimal codFee,
        BigDecimal declared,
        BigDecimal discount
    ) {}

    /** Phần phí ứng với số tiền {@code collected} trong tổng cước. */
    record Share(
        BigDecimal goods,
        BigDecimal delivery,
        BigDecimal pickup,
        BigDecimal codFee,
        BigDecimal declared,
        BigDecimal discount,
        BigDecimal total
    ) {}

    private final EntityManager em;
    private final RevenueLedger revenueLedger;
    private final OfficeRepository officeRepository;
    private final StaffAccessService staffAccessService;

    public RevenueReportService(
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

    public Report revenue(LocalDate from, LocalDate to, String officeCode, Kind kind) {
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
        Kind k = kind == null ? Kind.ALL : kind;
        String office = resolveOffice(officeCode);
        Instant start = f.atStartOfDay(VN).toInstant();
        Instant end = t.plusDays(1).atStartOfDay(VN).toInstant();
        Map<String, String> names = officeNames();
        List<Row> rows = new ArrayList<>();

        List<RevenueLedger.ReceiptLine> lines = revenueLedger.receiptLines(start, end);
        if (k != Kind.BACKLOG) {
            List<RevenueLedger.ReceiptLine> mine = lines
                .stream()
                .filter(l -> l.fare().signum() > 0 && (office == null || office.equals(l.officeCode())))
                .toList();
            Map<Long, ShipmentOrder> orders = ordersById(mine.stream().map(RevenueLedger.ReceiptLine::orderId).distinct().toList());
            for (RevenueLedger.ReceiptLine l : mine) {
                ShipmentOrder o = orders.get(l.orderId());
                if (o != null) {
                    addRow(rows, o, l.officeCode(), "RECEIPT", l.receiptCode(), share(feesOf(o), l.fare()), names);
                }
            }
        }
        if (k != Kind.RECEIPT) {
            for (RevenueLedger.BacklogOrder b : revenueLedger.backlog(start, end, lines)) {
                if (office == null || office.equals(b.officeCode())) {
                    addRow(rows, b.order(), b.officeCode(), "BACKLOG", null, share(feesOf(b.order()), b.remaining()), names);
                }
            }
        }
        rows.sort(
            Comparator.comparing(Row::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Row::orderCode)
                .thenComparing(Row::source, Comparator.reverseOrder())
                .thenComparing(Row::receiptCode, Comparator.nullsLast(Comparator.naturalOrder()))
        );
        return new Report(f, t, office, k, rows, totals(rows));
    }

    private static void addRow(
        List<Row> rows,
        ShipmentOrder o,
        String officeCode,
        String source,
        String receiptCode,
        Share s,
        Map<String, String> names
    ) {
        if (s == null || s.total().signum() <= 0) {
            return;
        }
        rows.add(
            new Row(
                o.getOrderCode(),
                o.getCreatedAt(),
                officeCode,
                names.getOrDefault(officeCode, officeCode.isEmpty() ? "Chưa rõ VP" : officeCode),
                source,
                receiptCode,
                o.getStatus(),
                s.goods(),
                s.delivery(),
                s.pickup(),
                s.codFee(),
                s.declared(),
                s.discount(),
                s.total()
            )
        );
    }

    private Map<Long, ShipmentOrder> ordersById(List<Long> ids) {
        Map<Long, ShipmentOrder> out = new HashMap<>();
        for (int i = 0; i < ids.size(); i += ID_CHUNK) {
            for (ShipmentOrder o : em
                .createQuery("select o from ShipmentOrder o where o.id in :ids", ShipmentOrder.class)
                .setParameter("ids", ids.subList(i, Math.min(ids.size(), i + ID_CHUNK)))
                .getResultList()) {
                out.put(o.getId(), o);
            }
        }
        return out;
    }

    static Fees feesOf(ShipmentOrder o) {
        BigDecimal fare = OrderMoney.nz(o.getFareAmount());
        BigDecimal delivery = OrderMoney.nz(o.getDeliveryFeeAmount());
        BigDecimal pickup = OrderMoney.nz(o.getPickupFeeAmount());
        BigDecimal codFee = OrderMoney.nz(o.getCodFeeAmount());
        BigDecimal declared = OrderMoney.nz(o.getDeclaredFeeAmount());
        BigDecimal discount = OrderMoney.nz(o.getDiscountAmount());
        BigDecimal goods = o.getGoodsFareAmount() != null
            ? o.getGoodsFareAmount()
            : fare.subtract(delivery).subtract(pickup).subtract(codFee).subtract(declared).add(discount).max(BigDecimal.ZERO);
        return new Fees(fare, goods, delivery, pickup, codFee, declared, discount);
    }

    static Share share(Fees fees, BigDecimal collected) {
        BigDecimal total = OrderMoney.nz(collected);
        if (total.signum() <= 0) {
            return null;
        }
        // Chia theo tổng các khoản (= cước đơn khi dữ liệu khớp); đơn cũ lệch (phí tận nơi > cước) vẫn không ra số âm.
        BigDecimal base = fees
            .goods()
            .add(fees.delivery())
            .add(fees.pickup())
            .add(fees.codFee())
            .add(fees.declared())
            .subtract(fees.discount());
        if (base.signum() <= 0) {
            return new Share(total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total);
        }
        BigDecimal delivery = scale(fees.delivery(), total, base);
        BigDecimal pickup = scale(fees.pickup(), total, base);
        BigDecimal codFee = scale(fees.codFee(), total, base);
        BigDecimal declared = scale(fees.declared(), total, base);
        BigDecimal discount = scale(fees.discount(), total, base);
        BigDecimal goods = total.subtract(delivery).subtract(pickup).subtract(codFee).subtract(declared).add(discount);
        return new Share(goods, delivery, pickup, codFee, declared, discount, total);
    }

    private static BigDecimal scale(BigDecimal part, BigDecimal collected, BigDecimal base) {
        if (part.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (collected.compareTo(base) == 0) {
            return part;
        }
        return part.multiply(collected).divide(base, 0, RoundingMode.HALF_UP);
    }

    private static Totals totals(List<Row> rows) {
        BigDecimal g = BigDecimal.ZERO, d = BigDecimal.ZERO, p = BigDecimal.ZERO, c = BigDecimal.ZERO;
        BigDecimal k = BigDecimal.ZERO, disc = BigDecimal.ZERO, t = BigDecimal.ZERO;
        for (Row r : rows) {
            g = g.add(r.goodsFare());
            d = d.add(r.deliveryFee());
            p = p.add(r.pickupFee());
            c = c.add(r.codFee());
            k = k.add(r.declaredFee());
            disc = disc.add(r.discount());
            t = t.add(r.total());
        }
        return new Totals(g, d, p, c, k, disc, t);
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

    private Map<String, String> officeNames() {
        Map<String, String> out = new HashMap<>();
        for (Office o : officeRepository.findAll()) {
            if (o.getCode() != null) {
                out.put(o.getCode().toUpperCase(Locale.ROOT), o.getName());
            }
        }
        return out;
    }

    private static String key(Object code) {
        return code == null ? "" : code.toString().trim().toUpperCase(Locale.ROOT);
    }
}
