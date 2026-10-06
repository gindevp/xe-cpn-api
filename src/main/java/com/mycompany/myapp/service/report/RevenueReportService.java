package com.mycompany.myapp.service.report;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.finance.ReceiptSettlement;
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
 * Báo cáo doanh thu theo đơn: tiền thu ở VP nào tính doanh thu cho VP đó.
 * <ul>
 *   <li>Lọc đơn theo ngày tạo (giờ VN). Đơn tồn = chưa giao thành công (đang ở kho / trên xe / chờ giao); đơn giao thành công = DELIVERED.
 *       Đơn huỷ / hoàn không tính.</li>
 *   <li>Tiền thu phía gửi (thu trước, thu tay, phiếu thu phía gửi) → VP gửi; tiền thu lúc giao (POD / phiếu thu phía giao) → VP nhận.
 *       Đơn thu ở cả hai phía hiện ở cả 2 VP, mỗi VP phần mình thu. Tiền COD thu hộ không phải doanh thu.</li>
 *   <li>Các khoản phí của dòng = khoản phí của đơn × (tiền VP thu / tổng các khoản), làm tròn đồng; chênh làm tròn dồn vào cước hàng.</li>
 *   <li>Chưa thu đồng nào (vd. đơn tồn nhận trả, đơn công nợ chưa trả) → không có doanh thu, không hiện.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RevenueReportService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_RANGE_DAYS = 366;
    private static final int ID_CHUNK = 1000;

    static final List<OrderStatus> BACKLOG = List.of(
        OrderStatus.CONFIRMED,
        OrderStatus.WAITING,
        OrderStatus.IN_TRANSIT,
        OrderStatus.AT_DEST,
        OrderStatus.OUT_FOR_DELIVERY,
        OrderStatus.FAILED_DELIVERY
    );

    public enum Kind {
        ALL,
        BACKLOG,
        DELIVERED,
    }

    public record Row(
        String orderCode,
        Instant createdAt,
        String officeCode,
        String officeName,
        /** SENDER = tiền thu phía gửi, DELIVERY = tiền thu lúc giao. */
        String side,
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
    private final OfficeRepository officeRepository;
    private final StaffAccessService staffAccessService;

    public RevenueReportService(EntityManager em, OfficeRepository officeRepository, StaffAccessService staffAccessService) {
        this.em = em;
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
        List<OrderStatus> statuses =
            switch (k) {
                case BACKLOG -> BACKLOG;
                case DELIVERED -> List.of(OrderStatus.DELIVERED);
                case ALL -> {
                    List<OrderStatus> all = new ArrayList<>(BACKLOG);
                    all.add(OrderStatus.DELIVERED);
                    yield all;
                }
            };

        String jpql =
            "select o, f.code, coalesce(ft.code, tt.code) from ShipmentOrder o left join o.fromOffice f" +
            " left join o.finalToOffice ft left join o.toOffice tt" +
            " where o.createdAt >= :start and o.createdAt < :end and o.status in :statuses" +
            (office != null ? " and (upper(f.code) = :office or upper(coalesce(ft.code, tt.code)) = :office)" : "");
        var query = em
            .createQuery(jpql, Object[].class)
            .setParameter("start", f.atStartOfDay(VN).toInstant())
            .setParameter("end", t.plusDays(1).atStartOfDay(VN).toInstant())
            .setParameter("statuses", statuses);
        if (office != null) {
            query.setParameter("office", office);
        }
        List<Object[]> orders = query.getResultList();

        Map<Long, BigDecimal[]> paid = paidBySide(orders.stream().map(r -> ((ShipmentOrder) r[0]).getId()).toList());
        Map<String, String> names = officeNames();

        List<Row> rows = new ArrayList<>();
        for (Object[] r : orders) {
            ShipmentOrder o = (ShipmentOrder) r[0];
            String fromCode = key(r[1]);
            String toCode = key(r[2]);
            BigDecimal[] sides = paid.getOrDefault(o.getId(), new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            Fees fees = feesOf(o);
            addRow(rows, o, fromCode, "SENDER", share(fees, sides[0]), office, names);
            addRow(rows, o, toCode, "DELIVERY", share(fees, sides[1]), office, names);
        }
        rows.sort(
            Comparator.comparing(Row::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Row::orderCode)
                .thenComparing(Row::side, Comparator.reverseOrder())
        );
        return new Report(f, t, office, k, rows, totals(rows));
    }

    private void addRow(
        List<Row> rows,
        ShipmentOrder o,
        String officeCode,
        String side,
        Share s,
        String office,
        Map<String, String> names
    ) {
        if (s == null || s.total().signum() <= 0) {
            return;
        }
        if (office != null && !office.equals(officeCode)) {
            return;
        }
        rows.add(
            new Row(
                o.getOrderCode(),
                o.getCreatedAt(),
                officeCode,
                names.getOrDefault(officeCode, officeCode.isEmpty() ? "Chưa rõ VP" : officeCode),
                side,
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

    /** orderId → [thu phía gửi, thu lúc giao]; chỉ tính cước (TRUOC / SAU), bỏ COD thu hộ. */
    private Map<Long, BigDecimal[]> paidBySide(List<Long> ids) {
        Map<Long, BigDecimal[]> out = new HashMap<>();
        for (int i = 0; i < ids.size(); i += ID_CHUNK) {
            List<Object[]> rows = em
                .createQuery(
                    "select p.order.id, p.paymentKind, p.note, p.amount from OrderPayment p" +
                    " where p.order.id in :ids and p.paymentKind in :kinds",
                    Object[].class
                )
                .setParameter("ids", ids.subList(i, Math.min(ids.size(), i + ID_CHUNK)))
                .setParameter("kinds", List.of(PaymentKind.TRUOC, PaymentKind.SAU))
                .getResultList();
            for (Object[] r : rows) {
                BigDecimal[] sides = out.computeIfAbsent((Long) r[0], x -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
                int idx = ReceiptSettlement.isDeliverySidePayment((PaymentKind) r[1], (String) r[2]) ? 1 : 0;
                sides[idx] = sides[idx].add(OrderMoney.nz((BigDecimal) r[3]));
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
