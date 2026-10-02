package com.mycompany.myapp.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.TripRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Đẩy thay đổi đơn/chuyến xuống các tab web đang mở (SSE) thay cho polling dày.
 * Thay đổi được gom mỗi {@link #FLUSH_MS} ms; payload chỉ gồm mã đơn/mã chuyến — client tự tải lại qua API có phân quyền.
 * Lưu subscriber trong bộ nhớ: chỉ đúng khi BE chạy 1 instance.
 */
@Service
public class ServerEventService {

    private static final Logger LOG = LoggerFactory.getLogger(ServerEventService.class);

    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    private static final long FLUSH_MS = 500;
    private static final long HEARTBEAT_MS = 25_000;
    private static final int MAX_SUBSCRIBERS = 2000;
    private static final int LOAD_CHUNK = 500;

    /** @param integrationAlerts nhận cảnh báo tích hợp (lỗi Auto Call…) — người có quyền ghi màn Tích hợp */
    private record Subscriber(SseEmitter emitter, String login, String officeCode, boolean integrationAlerts) {}

    private record OrderChange(String code, Set<String> officeCodes) {}

    private record Changes(List<OrderChange> orders, List<String> trips) {}

    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final Set<Long> pendingOrderIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> pendingTripIds = ConcurrentHashMap.newKeySet();

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final TripRepository tripRepository;
    private final TransactionTemplate readTx;
    private final ObjectMapper objectMapper;

    private ScheduledExecutorService executor;

    public ServerEventService(
        ShipmentOrderRepository shipmentOrderRepository,
        TripRepository tripRepository,
        PlatformTransactionManager transactionManager,
        ObjectMapper objectMapper
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.tripRepository = tripRepository;
        this.objectMapper = objectMapper;
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
    }

    @PostConstruct
    void start() {
        // Luồng riêng: client chậm không làm trễ các @Scheduled khác (hủy nháp, token Ahamove…).
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-events");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(this::safeFlush, FLUSH_MS, FLUSH_MS, TimeUnit.MILLISECONDS);
        executor.scheduleWithFixedDelay(this::heartbeat, HEARTBEAT_MS, HEARTBEAT_MS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
        for (Subscriber s : subscribers) {
            try {
                s.emitter().complete();
            } catch (RuntimeException ignored) {
                // client đã đóng
            }
        }
        subscribers.clear();
    }

    /** @param officeCode null = nhận thay đổi của mọi văn phòng */
    public SseEmitter subscribe(String login, String officeCode, boolean integrationAlerts) {
        if (subscribers.size() >= MAX_SUBSCRIBERS) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Too many realtime connections");
        }
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        Subscriber sub = new Subscriber(emitter, login, officeCode, integrationAlerts);
        subscribers.add(sub);
        emitter.onCompletion(() -> subscribers.remove(sub));
        emitter.onTimeout(() -> {
            subscribers.remove(sub);
            emitter.complete();
        });
        emitter.onError(e -> subscribers.remove(sub));
        try {
            emitter.send(SseEmitter.event().name("ready").reconnectTime(3000).data("{}", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            subscribers.remove(sub);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public int subscriberCount() {
        return subscribers.size();
    }

    public void orderChanged(Long orderId) {
        if (orderId != null && !subscribers.isEmpty()) {
            pendingOrderIds.add(orderId);
        }
    }

    public void tripChanged(Long tripId) {
        if (tripId != null && !subscribers.isEmpty()) {
            pendingTripIds.add(tripId);
        }
    }

    /** Event "autocall-error" — chỉ gửi tới subscriber nhận cảnh báo tích hợp, trên luồng SSE (không chặn người gọi). */
    public void autoCallError(Map<String, Object> payload) {
        integrationAlert("autocall-error", payload);
    }

    /** Event "tax-lookup-error" — nguồn tra cứu MST lỗi, cùng nhóm người nhận với lỗi Auto Call. */
    public void taxLookupError(Map<String, Object> payload) {
        integrationAlert("tax-lookup-error", payload);
    }

    private void integrationAlert(String event, Map<String, Object> payload) {
        if (executor == null || subscribers.stream().noneMatch(Subscriber::integrationAlerts)) {
            return;
        }
        executor.execute(() -> {
            for (Subscriber s : subscribers) {
                if (s.integrationAlerts()) {
                    send(s, event, payload);
                }
            }
        });
    }

    private void safeFlush() {
        try {
            flush();
        } catch (RuntimeException e) {
            LOG.warn("Realtime flush failed: {}", e.getMessage());
        }
    }

    void flush() {
        if (pendingOrderIds.isEmpty() && pendingTripIds.isEmpty()) {
            return;
        }
        List<Long> orderIds = drain(pendingOrderIds);
        List<Long> tripIds = drain(pendingTripIds);
        if (subscribers.isEmpty()) {
            return;
        }
        Changes changes = readTx.execute(status -> load(orderIds, tripIds));
        if (changes == null || (changes.orders().isEmpty() && changes.trips().isEmpty())) {
            return;
        }
        for (Subscriber s : subscribers) {
            List<String> orders = changes
                .orders()
                .stream()
                .filter(o -> s.officeCode() == null || o.officeCodes().contains(s.officeCode()))
                .map(OrderChange::code)
                .toList();
            if (orders.isEmpty() && changes.trips().isEmpty()) {
                continue;
            }
            send(s, "change", Map.of("orders", orders, "trips", changes.trips()));
        }
    }

    private Changes load(List<Long> orderIds, List<Long> tripIds) {
        List<OrderChange> orders = new ArrayList<>();
        for (List<Long> chunk : chunks(orderIds)) {
            for (ShipmentOrder o : shipmentOrderRepository.findAllById(chunk)) {
                String code = o.getOrderCode() != null ? o.getOrderCode() : o.getDraftCode();
                if (code == null) {
                    continue;
                }
                Set<String> offices = new HashSet<>();
                addOffice(offices, o.getFromOffice());
                addOffice(offices, o.getToOffice());
                addOffice(offices, o.getHubOffice());
                addOffice(offices, o.getFinalToOffice());
                orders.add(new OrderChange(code, offices));
            }
        }
        List<String> trips = new ArrayList<>();
        for (List<Long> chunk : chunks(tripIds)) {
            for (Trip t : tripRepository.findAllById(chunk)) {
                if (t.getTripCode() != null) {
                    trips.add(t.getTripCode());
                }
            }
        }
        return new Changes(orders, trips);
    }

    private void heartbeat() {
        for (Subscriber s : subscribers) {
            try {
                s.emitter().send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                drop(s);
            }
        }
    }

    private void send(Subscriber s, String name, Object payload) {
        try {
            String json = objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT).writeValueAsString(payload);
            s.emitter().send(SseEmitter.event().name(name).data(json, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            drop(s);
        }
    }

    private void drop(Subscriber s) {
        subscribers.remove(s);
        try {
            s.emitter().complete();
        } catch (RuntimeException ignored) {
            // đã đóng
        }
    }

    private static void addOffice(Set<String> out, Office office) {
        if (office != null && office.getCode() != null) {
            out.add(office.getCode());
        }
    }

    private static List<Long> drain(Set<Long> pending) {
        List<Long> out = new ArrayList<>();
        for (Iterator<Long> it = pending.iterator(); it.hasNext();) {
            out.add(it.next());
            it.remove();
        }
        return out;
    }

    private static List<List<Long>> chunks(List<Long> ids) {
        List<Long> clean = ids.stream().filter(Objects::nonNull).toList();
        List<List<Long>> out = new ArrayList<>();
        for (int i = 0; i < clean.size(); i += LOAD_CHUNK) {
            out.add(clean.subList(i, Math.min(clean.size(), i + LOAD_CHUNK)));
        }
        return out;
    }
}
