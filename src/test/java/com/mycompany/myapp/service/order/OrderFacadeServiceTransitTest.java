package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Branch;
import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderLeg;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.LegStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.ItineraryRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.config.TransitHubPolicyService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.CreateOrderRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/** Đơn tỉnh đi tỉnh qua VP trung chuyển: tạo 2 chặng, nhập kho trung chuyển, hoàn qua VP trung chuyển. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderFacadeServiceTransitTest {

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
    private ItineraryRepository itineraryRepository;

    @Mock
    private TransitHubPolicyService transitHubPolicyService;

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    private OrderFacadeService service;
    private final List<OrderLeg> legs = new ArrayList<>();
    private Office nd;
    private Office tb;
    private Office hub;

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
        service.setItineraryRepository(itineraryRepository);
        service.setTransitHubPolicyService(transitHubPolicyService);
        service.setOrderPaymentRepository(orderPaymentRepository);

        nd = office("VP_ND", 1L, "ND");
        tb = office("VP_TB", 2L, "TB");
        hub = office("VP_72TNT", 3L, "BC");
        when(officeRepository.findOneByCode("VP_ND")).thenReturn(Optional.of(nd));
        when(officeRepository.findOneByCode("VP_TB")).thenReturn(Optional.of(tb));
        when(transitHubPolicyService.resolveHub(nd, tb)).thenReturn(Optional.of(hub));
        when(transitHubPolicyService.resolveHub(tb, nd)).thenReturn(Optional.of(hub));
        when(orderCodeGenerator.nextOrderCode(anyString(), anyBoolean())).thenReturn("ND1310ABCD");
        when(fareCalculator.estimate(any(), anyBoolean(), anyBoolean(), any(), any(), any(), any(), any())).thenReturn(
            new SimpleFareCalculator.FareBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        itinerary("ND-BC", "Nam Định - Hà Nội", "Tuyến NĐ");
        itinerary("BC-TB", "Hà Nội - Thái Bình", "Tuyến TB");
        itinerary("TB-BC", "Thái Bình - Hà Nội", "Tuyến TB");
        itinerary("BC-ND", "Hà Nội - Nam Định", "Tuyến NĐ");

        AtomicLong idSeq = new AtomicLong(100);
        when(shipmentOrderRepository.save(any(ShipmentOrder.class))).thenAnswer(inv -> {
            ShipmentOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(idSeq.getAndIncrement());
            }
            return o;
        });
        when(orderEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderLegRepository.findByOrder_IdOrderByLegIndexAsc(any())).thenAnswer(inv ->
            legs.stream().sorted(Comparator.comparing(OrderLeg::getLegIndex)).toList()
        );
        when(orderLegRepository.save(any(OrderLeg.class))).thenAnswer(inv -> {
            OrderLeg leg = inv.getArgument(0);
            if (!legs.contains(leg)) {
                legs.add(leg);
            }
            return leg;
        });
        org.mockito.Mockito.doAnswer(inv -> {
            Iterable<OrderLeg> gone = inv.getArgument(0);
            gone.forEach(legs::remove);
            return null;
        })
            .when(orderLegRepository)
            .deleteAll(any());
    }

    @Test
    void createProvinceToProvince_setsHubFirstLegItineraryAndTwoLegs() {
        service.createConfirmed(baseReq());

        assertThat(legs).hasSize(2);
        OrderLeg first = legs.get(0);
        OrderLeg second = legs.get(1);
        assertThat(first.getFromOffice()).isSameAs(nd);
        assertThat(first.getToOffice()).isSameAs(hub);
        assertThat(second.getFromOffice()).isSameAs(hub);
        assertThat(second.getToOffice()).isSameAs(tb);
        ShipmentOrder saved = first.getOrder();
        assertThat(saved.getHubOffice()).isSameAs(hub);
        assertThat(saved.getFromOffice()).isSameAs(nd);
        assertThat(saved.getToOffice()).isSameAs(tb);
        assertThat(saved.getFinalToOffice()).isSameAs(tb);
        assertThat(saved.getItineraryLabel()).isEqualTo("Nam Định - Hà Nội");
        assertThat(saved.getRouteLabel()).isEqualTo("Tuyến NĐ");
    }

    @Test
    void createProvinceToProvince_blocksWhenSecondLegItineraryMissing() {
        when(itineraryRepository.findOneByCode("BC-TB")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createConfirmed(baseReq()))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Chưa có lộ trình");
        assertThat(legs).isEmpty();
    }

    @Test
    void createNonTransit_hasNoHubNoLegs() {
        when(transitHubPolicyService.resolveHub(nd, tb)).thenReturn(Optional.empty());

        service.createConfirmed(baseReq());

        assertThat(legs).isEmpty();
    }

    @Test
    void scanAtHub_movesOrderToSenderWarehouseOfHubForNextLeg() {
        ShipmentOrder order = transitOrder(OrderStatus.IN_TRANSIT, ForwardStage.TRANSFERRING);
        order.setNote("ghi chú\n[WHOUT]1,2[/WHOUT]\n[WHIN]1,2[/WHIN]");
        order.setCurrentTrip(new com.mycompany.myapp.domain.Trip());

        boolean handled = service.arriveAtTransitHub(order.getOrderCode(), "VP_72TNT");

        assertThat(handled).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getForwardStage()).isEqualTo(ForwardStage.WH_IN);
        assertThat(order.getCurrentTrip()).isNull();
        assertThat(order.getNote()).isEqualTo("ghi chú");
        assertThat(order.getFromOffice()).isSameAs(nd);
        assertThat(order.getToOffice()).isSameAs(tb);
        assertThat(order.getItineraryLabel()).isEqualTo("Hà Nội - Thái Bình");
        assertThat(legs.get(0).getStatus()).isEqualTo(LegStatus.AT_HUB);
        assertThat(legs.get(0).getTrip()).isNotNull();
        assertThat(service.goodsAtTransitHub(order)).isTrue();
        assertThat(service.transitLoadOfficeCode(order)).isEqualTo("VP_72TNT");
    }

    @Test
    void scanAtOtherOffice_isNotHubArrival() {
        ShipmentOrder order = transitOrder(OrderStatus.IN_TRANSIT, ForwardStage.TRANSFERRING);

        assertThat(service.arriveAtTransitHub(order.getOrderCode(), "VP_TB")).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);
        assertThat(legs.get(0).getStatus()).isEqualTo(LegStatus.PENDING);
    }

    @Test
    void warehouseInAtHub_doesNotCollectSenderFareAgain() {
        ShipmentOrder order = transitOrder(OrderStatus.IN_TRANSIT, ForwardStage.TRANSFERRING);
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setFareAmount(new BigDecimal("50000"));
        order.setPaidAmount(BigDecimal.ZERO);
        service.arriveAtTransitHub(order.getOrderCode(), "VP_72TNT");

        service.collectSenderFareOnWarehouseIn(order);

        verify(orderPaymentRepository, never()).save(any());
        assertThat(order.getPaidAmount()).isEqualByComparingTo("0");
    }

    @Test
    void stripWarehouseIn_dropsOnlyDestinationInMarks() {
        assertThat(OrderFacadeService.stripWarehouseIn("x\n[WHOUT]1[/WHOUT]\n[WHIN]1[/WHIN]")).isEqualTo("x\n[WHOUT]1[/WHOUT]");
    }

    @Test
    void returnFromHubWarehouse_goesBackHubToOrigin_andCancelRestores() {
        ShipmentOrder order = transitOrder(OrderStatus.IN_TRANSIT, ForwardStage.TRANSFERRING);
        service.arriveAtTransitHub(order.getOrderCode(), "VP_72TNT");

        String token = service.prepareTransitReturn(order, true);

        assertThat(token).isEqualTo("H1");
        OrderLeg open = openLeg();
        assertThat(open.getFromOffice()).isSameAs(hub);
        assertThat(open.getToOffice()).isSameAs(nd);
        assertThat(order.getItineraryLabel()).isEqualTo("Hà Nội - Nam Định");

        service.revertTransitReturn(order, token);
        assertThat(openLeg().getToOffice()).isSameAs(tb);
        assertThat(legs).hasSize(2);
    }

    @Test
    void returnAfterReachingReceiver_addsReceiverHubAndHubOriginLegs() {
        ShipmentOrder order = transitOrder(OrderStatus.IN_TRANSIT, ForwardStage.TRANSFERRING);
        service.arriveAtTransitHub(order.getOrderCode(), "VP_72TNT");
        legs.get(1).setStatus(LegStatus.AT_DEST);
        order.setStatus(OrderStatus.FAILED_DELIVERY);
        order.setForwardStage(ForwardStage.FAILED);

        String token = service.prepareTransitReturn(order, false);

        assertThat(token).isEqualTo("D1");
        assertThat(legs).hasSize(4);
        OrderLeg open = openLeg();
        assertThat(open.getFromOffice()).isSameAs(tb);
        assertThat(open.getToOffice()).isSameAs(hub);

        order.setStatus(OrderStatus.RETURNING);
        order.setForwardStage(ForwardStage.TRANSFERRING);
        assertThat(service.arriveAtTransitHub(order.getOrderCode(), "VP_72TNT")).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNING);
        assertThat(order.getForwardStage()).isEqualTo(ForwardStage.WH_IN);
        assertThat(openLeg().getToOffice()).isSameAs(nd);
        assertThat(service.goodsAtTransitHub(order)).isTrue();

        service.revertTransitReturn(order, "D1");
        assertThat(legs).hasSize(2);
    }

    @Test
    void returnAtOriginWarehouse_keepsLegsAndOldReturnFlow() {
        ShipmentOrder order = transitOrder(OrderStatus.CONFIRMED, ForwardStage.WH_IN);

        assertThat(service.prepareTransitReturn(order, true)).isNull();
        assertThat(legs.get(0).getStatus()).isEqualTo(LegStatus.PENDING);
        order.setStatus(OrderStatus.RETURNING);
        assertThat(service.transitLoadOfficeCode(order)).isNull();
    }

    private OrderLeg openLeg() {
        return legs
            .stream()
            .sorted(Comparator.comparing(OrderLeg::getLegIndex))
            .filter(l -> l.getStatus() == LegStatus.PENDING || l.getStatus() == LegStatus.IN_TRANSIT)
            .findFirst()
            .orElseThrow();
    }

    private ShipmentOrder transitOrder(OrderStatus status, ForwardStage stage) {
        ShipmentOrder order = new ShipmentOrder();
        order.setId(500L);
        order.setOrderCode("ND1310WXYZ");
        order.setStatus(status);
        order.setForwardStage(stage);
        order.setFromOffice(nd);
        order.setToOffice(tb);
        order.setFinalToOffice(tb);
        order.setHubOffice(hub);
        order.setQuantity(2);
        legs.add(leg(order, 0, nd, hub));
        legs.add(leg(order, 1, hub, tb));
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("ND1310WXYZ")).thenReturn(Optional.of(order));
        return order;
    }

    private static OrderLeg leg(ShipmentOrder order, int index, Office from, Office to) {
        OrderLeg leg = new OrderLeg();
        leg.setOrder(order);
        leg.setLegIndex(index);
        leg.setStatus(LegStatus.PENDING);
        leg.setFromOffice(from);
        leg.setToOffice(to);
        return leg;
    }

    private void itinerary(String code, String name, String branchName) {
        Itinerary it = new Itinerary();
        it.setCode(code);
        it.setName(name);
        it.setActive(true);
        Branch branch = new Branch();
        branch.setName(branchName);
        it.setBranch(branch);
        when(itineraryRepository.findOneByCode(eq(code))).thenReturn(Optional.of(it));
    }

    private static CreateOrderRequest baseReq() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setSenderPhone("0901234567");
        req.setSenderName("Sender");
        req.setReceiverName("Receiver");
        req.setReceiverPhone("0912345678");
        req.setGoodsType(GoodsType.THUONG);
        req.setPaymentTerm(PaymentTerm.NHAN_TRA);
        req.setFromOfficeCode("VP_ND");
        req.setToOfficeCode("VP_TB");
        req.setFareAmount(new BigDecimal("60000"));
        return req;
    }

    private static Office office(String code, long id, String point) {
        Office o = new Office();
        o.setId(id);
        o.setCode(code);
        o.setName(code);
        o.setItineraryPoint(point);
        return o;
    }
}
