package com.mycompany.myapp.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.IssueInvoiceRequest;
import com.mycompany.myapp.service.order.OrderMoney;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phát hành HĐĐT MISA — idempotent theo RefID {@code XE-{orderCode}}.
 * <ul>
 *   <li>Công tắc tự xuất tắt (mặc định): giữ cách cũ — đơn có yêu cầu HĐ công ty tự xuất ngay khi giao thành công.</li>
 *   <li>Công tắc bật: {@link InvoiceAutoIssueService} xuất sau mốc thanh toán + 3 tiếng (DN nếu có yêu cầu, còn lại cá nhân).</li>
 * </ul>
 * Lỗi MISA → status=FAILED, không tự thử lại; kế toán xuất lại bằng tay / xuất bù.
 */
@Service
public class MeInvoiceIssueService {

    private static final Logger LOG = LoggerFactory.getLogger(MeInvoiceIssueService.class);
    private static final String ENTITY = "meInvoice";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ISSUED = "ISSUED";
    public static final String STATUS_DUPLICATE = "DUPLICATE";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";
    /** Kế toán tích "đã xuất cá nhân" (xuất ngoài hệ thống) — hệ thống không gửi MISA cho đơn này nữa. */
    public static final String STATUS_MANUAL = "MANUAL";

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final DayClosureGuard dayClosureGuard;
    private final MisaMeInvoiceClient client;
    private final ObjectMapper objectMapper;
    private final boolean issueOnlyWhenRequested;
    private PhoneTaxLinkService phoneTaxLinks;

    /**
     * Đơn chưa khai đủ HĐ công ty thì lấy MST đã lưu của người gửi hoặc người nhận.
     * Không xét hình thức thanh toán. Đơn đã khai MST thì giữ nguyên.
     */
    private void applySavedPartyTax(ShipmentOrder order) {
        if (phoneTaxLinks == null || order == null || explicitCompany(order)) {
            return;
        }
        phoneTaxLinks
            .latestPartyProfile(order.getSenderPhone(), order.getReceiverPhone())
            .ifPresent(row -> {
                order.setInvoiceRequested(true);
                order.setInvoiceTaxCode(row.getTaxCode());
                order.setInvoiceCompanyName(row.getCompanyName());
                order.setInvoiceCompanyAddress(row.getAddress());
                if (blankToEmpty(order.getInvoiceEmail()).isBlank() && row.getEmail() != null && !row.getEmail().isBlank()) {
                    order.setInvoiceEmail(row.getEmail().trim());
                }
                if (blankToEmpty(order.getInvoiceBuyerName()).isBlank() && row.getContactName() != null) {
                    order.setInvoiceBuyerName(InvoicePolicy.upperBuyerName(row.getContactName()));
                }
                if (blankToEmpty(order.getInvoiceBuyerPhone()).isBlank()) {
                    order.setInvoiceBuyerPhone(row.getPhone());
                }
            });
    }

    private static boolean explicitCompany(ShipmentOrder order) {
        return (
            Boolean.TRUE.equals(order.getInvoiceRequested()) &&
            VietnamTaxCode.isValid(order.getInvoiceTaxCode()) &&
            !blankToEmpty(order.getInvoiceCompanyName()).isBlank() &&
            !blankToEmpty(order.getInvoiceCompanyAddress()).isBlank()
        );
    }

    private void rememberBuyerTax(ShipmentOrder order) {
        if (phoneTaxLinks == null || order == null) {
            return;
        }
        try {
            phoneTaxLinks.rememberIssued(order);
        } catch (RuntimeException e) {
            LOG.warn("Không lưu được MST theo SĐT của đơn {}: {}", order.getOrderCode(), e.toString());
        }
    }

    @Autowired
    void setPhoneTaxLinks(PhoneTaxLinkService phoneTaxLinks) {
        this.phoneTaxLinks = phoneTaxLinks;
    }

    public MeInvoiceIssueService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderEventRepository orderEventRepository,
        IntegrationConfigRepository integrationConfigRepository,
        DayClosureGuard dayClosureGuard,
        MisaMeInvoiceClient client,
        ObjectMapper objectMapper,
        @Value("${cpn.misa.issue-only-when-requested:true}") boolean issueOnlyWhenRequested
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderEventRepository = orderEventRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.dayClosureGuard = dayClosureGuard;
        this.client = client;
        this.objectMapper = objectMapper;
        this.issueOnlyWhenRequested = issueOnlyWhenRequested;
    }

    /** Sau commit DELIVERED → gọi MISA ngay (async). Khi bật tự xuất 3 tiếng thì job định kỳ lo, bỏ qua ở đây. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onOrderDelivered(OrderDeliveredEvent event) {
        if (event == null || event.orderCode() == null || event.orderCode().isBlank()) {
            return;
        }
        if (autoIssueEnabled()) {
            return;
        }
        try {
            issueForOrderCode(event.orderCode().trim());
        } catch (Exception e) {
            LOG.error("MISA auto-issue failed for {}: {}", event.orderCode(), e.getMessage());
        }
    }

    public boolean autoIssueEnabled() {
        return autoIssueConfig() != null;
    }

    /** Cấu hình khi công tắc tự xuất đang bật (kèm mốc bật); tắt → null. */
    public IntegrationConfig autoIssueConfig() {
        return integrationConfigRepository
            .findAll()
            .stream()
            .findFirst()
            .filter(c -> Boolean.TRUE.equals(c.getMisaAutoIssueEnabled()) && c.getMisaAutoIssueSince() != null)
            .orElse(null);
    }

    public boolean isClientEnabled() {
        return client.isEnabled();
    }

    @Transactional
    public void issueForOrderCode(String orderCode) {
        issueForOrder(requireOrder(orderCode));
    }

    /**
     * Xuất HĐ công ty thủ công (popup đơn / màn Giao thành công): lưu thông tin người mua rồi gọi MISA ngay.
     * Cho xuất muộn (sau hạn 3 tiếng) — FE cảnh báo trước. Lỗi nghiệp vụ → 400; lỗi MISA → FAILED (gọi lại được).
     * Đơn kế toán đã tích bỏ xuất tự động vẫn xuất DN được; MISA chưa xuất thì giữ nguyên tích (không lọt xuất bù).
     */
    @Transactional
    public ShipmentOrder issueManual(String orderCode, IssueInvoiceRequest request, String actor) {
        requireClient();
        ShipmentOrder order = requireOrderForUpdate(orderCode);
        assertPaymentReached(order);
        assertNotIssued(order, "không xuất lại");
        dayClosureGuard.assertOrderMutable(order);
        boolean wasMarked = STATUS_MANUAL.equals(order.getInvoiceStatus());

        IssueInvoiceRequest req = request != null ? request : new IssueInvoiceRequest();
        applyBuyer(
            order,
            firstNonBlank(req.getTaxCode(), order.getInvoiceTaxCode()),
            firstNonBlank(req.getCompanyName(), order.getInvoiceCompanyName()),
            firstNonBlank(req.getAddress(), order.getInvoiceCompanyAddress()),
            firstNonBlank(req.getEmail(), order.getInvoiceEmail())
        );
        if (req.getBuyerName() != null) {
            order.setInvoiceBuyerName(InvoicePolicy.upperBuyerName(req.getBuyerName()));
        }
        applyOptionalBuyerIds(order, req.getBuyerIdNumber(), req.getBuyerPhone());
        shipmentOrderRepository.save(order);

        publish(order, InvoicePolicy.TYPE_COMPANY);
        if (wasMarked && !isIssued(order)) {
            order.setInvoiceStatus(STATUS_MANUAL);
            order.setInvoiceType(InvoicePolicy.TYPE_PERSONAL);
            shipmentOrderRepository.save(order);
        }
        appendEvent(order, "INVOICE_ISSUE", issueDetail(order), actor);
        return order;
    }

    /**
     * Xuất bù một đơn (màn Quản lý hoá đơn / Giao thành công): DN nếu có yêu cầu kèm MST, còn lại cá nhân.
     * Trả mã kết quả: ISSUED / DUPLICATE / FAILED / hoặc lý do bỏ qua (không ném lỗi để chạy hàng loạt).
     */
    @Transactional
    public String backfillOne(String orderCode, String actor) {
        ShipmentOrder order = shipmentOrderRepository.findOneByOrderCodeOrDraftCodeForUpdate(orderCode.trim()).orElse(null);
        if (order == null) {
            return "NOT_FOUND";
        }
        String st = order.getInvoiceStatus();
        if (isIssued(order) || STATUS_MANUAL.equals(st) || STATUS_PENDING.equals(st)) {
            return "ALREADY";
        }
        String blocked = autoBlockReason(order);
        if (blocked != null) {
            return blocked;
        }
        if (!paymentReached(order)) {
            return "NOT_PAID_YET";
        }
        if (OrderMoney.hasUnpaidResidue(order) && !Boolean.TRUE.equals(order.getOnCredit())) {
            return "UNPAID_RESIDUE";
        }
        applySavedPartyTax(order);
        publish(order, InvoicePolicy.typeToIssue(order));
        appendEvent(order, "INVOICE_ISSUE", "Xuất bù · " + issueDetail(order), actor);
        return order.getInvoiceStatus();
    }

    /**
     * Đồng bộ lại đơn DUPLICATE thiếu InvNo/TransactionID: gửi lại cùng RefID, lấy số HĐ từ response trùng nếu có.
     * Không tạo RefID mới — tránh xuất HĐ thật thứ hai.
     */
    @Transactional
    public String resyncDuplicateInvoice(String orderCode, String actor) {
        requireClient();
        if (orderCode == null || orderCode.isBlank()) {
            return "NOT_FOUND";
        }
        ShipmentOrder order = shipmentOrderRepository.findOneByOrderCodeOrDraftCodeForUpdate(orderCode.trim()).orElse(null);
        if (order == null) {
            return "NOT_FOUND";
        }
        if (!STATUS_DUPLICATE.equals(order.getInvoiceStatus())) {
            return "NOT_DUPLICATE";
        }
        if (hasInvoiceIds(order)) {
            return "ALREADY_HAS_IDS";
        }
        String type = blankToEmpty(order.getInvoiceType()).isBlank() ? InvoicePolicy.typeToIssue(order) : order.getInvoiceType().trim();
        publish(order, type);
        appendEvent(order, "INVOICE_RESYNC", "Đồng bộ lại HĐ trùng RefID · " + issueDetail(order), actor);
        if (STATUS_ISSUED.equals(order.getInvoiceStatus()) && hasInvoiceIds(order)) {
            return STATUS_ISSUED;
        }
        if (STATUS_DUPLICATE.equals(order.getInvoiceStatus())) {
            return hasInvoiceIds(order) ? STATUS_ISSUED : STATUS_DUPLICATE;
        }
        return order.getInvoiceStatus() != null ? order.getInvoiceStatus() : "FAILED";
    }

    /**
     * Tự xuất khi đã quá mốc thanh toán + 3 tiếng và đơn đã hoàn tất — giao thành công / hoàn xong (gọi từ job).
     * Bỏ qua đơn công nợ, đơn còn nợ cước, đơn đã có trạng thái HĐ (trừ SKIPPED của luồng cũ).
     */
    @Transactional
    public String autoIssueOne(Long orderId) {
        ShipmentOrder order = shipmentOrderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return "NOT_FOUND";
        }
        String st = order.getInvoiceStatus();
        if (st != null && !STATUS_SKIPPED.equals(st)) {
            return "ALREADY";
        }
        if (Boolean.TRUE.equals(order.getOnCredit())) {
            return "ON_CREDIT";
        }
        String blocked = autoBlockReason(order);
        if (blocked != null) {
            return blocked;
        }
        if (!paymentReached(order)) {
            return "NOT_PAID_YET";
        }
        if (!InvoicePolicy.isDone(order)) {
            return "NOT_DONE";
        }
        if (OrderMoney.hasUnpaidResidue(order)) {
            return "UNPAID_RESIDUE";
        }
        applySavedPartyTax(order);
        publish(order, InvoicePolicy.typeToIssue(order));
        appendEvent(order, "INVOICE_ISSUE", "Tự xuất sau 3 tiếng · " + issueDetail(order), "system");
        return order.getInvoiceStatus();
    }

    /** Kế toán tích / bỏ tích "đã xuất cá nhân" (xuất ngoài hệ thống). */
    @Transactional
    public ShipmentOrder markPersonalIssued(String orderCode, boolean marked, String actor) {
        ShipmentOrder order = requireOrder(orderCode);
        String st = order.getInvoiceStatus();
        if (marked) {
            if (STATUS_MANUAL.equals(st)) {
                return order;
            }
            assertNotIssuedOrMarked(order, "không tích được");
            if (STATUS_PENDING.equals(st)) {
                throw new BadRequestAlertException("Đơn đang gửi MISA — chờ kết quả rồi tích", ENTITY, "invoicePending");
            }
            order.setInvoiceStatus(STATUS_MANUAL);
            order.setInvoiceType(InvoicePolicy.TYPE_PERSONAL);
            order.setInvoiceIssuedAt(Instant.now());
            order.setInvoiceError(null);
            shipmentOrderRepository.save(order);
            appendEvent(order, "INVOICE_MARK", "Kế toán tích bỏ xuất tự động hoá đơn", actor);
        } else {
            if (!STATUS_MANUAL.equals(st)) {
                return order;
            }
            order.setInvoiceStatus(null);
            order.setInvoiceType(null);
            order.setInvoiceIssuedAt(null);
            shipmentOrderRepository.save(order);
            appendEvent(order, "INVOICE_MARK", "Bỏ tích bỏ xuất tự động hoá đơn", actor);
        }
        return order;
    }

    /**
     * Lưu / bỏ thông tin xuất hoá đơn ở mọi trạng thái đơn (không qua chốt ngày vì không đụng tiền). Đơn đã xuất HĐ
     * hoặc đã tích xuất cá nhân thì khoá.
     */
    @Transactional
    public ShipmentOrder saveInfo(String orderCode, InvoiceInfoRequest request, String actor) {
        ShipmentOrder order = requireOrder(orderCode);
        assertNotIssuedOrMarked(order, "không sửa thông tin hoá đơn");
        InvoiceInfoRequest req = request != null ? request : new InvoiceInfoRequest(true, null, null, null, null);
        String detail;
        if (Boolean.FALSE.equals(req.requested())) {
            order.setInvoiceRequested(false);
            order.setInvoiceTaxCode(null);
            order.setInvoiceCompanyName(null);
            order.setInvoiceCompanyAddress(null);
            order.setInvoiceEmail(null);
            order.setInvoiceBuyerName(null);
            order.setInvoiceBuyerIdNumber(null);
            order.setInvoiceBuyerPhone(null);
            detail = "Bỏ yêu cầu xuất hoá đơn";
        } else {
            applyBuyer(
                order,
                firstNonBlank(req.taxCode()),
                firstNonBlank(req.companyName()),
                firstNonBlank(req.address()),
                firstNonBlank(req.email())
            );
            if (req.buyerName() != null) {
                order.setInvoiceBuyerName(InvoicePolicy.upperBuyerName(req.buyerName()));
            }
            applyOptionalBuyerIds(order, req.buyerIdNumber(), req.buyerPhone());
            detail = "Cập nhật thông tin hoá đơn · MST " + order.getInvoiceTaxCode() + " · " + order.getInvoiceCompanyName();
        }
        shipmentOrderRepository.save(order);
        appendEvent(order, "INVOICE_INFO", detail, actor);
        return order;
    }

    /** Luồng cũ (công tắc tắt): chỉ xuất HĐ công ty cho đơn có yêu cầu, ngay khi giao thành công. */
    @Transactional
    public void issueForOrder(ShipmentOrder order) {
        if (!client.isEnabled()) {
            LOG.debug("MISA disabled — skip {}", order.getOrderCode());
            return;
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            markSkipped(order, "Not DELIVERED");
            return;
        }
        if (isIssued(order) || STATUS_MANUAL.equals(order.getInvoiceStatus())) {
            LOG.info("MISA already issued for {} status={}", order.getOrderCode(), order.getInvoiceStatus());
            return;
        }
        if (issueOnlyWhenRequested && !Boolean.TRUE.equals(order.getInvoiceRequested())) {
            markSkipped(order, "invoiceRequested=false");
            return;
        }
        publish(order, InvoicePolicy.TYPE_COMPANY);
    }

    /** Gửi MISA theo loại HĐ; kết quả ghi lên đơn (ISSUED / DUPLICATE / FAILED / SKIPPED). */
    private void publish(ShipmentOrder order, String type) {
        if (!client.isEnabled()) {
            LOG.debug("MISA disabled — skip {}", order.getOrderCode());
            return;
        }
        MeInvoiceAmounts.Breakdown amounts = MeInvoiceAmounts.fromOrder(order);
        if (amounts.gross().signum() <= 0) {
            markSkipped(order, "gross=0");
            return;
        }

        if (InvoicePolicy.TYPE_COMPANY.equals(type)) {
            String buyerTax = blankToEmpty(order.getInvoiceTaxCode());
            if (buyerTax.isBlank()) {
                markFailed(order, amounts, "Thiếu MST người mua — không gửi MISA");
                return;
            }
            if (!VietnamTaxCode.isValid(buyerTax)) {
                markFailed(order, amounts, "MST người mua không hợp lệ — không gửi MISA");
                return;
            }
            if (blankToEmpty(order.getInvoiceCompanyName()).isBlank() || blankToEmpty(order.getInvoiceCompanyAddress()).isBlank()) {
                markFailed(order, amounts, "HĐ công ty cần Tên công ty + Địa chỉ");
                return;
            }
            order.setInvoiceTaxCode(VietnamTaxCode.normalize(buyerTax));
        }

        String refId = MeInvoiceAmounts.refIdFor(order.getOrderCode());
        order.setInvoiceRefId(refId);
        order.setInvoiceType(type);
        order.setInvoiceStatus(STATUS_PENDING);
        order.setInvoiceGrossAmount(amounts.gross());
        order.setInvoiceNetAmount(amounts.net());
        order.setInvoiceVatAmount(amounts.vat());
        order.setInvoiceError(null);
        shipmentOrderRepository.save(order);

        try {
            ObjectNode body = buildPublishBody(order, amounts, refId, type);
            MisaMeInvoiceClient.PublishResult result = client.publish(body);
            if (result.duplicated()) {
                applyPublishSuccess(order, result, type, true);
                return;
            }
            applyPublishSuccess(order, result, type, false);
        } catch (Exception e) {
            markFailed(order, amounts, e.getMessage());
            LOG.warn("MISA issue failed order={}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public String viewLink(String orderCode) {
        ShipmentOrder order = requireIssued(orderCode);
        if (blankToEmpty(order.getInvoiceTransactionId()).isBlank()) {
            throw new BadRequestAlertException("Thiếu TransactionID — không xem được (trùng RefID?)", ENTITY, "noTransactionId");
        }
        return client.publishViewLink(order.getInvoiceTransactionId());
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(String orderCode) {
        ShipmentOrder order = requireIssued(orderCode);
        if (blankToEmpty(order.getInvoiceTransactionId()).isBlank()) {
            throw new BadRequestAlertException("Thiếu TransactionID — không tải PDF", ENTITY, "noTransactionId");
        }
        return client.downloadPdf(List.of(order.getInvoiceTransactionId()));
    }

    /** Lúc giao thành công gần nhất (null nếu chưa giao). */
    public Instant deliveredAt(ShipmentOrder order) {
        if (order.getId() == null) {
            return null;
        }
        return orderEventRepository
            .latestEventAtByOrderIds(List.of(order.getId()), InvoicePolicy.DELIVERED_ACTIONS)
            .stream()
            .map(row -> (Instant) row[1])
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /** Đã tới mốc thanh toán: gửi trả = đã nhập kho gửi; còn lại = đã giao thành công. */
    boolean paymentReached(ShipmentOrder order) {
        OrderStatus s = order.getStatus();
        if (s == OrderStatus.DRAFT || s == OrderStatus.CANCELLED) {
            return false;
        }
        if (InvoicePolicy.paidAtWarehouseIn(order)) {
            return order.getPickedUpAt() != null;
        }
        return s == OrderStatus.DELIVERED;
    }

    private void assertPaymentReached(ShipmentOrder order) {
        if (!paymentReached(order)) {
            String msg = InvoicePolicy.paidAtWarehouseIn(order)
                ? "Đơn gửi trả chưa nhập kho gửi — chưa xuất được hoá đơn"
                : "Chỉ xuất hoá đơn cho đơn đã giao thành công";
            throw new BadRequestAlertException(msg, ENTITY, "notDelivered");
        }
    }

    private void assertNotIssuedOrMarked(ShipmentOrder order, String suffix) {
        assertNotIssued(order, suffix);
        if (STATUS_MANUAL.equals(order.getInvoiceStatus())) {
            throw new BadRequestAlertException("Kế toán đã tích bỏ xuất tự động — " + suffix, ENTITY, "invoiceMarked");
        }
    }

    private void assertNotIssued(ShipmentOrder order, String suffix) {
        if (isIssued(order)) {
            String no = blankToEmpty(order.getInvoiceNo());
            throw new BadRequestAlertException(
                "Đơn đã xuất hoá đơn" + (no.isEmpty() ? "" : " số " + no) + " — " + suffix,
                ENTITY,
                "alreadyIssued"
            );
        }
    }

    private void requireClient() {
        if (!client.isEnabled()) {
            throw new BadRequestAlertException("Chưa bật kết nối MISA — không xuất được hoá đơn", ENTITY, "misaDisabled");
        }
    }

    private ShipmentOrder requireOrder(String orderCode) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(orderCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderCode));
    }

    private ShipmentOrder requireOrderForUpdate(String orderCode) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCodeForUpdate(orderCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderCode));
    }

    /** Có InvNo hoặc TransactionID để xem/tải HĐ. */
    static boolean hasInvoiceIds(ShipmentOrder order) {
        return !blankToEmpty(order.getInvoiceNo()).isBlank() || !blankToEmpty(order.getInvoiceTransactionId()).isBlank();
    }

    /**
     * Ghi kết quả publish lên đơn. {@code fromDuplicate}: MISA báo trùng RefID — nếu có InvNo/Tx thì coi như ISSUED
     * (lấy lại số HĐ đã có); không có thì giữ DUPLICATE.
     */
    private void applyPublishSuccess(ShipmentOrder order, MisaMeInvoiceClient.PublishResult result, String type, boolean fromDuplicate) {
        boolean hasIds = !blankToEmpty(result.transactionId()).isBlank() || !blankToEmpty(result.invNo()).isBlank();
        if (fromDuplicate && !hasIds) {
            order.setInvoiceStatus(STATUS_DUPLICATE);
            order.setInvoiceError(truncate("InvoiceDuplicated"));
            order.setInvoiceIssuedAt(Instant.now());
            shipmentOrderRepository.save(order);
            rememberBuyerTax(order);
            LOG.info("MISA InvoiceDuplicated RefID={} order={} (no InvNo/Tx in response)", order.getInvoiceRefId(), order.getOrderCode());
            return;
        }
        order.setInvoiceStatus(STATUS_ISSUED);
        if (!blankToEmpty(result.transactionId()).isBlank()) {
            order.setInvoiceTransactionId(result.transactionId());
        }
        if (!blankToEmpty(result.invNo()).isBlank()) {
            order.setInvoiceNo(result.invNo());
        }
        if (!blankToEmpty(result.invSeries()).isBlank()) {
            order.setInvoiceSeries(result.invSeries());
        } else if (blankToEmpty(order.getInvoiceSeries()).isBlank()) {
            order.setInvoiceSeries(client.getInvSeries());
        }
        if (!blankToEmpty(result.invCode()).isBlank()) {
            order.setInvoiceCode(result.invCode());
        }
        order.setInvoiceIssuedAt(Instant.now());
        order.setInvoiceError(null);
        shipmentOrderRepository.save(order);
        rememberBuyerTax(order);
        LOG.info(
            "MISA {} order={} type={} InvNo={} Tx={} Code={}",
            fromDuplicate ? "recovered-duplicate" : "issued",
            order.getOrderCode(),
            type,
            result.invNo(),
            result.transactionId(),
            result.invCode()
        );
    }

    private ShipmentOrder requireIssued(String orderCode) {
        ShipmentOrder order = requireOrder(orderCode);
        if (!isIssued(order)) {
            throw new BadRequestAlertException("Đơn chưa có HĐĐT (status=" + order.getInvoiceStatus() + ")", ENTITY, "notIssued");
        }
        return order;
    }

    private static String issueDetail(ShipmentOrder order) {
        if (!isIssued(order)) {
            return "Xuất HĐĐT MISA lỗi: " + blankToEmpty(order.getInvoiceError());
        }
        String no = order.getInvoiceNo() != null ? " số " + order.getInvoiceNo() : "";
        if (InvoicePolicy.TYPE_COMPANY.equals(order.getInvoiceType())) {
            return "Xuất HĐĐT MISA DN" + no + " · MST " + order.getInvoiceTaxCode() + " · gửi " + blankToEmpty(order.getInvoiceEmail());
        }
        return "Xuất HĐĐT MISA cá nhân" + no + " · " + InvoicePolicy.buyerPersonName(order);
    }

    /** Kiểm tra + ghi thông tin người mua (MST, tên, địa chỉ, email đều bắt buộc). */
    private static void applyBuyer(ShipmentOrder order, String rawTaxCode, String companyName, String address, String email) {
        String taxCode = VietnamTaxCode.compact(rawTaxCode);
        if (taxCode.isEmpty()) {
            throw new BadRequestAlertException("Mã số thuế người mua là bắt buộc", ENTITY, "invoiceTaxRequired");
        }
        if (!VietnamTaxCode.isValid(taxCode)) {
            throw new BadRequestAlertException("Mã số thuế không hợp lệ (sai định dạng hoặc checksum)", ENTITY, "invoiceTaxInvalid");
        }
        if (companyName == null) {
            throw new BadRequestAlertException("Tên công ty là bắt buộc", ENTITY, "invoiceCompanyRequired");
        }
        if (address == null) {
            throw new BadRequestAlertException("Địa chỉ công ty là bắt buộc", ENTITY, "invoiceAddressRequired");
        }
        if (email == null || !EMAIL.matcher(email).matches()) {
            throw new BadRequestAlertException("Email nhận hoá đơn không hợp lệ", ENTITY, "invoiceEmailInvalid");
        }
        if (companyName.length() > 200 || address.length() > 255 || email.length() > 120) {
            throw new BadRequestAlertException("Thông tin hoá đơn quá dài", ENTITY, "invoiceFieldTooLong");
        }
        order.setInvoiceRequested(true);
        order.setInvoiceTaxCode(VietnamTaxCode.normalize(taxCode));
        order.setInvoiceCompanyName(companyName);
        order.setInvoiceCompanyAddress(address);
        order.setInvoiceEmail(email);
    }

    /** Thông tin hoá đơn gửi từ popup đơn; requested=false = bỏ yêu cầu xuất. */
    public record InvoiceInfoRequest(
        Boolean requested,
        String taxCode,
        String companyName,
        String address,
        String email,
        String buyerName,
        String buyerIdNumber,
        String buyerPhone
    ) {
        public InvoiceInfoRequest(Boolean requested, String taxCode, String companyName, String address, String email) {
            this(requested, taxCode, companyName, address, email, null, null, null);
        }

        public InvoiceInfoRequest(Boolean requested, String taxCode, String companyName, String address, String email, String buyerName) {
            this(requested, taxCode, companyName, address, email, buyerName, null, null);
        }
    }

    private static void applyOptionalBuyerIds(ShipmentOrder order, String buyerIdNumber, String buyerPhone) {
        try {
            if (buyerIdNumber != null) {
                order.setInvoiceBuyerIdNumber(InvoicePolicy.normalizeBuyerIdNumber(buyerIdNumber));
            }
            if (buyerPhone != null) {
                order.setInvoiceBuyerPhone(InvoicePolicy.normalizeBuyerPhone(buyerPhone));
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY, "invoiceBuyerIdsInvalid");
        }
    }

    private void appendEvent(ShipmentOrder order, String action, String detail, String actor) {
        OrderEvent event = new OrderEvent();
        event.setEventAt(Instant.now());
        event.setAction(action);
        event.setDetail(detail.length() > 255 ? detail.substring(0, 255) : detail);
        event.setActorUsername(actor == null ? "system" : actor);
        event.setOrder(order);
        orderEventRepository.save(event);
    }

    /**
     * Cá nhân: họ tên người trên hóa đơn (mặc định người trả cước) + SĐT người trả, không MST.
     * Doanh nghiệp: thông tin công ty, kèm họ tên người (mặc định người trả cước, viết hoa).
     */
    ObjectNode buildPublishBody(ShipmentOrder order, MeInvoiceAmounts.Breakdown amounts, String refId, String type) {
        String invDate = LocalDate.now(VN).toString();
        boolean company = InvoicePolicy.TYPE_COMPANY.equals(type);

        String legalName;
        String fullName;
        String phone;
        String address;
        String buyerTax;
        String email;
        String person = InvoicePolicy.buyerPersonName(order);
        if (company) {
            legalName = blankToEmpty(order.getInvoiceCompanyName());
            fullName = person;
            phone = InvoicePolicy.buyerPhoneForInvoice(order, true);
            address = blankToEmpty(order.getInvoiceCompanyAddress());
            buyerTax = blankToEmpty(order.getInvoiceTaxCode());
            email = blankToEmpty(order.getInvoiceEmail());
        } else {
            legalName = "";
            fullName = person;
            phone = InvoicePolicy.buyerPhoneForInvoice(order, false);
            address = "";
            buyerTax = "";
            email = "";
        }
        String buyerId = blankToEmpty(order.getInvoiceBuyerIdNumber());
        boolean sendEmail = !email.isBlank();

        ObjectNode invoice = objectMapper.createObjectNode();
        invoice.put("RefID", refId);
        invoice.put("InvSeries", client.getInvSeries());
        invoice.put("InvDate", invDate);
        invoice.put("CurrencyCode", "VND");
        invoice.put("ExchangeRate", 1);
        invoice.put("PaymentMethodName", company ? "TM/CK" : "TM");
        invoice.put("IsInvoiceCalculatingMachine", true);
        invoice.put("BuyerLegalName", legalName);
        invoice.put("BuyerTaxCode", buyerTax);
        invoice.put("BuyerAddress", address);
        invoice.put("BuyerEmail", email);
        invoice.put("IsSendEmail", sendEmail);
        invoice.put("ReceiverName", company ? legalName : fullName);
        invoice.put("ReceiverEmail", email);
        invoice.put("BuyerPhoneNumber", phone);
        invoice.put("BuyerFullName", fullName);
        // MISA: CCCD/CMND người mua (không bắt buộc). Có thì gửi kèm.
        if (!buyerId.isBlank()) {
            invoice.put("BuyerIDNumber", buyerId);
        }

        invoice.put("TotalSaleAmountOC", amounts.net());
        invoice.put("TotalSaleAmount", amounts.net());
        invoice.put("TotalAmountWithoutVATOC", amounts.net());
        invoice.put("TotalAmountWithoutVAT", amounts.net());
        invoice.put("TotalVATAmountOC", amounts.vat());
        invoice.put("TotalVATAmount", amounts.vat());
        invoice.put("TotalDiscountAmountOC", 0);
        invoice.put("TotalDiscountAmount", 0);
        invoice.put("TotalAmountOC", amounts.gross());
        invoice.put("TotalAmount", amounts.gross());
        invoice.put("CustomField1", order.getOrderCode());

        ArrayNode details = invoice.putArray("OriginalInvoiceDetail");
        ObjectNode line = details.addObject();
        line.put("ItemType", 1);
        line.put("LineNumber", 1);
        line.put("SortOrder", 1);
        line.put("ItemCode", MeInvoiceAmounts.ITEM_CODE);
        line.put("ItemName", MeInvoiceAmounts.itemNameFor(order));
        line.put("UnitName", MeInvoiceAmounts.UNIT_NAME);
        line.put("Quantity", 1);
        line.put("UnitPrice", amounts.net());
        line.put("AmountOC", amounts.net());
        line.put("Amount", amounts.net());
        line.put("AmountWithoutVATOC", amounts.net());
        line.put("AmountWithoutVAT", amounts.net());
        line.put("DiscountRate", 0);
        line.put("DiscountAmountOC", 0);
        line.put("DiscountAmount", 0);
        line.put("VATRateName", MeInvoiceAmounts.VAT_RATE_NAME);
        line.put("VATAmountOC", amounts.vat());
        line.put("VATAmount", amounts.vat());

        ArrayNode tax = invoice.putArray("TaxRateInfo");
        ObjectNode taxRow = tax.addObject();
        taxRow.put("VATRateName", MeInvoiceAmounts.VAT_RATE_NAME);
        taxRow.put("AmountWithoutVATOC", amounts.net());
        taxRow.put("VATAmountOC", amounts.vat());

        ObjectNode root = objectMapper.createObjectNode();
        root.put("SignType", client.getSignType());
        String certificateSn = client.getCertificateSn();
        if (certificateSn != null && !certificateSn.isBlank()) {
            root.put("CertificateSN", certificateSn.trim());
        }
        ArrayNode data = root.putArray("InvoiceData");
        data.add(invoice);
        root.putNull("PublishInvoiceData");
        return root;
    }

    private void markSkipped(ShipmentOrder order, String reason) {
        if (isIssued(order)) {
            return;
        }
        order.setInvoiceStatus(STATUS_SKIPPED);
        order.setInvoiceError(truncate(reason));
        order.setInvoiceRefId(MeInvoiceAmounts.refIdFor(order.getOrderCode()));
        shipmentOrderRepository.save(order);
    }

    private void markFailed(ShipmentOrder order, MeInvoiceAmounts.Breakdown amounts, String message) {
        order.setInvoiceStatus(STATUS_FAILED);
        order.setInvoiceGrossAmount(amounts.gross());
        order.setInvoiceNetAmount(amounts.net());
        order.setInvoiceVatAmount(amounts.vat());
        order.setInvoiceError(truncate(message));
        shipmentOrderRepository.save(order);
    }

    /** Đơn huỷ (đã trả tiền khách) hoặc đang có ngoại lệ mở: không tự xuất, không xuất bù. Null = được xuất. */
    static String autoBlockReason(ShipmentOrder order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return "CANCELLED";
        }
        return openIssueType(order) != null ? "EXCEPTION" : null;
    }

    /** Loại ngoại lệ đang mở trên đơn (EXCEPTION / LOST / DAMAGED); không có → null. */
    static String openIssueType(ShipmentOrder order) {
        OrderIssue issue = order.getIssue();
        if (issue == null || issue.getIssueStatus() != IssueStatus.OPEN) {
            return null;
        }
        return issue.getIssueType() != null ? issue.getIssueType().name() : IssueType.EXCEPTION.name();
    }

    static boolean isIssued(ShipmentOrder order) {
        String st = order.getInvoiceStatus();
        return STATUS_ISSUED.equals(st) || STATUS_DUPLICATE.equals(st);
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
