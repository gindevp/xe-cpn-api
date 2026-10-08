package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.AuditLogRepository;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.repository.PartnerFeeExpenseRepository;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.repository.ReceiptWaiverRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.audit.AuditRecorder;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CandidateDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

/** Người chịu nợ phần VP gửi khi đơn gửi trả chưa có khoản thu nào. */
@ExtendWith(MockitoExtension.class)
class FinanceFacadeServiceCandidateOwnerTest {

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

    @Mock
    private PartnerFeeExpenseRepository partnerFeeExpenseRepository;

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
            staffAccessService,
            partnerFeeExpenseRepository
        );
        Office from = new Office();
        from.setCode("VP_NB");
        order = new ShipmentOrder();
        order.setId(7L);
        order.setOrderCode("NB-TEST-1");
        order.setFromOffice(from);
        order.setFareAmount(new BigDecimal("30000"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setCodAmount(BigDecimal.ZERO);
        order.setPaymentTerm(PaymentTerm.GUI_TRA);
        order.setStatus(OrderStatus.IN_TRANSIT);
        order.setForwardStage(ForwardStage.TRANSFERRING);
        when(shipmentOrderRepository.findAll(any(Specification.class))).thenReturn(List.of(order));
    }

    private static OrderEvent event(String action, String actor) {
        OrderEvent e = new OrderEvent();
        e.setAction(action);
        e.setActorUsername(actor);
        e.setEventAt(Instant.now());
        return e;
    }

    private String senderOwner() {
        List<CandidateDTO> rows = service.candidates(null, null);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).portion()).isEqualTo(ReceiptSettlement.SENDER);
        return rows.get(0).debtOwnerUsername();
    }

    @Test
    void customerOrderLoadedWithoutWarehouseIn_ownerIsFirstScanOutStaff() {
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(7L)).thenReturn(
            List.of(event("CREATE", "customer"), event("ASSIGN_TRIP", "xe00102"), event("SCAN_OUT", "xe00564"), event("SCAN_OUT", "hub01"))
        );

        assertThat(senderOwner()).isEqualTo("xe00564");
    }

    @Test
    void staffOrderWithoutPayment_ownerIsCreatorFromCreateEvent() {
        order.setForwardStage(null);
        order.setStatus(OrderStatus.AT_DEST);
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(7L)).thenReturn(List.of(event("CREATE", "vietnc")));

        assertThat(senderOwner()).isEqualTo("vietnc");
    }

    @Test
    void ahamoveAdvance_ownerIsCollectorNotAnonymousPod() {
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaidAmount(new BigDecimal("125000"));
        order.setFareAmount(new BigDecimal("125000"));
        Office to = new Office();
        to.setCode("VP_LD");
        order.setToOffice(to);
        order.setFinalToOffice(to);
        OrderPayment prepaid = new OrderPayment();
        prepaid.setAmount(new BigDecimal("55000"));
        prepaid.setPaymentKind(PaymentKind.TRUOC);
        prepaid.setNote("Thu đầu gửi");
        prepaid.setCollectorUsername("xe00102");
        prepaid.setPaymentAt(Instant.parse("2026-10-07T07:36:25Z"));
        OrderPayment advance = new OrderPayment();
        advance.setAmount(new BigDecimal("70000"));
        advance.setPaymentKind(PaymentKind.SAU);
        advance.setNote("POD AHAMOVE ỨNG");
        advance.setCollectorUsername("dungtm");
        advance.setPaymentAt(Instant.parse("2026-10-07T09:17:08Z"));
        when(orderPaymentRepository.findByOrder_IdOrderByPaymentAtDesc(7L)).thenReturn(List.of(advance, prepaid));
        when(orderPaymentRepository.sumGroupedByOrderIds(any())).thenReturn(
            List.<Object[]>of(new Object[] { 7L, PaymentKind.SAU, "POD AHAMOVE ỨNG", new BigDecimal("70000") })
        );

        List<CandidateDTO> rows = service.candidates(null, null);
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.portion()).isEqualTo(ReceiptSettlement.SENDER);
            assertThat(r.dueAmount()).isEqualByComparingTo("55000");
            assertThat(r.debtOwnerUsername()).isEqualTo("xe00102");
        });
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.portion()).isEqualTo(ReceiptSettlement.DELIVERY);
            assertThat(r.dueAmount()).isEqualByComparingTo("70000");
            assertThat(r.debtOwnerUsername()).isEqualTo("dungtm");
        });
    }

    @Test
    void pendingAhamoveAdvance_ownerIsShipStaffNotAnonymousPod() {
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaidAmount(new BigDecimal("55000"));
        order.setFareAmount(new BigDecimal("125000"));
        order.setPartnerCodAmount(new BigDecimal("70000"));
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(7L)).thenReturn(
            List.of(event("CREATE", "xe00102"), event("PUSH_SHIP", "dungtm"), event("POD", "anonymousUser"))
        );

        List<CandidateDTO> rows = service.candidates(null, null);
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.portion()).isEqualTo(ReceiptSettlement.DELIVERY);
            assertThat(r.dueAmount()).isEqualByComparingTo("70000");
            assertThat(r.debtOwnerUsername()).isEqualTo("dungtm");
        });
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.portion()).isEqualTo(ReceiptSettlement.SENDER);
            assertThat(r.dueAmount()).isEqualByComparingTo("55000");
            assertThat(r.debtOwnerUsername()).isEqualTo("xe00102");
        });
    }

    @Test
    void openPartnerFee_listedAsNegativeRowOfPayer_andFilteredByInvolvedLogin() {
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(7L)).thenReturn(List.of(event("CREATE", "vietnc")));
        Office to = new Office();
        to.setCode("VP_HN");
        ShipmentOrder ah = new ShipmentOrder();
        ah.setId(8L);
        ah.setOrderCode("AH-1");
        ah.setToOffice(to);
        ah.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        com.mycompany.myapp.domain.PartnerFeeExpense e = new com.mycompany.myapp.domain.PartnerFeeExpense();
        e.setOrder(ah);
        e.setAmount(new BigDecimal("32000"));
        e.setPayerUsername("dungtm");
        e.setIncurredAt(Instant.now());
        when(partnerFeeExpenseRepository.findOpenWithOrder()).thenReturn(List.of(e));

        List<CandidateDTO> rows = service.candidates(null, null);
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.orderCode()).isEqualTo("AH-1");
            assertThat(r.portion()).isEqualTo(FinanceFacadeService.PARTNER_FEE);
            assertThat(r.dueAmount()).isEqualByComparingTo("-32000");
            assertThat(r.debtOwnerUsername()).isEqualTo("dungtm");
        });
        assertThat(service.candidates("VP_HN", null)).anySatisfy(r -> assertThat(r.orderCode()).isEqualTo("AH-1"));
        assertThat(service.candidates("VP_XX", null)).noneSatisfy(r -> assertThat(r.orderCode()).isEqualTo("AH-1"));
        assertThat(service.candidatesInvolving("other")).noneSatisfy(r -> assertThat(r.orderCode()).isEqualTo("AH-1"));
    }
}
