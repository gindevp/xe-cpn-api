package com.mycompany.myapp.service.report;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.service.invoice.InvoicePolicy;
import com.mycompany.myapp.service.order.OrderMoney;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Doanh thu dùng chung cho Báo cáo kinh doanh và Báo cáo doanh thu:
 * <ul>
 *   <li>Phiếu thu: phiếu đã xác nhận, ngày thu tiền trong khoảng, tính cho VP lập phiếu. Mỗi dòng chỉ tính phần cước,
 *       tối đa bằng cước đơn — phần vượt là COD thu hộ, không phải doanh thu. Đơn trên nhiều phiếu: phiếu lập trước
 *       ăn cước trước (khớp thứ tự trừ của phiếu thu: VP gửi → cước phía giao → COD).</li>
 *   <li>Đơn tồn: tạo trong khoảng, chưa kết thúc (giao / huỷ / hoàn xong) tại cuối khoảng, tính cho VP gửi;
 *       doanh thu = cước còn lại sau khi trừ phần cước đã lên phiếu thu của kỳ (không cộng 2 lần).</li>
 * </ul>
 */
@Component
@Transactional(readOnly = true)
public class RevenueLedger {

    private static final int ID_CHUNK = 1000;
    private static final List<OrderStatus> TERMINAL = List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.RETURNED);
    private static final List<String> END_ACTIONS;

    static {
        List<String> end = new ArrayList<>(InvoicePolicy.DONE_ACTIONS);
        end.add("CANCEL");
        end.add("AUTO_CANCEL");
        END_ACTIONS = List.copyOf(end);
    }

    /** Một dòng phiếu thu; {@code fare} = phần cước (đã bỏ COD). */
    public record ReceiptLine(String receiptCode, String officeCode, Long orderId, BigDecimal fare) {}

    /** Đơn tồn; {@code remaining} = cước chưa lên phiếu thu của kỳ. */
    public record BacklogOrder(ShipmentOrder order, String officeCode, BigDecimal remaining) {}

    /** Dòng phiếu thu của một đơn, theo thứ tự lập phiếu. */
    record LineAmount(Long lineId, Long receiptId, BigDecimal amount) {}

    private final EntityManager em;
    private final ReceiptRepository receiptRepository;

    public RevenueLedger(EntityManager em, ReceiptRepository receiptRepository) {
        this.em = em;
        this.receiptRepository = receiptRepository;
    }

    /** Dòng phiếu thu đã xác nhận có ngày thu tiền trong [start, end), mọi VP. */
    public List<ReceiptLine> receiptLines(Instant start, Instant end) {
        List<Long> receiptIds = receiptRepository.findListIds(null, null, null, null, null, null, null, "CONFIRMED", start, end);
        if (receiptIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < receiptIds.size(); i += ID_CHUNK) {
            rows.addAll(
                em
                    .createQuery(
                        "select l.id, r.receiptCode, ro.code, o.id from ReceiptOrderLine l join l.receipt r left join r.office ro" +
                        " join l.order o where r.id in :ids",
                        Object[].class
                    )
                    .setParameter("ids", receiptIds.subList(i, Math.min(receiptIds.size(), i + ID_CHUNK)))
                    .getResultList()
            );
        }
        Set<Long> orderIds = new LinkedHashSet<>();
        for (Object[] r : rows) {
            orderIds.add((Long) r[3]);
        }
        Map<Long, BigDecimal> fareShares = fareSharesByLine(new ArrayList<>(orderIds));
        List<ReceiptLine> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            out.add(new ReceiptLine((String) r[1], key(r[2]), (Long) r[3], fareShares.getOrDefault((Long) r[0], BigDecimal.ZERO)));
        }
        return out;
    }

    /** Đơn tồn của khoảng; {@code periodLines} = {@link #receiptLines} cùng khoảng (mọi VP) để trừ phần đã lên phiếu. */
    public List<BacklogOrder> backlog(Instant start, Instant end, List<ReceiptLine> periodLines) {
        List<Object[]> rows = em
            .createQuery(
                "select o, f.code from ShipmentOrder o left join o.fromOffice f" +
                " where o.createdAt >= :start and o.createdAt < :end and o.status <> :draft" +
                " and (o.status not in :terminal or exists (select 1 from OrderEvent e where e.order = o" +
                " and upper(e.action) in :endActs and e.eventAt >= :end))",
                Object[].class
            )
            .setParameter("start", start)
            .setParameter("end", end)
            .setParameter("draft", OrderStatus.DRAFT)
            .setParameter("terminal", TERMINAL)
            .setParameter("endActs", END_ACTIONS)
            .getResultList();
        Map<Long, BigDecimal> receipted = new HashMap<>();
        for (ReceiptLine l : periodLines) {
            receipted.merge(l.orderId(), l.fare(), BigDecimal::add);
        }
        List<BacklogOrder> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            ShipmentOrder o = (ShipmentOrder) r[0];
            BigDecimal remaining = OrderMoney.nz(o.getFareAmount())
                .subtract(receipted.getOrDefault(o.getId(), BigDecimal.ZERO))
                .max(BigDecimal.ZERO);
            out.add(new BacklogOrder(o, key(r[1]), remaining));
        }
        return out;
    }

    /** lineId → phần cước của dòng, xét mọi dòng phiếu thu (kể cả chưa xác nhận) của các đơn. */
    private Map<Long, BigDecimal> fareSharesByLine(List<Long> orderIds) {
        Map<Long, BigDecimal> fares = new HashMap<>();
        Map<Long, List<LineAmount>> linesByOrder = new HashMap<>();
        for (int i = 0; i < orderIds.size(); i += ID_CHUNK) {
            List<Long> chunk = orderIds.subList(i, Math.min(orderIds.size(), i + ID_CHUNK));
            for (Object[] r : em
                .createQuery("select o.id, o.fareAmount from ShipmentOrder o where o.id in :ids", Object[].class)
                .setParameter("ids", chunk)
                .getResultList()) {
                fares.put((Long) r[0], OrderMoney.nz((BigDecimal) r[1]));
            }
            for (Object[] r : em
                .createQuery(
                    "select l.order.id, l.id, l.receipt.id, l.amountCollected from ReceiptOrderLine l where l.order.id in :ids",
                    Object[].class
                )
                .setParameter("ids", chunk)
                .getResultList()) {
                linesByOrder
                    .computeIfAbsent((Long) r[0], x -> new ArrayList<>())
                    .add(new LineAmount((Long) r[1], (Long) r[2], OrderMoney.nz((BigDecimal) r[3])));
            }
        }
        Map<Long, BigDecimal> out = new HashMap<>();
        for (var e : linesByOrder.entrySet()) {
            out.putAll(allocateFare(fares.getOrDefault(e.getKey(), BigDecimal.ZERO), e.getValue()));
        }
        return out;
    }

    /** Chia cước cho các dòng phiếu theo thứ tự lập phiếu; phần vượt cước là COD. */
    static Map<Long, BigDecimal> allocateFare(BigDecimal fare, List<LineAmount> lines) {
        List<LineAmount> sorted = new ArrayList<>(lines);
        sorted.sort(Comparator.comparing(LineAmount::receiptId).thenComparing(LineAmount::lineId));
        BigDecimal left = OrderMoney.nz(fare).max(BigDecimal.ZERO);
        Map<Long, BigDecimal> out = new HashMap<>();
        for (LineAmount l : sorted) {
            BigDecimal share = OrderMoney.nz(l.amount()).max(BigDecimal.ZERO).min(left);
            left = left.subtract(share);
            out.put(l.lineId(), share);
        }
        return out;
    }

    static String key(Object code) {
        return code == null ? "" : code.toString().trim().toUpperCase(Locale.ROOT);
    }
}
