package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.PatchOrderRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFacadeServicePatchFareTest {

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
            draftExpiryService
        );

        Office from = new Office();
        from.setId(1L);
        from.setCode("GP");

        order = new ShipmentOrder();
        order.setId(10L);
        order.setOrderCode("XE-H1-001");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSenderName("Sender");
        order.setSenderPhone("0901234567");
        order.setReceiverName("Receiver");
        order.setReceiverPhone("0912345678");
        order.setFromOffice(from);
        order.setFareAmount(new BigDecimal("40000"));
        order.setPaidAmount(new BigDecimal("30000"));
        order.setNote("keep-me");
    }

    @Test
    void assertFareNotBelowPaid_rejectsStrictlyLess() {
        assertThatThrownBy(() -> OrderFacadeService.assertFareNotBelowPaid(new BigDecimal("10000"), new BigDecimal("30000")))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("fareBelowPaid");
    }

    @Test
    void assertFareNotBelowPaid_allowsEqualAndGreater() {
        assertThatCode(() -> OrderFacadeService.assertFareNotBelowPaid(new BigDecimal("30000"), new BigDecimal("30000"))
        ).doesNotThrowAnyException();
        assertThatCode(() -> OrderFacadeService.assertFareNotBelowPaid(new BigDecimal("50000"), new BigDecimal("30000"))
        ).doesNotThrowAnyException();
    }

    @Test
    void assertFareNotBelowPaid_nullPaidTreatedAsZero() {
        assertThatCode(() -> OrderFacadeService.assertFareNotBelowPaid(BigDecimal.ZERO, null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> OrderFacadeService.assertFareNotBelowPaid(new BigDecimal("-1"), null))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("fareBelowPaid");
    }

    @Test
    void patch_fareGreaterThanPaid_ok() {
        stubLoadAndDetail();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("50000"));

        OrderDetailDTO dto = service.patch("XE-H1-001", req);

        assertThat(dto.getFareAmount()).isEqualByComparingTo("50000");
        assertThat(dto.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("50000");
        verify(shipmentOrderRepository).save(order);
    }

    @Test
    void patch_fareEqualPaid_ok() {
        stubLoadAndDetail();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("30000"));

        OrderDetailDTO dto = service.patch("XE-H1-001", req);

        assertThat(dto.getFareAmount()).isEqualByComparingTo("30000");
        assertThat(order.getFareAmount()).isEqualByComparingTo("30000");
        verify(shipmentOrderRepository).save(order);
    }

    @Test
    void patch_fareLessThanPaid_throws400Key() {
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("XE-H1-001")).thenReturn(Optional.of(order));
        PatchOrderRequest req = new PatchOrderRequest();
        req.setFareAmount(new BigDecimal("10000"));

        assertThatThrownBy(() -> service.patch("XE-H1-001", req))
            .isInstanceOf(BadRequestAlertException.class)
            .satisfies(ex -> {
                BadRequestAlertException bad = (BadRequestAlertException) ex;
                assertThat(bad.getErrorKey()).isEqualTo("fareBelowPaid");
                assertThat(bad.getStatusCode().value()).isEqualTo(400);
            });

        assertThat(order.getFareAmount()).isEqualByComparingTo("40000");
        verify(shipmentOrderRepository, never()).save(any());
    }

    @Test
    void patch_partialNote_preservesFareAndPaidAndSender() {
        stubLoadAndDetail();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setNote("only-note");

        OrderDetailDTO dto = service.patch("XE-H1-001", req);

        assertThat(dto.getNote()).isEqualTo("only-note");
        assertThat(dto.getFareAmount()).isEqualByComparingTo("40000");
        assertThat(dto.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(dto.getSenderName()).isEqualTo("Sender");
        assertThat(order.getFareAmount()).isEqualByComparingTo("40000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(order.getSenderName()).isEqualTo("Sender");
    }

    @Test
    void patch_withoutFare_doesNotChangeFare() {
        stubLoadAndDetail();
        PatchOrderRequest req = new PatchOrderRequest();
        req.setSenderName("Renamed");

        OrderDetailDTO dto = service.patch("XE-H1-001", req);

        assertThat(dto.getSenderName()).isEqualTo("Renamed");
        assertThat(dto.getFareAmount()).isEqualByComparingTo("40000");
        assertThat(dto.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(dto.getNote()).isEqualTo("keep-me");

        ArgumentCaptor<ShipmentOrder> captor = ArgumentCaptor.forClass(ShipmentOrder.class);
        verify(shipmentOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getFareAmount()).isEqualByComparingTo("40000");
    }

    private void stubLoadAndDetail() {
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("XE-H1-001")).thenReturn(Optional.of(order));
        when(shipmentOrderRepository.save(any(ShipmentOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(10L)).thenReturn(List.of());
        when(orderPodPhotoRepository.findByOrder_IdOrderBySequenceNoAsc(10L)).thenReturn(List.of());
        when(orderIssueRepository.findByOrder_IdOrderByOpenedAtAscIdAsc(10L)).thenReturn(List.of());
        when(orderLegRepository.findByOrder_IdOrderByLegIndexAsc(10L)).thenReturn(List.of());
    }
}
