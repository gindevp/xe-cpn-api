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
     * Format: {@code {office}{DDMMYY}{5 digits}} e.g. TDN05092600001
     * (tên VP + ngày tạo + mã định danh).
     */
    public String nextOrderCode(String officeCode) {
        String office = normalizeOffice(officeCode);
        String day = LocalDate.now().format(DDMMYY);
        String prefix = office + day;
        int next = shipmentOrderRepository
            .findMaxOrderCodeByPrefix(prefix)
            .map(max -> {
                try {
                    String suffix = max.substring(prefix.length());
                    return Integer.parseInt(suffix) + 1;
                } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                    return 1;
                }
            })
            .orElse(1);
        return prefix + String.format("%05d", next);
    }

    private static String normalizeOffice(String officeCode) {
        if (officeCode == null || officeCode.isBlank()) {
            return "XX";
        }
        return officeCode.trim().toUpperCase();
    }
}
