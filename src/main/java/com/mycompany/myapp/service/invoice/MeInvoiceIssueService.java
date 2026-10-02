package com.mycompany.myapp.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.IssueInvoiceRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phát hành HĐĐT MISA ngay khi đơn DELIVERED — idempotent theo RefID {@code XE-{orderCode}}.
 * Không retry định kỳ; lỗi thì status=FAILED (có thể gọi lại thủ công {@code /invoice/issue}).
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

    private static final List<String> DELIVERED_ACTIONS = List.of("POD", "POD_QUAY", "DELIVERED", "TRANSITION_DELIVERED");
    private static final java.time.format.DateTimeFormatter DAY_FMT = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final DayClosureGuard dayClosureGuard;
    private final MisaMeInvoiceClient client;
    private final ObjectMapper objectMapper;
    private final boolean issueOnlyWhenRequested;

    public MeInvoiceIssueService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderEventRepository orderEventRepository,
        DayClosureGuard dayClosureGuard,
        MisaMeInvoiceClient client,
        ObjectMapper objectMapper,
        @Value("${cpn.misa.issue-only-when-requested:true}") boolean issueOnlyWhenRequested
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderEventRepository = orderEventRepository;
        this.dayClosureGuard = dayClosureGuard;
        this.client = client;
        this.objectMapper = objectMapper;
        this.issueOnlyWhenRequested = issueOnlyWhenRequested;
    }

    /** Sau commit DELIVERED → gọi MISA ngay (async, không chặn response POD). */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onOrderDelivered(OrderDeliveredEvent event) {
        if (event == null || event.orderCode() == null || event.orderCode().isBlank()) {
            return;
        }
        try {
            issueForOrderCode(event.orderCode().trim());
        } catch (Exception e) {
            LOG.error("MISA auto-issue failed for {}: {}", event.orderCode(), e.getMessage());
        }
    }

    @Transactional
    public void issueForOrderCode(String orderCode) {
        ShipmentOrder order = shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(orderCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderCode));
        issueForOrder(order);
    }

    /**
     * Xuất HĐĐT thủ công từ màn Giao thành công: lưu thông tin người mua (bắt buộc MST + tên + địa chỉ + email)
     * rồi gọi MISA ngay. Lỗi nghiệp vụ → 400; lỗi MISA → invoiceStatus=FAILED + invoiceError (gọi lại được).
     */
    @Transactional
    public ShipmentOrder issueManual(String orderCode, IssueInvoiceRequest request, String actor) {
        if (!client.isEnabled()) {
            throw new BadRequestAlertException("Chưa bật kết nối MISA — không xuất được hoá đơn", ENTITY, "misaDisabled");
        }
        ShipmentOrder order = shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(orderCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderCode));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestAlertException("Chỉ xuất hoá đơn cho đơn đã giao thành công", ENTITY, "notDelivered");
        }
        if (isAlreadyIssued(order)) {
            String no = blankToEmpty(order.getInvoiceNo());
            throw new BadRequestAlertException(
                "Đơn đã xuất hoá đơn" + (no.isEmpty() ? "" : " số " + no) + " — không xuất lại",
                ENTITY,
                "alreadyIssued"
            );
        }
        dayClosureGuard.assertOrderMutable(order);
        assertIssuedOnDeliveryDay(order);

        IssueInvoiceRequest req = request != null ? request : new IssueInvoiceRequest();
        applyBuyer(
            order,
            firstNonBlank(req.getTaxCode(), order.getInvoiceTaxCode()),
            firstNonBlank(req.getCompanyName(), order.getInvoiceCompanyName()),
            firstNonBlank(req.getAddress(), order.getInvoiceCompanyAddress()),
            firstNonBlank(req.getEmail(), order.getInvoiceEmail())
        );
        shipmentOrderRepository.save(order);

        issueForOrder(order);

        String st = order.getInvoiceStatus();
        String detail = STATUS_ISSUED.equals(st) || STATUS_DUPLICATE.equals(st)
            ? "Xuất HĐĐT MISA" +
            (order.getInvoiceNo() != null ? " số " + order.getInvoiceNo() : "") +
            " · MST " +
            order.getInvoiceTaxCode() +
            " · gửi " +
            order.getInvoiceEmail()
            : "Xuất HĐĐT MISA lỗi: " + blankToEmpty(order.getInvoiceError());
        appendEvent(order, "INVOICE_ISSUE", detail, actor);
        return order;
    }

    /**
     * Lưu / bỏ thông tin xuất hoá đơn ở mọi trạng thái đơn (không qua chốt ngày vì không đụng tiền). Đơn đã xuất HĐ
     * thành công thì khoá. Đơn chưa giao có thông tin sẽ tự xuất khi giao thành công.
     */
    @Transactional
    public ShipmentOrder saveInfo(String orderCode, InvoiceInfoRequest request, String actor) {
        ShipmentOrder order = shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(orderCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderCode));
        if (isAlreadyIssued(order)) {
            String no = blankToEmpty(order.getInvoiceNo());
            throw new BadRequestAlertException(
                "Đơn đã xuất hoá đơn" + (no.isEmpty() ? "" : " số " + no) + " — không sửa thông tin hoá đơn",
                ENTITY,
                "alreadyIssued"
            );
        }
        InvoiceInfoRequest req = request != null ? request : new InvoiceInfoRequest(true, null, null, null, null);
        String detail;
        if (Boolean.FALSE.equals(req.requested())) {
            order.setInvoiceRequested(false);
            order.setInvoiceTaxCode(null);
            order.setInvoiceCompanyName(null);
            order.setInvoiceCompanyAddress(null);
            order.setInvoiceEmail(null);
            detail = "Bỏ yêu cầu xuất hoá đơn";
        } else {
            applyBuyer(
                order,
                firstNonBlank(req.taxCode()),
                firstNonBlank(req.companyName()),
                firstNonBlank(req.address()),
                firstNonBlank(req.email())
            );
            detail = "Cập nhật thông tin hoá đơn · MST " + order.getInvoiceTaxCode() + " · " + order.getInvoiceCompanyName();
        }
        shipmentOrderRepository.save(order);
        appendEvent(order, "INVOICE_INFO", detail, actor);
        return order;
    }

    /** Kế toán: HĐ xuất muộn hơn ngày giao thành công bị phạt — chỉ cho xuất thủ công trong ngày giao (giờ VN). */
    private void assertIssuedOnDeliveryDay(ShipmentOrder order) {
        if (order.getId() == null) {
            return;
        }
        Instant deliveredAt = orderEventRepository
            .latestEventAtByOrderIds(List.of(order.getId()), DELIVERED_ACTIONS)
            .stream()
            .map(row -> (Instant) row[1])
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (deliveredAt == null) {
            return;
        }
        LocalDate deliveredDay = deliveredAt.atZone(VN).toLocalDate();
        if (deliveredDay.isBefore(LocalDate.now(VN))) {
            throw new BadRequestAlertException(
                "Chỉ xuất hoá đơn trong ngày giao thành công (" + deliveredDay.format(DAY_FMT) + ") — đã quá hạn",
                ENTITY,
                "invoiceDayPassed"
            );
        }
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
    public record InvoiceInfoRequest(Boolean requested, String taxCode, String companyName, String address, String email) {}

    private void appendEvent(ShipmentOrder order, String action, String detail, String actor) {
        OrderEvent event = new OrderEvent();
        event.setEventAt(Instant.now());
        event.setAction(action);
        event.setDetail(detail.length() > 255 ? detail.substring(0, 255) : detail);
        event.setActorUsername(actor == null ? "system" : actor);
        event.setOrder(order);
        orderEventRepository.save(event);
    }

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
        if (isAlreadyIssued(order)) {
            LOG.info("MISA already issued for {} status={}", order.getOrderCode(), order.getInvoiceStatus());
            return;
        }
        if (issueOnlyWhenRequested && !Boolean.TRUE.equals(order.getInvoiceRequested())) {
            markSkipped(order, "invoiceRequested=false");
            return;
        }

        MeInvoiceAmounts.Breakdown amounts = MeInvoiceAmounts.fromOrder(order);
        if (amounts.gross().signum() <= 0) {
            markSkipped(order, "gross=0");
            return;
        }

        String buyerTax = blankToEmpty(order.getInvoiceTaxCode());
        if (!buyerTax.isBlank()) {
            if (!VietnamTaxCode.isValid(buyerTax)) {
                markFailed(order, amounts, "MST người mua không hợp lệ — không gửi MISA");
                return;
            }
            if (blankToEmpty(order.getInvoiceCompanyName()).isBlank() || blankToEmpty(order.getInvoiceCompanyAddress()).isBlank()) {
                markFailed(order, amounts, "HĐ công ty cần Tên công ty + Địa chỉ");
                return;
            }
            buyerTax = VietnamTaxCode.normalize(buyerTax);
            order.setInvoiceTaxCode(buyerTax);
        } else if (Boolean.TRUE.equals(order.getInvoiceRequested())) {
            markFailed(order, amounts, "Thiếu MST người mua — không gửi MISA");
            return;
        }

        String refId = MeInvoiceAmounts.refIdFor(order.getOrderCode());
        order.setInvoiceRefId(refId);
        order.setInvoiceStatus(STATUS_PENDING);
        order.setInvoiceGrossAmount(amounts.gross());
        order.setInvoiceNetAmount(amounts.net());
        order.setInvoiceVatAmount(amounts.vat());
        order.setInvoiceError(null);
        shipmentOrderRepository.save(order);

        try {
            ObjectNode body = buildPublishBody(order, amounts, refId);
            MisaMeInvoiceClient.PublishResult result = client.publish(body);
            if (result.duplicated()) {
                order.setInvoiceStatus(STATUS_DUPLICATE);
                order.setInvoiceError(truncate("InvoiceDuplicated"));
                order.setInvoiceIssuedAt(Instant.now());
                shipmentOrderRepository.save(order);
                LOG.info("MISA InvoiceDuplicated RefID={} order={}", refId, order.getOrderCode());
                return;
            }
            order.setInvoiceStatus(STATUS_ISSUED);
            order.setInvoiceTransactionId(result.transactionId());
            order.setInvoiceNo(result.invNo());
            order.setInvoiceSeries(result.invSeries() != null ? result.invSeries() : client.getInvSeries());
            order.setInvoiceCode(result.invCode());
            order.setInvoiceIssuedAt(Instant.now());
            order.setInvoiceError(null);
            shipmentOrderRepository.save(order);
            LOG.info(
                "MISA issued order={} InvNo={} Tx={} Code={}",
                order.getOrderCode(),
                result.invNo(),
                result.transactionId(),
                result.invCode()
            );
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

    private ShipmentOrder requireIssued(String orderCode) {
        ShipmentOrder order = shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(orderCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderCode));
        String st = order.getInvoiceStatus();
        if (!STATUS_ISSUED.equals(st) && !STATUS_DUPLICATE.equals(st)) {
            throw new BadRequestAlertException("Đơn chưa có HĐĐT (status=" + st + ")", ENTITY, "notIssued");
        }
        return order;
    }

    private ObjectNode buildPublishBody(ShipmentOrder order, MeInvoiceAmounts.Breakdown amounts, String refId) {
        String invDate = LocalDate.now(VN).toString();
        String email = firstNonBlank(order.getInvoiceEmail(), null);
        boolean sendEmail = email != null && !email.isBlank();

        String legalName = firstNonBlank(order.getInvoiceCompanyName(), order.getSenderName(), order.getReceiverName(), "Khach le");
        String fullName = firstNonBlank(order.getSenderName(), order.getReceiverName(), legalName);
        String phone = firstNonBlank(order.getSenderPhone(), order.getReceiverPhone(), "");
        String address = blankToEmpty(order.getInvoiceCompanyAddress());
        String buyerTax = blankToEmpty(order.getInvoiceTaxCode());

        ObjectNode invoice = objectMapper.createObjectNode();
        invoice.put("RefID", refId);
        invoice.put("InvSeries", client.getInvSeries());
        invoice.put("InvDate", invDate);
        invoice.put("CurrencyCode", "VND");
        invoice.put("ExchangeRate", 1);
        invoice.put("PaymentMethodName", "TM/CK");
        invoice.put("IsInvoiceCalculatingMachine", true);
        invoice.put("BuyerLegalName", legalName);
        invoice.put("BuyerTaxCode", buyerTax);
        invoice.put("BuyerAddress", address);
        invoice.put("BuyerEmail", blankToEmpty(email));
        invoice.put("IsSendEmail", sendEmail);
        invoice.put("ReceiverName", fullName);
        invoice.put("ReceiverEmail", blankToEmpty(email));
        invoice.put("BuyerPhoneNumber", phone);
        invoice.put("BuyerFullName", fullName);

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
        ArrayNode data = root.putArray("InvoiceData");
        data.add(invoice);
        root.putNull("PublishInvoiceData");
        return root;
    }

    private void markSkipped(ShipmentOrder order, String reason) {
        if (STATUS_ISSUED.equals(order.getInvoiceStatus()) || STATUS_DUPLICATE.equals(order.getInvoiceStatus())) {
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

    private static boolean isAlreadyIssued(ShipmentOrder order) {
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
