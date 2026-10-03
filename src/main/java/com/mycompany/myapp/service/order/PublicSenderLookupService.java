package com.mycompany.myapp.service.order;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Trang khách tự tạo đơn: khách nhập SĐT của mình → điền lại tên người gửi từ đơn gần nhất.
 * Chỉ trả tên người gửi (không địa chỉ, không người nhận) và giới hạn theo IP để khó dò tên chủ số.
 */
@Service
public class PublicSenderLookupService {

    static final int MAX_PER_IP = 40;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public PublicSenderLookupService(ShipmentOrderRepository shipmentOrderRepository) {
        this.shipmentOrderRepository = shipmentOrderRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> senderName(String rawPhone, String ip) {
        String phone = normalizePhone(rawPhone);
        if (phone == null) {
            return Map.of("found", false);
        }
        throttle("ip:" + (ip == null ? "" : ip));
        List<String> names = shipmentOrderRepository.findLatestSenderNames(phone, PageRequest.of(0, 1));
        if (names.isEmpty()) {
            return Map.of("found", false);
        }
        return Map.of("found", true, "name", names.get(0).trim());
    }

    /** {@code 0xxxxxxxxx} hoặc null; nhận cả +84 / 84 / có dấu cách, chấm. */
    static String normalizePhone(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("84") && d.length() == 11) d = "0" + d.substring(2);
        return d.matches("0\\d{9}") ? d : null;
    }

    private void throttle(String key) {
        if (hits.size() > 50_000) hits.clear();
        Deque<Instant> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            Instant cutoff = Instant.now().minus(WINDOW);
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            if (q.size() >= MAX_PER_IP) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Thao tác quá nhiều lần — vui lòng thử lại sau");
            }
            q.addLast(Instant.now());
        }
    }
}
