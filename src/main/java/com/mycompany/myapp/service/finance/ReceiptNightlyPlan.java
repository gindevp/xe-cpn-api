package com.mycompany.myapp.service.finance;

import com.mycompany.myapp.service.finance.FinanceFacadeService.CandidateDTO;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CreateReceiptRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptLineRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gom phiếu thu tự động sau nửa đêm: mọi khoản đã nhận tiền trước mốc, trừ khoản người giao
 * (người nhận nợ ship) chưa xác nhận bù — những khoản đó để lại trên màn Phiếu thu.
 */
final class ReceiptNightlyPlan {

    private ReceiptNightlyPlan() {}

    static List<CreateReceiptRequest> requests(List<CandidateDTO> rows, Instant cutoff) {
        Map<String, Owner> owners = new LinkedHashMap<>();
        for (CandidateDTO row : rows == null ? List.<CandidateDTO>of() : rows) {
            if (row == null || Boolean.TRUE.equals(row.shipDebtSubstitute())) {
                continue;
            }
            if (row.collectedAt() != null && cutoff != null && !row.collectedAt().isBefore(cutoff)) {
                continue;
            }
            String owner = row.debtOwnerUsername() == null ? "" : row.debtOwnerUsername().trim();
            if (owner.isEmpty() || row.orderCode() == null || row.orderCode().isBlank()) {
                continue;
            }
            BigDecimal amount = row.dueAmount() == null ? BigDecimal.ZERO : row.dueAmount();
            if (amount.signum() == 0) {
                continue;
            }
            String key = owner.toUpperCase(Locale.ROOT);
            Owner acc = owners.computeIfAbsent(key, k -> new Owner(owner, blankToNull(row.debtOwnerName())));
            String code = row.orderCode().trim();
            if (FinanceFacadeService.PARTNER_FEE.equalsIgnoreCase(row.portion())) {
                acc.fees.merge(code, amount, BigDecimal::add);
            } else if (ReceiptSettlement.SENDER.equals(row.portion())) {
                acc.sender.merge(code, amount, BigDecimal::add);
            } else if (ReceiptSettlement.DELIVERY.equals(row.portion())) {
                acc.delivery.merge(code, amount, BigDecimal::add);
            } else {
                acc.sender.merge(code, amount, BigDecimal::add);
            }
        }
        List<CreateReceiptRequest> out = new ArrayList<>();
        for (Owner acc : owners.values()) {
            List<ReceiptLineRequest> lines = new ArrayList<>();
            for (String code : acc.orderCodes()) {
                BigDecimal sender = acc.sender.getOrDefault(code, BigDecimal.ZERO);
                BigDecimal delivery = acc.delivery.getOrDefault(code, BigDecimal.ZERO);
                if (sender.signum() > 0 && delivery.signum() > 0) {
                    lines.add(new ReceiptLineRequest(code, sender.add(delivery), null));
                } else if (sender.signum() > 0) {
                    lines.add(new ReceiptLineRequest(code, sender, ReceiptSettlement.SENDER));
                } else if (delivery.signum() > 0) {
                    lines.add(new ReceiptLineRequest(code, delivery, ReceiptSettlement.DELIVERY));
                }
                BigDecimal fee = acc.fees.getOrDefault(code, BigDecimal.ZERO);
                if (fee.signum() != 0) {
                    lines.add(new ReceiptLineRequest(code, fee, FinanceFacadeService.PARTNER_FEE));
                }
            }
            if (!lines.isEmpty()) {
                out.add(new CreateReceiptRequest(acc.name == null ? acc.login : acc.name, acc.login, null, lines));
            }
        }
        return out;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static final class Owner {

        final String login;
        final String name;
        final Map<String, BigDecimal> sender = new LinkedHashMap<>();
        final Map<String, BigDecimal> delivery = new LinkedHashMap<>();
        final Map<String, BigDecimal> fees = new LinkedHashMap<>();

        Owner(String login, String name) {
            this.login = login;
            this.name = name;
        }

        List<String> orderCodes() {
            Map<String, Boolean> codes = new LinkedHashMap<>();
            sender.keySet().forEach(c -> codes.put(c, true));
            delivery.keySet().forEach(c -> codes.put(c, true));
            fees.keySet().forEach(c -> codes.put(c, true));
            return new ArrayList<>(codes.keySet());
        }
    }
}
