package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.service.finance.FinanceFacadeService.CandidateDTO;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CreateReceiptRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptLineRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceiptNightlyPlanTest {

    private static final Instant CUTOFF = Instant.parse("2026-10-12T17:00:00Z");

    @Test
    void keepsMoneyReceivedBeforeMidnightAndLeavesUnconfirmedShipDebt() {
        List<CreateReceiptRequest> reqs = ReceiptNightlyPlan.requests(
            List.of(
                row("HD1", "SENDER", "an", "An", "100", "2026-10-11T10:00:00Z", null),
                row("HD1", "DELIVERY", "an", "An", "40", "2026-10-11T12:00:00Z", true),
                row("HD2", "DELIVERY", "an", "An", "50", "2026-10-11T15:00:00Z", false),
                row("HD3", "DELIVERY", "binh", "Bình", "70", "2026-10-12T17:00:00Z", null),
                row("HD2", "PARTNER_FEE", "an", "An", "-15", "2026-10-11T15:00:00Z", true),
                row("HD4", "PARTNER_FEE", "an", "An", "-9", "2026-10-11T08:00:00Z", null)
            ),
            CUTOFF
        );

        assertThat(reqs).hasSize(1);
        CreateReceiptRequest an = reqs.get(0);
        assertThat(an.payerCode()).isEqualTo("an");
        assertThat(an.payerName()).isEqualTo("An");
        assertThat(an.lines()).extracting(ReceiptLineRequest::orderCode).containsExactly("HD1", "HD2", "HD4");
        assertThat(an.lines().get(0).portion()).isEqualTo("SENDER");
        assertThat(an.lines().get(0).amountCollected()).isEqualByComparingTo("100");
        assertThat(an.lines().get(1).portion()).isEqualTo("DELIVERY");
        assertThat(an.lines().get(1).amountCollected()).isEqualByComparingTo("50");
        assertThat(an.lines().get(2).portion()).isEqualTo("PARTNER_FEE");
        assertThat(an.lines().get(2).amountCollected()).isEqualByComparingTo("-9");
    }

    @Test
    void mergesBothPortionsOfSameOrderIntoOneLine() {
        List<CreateReceiptRequest> reqs = ReceiptNightlyPlan.requests(
            List.of(row("HD1", "SENDER", "an", null, "20", null, null), row("HD1", "DELIVERY", "an", null, "30", null, null)),
            CUTOFF
        );
        assertThat(reqs).hasSize(1);
        ReceiptLineRequest line = reqs.get(0).lines().get(0);
        assertThat(line.portion()).isNull();
        assertThat(line.amountCollected()).isEqualByComparingTo("50");
        assertThat(reqs.get(0).payerName()).isEqualTo("an");
    }

    private static CandidateDTO row(String code, String portion, String owner, String name, String amount, String at, Boolean substitute) {
        return new CandidateDTO(
            code,
            "Nhận",
            "090",
            new BigDecimal("100"),
            BigDecimal.ZERO,
            new BigDecimal(amount),
            "DELIVERED",
            "VP",
            owner,
            name,
            portion,
            at == null ? null : Instant.parse(at),
            substitute
        );
    }
}
