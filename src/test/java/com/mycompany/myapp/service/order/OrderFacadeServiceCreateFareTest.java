package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.CreateOrderRequest;
import com.mycompany.myapp.service.dto.order.OrderSummaryDTO;
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
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tạo đơn NV: cước FE (tổng kiện) là SoT — không bị bảng giá master theo tổng cân ghi đè.
 */
@ExtendWith(MockitoExtension.class)
class OrderFacadeServiceCreateFareTest {

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
    private Office gp;
    private Office nb;

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

        gp = office("GP", 1L);
        nb = office("NB", 2L);
        when(officeRepository.findOneByCode("GP")).thenReturn(Optional.of(gp));
        when(officeRepository.findOneByCode("NB")).thenReturn(Optional.of(nb));
        when(orderCodeGenerator.nextOrderCode(eq("GP"), anyBoolean())).thenReturn("GP170926A1B2C");
        AtomicLong idSeq = new AtomicLong(100);
        when(shipmentOrderRepository.save(any(ShipmentOrder.class))).thenAnswer(inv -> {
            ShipmentOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(idSeq.getAndIncrement());
            }
            return o;
        });
        when(orderLegRepository.findByOrder_IdOrderByLegIndexAsc(any())).thenReturn(List.of());
        when(orderEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createConfirmed_prefersFeFareWhenPricingRuleMatches() {
        // BE ước lượng theo tổng cân (1 lần) — thấp hơn tổng 2 kiện FE.
        when(fareCalculator.estimate(any(), eq(false), eq(false), eq(gp), eq(nb), isNull())).thenReturn(
            new SimpleFareCalculator.FareBreakdown(
                new BigDecimal("12000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("15000"),
                new BigDecimal("3000"),
                99L
            )
        );

        CreateOrderRequest req = baseReq();
        req.setWeightKg(new BigDecimal("4"));
        req.setQuantity(2);
        // FE: 2 kiện × (12000+3000) + COD fee
        req.setGoodsFareAmount(new BigDecimal("30000"));
        req.setCodFeeAmount(new BigDecimal("5000"));
        req.setFareAmount(new BigDecimal("35000"));

        OrderSummaryDTO dto = service.createConfirmed(req);

        assertThat(dto.getFareAmount()).isEqualByComparingTo("35000");
        assertThat(dto.getGoodsFareAmount()).isEqualByComparingTo("30000");

        ArgumentCaptor<ShipmentOrder> captor = ArgumentCaptor.forClass(ShipmentOrder.class);
        verify(shipmentOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getFareAmount()).isEqualByComparingTo("35000");
        assertThat(captor.getValue().getGoodsFareAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void createConfirmed_withoutFeFare_usesMasterPlusServiceFees() {
        when(fareCalculator.estimate(any(), eq(false), eq(false), eq(gp), eq(nb), isNull())).thenReturn(
            new SimpleFareCalculator.FareBreakdown(
                new BigDecimal("12000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("15000"),
                new BigDecimal("3000"),
                99L
            )
        );

        CreateOrderRequest req = baseReq();
        req.setWeightKg(new BigDecimal("4"));
        req.setCodFeeAmount(new BigDecimal("5000"));
        // no fareAmount / goodsFareAmount

        OrderSummaryDTO dto = service.createConfirmed(req);

        assertThat(dto.getFareAmount()).isEqualByComparingTo("20000"); // 15000 + 5000
        assertThat(dto.getGoodsFareAmount()).isEqualByComparingTo("15000");
    }

    private static CreateOrderRequest baseReq() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setSenderPhone("0901234567");
        req.setSenderName("Sender");
        req.setReceiverName("Receiver");
        req.setReceiverPhone("0912345678");
        req.setGoodsType(GoodsType.THUONG);
        req.setPaymentTerm(PaymentTerm.GUI_TRA);
        req.setFromOfficeCode("GP");
        req.setToOfficeCode("NB");
        return req;
    }

    private static Office office(String code, long id) {
        Office o = new Office();
        o.setId(id);
        o.setCode(code);
        return o;
    }
}
