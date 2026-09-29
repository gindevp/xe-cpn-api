package com.mycompany.myapp.service.realtime;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.OrderLeg;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.OrderPodPhoto;
import com.mycompany.myapp.domain.OrderReturnRequest;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.TripOrderAssignment;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Component;

/**
 * Ghi nhận đơn/chuyến thay đổi SAU KHI commit thành công (rollback không phát sự kiện).
 * Bắt ở tầng Hibernate để mọi luồng ghi (facade, CRUD sinh sẵn, scheduler) đều được phủ.
 * Chỉ lấy id — không chạm lazy association sau commit.
 */
@Component
public class RealtimeChangeListener implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

    private final EntityManagerFactory entityManagerFactory;
    private final ServerEventService serverEventService;

    public RealtimeChangeListener(EntityManagerFactory entityManagerFactory, ServerEventService serverEventService) {
        this.entityManagerFactory = entityManagerFactory;
        this.serverEventService = serverEventService;
    }

    @PostConstruct
    void register() {
        EventListenerRegistry registry = entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getServiceRegistry()
            .getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_COMMIT_INSERT, this);
        registry.appendListeners(EventType.POST_COMMIT_UPDATE, this);
        registry.appendListeners(EventType.POST_COMMIT_DELETE, this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        route(event.getEntity());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        route(event.getEntity());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        route(event.getEntity());
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {}

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {}

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {}

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return true;
    }

    private void route(Object entity) {
        try {
            if (entity instanceof ShipmentOrder o) {
                serverEventService.orderChanged(o.getId());
            } else if (entity instanceof Trip t) {
                serverEventService.tripChanged(t.getId());
            } else if (entity instanceof OrderLeg l) {
                serverEventService.orderChanged(idOf(l.getOrder()));
                serverEventService.tripChanged(idOf(l.getTrip()));
            } else if (entity instanceof TripOrderAssignment a) {
                serverEventService.orderChanged(idOf(a.getOrder()));
                serverEventService.tripChanged(idOf(a.getTrip()));
            } else if (entity instanceof OrderEvent e) {
                serverEventService.orderChanged(idOf(e.getOrder()));
            } else if (entity instanceof OrderPayment p) {
                serverEventService.orderChanged(idOf(p.getOrder()));
            } else if (entity instanceof OrderIssue i) {
                serverEventService.orderChanged(idOf(i.getOrder()));
            } else if (entity instanceof OrderReturnRequest r) {
                serverEventService.orderChanged(idOf(r.getOrder()));
            } else if (entity instanceof OrderPodPhoto ph) {
                serverEventService.orderChanged(idOf(ph.getOrder()));
            }
        } catch (RuntimeException ignored) {
            // Realtime là phụ: không bao giờ làm hỏng luồng ghi đã commit.
        }
    }

    private static Long idOf(Object entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof HibernateProxy proxy) {
            Object id = proxy.getHibernateLazyInitializer().getInternalIdentifier();
            return id instanceof Long l ? l : null;
        }
        if (entity instanceof ShipmentOrder o) {
            return o.getId();
        }
        if (entity instanceof Trip t) {
            return t.getId();
        }
        return null;
    }
}
