package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.*;
import com.mycompany.myapp.security.PermissionService;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class CancelRequestTest {

    private OrderIssueRepository issueRepository;
    private ShipmentOrderRepository orderRepository;
    private OrderFacadeService orderFacadeService;
    private PermissionService permissionService;
    private ExceptionFacadeService service;
    private ShipmentOrder order;

    @BeforeEach
    void setUp() {
        issueRepository = mock(OrderIssueRepository.class);
        orderRepository = mock(ShipmentOrderRepository.class);
        orderFacadeService = mock(OrderFacadeService.class);
        permissionService = mock(PermissionService.class);
        service = new ExceptionFacadeService(
            orderRepository,
            issueRepository,
            mock(OrderReturnRequestRepository.class),
            mock(OrderPodPhotoRepository.class),
            mock(OrderEventRepository.class),
            orderFacadeService,
            mock(DayClosureGuard.class),
            mock(StaffAccessService.class),
            permissionService
        );
        order = new ShipmentOrder();
        order.setId(5L);
        order.setOrderCode("ORD5");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setForwardStage(ForwardStage.WH_IN);
        when(orderRepository.findOneByOrderCodeOrDraftCode("ORD5")).thenReturn(Optional.of(order));
        when(issueRepository.saveAndFlush(any(OrderIssue.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void openRequiresReason() {
        assertThatThrownBy(() -> service.openIssue("ORD5", IssueType.CANCEL_REQUEST, "  ", List.of())).isInstanceOf(
            BadRequestAlertException.class
        );
    }

    @Test
    void openOnlyAtSenderWarehouse() {
        order.setStatus(OrderStatus.WAITING);
        order.setForwardStage(ForwardStage.TRANSFER_PENDING);
        order.setCurrentTrip(new Trip());
        assertThatThrownBy(() -> service.openIssue("ORD5", IssueType.CANCEL_REQUEST, "Khách không gửi", List.of())).isInstanceOf(
            BadRequestAlertException.class
        );
    }

    @Test
    void openCreatesPendingRequest() {
        service.openIssue("ORD5", IssueType.CANCEL_REQUEST, "Khách không gửi", List.of());
        assertThat(order.getIssue()).isNotNull();
        assertThat(order.getIssue().getIssueType()).isEqualTo(IssueType.CANCEL_REQUEST);
        assertThat(order.getIssue().getIssueStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    private void withOpenRequest() {
        OrderIssue issue = new OrderIssue();
        issue.setIssueType(IssueType.CANCEL_REQUEST);
        issue.setIssueStatus(IssueStatus.OPEN);
        issue.setReason("Khách không gửi");
        issue.setOpenedByUsername("dp01");
        order.setIssue(issue);
    }

    @Test
    void approveRequiresAdmin() {
        withOpenRequest();
        assertThatThrownBy(() -> service.approveCancelRequest("ORD5", null)).isInstanceOf(ResponseStatusException.class);
        verify(orderFacadeService, never()).transition(any(), any());
    }

    @Test
    void approveCancelsOrder() {
        withOpenRequest();
        when(permissionService.isSystemAdmin()).thenReturn(true);
        service.approveCancelRequest("ORD5", null);
        assertThat(order.getIssue().getIssueStatus()).isEqualTo(IssueStatus.RESOLVED);
        ArgumentCaptor<OrderTransitionRequest> tr = ArgumentCaptor.forClass(OrderTransitionRequest.class);
        verify(orderFacadeService).transition(eq("ORD5"), tr.capture());
        assertThat(tr.getValue().getToStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(tr.getValue().getDetail()).contains("dp01").contains("Khách không gửi");
    }

    @Test
    void rejectRestoresOrder() {
        withOpenRequest();
        when(permissionService.isSystemAdmin()).thenReturn(true);
        service.rejectCancelRequest("ORD5", "Khách vẫn gửi");
        assertThat(order.getIssue()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderFacadeService, never()).transition(any(), any());
    }
}
