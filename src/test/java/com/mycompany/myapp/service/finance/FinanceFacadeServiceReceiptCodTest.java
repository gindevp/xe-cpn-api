package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ReceiptOrderLine;
import com.mycompany.myapp.domain.ReceiptWaiver;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.AuditLogRepository;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.repository.ReceiptWaiverRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.audit.AuditRecorder;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CreateReceiptRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptDTO;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptLineRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.WaiveItem;
import com.mycompany.myapp.service.finance.FinanceFacadeService.WaiveRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phiếu thu gồm COD: tổng line = fareDue + cod; paidAmount chỉ tăng phần cước.
 */
@ExtendWith(MockitoExtension.class)
class FinanceFacadeServiceReceiptCodTest {

    @Mock
    private ShipmentOrderRepository shipmentOrderRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ReceiptOrderLineRepository receiptOrderLineRepository;

    @Mock
    private DayClosureRepository dayClosureRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @Mock
    private DayClosureGuard dayClosureGuard;

    @Mock
    private ReceiptWaiverRepository receiptWaiverRepository;

    @Mock
    private AuditRecorder auditRecorder;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private StaffAccessService staffAccessService;

    private FinanceFacadeService service;
    private ShipmentOrder order;

    @BeforeEach
    void setUp() {
        service = new FinanceFacadeService(
            shipmentOrderRepository,
            receiptRepository,
            receiptOrderLineRepository,
            dayClosureRepository,
            officeRepository,
            orderPaymentRepository,
            orderEventRepository,
            staffProfileRepository,
            dayClosureGuard,
            receiptWaiverRepository,
            auditRecorder,
            auditLogRepository,
            staffAccessService
        );

        order = new ShipmentOrder();
        order.setId(1L);
        order.setOrderCode("GP-COD-001");
        order.setFareAmount(new BigDecimal("40000"));
        order.setPaidAmount(new BigDecimal("10000"));
        order.setCodAmount(new BigDecimal("50000"));
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);

        lenient().when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("GP-COD-001")).thenReturn(Optional.of(order));
        lenient().when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(shipmentOrderRepository.save(any(ShipmentOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(receiptOrderLineRepository.save(any(ReceiptOrderLine.class))).thenAnswer(inv -> inv.getArgument(0));
        AtomicLong id = new AtomicLong(10);
        lenient()
            .when(receiptRepository.save(any(Receipt.class)))
            .thenAnswer(inv -> {
                Receipt r = inv.getArgument(0);
                r.setId(id.getAndIncrement());
                if (r.getReceiptCode() == null) {
                    r.setReceiptCode("PT-TEST-1");
                }
                return r;
            });
        lenient().when(receiptRepository.countByReceiptCodeStartingWith(any())).thenReturn(0L);
    }

    @Test
    void createReceipt_splitsFareAndCod_paidOnlyFare() {
        CreateReceiptRequest req = new CreateReceiptRequest(
            "NV A",
            "NVA",
            null,
            List.of(new ReceiptLineRequest("GP-COD-001", new BigDecimal("80000"), ReceiptSettlement.DELIVERY))
        );

        ReceiptDTO dto = service.createReceipt(req);

        assertThat(dto.totalAmount()).isEqualByComparingTo("80000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("40000"); // 10000 + 30000 fare due

        ArgumentCaptor<OrderPayment> payCap = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository, org.mockito.Mockito.times(2)).save(payCap.capture());
        List<OrderPayment> pays = payCap.getAllValues();
        assertThat(pays).anySatisfy(p -> {
            assertThat(p.getPaymentKind()).isEqualTo(PaymentKind.SAU);
            assertThat(p.getAmount()).isEqualByComparingTo("30000");
            assertThat(p.getNote()).isEqualTo("RECEIPT");
        });
        assertThat(pays).anySatisfy(p -> {
            assertThat(p.getPaymentKind()).isEqualTo(PaymentKind.COD);
            assertThat(p.getAmount()).isEqualByComparingTo("50000");
            assertThat(p.getNote()).isEqualTo("RECEIPT_COD");
        });
    }

    @Test
    void createReceipt_skipsCodeStillUsedAfterDeletedReceipt() {
        when(receiptRepository.countByReceiptCodeStartingWith(any())).thenReturn(5L);
        when(receiptRepository.existsByReceiptCode(org.mockito.ArgumentMatchers.endsWith("-006"))).thenReturn(true);
        CreateReceiptRequest req = new CreateReceiptRequest(
            "NV A",
            null,
            null,
            List.of(new ReceiptLineRequest("GP-COD-001", new BigDecimal("50000"), null))
        );

        ReceiptDTO dto = service.createReceipt(req);

        assertThat(dto.receiptCode()).endsWith("-007");
    }

    @Test
    void createReceipt_rejectsAboveFareDuePlusCod() {
        CreateReceiptRequest req = new CreateReceiptRequest(
            "NV A",
            null,
            null,
            List.of(new ReceiptLineRequest("GP-COD-001", new BigDecimal("80001"), ReceiptSettlement.DELIVERY))
        );

        assertThatThrownBy(() -> service.createReceipt(req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("amountExceedsDue");

        verify(orderPaymentRepository, never()).save(any());
        assertThat(order.getPaidAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void createReceipt_codOnly_whenFareAlreadyPaid() {
        order.setPaidAmount(new BigDecimal("40000"));
        CreateReceiptRequest req = new CreateReceiptRequest(
            "NV A",
            null,
            null,
            List.of(new ReceiptLineRequest("GP-COD-001", new BigDecimal("50000"), null))
        );

        ReceiptDTO dto = service.createReceipt(req);

        assertThat(dto.totalAmount()).isEqualByComparingTo("50000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("40000");

        ArgumentCaptor<OrderPayment> payCap = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(payCap.capture());
        assertThat(payCap.getValue().getPaymentKind()).isEqualTo(PaymentKind.COD);
        assertThat(payCap.getValue().getAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void waiveDues_rejectsNonAdmin() {
        when(staffAccessService.isSystemAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.waiveDues(new WaiveRequest("x", List.of(new WaiveItem("GP-COD-001", null))))).isInstanceOf(
            ResponseStatusException.class
        );
        verify(receiptWaiverRepository, never()).save(any());
    }

    @Test
    void waiveDues_requiresReason() {
        when(staffAccessService.isSystemAdmin()).thenReturn(true);

        assertThatThrownBy(() -> service.waiveDues(new WaiveRequest("  ", List.of(new WaiveItem("GP-COD-001", null)))))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("waiveReasonRequired");
    }

    @Test
    void waiveDues_savesOutstandingDeliveryAndAudits() {
        when(staffAccessService.isSystemAdmin()).thenReturn(true);

        var result = service.waiveDues(new WaiveRequest("Khách bùng", List.of(new WaiveItem("GP-COD-001", ReceiptSettlement.DELIVERY))));

        ArgumentCaptor<ReceiptWaiver> cap = ArgumentCaptor.forClass(ReceiptWaiver.class);
        verify(receiptWaiverRepository).save(cap.capture());
        assertThat(cap.getValue().getPortion()).isEqualTo(ReceiptSettlement.DELIVERY);
        assertThat(cap.getValue().getAmount()).isEqualByComparingTo("80000");
        assertThat(cap.getValue().getReason()).isEqualTo("Khách bùng");
        assertThat(result.count()).isEqualTo(1);
        verify(auditRecorder).record(eq("RECEIPT_DUE_WAIVE"), eq("ReceiptDue"), eq("GP-COD-001"), anyString());
    }

    @Test
    void assertNoHeldMoney_blocksCancelWhenCollectedNotSubmitted() {
        order.setStatus(OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> service.assertNoHeldMoney("GP-COD-001"))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("cancelHasHeldMoney");
    }

    @Test
    void assertNoHeldMoney_allowsWhenNothingCollected() {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaidAmount(BigDecimal.ZERO);

        service.assertNoHeldMoney("GP-COD-001");
    }

    @Test
    void assertNoHeldMoney_allowsAfterWaive() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(receiptWaiverRepository.sumByOrderIds(any())).thenReturn(
            List.<Object[]>of(new Object[] { 1L, ReceiptSettlement.SENDER, new BigDecimal("10000") })
        );

        service.assertNoHeldMoney("GP-COD-001");
    }

    @Test
    void settleHeldMoneyForCancel_adminWaivesHeldMoney() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(staffAccessService.isSystemAdmin()).thenReturn(true);

        service.settleHeldMoneyForCancel("GP-COD-001", "Khách không gửi nữa");

        ArgumentCaptor<ReceiptWaiver> cap = ArgumentCaptor.forClass(ReceiptWaiver.class);
        verify(receiptWaiverRepository).save(cap.capture());
        assertThat(cap.getValue().getPortion()).isEqualTo(ReceiptSettlement.SENDER);
        assertThat(cap.getValue().getAmount()).isEqualByComparingTo("10000");
        assertThat(cap.getValue().getReason()).isEqualTo("Huỷ đơn · Khách không gửi nữa");
        verify(auditRecorder).record(eq("RECEIPT_DUE_WAIVE"), eq("ReceiptDue"), eq("GP-COD-001"), anyString());
    }

    @Test
    void settleHeldMoneyForCancel_nonAdminStillBlocked() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(staffAccessService.isSystemAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.settleHeldMoneyForCancel("GP-COD-001", "x"))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("cancelHasHeldMoney");
        verify(receiptWaiverRepository, never()).save(any());
    }

    @Test
    void settleHeldMoneyForCancel_adminNoopWhenNothingHeld() {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaidAmount(BigDecimal.ZERO);
        when(staffAccessService.isSystemAdmin()).thenReturn(true);

        service.settleHeldMoneyForCancel("GP-COD-001", "x");

        verify(receiptWaiverRepository, never()).save(any());
    }

    private static OrderPayment payment(PaymentKind kind, String amount, String note, Instant at, String collector) {
        OrderPayment p = new OrderPayment();
        p.setPaymentKind(kind);
        p.setAmount(new BigDecimal(amount));
        p.setNote(note);
        p.setPaymentAt(at);
        p.setCollectorUsername(collector);
        return p;
    }

    @Test
    void cancelReceipt_rejectsNonAdmin() {
        when(staffAccessService.isSystemAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.cancelReceipt("PT-1", "sai")).isInstanceOf(ResponseStatusException.class);
        verify(receiptRepository, never()).delete(any(Receipt.class));
    }

    @Test
    void cancelReceipt_requiresReason() {
        when(staffAccessService.isSystemAdmin()).thenReturn(true);

        assertThatThrownBy(() -> service.cancelReceipt("PT-1", " "))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("receiptCancelReasonRequired");
    }

    @Test
    void cancelReceipt_reversesOnlyReceiptPaymentsAndAudits() {
        when(staffAccessService.isSystemAdmin()).thenReturn(true);
        Instant at = Instant.parse("2025-05-01T03:00:00Z");
        Receipt receipt = new Receipt();
        receipt.setId(5L);
        receipt.setReceiptCode("PT-1");
        receipt.setPayerName("NV A");
        receipt.setTotalAmount(new BigDecimal("80000"));
        receipt.setCreatedAt(at);
        receipt.setCreatedByUsername("dp1");
        ReceiptOrderLine line = new ReceiptOrderLine();
        line.setOrder(order);
        line.setReceipt(receipt);
        line.setAmountCollected(new BigDecimal("80000"));
        order.setPaidAmount(new BigDecimal("40000"));
        OrderPayment fare = payment(PaymentKind.SAU, "30000", "RECEIPT", at, "dp1");
        OrderPayment cod = payment(PaymentKind.COD, "50000", "RECEIPT_COD", at, "dp1");
        OrderPayment prepaid = payment(PaymentKind.SAU, "10000", null, at.minusSeconds(3600), "dp1");
        when(receiptRepository.findOneByReceiptCode("PT-1")).thenReturn(Optional.of(receipt));
        when(receiptOrderLineRepository.findByReceipt_Id(5L)).thenReturn(List.of(line));
        when(orderPaymentRepository.findByOrder_IdOrderByPaymentAtDesc(1L)).thenReturn(List.of(fare, cod, prepaid));

        service.cancelReceipt("PT-1", "Lập nhầm");

        assertThat(order.getPaidAmount()).isEqualByComparingTo("10000");
        verify(orderPaymentRepository).delete(fare);
        verify(orderPaymentRepository).delete(cod);
        verify(orderPaymentRepository, never()).delete(prepaid);
        verify(dayClosureGuard).assertCollectionMutable(order);
        verify(receiptOrderLineRepository).deleteAll(List.of(line));
        verify(receiptRepository).delete(receipt);
        verify(auditRecorder).record(eq("RECEIPT_CANCEL"), eq("Receipt"), eq("PT-1"), org.mockito.ArgumentMatchers.contains("Lập nhầm"));
    }

    @Test
    void createReceipt_rejectsWaivedAmount() {
        when(receiptWaiverRepository.sumByOrderIds(any())).thenReturn(
            List.<Object[]>of(new Object[] { 1L, ReceiptSettlement.DELIVERY, new BigDecimal("80000") })
        );
        CreateReceiptRequest req = new CreateReceiptRequest(
            "NV A",
            null,
            null,
            List.of(new ReceiptLineRequest("GP-COD-001", new BigDecimal("1000"), ReceiptSettlement.DELIVERY))
        );

        assertThatThrownBy(() -> service.createReceipt(req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("amountExceedsDue");
    }
}
