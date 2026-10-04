package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderGoodsPhoto;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.OrderGoodsPhotoRepository;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.CreateDraftOrderRequest;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Ảnh đơn hàng khách gửi khi tạo đơn công khai (tối đa 1 ảnh, không bắt buộc). */
@ExtendWith(MockitoExtension.class)
class OrderFacadeServiceGoodsPhotoTest {

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
    private OrderGoodsPhotoRepository goodsPhotoRepository;

    private OrderFacadeService service;

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
        service.setGoodsPhotoRepository(goodsPhotoRepository);

        lenient().when(officeRepository.findOneByCode("GP")).thenReturn(Optional.of(office("GP", 1L)));
        lenient().when(officeRepository.findOneByCode("NB")).thenReturn(Optional.of(office("NB", 2L)));
        lenient().when(orderCodeGenerator.nextOrderCode(eq("GP"), anyBoolean())).thenReturn("GP041026ABCDE");
        lenient()
            .when(fareCalculator.estimate(any(), anyBoolean(), anyBoolean(), any(), any(), any(), any(), any()))
            .thenReturn(new SimpleFareCalculator.FareBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30000")));
        AtomicLong idSeq = new AtomicLong(100);
        lenient()
            .when(shipmentOrderRepository.save(any(ShipmentOrder.class)))
            .thenAnswer(inv -> {
                ShipmentOrder o = inv.getArgument(0);
                if (o.getId() == null) {
                    o.setId(idSeq.getAndIncrement());
                }
                return o;
            });
        lenient().when(orderLegRepository.findByOrder_IdOrderByLegIndexAsc(any())).thenReturn(List.of());
        lenient().when(orderEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createDraft_withPhoto_savesPhotoAndLogsHistory() {
        CreateDraftOrderRequest req = baseReq();
        req.setGoodsPhoto("  data:image/jpeg;base64,AAAA  ");

        service.createDraft(req);

        ArgumentCaptor<OrderGoodsPhoto> photo = ArgumentCaptor.forClass(OrderGoodsPhoto.class);
        verify(goodsPhotoRepository).save(photo.capture());
        assertThat(photo.getValue().getOrderId()).isEqualTo(100L);
        assertThat(photo.getValue().getPhotoUrl()).isEqualTo("data:image/jpeg;base64,AAAA");
        assertThat(photo.getValue().getCapturedByUsername()).isEqualTo("customer");

        ArgumentCaptor<OrderEvent> events = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository, Mockito.atLeast(2)).save(events.capture());
        assertThat(events.getAllValues()).anySatisfy(e -> {
            assertThat(e.getAction()).isEqualTo("GOODS_PHOTO");
            assertThat(e.getDetail()).isEqualTo("Khách gửi ảnh đơn hàng");
        });
    }

    @Test
    void createDraft_withoutPhoto_unchanged() {
        service.createDraft(baseReq());

        verify(goodsPhotoRepository, never()).save(any());
        ArgumentCaptor<OrderEvent> events = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository).save(events.capture());
        assertThat(events.getValue().getAction()).isEqualTo("CREATE");
    }

    @Test
    void createDraft_rejectsNonImage_beforeCreatingOrder() {
        CreateDraftOrderRequest req = baseReq();
        req.setGoodsPhoto("data:text/html;base64,PHNjcmlwdD4=");

        assertThatThrownBy(() -> service.createDraft(req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("goodsPhotoInvalid");
        verify(shipmentOrderRepository, never()).save(any());
    }

    @Test
    void createDraft_rejectsTooLargePhoto() {
        CreateDraftOrderRequest req = baseReq();
        req.setGoodsPhoto("data:image/png;base64," + "A".repeat(OrderFacadeService.MAX_GOODS_PHOTO_LENGTH));

        assertThatThrownBy(() -> service.createDraft(req))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("goodsPhotoTooLarge");
        verify(shipmentOrderRepository, never()).save(any());
    }

    @Test
    void goodsPhoto_returnsStoredImage() {
        ShipmentOrder order = new ShipmentOrder();
        order.setId(7L);
        order.setOrderCode("GP1");
        when(shipmentOrderRepository.findOneByOrderCodeOrDraftCode("GP1")).thenReturn(Optional.of(order));
        OrderGoodsPhoto p = new OrderGoodsPhoto();
        p.setPhotoUrl("data:image/jpeg;base64,BBBB");
        when(goodsPhotoRepository.findOneByOrderId(7L)).thenReturn(Optional.of(p));

        assertThat(service.goodsPhoto("GP1")).isEqualTo("data:image/jpeg;base64,BBBB");
    }

    private static CreateDraftOrderRequest baseReq() {
        CreateDraftOrderRequest req = new CreateDraftOrderRequest();
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
