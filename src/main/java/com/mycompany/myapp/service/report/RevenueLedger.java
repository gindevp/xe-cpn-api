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
 * Doanh thu dùng chung cho Báo cáo kinh doanh và Báo cáo doanh thu ({@link #createdOrders}):
 * <ul>
 *   <li>Đơn tạo trong khoảng (theo ngày tạo đơn), trừ nháp và đơn huỷ, tính cho VP gửi.</li>
 *   <li>Doanh thu = cước khách trả − phí Ahamove đã lưu trên đơn (không âm). Không gồm tiền COD thu hộ.</li>
 *   <li>Đã thu = tiền cước đã thu của đơn (không vượt doanh thu); còn phải thu = phần còn lại.</li>
 * </ul>
 * {@link #receiptLines} / {@link #backlog} giữ cho số đơn tồn và các màn đối soát phiếu thu.
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

    /** Đơn tính doanh thu theo ngày tạo; {@code revenue} = cước đã trừ phí Ahamove, {@code collected} = phần đã thu. */
    public record CreatedOrder(ShipmentOrder order, String officeCode, BigDecimal revenue, BigDecimal collected) {}

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
            BigDecimal remaining = netFare(o.getFareAmount(), o.getPartnerCode(), o.getPartnerStatus(), o.getPartnerFeeAmount())
                .subtract(receipted.getOrDefault(o.getId(), BigDecimal.ZERO))
                .max(BigDecimal.ZERO);
            out.add(new BacklogOrder(o, key(r[1]), remaining));
        }
        return out;
    }

    /** Đơn tạo trong [start, end), trừ nháp và huỷ, tính cho VP gửi. */
    public List<CreatedOrder> createdOrders(Instant start, Instant end) {
        List<Object[]> rows = em
            .createQuery(
                "select o, f.code from ShipmentOrder o left join o.fromOffice f" +
                " where o.createdAt >= :start and o.createdAt < :end and o.status not in :excluded",
                Object[].class
            )
            .setParameter("start", start)
            .setParameter("end", end)
            .setParameter("excluded", List.of(OrderStatus.DRAFT, OrderStatus.CANCELLED))
            .getResultList();
        List<CreatedOrder> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            ShipmentOrder o = (ShipmentOrder) r[0];
            BigDecimal revenue = netFare(o.getFareAmount(), o.getPartnerCode(), o.getPartnerStatus(), o.getPartnerFeeAmount());
            out.add(new CreatedOrder(o, key(r[1]), revenue, collectedOf(revenue, o.getPaidAmount())));
        }
        return out;
    }

    /** Phần doanh thu đã thu: tiền cước đã thu, không vượt doanh thu của đơn. */
    static BigDecimal collectedOf(BigDecimal revenue, BigDecimal paid) {
        return OrderMoney.nz(paid).max(BigDecimal.ZERO).min(OrderMoney.nz(revenue));
    }

    /** lineId → phần cước của dòng, xét mọi dòng phiếu thu (kể cả chưa xác nhận) của các đơn. */
    private Map<Long, BigDecimal> fareSharesByLine(List<Long> orderIds) {
        Map<Long, BigDecimal> fares = new HashMap<>();
        Map<Long, List<LineAmount>> linesByOrder = new HashMap<>();
        for (int i = 0; i < orderIds.size(); i += ID_CHUNK) {
            List<Long> chunk = orderIds.subList(i, Math.min(orderIds.size(), i + ID_CHUNK));
            for (Object[] r : em
                .createQuery(
                    "select o.id, o.fareAmount, o.partnerCode, o.partnerStatus, o.partnerFeeAmount from ShipmentOrder o where o.id in :ids",
                    Object[].class
                )
                .setParameter("ids", chunk)
                .getResultList()) {
                fares.put((Long) r[0], netFare((BigDecimal) r[1], (String) r[2], (String) r[3], (BigDecimal) r[4]));
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

    /** Phí Ahamove còn phải trừ. Đơn không book Ahamove, hoặc đã hủy, thì bằng 0. */
    static BigDecimal ahamoveCost(String partnerCode, String partnerStatus, BigDecimal partnerFee) {
        if (partnerCode == null || !"AHAMOVE".equalsIgnoreCase(partnerCode.trim())) {
            return BigDecimal.ZERO;
        }
        if (partnerStatus != null && "CANCELLED".equalsIgnoreCase(partnerStatus.trim())) {
            return BigDecimal.ZERO;
        }
        return OrderMoney.nz(partnerFee);
    }

    /** Cước tính doanh thu: cước khách trả trừ phí Ahamove còn hiệu lực, không âm. */
    static BigDecimal netFare(BigDecimal fare, String partnerCode, String partnerStatus, BigDecimal partnerFee) {
        return OrderMoney.nz(fare).subtract(ahamoveCost(partnerCode, partnerStatus, partnerFee)).max(BigDecimal.ZERO);
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
