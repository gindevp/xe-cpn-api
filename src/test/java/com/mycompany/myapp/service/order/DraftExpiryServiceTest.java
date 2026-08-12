package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DraftExpiryServiceTest {

    @Mock
    private ShipmentOrderRepository shipmentOrderRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    private DraftExpiryService service;

    @BeforeEach
    void setUp() {
        service = new DraftExpiryService(shipmentOrderRepository, orderEventRepository);
    }

    @Test
    void isDraftExpired_trueWhenOlderThan24h() {
        ShipmentOrder order = new ShipmentOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.DRAFT);
        OrderEvent ev = new OrderEvent();
        ev.setEventAt(Instant.now().minus(25, ChronoUnit.HOURS));
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(1L)).thenReturn(List.of(ev));

        assertThat(service.isDraftExpired(order)).isTrue();
    }

    @Test
    void isDraftExpired_falseWhenFresh() {
        ShipmentOrder order = new ShipmentOrder();
        order.setId(2L);
        order.setStatus(OrderStatus.DRAFT);
        OrderEvent ev = new OrderEvent();
        ev.setEventAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(2L)).thenReturn(List.of(ev));

        assertThat(service.isDraftExpired(order)).isFalse();
    }

    @Test
    void expireDueDrafts_cancelsOld() {
        ShipmentOrder old = new ShipmentOrder();
        old.setId(3L);
        old.setStatus(OrderStatus.DRAFT);
        OrderEvent ev = new OrderEvent();
        ev.setEventAt(Instant.now().minus(30, ChronoUnit.HOURS));
        when(shipmentOrderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(old));
        when(orderEventRepository.findByOrder_IdOrderByEventAtAsc(3L)).thenReturn(List.of(ev));
        when(shipmentOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(orderEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        int n = service.expireDueDrafts();

        assertThat(n).isEqualTo(1);
        assertThat(old.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(old.getCancelReason()).contains("24h");
    }
}
