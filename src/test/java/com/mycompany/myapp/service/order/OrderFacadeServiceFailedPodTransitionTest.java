package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderFacadeServiceFailedPodTransitionTest {

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
        order = new ShipmentOrder();
        order.setId(7L);
        order.setOrderCode("FAIL-1");
        order.setStatus(OrderStatus.FAILED_DELIVERY);
        order.setForwardStage(ForwardStage.FAILED);
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("FAIL-1")).thenReturn(Optional.of(order));
    }

    @Test
    void failedToDeliveredWithoutReasonIsRejectedAndDoesNotSave() {
        OrderTransitionRequest req = new OrderTransitionRequest();
        req.setToStatus(OrderStatus.DELIVERED);
        req.setAction("POD");
        req.setDetail("   ");

        assertThatThrownBy(() -> service.transition("FAIL-1", req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("podFailedReasonRequired");
        verify(shipmentOrderRepository, never()).save(any());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED_DELIVERY);
    }

    @Test
    void failedToDeliveredWithReasonClearsStage() {
        OrderTransitionRequest req = new OrderTransitionRequest();
        req.setToStatus(OrderStatus.DELIVERED);
        req.setAction("POD");
        req.setDetail("Nguyễn Văn A · Giao thất bại → thành công: Khách đến bưu cục nhận");

        var res = service.transition("FAIL-1", req);

        assertThat(res.isOk()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getForwardStage()).isNull();
        ArgumentCaptor<OrderEvent> event = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository).save(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("POD");
        assertThat(event.getValue().getDetail()).contains("Khách đến bưu cục nhận");
    }

    @Test
    void failedBackToDestUsesRedeliverInStage() {
        OrderTransitionRequest req = new OrderTransitionRequest();
        req.setToStatus(OrderStatus.AT_DEST);
        req.setAction("REDELIVER_IN");
        req.setDetail("Nhập kho giao lại");

        service.transition("FAIL-1", req);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AT_DEST);
        assertThat(order.getForwardStage()).isEqualTo(ForwardStage.DEST_WH_IN);
        ArgumentCaptor<OrderEvent> event = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository).save(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("REDELIVER_IN");
    }
}
