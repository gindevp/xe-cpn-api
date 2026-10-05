package com.mycompany.myapp.service.invoice;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.dto.order.IssueInvoiceRequest;
import com.mycompany.myapp.service.order.OrderMoney;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Khách tự yêu cầu HĐĐT công ty từ trang tra cứu (mã đơn + 4 số cuối SĐT người trả cước).
 * Chưa tới mốc thanh toán → lưu thông tin, hệ thống tự xuất khi đủ điều kiện như luồng web;
 * đã tới mốc, còn trong 3 tiếng, không nợ cước → xuất MISA ngay. Tên / địa chỉ công ty luôn lấy theo MST tra được.
 */
@Service
public class PublicInvoiceService {

    public static final String ACTOR = "customer";
    private static final String ENTITY = "meInvoice";
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int MAX_FAILED_VERIFY_PER_ORDER = 10;
    private static final int MAX_LOOKUP_PER_IP = 30;
    private static final int MAX_SUBMIT_PER_ORDER = 5;
    private static final int MAX_SUBMIT_PER_IP = 20;

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final MeInvoiceIssueService issueService;
    private final TaxCodeLookupService taxCodeLookupService;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public PublicInvoiceService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderEventRepository orderEventRepository,
        MeInvoiceIssueService issueService,
        TaxCodeLookupService taxCodeLookupService
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderEventRepository = orderEventRepository;
        this.issueService = issueService;
        this.taxCodeLookupService = taxCodeLookupService;
    }

    public record LookupRequest(String code, String phone, String taxCode) {}

    public record SubmitRequest(String code, String phone, String taxCode, String email) {}

    /** action: ISSUED (đã xuất) / SAVED (đã lưu, tự xuất sau) / FAILED (MISA lỗi, VP xử lý). */
    public record SubmitResult(String action, String invoiceNo, String email, String message) {}

    public Map<String, Object> lookupTaxCode(LookupRequest req, String ip) {
        throttle("lookup-ip:" + ip, MAX_LOOKUP_PER_IP);
        verifyPayer(req.code(), req.phone());
        Map<String, Object> r = taxCodeLookupService.lookup(req.taxCode());
        if (!Boolean.TRUE.equals(r.get("ok"))) {
            return Map.of("ok", false, "message", String.valueOf(r.getOrDefault("message", "Không tra được mã số thuế")));
        }
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("ok", true);
        out.put("taxCode", r.get("taxCode"));
        out.put("companyName", r.get("companyName"));
        out.put("address", r.get("address"));
        if (r.get("orgType") != null) out.put("orgType", r.get("orgType"));
        return out;
    }

    @Transactional
    public SubmitResult submit(SubmitRequest req, String ip) {
        throttle("submit-ip:" + ip, MAX_SUBMIT_PER_IP);
        ShipmentOrder order = verifyPayer(req.code(), req.phone());
        throttle("submit-order:" + order.getId(), MAX_SUBMIT_PER_ORDER);

        if (MeInvoiceIssueService.isIssued(order)) {
            throw bad("Đơn đã xuất hoá đơn" + (order.getInvoiceNo() != null ? " số " + order.getInvoiceNo() : ""), "invoiceIssued");
        }
        if (MeInvoiceIssueService.STATUS_MANUAL.equals(order.getInvoiceStatus())) {
            throw bad("Hoá đơn của đơn này do văn phòng xử lý — vui lòng liên hệ văn phòng", "invoiceMarked");
        }
        if (MeInvoiceIssueService.STATUS_PENDING.equals(order.getInvoiceStatus())) {
            throw bad("Hoá đơn đang được xuất — vui lòng chờ ít phút", "invoicePending");
        }
        OrderStatus st = order.getStatus();
        if (st == OrderStatus.CANCELLED || st == OrderStatus.RETURNING || st == OrderStatus.RETURNED || st == OrderStatus.DRAFT) {
            throw bad("Đơn này không xuất được hoá đơn", "invoiceNotAllowed");
        }
        if (MeInvoiceIssueService.autoBlockReason(order) != null) {
            throw bad("Đơn đang có sự cố — vui lòng liên hệ văn phòng để xuất hoá đơn", "invoiceException");
        }

        Map<String, Object> tax = taxCodeLookupService.lookup(req.taxCode());
        if (!Boolean.TRUE.equals(tax.get("ok"))) {
            throw bad(String.valueOf(tax.getOrDefault("message", "Không tra được mã số thuế")), "invoiceTaxLookup");
        }
        String taxCode = String.valueOf(tax.get("taxCode"));
        String companyName = (String) tax.get("companyName");
        String address = (String) tax.get("address");
        String email = req.email() == null ? null : req.email().trim();

        if (!issueService.paymentReached(order)) {
            issueService.saveInfo(
                order.getOrderCode(),
                new MeInvoiceIssueService.InvoiceInfoRequest(true, taxCode, companyName, address, email),
                ACTOR
            );
            return new SubmitResult(
                "SAVED",
                null,
                email,
                "Đã lưu thông tin. Hoá đơn sẽ tự động xuất và gửi về email khi " +
                (InvoicePolicy.paidAtWarehouseIn(order) ? "đơn nhập kho gửi / giao thành công." : "đơn giao thành công.")
            );
        }

        Instant deadline = InvoicePolicy.deadline(order, deliveredAt(order));
        if (deadline != null && Instant.now().isAfter(deadline)) {
            throw bad("Đã quá 3 tiếng kể từ khi thanh toán — vui lòng liên hệ văn phòng để được hỗ trợ xuất hoá đơn", "invoiceLate");
        }
        if (OrderMoney.hasUnpaidResidue(order) && !Boolean.TRUE.equals(order.getOnCredit())) {
            throw bad("Đơn còn cước chưa thanh toán — vui lòng thanh toán đủ rồi xuất hoá đơn", "invoiceUnpaid");
        }

        IssueInvoiceRequest issue = new IssueInvoiceRequest();
        issue.setTaxCode(taxCode);
        issue.setCompanyName(companyName);
        issue.setAddress(address);
        issue.setEmail(email);
        ShipmentOrder done = issueService.issueManual(order.getOrderCode(), issue, ACTOR);
        if (MeInvoiceIssueService.isIssued(done)) {
            return new SubmitResult("ISSUED", done.getInvoiceNo(), email, "Đã xuất hoá đơn và gửi về email " + email);
        }
        return new SubmitResult(
            "FAILED",
            null,
            email,
            "Đã ghi nhận yêu cầu nhưng hệ thống hoá đơn chưa xuất được — văn phòng sẽ xử lý và gửi hoá đơn về email của bạn"
        );
    }

    /** Đơn tồn tại, cho tra cứu công khai, 4 số cuối (hoặc đủ số) khớp SĐT người trả cước. */
    ShipmentOrder verifyPayer(String code, String phone) {
        String c = code == null ? "" : code.trim();
        ShipmentOrder order = c.isEmpty() ? null : shipmentOrderRepository.findOneByOrderCodeOrDraftCode(c).orElse(null);
        if (order == null || !Boolean.TRUE.equals(order.getPublicTrackingAllowed())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        }
        String key = "verify-fail:" + order.getId();
        if (count(key) >= MAX_FAILED_VERIFY_PER_ORDER) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Nhập sai quá nhiều lần — vui lòng thử lại sau 1 giờ");
        }
        if (!payerPhoneMatches(phone, InvoicePolicy.payerPhone(order))) {
            record(key);
            throw bad("Chỉ người thanh toán cước mới xuất được hoá đơn — nhập 4 số cuối SĐT người trả cước", "invoiceNotPayer");
        }
        return order;
    }

    static boolean payerPhoneMatches(String input, String payerPhone) {
        String in = input == null ? "" : input.replaceAll("\\D+", "");
        String payer = payerPhone == null ? "" : payerPhone.replaceAll("\\D+", "");
        if (in.isEmpty() || payer.isEmpty()) return false;
        return in.length() == 4 ? payer.endsWith(in) : in.equals(payer);
    }

    private Instant deliveredAt(ShipmentOrder order) {
        if (order.getStatus() != OrderStatus.DELIVERED || order.getId() == null) return null;
        for (OrderEvent e : orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId())) {
            if (InvoicePolicy.DELIVERED_ACTIONS.contains(e.getAction()) || "POD_HOME".equals(e.getAction())) {
                return e.getEventAt();
            }
        }
        return order.getUpdatedAt();
    }

    private void throttle(String key, int max) {
        if (count(key) >= max) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Thao tác quá nhiều lần — vui lòng thử lại sau");
        }
        record(key);
    }

    private int count(String key) {
        Deque<Instant> q = hits.get(key);
        if (q == null) return 0;
        synchronized (q) {
            Instant cutoff = Instant.now().minus(WINDOW);
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            return q.size();
        }
    }

    private void record(String key) {
        if (hits.size() > 50_000) hits.clear();
        Deque<Instant> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(Instant.now());
        }
    }

    private static BadRequestAlertException bad(String msg, String key) {
        return new BadRequestAlertException(msg, ENTITY, key);
    }
}
