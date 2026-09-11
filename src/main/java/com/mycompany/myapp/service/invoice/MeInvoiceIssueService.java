package com.mycompany.myapp.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final MisaMeInvoiceClient client;
    private final ObjectMapper objectMapper;
    private final boolean issueOnlyWhenRequested;

    public MeInvoiceIssueService(
        ShipmentOrderRepository shipmentOrderRepository,
        MisaMeInvoiceClient client,
        ObjectMapper objectMapper,
        @Value("${cpn.misa.issue-only-when-requested:true}") boolean issueOnlyWhenRequested
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
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
            if (blankToEmpty(order.getInvoiceCompanyName()).isBlank() || blankToEmpty(order.getInvoiceCompanyAddress()).isBlank()) {
                markFailed(order, amounts, "HĐ công ty cần Tên công ty + Địa chỉ");
                return;
            }
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
        line.put("ItemName", MeInvoiceAmounts.ITEM_NAME);
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
