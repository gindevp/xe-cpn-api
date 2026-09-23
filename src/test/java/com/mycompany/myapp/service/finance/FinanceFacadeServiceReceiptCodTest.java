package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ReceiptOrderLine;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CreateReceiptRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptDTO;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptLineRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private DayClosureGuard dayClosureGuard;

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
            dayClosureGuard
        );

        order = new ShipmentOrder();
        order.setId(1L);
        order.setOrderCode("GP-COD-001");
        order.setFareAmount(new BigDecimal("40000"));
        order.setPaidAmount(new BigDecimal("10000"));
        order.setCodAmount(new BigDecimal("50000"));

        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("GP-COD-001")).thenReturn(Optional.of(order));
        lenient().when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderPaymentRepository.sumTruocByOrderId(any())).thenReturn(BigDecimal.ZERO);
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
            List.of(new ReceiptLineRequest("GP-COD-001", new BigDecimal("80000")))
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
    void createReceipt_rejectsAboveFareDuePlusCod() {
        CreateReceiptRequest req = new CreateReceiptRequest(
            "NV A",
            null,
            null,
            List.of(new ReceiptLineRequest("GP-COD-001", new BigDecimal("80001")))
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
            List.of(new ReceiptLineRequest("GP-COD-001", new BigDecimal("50000")))
        );

        ReceiptDTO dto = service.createReceipt(req);

        assertThat(dto.totalAmount()).isEqualByComparingTo("50000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("40000");

        ArgumentCaptor<OrderPayment> payCap = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(payCap.capture());
        assertThat(payCap.getValue().getPaymentKind()).isEqualTo(PaymentKind.COD);
        assertThat(payCap.getValue().getAmount()).isEqualByComparingTo("50000");
    }
}
