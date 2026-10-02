package com.mycompany.myapp.service.invoice;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tự xuất HĐĐT sau mốc thanh toán + 3 tiếng (khi bật công tắc), xuất bù hàng loạt, danh sách cho kế toán.
 * Mọi lần gửi MISA chạy trên MỘT luồng riêng để job tự xuất và xuất bù không cùng gửi một đơn.
 */
@Service
public class InvoiceAutoIssueService {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceAutoIssueService.class);
    private static final String ENTITY = "meInvoice";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int AUTO_BATCH = 200;
    private static final int BACKFILL_MAX = 2000;
    private static final int LIST_MAX_DAYS = 62;
    private static final List<OrderStatus> NOT_PAID_STATUSES = List.of(OrderStatus.DRAFT, OrderStatus.CANCELLED);

    private final MeInvoiceIssueService issueService;
    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderEventRepository orderEventRepository;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "misa-invoice");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean autoQueued = new AtomicBoolean(false);
    private volatile BackfillStatus backfill = BackfillStatus.idle();

    public InvoiceAutoIssueService(
        MeInvoiceIssueService issueService,
        ShipmentOrderRepository shipmentOrderRepository,
        OrderEventRepository orderEventRepository
    ) {
        this.issueService = issueService;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @PreDestroy
    void shutdown() {
        worker.shutdownNow();
    }

    // ---------------------------------------------------------------- tự xuất

    @Scheduled(fixedDelay = 300_000, initialDelay = 120_000)
    public void scheduledAutoIssue() {
        if (!issueService.isClientEnabled() || !issueService.autoIssueEnabled()) {
            return;
        }
        if (autoQueued.compareAndSet(false, true)) {
            worker.execute(() -> {
                try {
                    runAutoIssue(Instant.now());
                } catch (Exception e) {
                    LOG.error("MISA auto-issue run failed: {}", e.getMessage(), e);
                } finally {
                    autoQueued.set(false);
                }
            });
        }
    }

    /** Một lượt: đơn có mốc thanh toán trong [lúc bật công tắc, now − 3h]. Trả số đơn đã thử. */
    int runAutoIssue(Instant now) {
        IntegrationConfig cfg = issueService.autoIssueConfig();
        if (cfg == null) {
            return 0;
        }
        Instant since = cfg.getMisaAutoIssueSince();
        Instant cutoff = now.minus(InvoicePolicy.WINDOW);
        if (!since.isBefore(cutoff)) {
            return 0;
        }
        Set<Long> ids = new LinkedHashSet<>(
            shipmentOrderRepository.findAutoInvoiceWarehouseInIds(since, cutoff, NOT_PAID_STATUSES, PageRequest.of(0, AUTO_BATCH))
        );
        ids.addAll(
            orderEventRepository.findAutoInvoiceDeliveredIds(InvoicePolicy.DELIVERED_ACTIONS, since, cutoff, PageRequest.of(0, AUTO_BATCH))
        );
        Map<String, Integer> outcome = new HashMap<>();
        for (Long id : ids) {
            if (!issueService.autoIssueEnabled()) {
                break;
            }
            String result;
            try {
                result = issueService.autoIssueOne(id);
            } catch (Exception e) {
                result = "ERROR";
                LOG.warn("MISA auto-issue order id={} failed: {}", id, e.getMessage());
            }
            outcome.merge(String.valueOf(result), 1, Integer::sum);
        }
        if (!ids.isEmpty()) {
            LOG.info("MISA auto-issue run: {} orders → {}", ids.size(), outcome);
        }
        return ids.size();
    }

    // ---------------------------------------------------------------- xuất bù

    /** Đưa danh sách mã đơn vào hàng đợi xuất bù (chạy nền). Đang chạy lượt khác → 400. */
    public synchronized BackfillStatus startBackfill(List<String> orderCodes, String actor) {
        if (!issueService.isClientEnabled()) {
            throw new BadRequestAlertException("Chưa bật kết nối MISA — không xuất được hoá đơn", ENTITY, "misaDisabled");
        }
        if (backfill.running()) {
            throw new BadRequestAlertException("Đang xuất bù lượt trước — chờ xong rồi chạy tiếp", ENTITY, "backfillRunning");
        }
        List<String> codes = orderCodes == null
            ? List.of()
            : orderCodes.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        if (codes.isEmpty()) {
            throw new BadRequestAlertException("Chưa chọn đơn nào để xuất bù", ENTITY, "backfillEmpty");
        }
        if (codes.size() > BACKFILL_MAX) {
            throw new BadRequestAlertException("Tối đa " + BACKFILL_MAX + " đơn mỗi lượt xuất bù", ENTITY, "backfillTooMany");
        }
        backfill = BackfillStatus.started(codes.size(), actor);
        worker.execute(() -> runBackfill(codes, actor));
        return backfill;
    }

    public BackfillStatus backfillStatus() {
        return backfill;
    }

    private void runBackfill(List<String> codes, String actor) {
        for (String code : codes) {
            String result;
            try {
                result = issueService.backfillOne(code, actor);
            } catch (Exception e) {
                result = "ERROR";
                LOG.warn("MISA backfill {} failed: {}", code, e.getMessage());
            }
            backfill = backfill.record(code, result);
        }
        backfill = backfill.finish();
        LOG.info("MISA backfill by {} done: {}", actor, backfill.results());
    }

    // ---------------------------------------------------------------- danh sách kế toán

    /** Đơn có mốc thanh toán trong [from, to] (ngày VN): gửi trả theo lúc nhập kho gửi, còn lại theo lúc giao. */
    @Transactional(readOnly = true)
    public List<InvoiceRow> list(LocalDate from, LocalDate to, Optional<String> scopedOfficeCode) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestAlertException("Khoảng ngày không hợp lệ", ENTITY, "badRange");
        }
        if (Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() >= LIST_MAX_DAYS) {
            throw new BadRequestAlertException("Chọn tối đa " + LIST_MAX_DAYS + " ngày mỗi lần", ENTITY, "rangeTooLong");
        }
        Instant start = from.atStartOfDay(VN).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(VN).toInstant();

        List<InvoiceRow> rows = new ArrayList<>();
        for (ShipmentOrder o : shipmentOrderRepository.findInvoiceWarehouseInBetween(start, end, NOT_PAID_STATUSES)) {
            rows.add(toRow(o, o.getPickedUpAt()));
        }
        Map<Long, Instant> delivered = new LinkedHashMap<>();
        for (Object[] r : orderEventRepository.findInvoiceDeliveredBetween(InvoicePolicy.DELIVERED_ACTIONS, start, end)) {
            delivered.put((Long) r[0], (Instant) r[1]);
        }
        if (!delivered.isEmpty()) {
            for (ShipmentOrder o : shipmentOrderRepository.findAllWithOfficesByIdIn(delivered.keySet())) {
                rows.add(toRow(o, delivered.get(o.getId())));
            }
        }
        String office = scopedOfficeCode.orElse(null);
        return rows
            .stream()
            .filter(r -> office == null || office.equals(r.fromOfficeCode()) || office.equals(r.toOfficeCode()))
            .sorted(Comparator.comparing(InvoiceRow::paidAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    static InvoiceRow toRow(ShipmentOrder o, Instant paidAt) {
        Office to = o.getFinalToOffice() != null ? o.getFinalToOffice() : o.getToOffice();
        Instant deadline = InvoicePolicy.deadline(paidAt);
        String st = o.getInvoiceStatus();
        boolean done = MeInvoiceIssueService.isIssued(o) || MeInvoiceIssueService.STATUS_MANUAL.equals(st);
        boolean late = done && deadline != null && o.getInvoiceIssuedAt() != null && o.getInvoiceIssuedAt().isAfter(deadline);
        return new InvoiceRow(
            o.getOrderCode(),
            o.getStatus() != null ? o.getStatus().name() : null,
            o.getPaymentTerm() != null ? o.getPaymentTerm().name() : null,
            Boolean.TRUE.equals(o.getOnCredit()),
            InvoicePolicy.senderPays(o) ? "SENDER" : "RECEIVER",
            paidAt,
            deadline,
            o.getFromOffice() != null ? o.getFromOffice().getCode() : null,
            o.getFromOffice() != null ? o.getFromOffice().getName() : null,
            to != null ? to.getCode() : null,
            to != null ? to.getName() : null,
            o.getSenderName(),
            o.getSenderPhone(),
            o.getReceiverName(),
            o.getReceiverPhone(),
            o.getFareAmount(),
            o.getPaidAmount(),
            MeInvoiceAmounts.fromOrder(o).gross(),
            Boolean.TRUE.equals(o.getInvoiceRequested()),
            o.getInvoiceTaxCode(),
            o.getInvoiceCompanyName(),
            st,
            InvoicePolicy.typeOf(o),
            o.getInvoiceNo(),
            o.getInvoiceSeries(),
            o.getInvoiceIssuedAt(),
            o.getInvoiceError(),
            late
        );
    }

    /** Một dòng màn Quản lý hoá đơn / file Excel. */
    public record InvoiceRow(
        String orderCode,
        String orderStatus,
        String paymentTerm,
        boolean onCredit,
        String payer,
        Instant paidAt,
        Instant deadlineAt,
        String fromOfficeCode,
        String fromOfficeName,
        String toOfficeCode,
        String toOfficeName,
        String senderName,
        String senderPhone,
        String receiverName,
        String receiverPhone,
        BigDecimal fareAmount,
        BigDecimal paidAmount,
        BigDecimal invoiceAmount,
        boolean invoiceRequested,
        String invoiceTaxCode,
        String invoiceCompanyName,
        String invoiceStatus,
        String invoiceType,
        String invoiceNo,
        String invoiceSeries,
        Instant invoiceIssuedAt,
        String invoiceError,
        boolean late
    ) {}

    // ---------------------------------------------------------------- thông tin công ty theo SĐT

    /** Thông tin HĐ công ty lần gần nhất mà SĐT này là người trả cước; không có → rỗng. */
    @Transactional(readOnly = true)
    public Optional<Map<String, String>> buyerProfile(String phone) {
        String p = phone == null ? "" : phone.trim();
        if (p.length() < 8) {
            return Optional.empty();
        }
        return shipmentOrderRepository
            .findInvoiceProfilesByPhone(p, PageRequest.of(0, 20))
            .stream()
            .filter(o -> p.equals(InvoicePolicy.payerPhone(o) == null ? null : InvoicePolicy.payerPhone(o).trim()))
            .findFirst()
            .map(o -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("phone", p);
                m.put("taxCode", o.getInvoiceTaxCode());
                m.put("companyName", o.getInvoiceCompanyName());
                m.put("address", o.getInvoiceCompanyAddress());
                m.put("email", o.getInvoiceEmail());
                m.put("fromOrderCode", o.getOrderCode());
                return m;
            });
    }

    // ---------------------------------------------------------------- trạng thái xuất bù

    public record BackfillStatus(
        boolean running,
        String actor,
        Instant startedAt,
        Instant finishedAt,
        int total,
        int done,
        Map<String, Integer> results,
        List<String> failedCodes
    ) {
        static BackfillStatus idle() {
            return new BackfillStatus(false, null, null, null, 0, 0, Map.of(), List.of());
        }

        static BackfillStatus started(int total, String actor) {
            return new BackfillStatus(true, actor, Instant.now(), null, total, 0, Map.of(), List.of());
        }

        BackfillStatus record(String code, String result) {
            Map<String, Integer> r = new LinkedHashMap<>(results);
            r.merge(String.valueOf(result), 1, Integer::sum);
            List<String> failed = failedCodes;
            if (MeInvoiceIssueService.STATUS_FAILED.equals(result) || "ERROR".equals(result)) {
                failed = new ArrayList<>(failedCodes);
                if (failed.size() < 200) {
                    failed.add(code);
                }
            }
            return new BackfillStatus(running, actor, startedAt, null, total, done + 1, Map.copyOf(r), List.copyOf(failed));
        }

        BackfillStatus finish() {
            return new BackfillStatus(false, actor, startedAt, Instant.now(), total, done, results, failedCodes);
        }
    }
}
