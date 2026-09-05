package com.mycompany.myapp.service.order;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Business code generators aligned with FE {@code pricing.ts}.
 */
@Component
public class OrderCodeGenerator {

    private static final DateTimeFormatter DDMMYY = DateTimeFormatter.ofPattern("ddMMyy");
    /** 0-9 A-Z — 36^5 ≈ 60M tổ hợp / VP / ngày. */
    private static final char[] ID_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int ID_LEN = 5;

    private final ShipmentOrderRepository shipmentOrderRepository;

    public OrderCodeGenerator(ShipmentOrderRepository shipmentOrderRepository) {
        this.shipmentOrderRepository = shipmentOrderRepository;
    }

    /** Format: {@code N-{office}-{4 digits}} e.g. N-GP-1234 */
    public String nextDraftCode(String officeCode) {
        String office = normalizeOffice(officeCode);
        for (int i = 0; i < 20; i++) {
            int n = ThreadLocalRandom.current().nextInt(1000, 10000);
            String code = "N-" + office + "-" + n;
            if (!shipmentOrderRepository.existsByDraftCode(code)) {
                return code;
            }
        }
        return "N-" + office + "-" + (System.currentTimeMillis() % 10000);
    }

    /**
     * Format: {@code {office}{DDMMYY}{5 A-Z0-9}} e.g. TDN050926A3K9M
     * (mã VP không có tiền tố VP_ + ngày tạo + định danh chữ–số).
     */
    public String nextOrderCode(String officeCode) {
        String office = normalizeOffice(officeCode);
        String day = LocalDate.now().format(DDMMYY);
        String prefix = office + day;
        for (int i = 0; i < 40; i++) {
            String code = prefix + randomId(ID_LEN);
            if (!shipmentOrderRepository.existsByOrderCode(code)) {
                return code;
            }
        }
        return prefix + randomId(3) + String.format("%02d", ThreadLocalRandom.current().nextInt(100));
    }

    private static String randomId(int len) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = ID_CHARS[rnd.nextInt(ID_CHARS.length)];
        }
        return new String(buf);
    }

    /** Bỏ tiền tố VP / VP_ / VP- để mã ngắn (TDN thay vì VP_TDN). */
    private static String normalizeOffice(String officeCode) {
        if (officeCode == null || officeCode.isBlank()) {
            return "XX";
        }
        String o = officeCode.trim().toUpperCase().replaceAll("^VP[_\\s.-]*", "").trim();
        return o.isBlank() ? "XX" : o;
    }
}
