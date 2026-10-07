package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.autocall.AutoCallService;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class StaleDestWarehouseServiceTest {

    @Mock
    private ShipmentOrderRepository shipmentOrderRepository;

    @Mock
    private OrderFacadeService orderFacadeService;

    @Mock
    private AutoCallService autoCallService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private StaleDestWarehouseService service;

    @BeforeEach
    void setUp() {
        service = new StaleDestWarehouseService(shipmentOrderRepository, orderFacadeService, transactionManager);
        service.setAutoCallService(autoCallService);
    }

    @Test
    void moveOne_atDestOverTwoDays_becomesFailedDelivery_andStopsCalls() {
        ShipmentOrder order = atDest();
        order.setUpdatedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        when(shipmentOrderRepository.findById(7L)).thenReturn(Optional.of(order));

        assertThat(service.moveOne(7L, Instant.now().minus(2, ChronoUnit.DAYS))).isTrue();

        ArgumentCaptor<OrderTransitionRequest> req = ArgumentCaptor.forClass(OrderTransitionRequest.class);
        verify(orderFacadeService).transition(eq("TB0410HACH"), req.capture());
        assertThat(req.getValue().getToStatus()).isEqualTo(OrderStatus.FAILED_DELIVERY);
        assertThat(req.getValue().getAction()).isEqualTo("STALE_DEST");
        assertThat(req.getValue().getDetail()).contains("2 ngày");
        verify(autoCallService).stopPendingCalls(eq(7L), any());
    }

    @Test
    void moveOne_recentOrOpenIssue_isLeftAtDest() {
        ShipmentOrder recent = atDest();
        recent.setUpdatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(shipmentOrderRepository.findById(7L)).thenReturn(Optional.of(recent));
        Instant cutoff = Instant.now().minus(2, ChronoUnit.DAYS);
        assertThat(service.moveOne(7L, cutoff)).isFalse();

        ShipmentOrder issue = atDest();
        issue.setUpdatedAt(Instant.now().minus(5, ChronoUnit.DAYS));
        OrderIssue open = new OrderIssue();
        open.setIssueType(IssueType.EXCEPTION);
        open.setIssueStatus(IssueStatus.OPEN);
        issue.setIssue(open);
        when(shipmentOrderRepository.findById(7L)).thenReturn(Optional.of(issue));
        assertThat(service.moveOne(7L, cutoff)).isFalse();

        verify(orderFacadeService, never()).transition(any(), any());
    }

    private static ShipmentOrder atDest() {
        ShipmentOrder order = new ShipmentOrder();
        order.setId(7L);
        order.setOrderCode("TB0410HACH");
        order.setStatus(OrderStatus.AT_DEST);
        order.setForwardStage(ForwardStage.DEST_WH_IN);
        return order;
    }
}
