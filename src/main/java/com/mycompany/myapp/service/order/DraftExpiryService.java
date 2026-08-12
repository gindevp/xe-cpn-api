package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FE expireDrafts: DRAFT older than 24h → CANCELLED.
 */
@Service
public class DraftExpiryService {

    private static final Logger LOG = LoggerFactory.getLogger(DraftExpiryService.class);
    private static final Duration DRAFT_TTL = Duration.ofHours(24);

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderEventRepository orderEventRepository;

    public DraftExpiryService(ShipmentOrderRepository shipmentOrderRepository, OrderEventRepository orderEventRepository) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    public boolean isDraftExpired(ShipmentOrder order) {
        if (order == null || order.getStatus() != OrderStatus.DRAFT) {
            return false;
        }
        Instant created = draftCreatedAt(order);
        return created != null && created.isBefore(Instant.now().minus(DRAFT_TTL));
    }

    public Instant draftCreatedAt(ShipmentOrder order) {
        if (order.getId() == null) {
            return null;
        }
        List<OrderEvent> events = orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId());
        if (events.isEmpty()) {
            return null;
        }
        return events.get(0).getEventAt();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void cancelExpiredDraft(ShipmentOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        ShipmentOrder managed = shipmentOrderRepository.findById(order.getId()).orElse(null);
        if (managed == null || managed.getStatus() != OrderStatus.DRAFT) {
            return;
        }
        managed.setStatus(OrderStatus.CANCELLED);
        managed.setCancelReason("Hết hạn 24h (hệ thống)");
        shipmentOrderRepository.save(managed);
        OrderEvent event = new OrderEvent();
        event.setEventAt(Instant.now());
        event.setAction("AUTO_CANCEL");
        event.setDetail("DRAFT >24h");
        event.setActorUsername("system");
        event.setOrder(managed);
        orderEventRepository.save(event);
    }

    @Transactional
    public int expireDueDrafts() {
        List<ShipmentOrder> drafts = shipmentOrderRepository.findAll((root, q, cb) -> cb.equal(root.get("status"), OrderStatus.DRAFT));
        int n = 0;
        Instant cutoff = Instant.now().minus(DRAFT_TTL);
        for (ShipmentOrder order : drafts) {
            Instant created = draftCreatedAt(order);
            if (created != null && created.isBefore(cutoff)) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelReason("Hết hạn 24h (hệ thống)");
                shipmentOrderRepository.save(order);
                OrderEvent event = new OrderEvent();
                event.setEventAt(Instant.now());
                event.setAction("AUTO_CANCEL");
                event.setDetail("DRAFT >24h");
                event.setActorUsername("system");
                event.setOrder(order);
                orderEventRepository.save(event);
                n++;
            }
        }
        if (n > 0) {
            LOG.info("Expired {} DRAFT orders older than 24h", n);
        }
        return n;
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void scheduledExpire() {
        expireDueDrafts();
    }
}
