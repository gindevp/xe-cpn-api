package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Nhập kho gửi đơn người gửi trả = đã thu cước người gửi. */
@ExtendWith(MockitoExtension.class)
class OrderFacadeServiceSenderPrepaidTest {

    @Mock
    private ShipmentOrderRepository shipmentOrderRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private OrderPodPhotoRepository orderPodPhotoRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private OrderCodeGenerator orderCodeGenerator;

    @Mock
    private SimpleFareCalculator fareCalculator;

    @Mock
    private StaffAccessService staffAccessService;

    @Mock
    private OrderLegRepository orderLegRepository;

    @Mock
    private DayClosureGuard dayClosureGuard;

    @Mock
    private OrderIssueRepository orderIssueRepository;

    @Mock
    private DraftExpiryService draftExpiryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    private OrderFacadeService service;
    private ShipmentOrder order;

    @BeforeEach
    void setUp() {
        service = new OrderFacadeService(
            shipmentOrderRepository,
            orderEventRepository,
            orderPodPhotoRepository,
            officeRepository,
            orderCodeGenerator,
            fareCalculator,
            staffAccessService,
            orderLegRepository,
            dayClosureGuard,
            orderIssueRepository,
            draftExpiryService,
            eventPublisher
        );
        service.setOrderPaymentRepository(orderPaymentRepository);
        order = new ShipmentOrder();
        order.setId(1L);
        order.setOrderCode("PT3009TEST");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setFareAmount(new BigDecimal("30000"));
        order.setPaidAmount(BigDecimal.ZERO);
    }

    @Test
    void senderPays_recordsPrepaidForRemainingFare() {
        order.setPaidAmount(new BigDecimal("10000"));

        service.collectSenderFareOnWarehouseIn(order);

        ArgumentCaptor<OrderPayment> cap = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentRepository).save(cap.capture());
        assertThat(cap.getValue().getAmount()).isEqualByComparingTo("20000");
        assertThat(cap.getValue().getPaymentKind()).isEqualTo(PaymentKind.TRUOC);
        assertThat(cap.getValue().getNote()).isEqualTo(OrderFacadeService.NOTE_SENDER_PREPAID);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void alreadyPaid_orReceiverPays_recordsNothing() {
        order.setPaidAmount(new BigDecimal("30000"));
        service.collectSenderFareOnWarehouseIn(order);

        order.setPaidAmount(BigDecimal.ZERO);
        order.setPaymentTerm(PaymentTerm.NHAN_TRA);
        service.collectSenderFareOnWarehouseIn(order);

        verify(orderPaymentRepository, never()).save(any());
    }

    @Test
    void returningOrder_recordsNothing() {
        order.setStatus(OrderStatus.RETURNING);

        service.collectSenderFareOnWarehouseIn(order);

        verify(orderPaymentRepository, never()).save(any());
    }

    @Test
    void closedCollectionDay_skipsWithoutBlockingWarehouseIn() {
        doThrow(new IllegalStateException("closed")).when(dayClosureGuard).assertCollectionMutable(order);

        service.collectSenderFareOnWarehouseIn(order);

        verify(orderPaymentRepository, never()).save(any());
        assertThat(order.getPaidAmount()).isEqualByComparingTo("0");
    }

    @Test
    void customerOrderNotWarehouseReceived_cannotBeLoaded() {
        when(orderEventRepository.existsByOrder_IdAndActionAndActorUsername(1L, "CREATE", "customer")).thenReturn(true);

        assertThatThrownBy(() -> service.assertSenderWarehouseReceived(order))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("notWarehouseReceived");
    }

    @Test
    void warehouseReceivedOrStaffOrder_canBeLoaded() {
        order.setPickedUpAt(Instant.now());
        assertThatCode(() -> service.assertSenderWarehouseReceived(order)).doesNotThrowAnyException();

        order.setPickedUpAt(null);
        when(orderEventRepository.existsByOrder_IdAndActionAndActorUsername(1L, "CREATE", "customer")).thenReturn(false);
        assertThatCode(() -> service.assertSenderWarehouseReceived(order)).doesNotThrowAnyException();
    }
}
