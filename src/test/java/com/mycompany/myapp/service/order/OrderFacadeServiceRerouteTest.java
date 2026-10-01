package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.RerouteDestinationRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderFacadeServiceRerouteTest {

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
    private final Office nd = office(1L, "VP_ND", "VP Nam Định");
    private final Office tb = office(2L, "VP_TB", "VP Thái Bình");

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
        order.setId(1L);
        order.setOrderCode("GA0110REROUTE");
        order.setStatus(OrderStatus.AT_DEST);
        order.setForwardStage(ForwardStage.DEST_WH_IN);
        order.setFromOffice(office(9L, "VP_GA", "VP Giáp Bát"));
        order.setToOffice(nd);
        order.setFinalToOffice(nd);
        order.setItineraryLabel("GA - ND");
        order.setRouteLabel("GA - ND");
        lenient().when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("GA0110REROUTE")).thenReturn(Optional.of(order));
        lenient().when(officeRepository.findOneByCode("VP_TB")).thenReturn(Optional.of(tb));
        lenient().when(officeRepository.findOneByCode("VP_ND")).thenReturn(Optional.of(nd));
    }

    private static Office office(Long id, String code, String name) {
        Office o = new Office();
        o.setId(id);
        o.setCode(code);
        o.setName(name);
        return o;
    }

    private void asDispatcher(String officeCode) {
        StaffProfile p = new StaffProfile();
        p.setRoleCode(RoleCode.DH);
        when(staffAccessService.isSystemAdmin()).thenReturn(false);
        when(staffAccessService.current()).thenReturn(Optional.of(p));
        lenient().when(staffAccessService.scopedOfficeCode()).thenReturn(Optional.of(officeCode));
    }

    private static String errorKey(Throwable ex) {
        return ((BadRequestAlertException) ex).getErrorKey();
    }

    @Test
    void dispatcherOfHoldingOffice_movesOrderKeepingItinerary() {
        asDispatcher("VP_ND");

        service.rerouteDestination("GA0110REROUTE", new RerouteDestinationRequest("VP_TB", "Gửi tay sang Thái Bình"));

        assertThat(order.getToOffice()).isSameAs(tb);
        assertThat(order.getFinalToOffice()).isSameAs(tb);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AT_DEST);
        assertThat(order.getForwardStage()).isEqualTo(ForwardStage.DEST_WH_IN);
        assertThat(order.getItineraryLabel()).isEqualTo("GA - ND");
        ArgumentCaptor<OrderEvent> ev = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository).save(ev.capture());
        assertThat(ev.getValue().getAction()).isEqualTo("DEST_REROUTE");
        assertThat(ev.getValue().getDetail()).contains("VP Nam Định → VP Thái Bình").contains("Gửi tay sang Thái Bình");
    }

    @Test
    void dispatcherOfOtherOffice_isForbidden() {
        asDispatcher("VP_TB");

        assertThatThrownBy(() -> service.rerouteDestination("GA0110REROUTE", new RerouteDestinationRequest("VP_TB", "Gửi tay"))
        ).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void inTransitOrder_isRejected() {
        when(staffAccessService.isSystemAdmin()).thenReturn(true);
        order.setStatus(OrderStatus.IN_TRANSIT);

        assertThatThrownBy(() -> service.rerouteDestination("GA0110REROUTE", new RerouteDestinationRequest("VP_TB", "Gửi tay")))
            .extracting(OrderFacadeServiceRerouteTest::errorKey)
            .isEqualTo("rerouteStatus");
    }

    @Test
    void orderWithShipper_isRejected() {
        when(staffAccessService.isSystemAdmin()).thenReturn(true);
        order.setForwardStage(ForwardStage.DELIVERING);

        assertThatThrownBy(() -> service.rerouteDestination("GA0110REROUTE", new RerouteDestinationRequest("VP_TB", "Gửi tay")))
            .extracting(OrderFacadeServiceRerouteTest::errorKey)
            .isEqualTo("rerouteStatus");
    }

    @Test
    void sameOffice_isRejected() {
        when(staffAccessService.isSystemAdmin()).thenReturn(true);

        assertThatThrownBy(() -> service.rerouteDestination("GA0110REROUTE", new RerouteDestinationRequest("VP_ND", "Gửi tay")))
            .extracting(OrderFacadeServiceRerouteTest::errorKey)
            .isEqualTo("rerouteSameOffice");
    }

    @Test
    void missingReason_isRejected() {
        assertThatThrownBy(() -> service.rerouteDestination("GA0110REROUTE", new RerouteDestinationRequest("VP_TB", " ")))
            .extracting(OrderFacadeServiceRerouteTest::errorKey)
            .isEqualTo("rerouteReasonRequired");
    }
}
