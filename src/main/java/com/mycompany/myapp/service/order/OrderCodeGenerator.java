package com.mycompany.myapp.service.order;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.time.Year;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Business code generators aligned with FE {@code pricing.ts}.
 */
@Component
public class OrderCodeGenerator {

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

    /** Format: {@code XE{yy}{office}{6 digits}} e.g. XE26GP000001 */
    public String nextOrderCode(String officeCode) {
        String office = normalizeOffice(officeCode);
        String yy = String.valueOf(Year.now().getValue()).substring(2);
        String prefix = "XE" + yy + office;
        int next = shipmentOrderRepository
            .findMaxOrderCodeByPrefix(prefix)
            .map(max -> {
                try {
                    return Integer.parseInt(max.substring(prefix.length())) + 1;
                } catch (NumberFormatException ex) {
                    return 1;
                }
            })
            .orElse(1);
        return prefix + String.format("%06d", next);
    }

    private static String normalizeOffice(String officeCode) {
        if (officeCode == null || officeCode.isBlank()) {
            return "XX";
        }
        return officeCode.trim().toUpperCase();
    }
}
