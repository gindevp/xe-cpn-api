package com.mycompany.myapp.service.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.service.finance.FinanceFacadeService.CandidateDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StaffDepositServiceTest {

    private static final Instant AT = Instant.parse("2026-10-02T18:30:00Z"); // 03/10/2026 01:30 giờ VN

    @Test
    void renderContentReplacesVariablesAndStripsAccents() {
        String s = StaffDepositService.renderContent(
            "NOP {MA_NV} {MA_PHIEU} {MA_VP} {NGAY}",
            "anhnh",
            "Nguyễn Hoàng Anh",
            "PTVP_ND251003-001",
            "VP_ND",
            AT
        );
        assertThat(s).isEqualTo("NOP ANHNH PTVPND251003001 VPND 031026");
        assertThat(StaffDepositService.renderContent("{TEN_NV} nộp", "x", "Đỗ Hoàng Anh", "PT", "VP", AT)).isEqualTo("DO HOANG ANH NOP");
    }

    @Test
    void renderContentUsesDefaultTemplateAndCapsLength() {
        assertThat(StaffDepositService.renderContent(null, "anhnh", "A", "PT1", "VP", AT)).isEqualTo("ANHNH NOP PT1");
        String longName = "Đặng Thị Phương Thảo Nguyễn Văn Bình An Khang Thịnh Vượng";
        assertThat(StaffDepositService.renderContent("{TEN_NV} {TEN_NV}", "x", longName, "PT", "VP", AT)).hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void vietQrUrlEncodesAmountContentAndName() {
        String url = StaffDepositService.vietQrUrl(
            "970407",
            "19036912310019",
            "DLLXEVIETNAM",
            new BigDecimal("145000.00"),
            "ANHNH NOP PT1"
        );
        assertThat(url).isEqualTo(
            "https://img.vietqr.io/image/970407-19036912310019-qr_only.png?amount=145000&addInfo=ANHNH%20NOP%20PT1&accountName=DLLXEVIETNAM"
        );
    }

    @Test
    void mergeOwnKeepsOnlyMineAndMergesBothPortions() {
        Instant early = Instant.parse("2026-10-01T03:00:00Z");
        Instant late = Instant.parse("2026-10-02T03:00:00Z");
        List<CandidateDTO> all = List.of(
            cand("A1", "30000", "SENDER", "AnhNH", late),
            cand("A1", "20000", "DELIVERY", "anhnh", early),
            cand("B2", "50000", "DELIVERY", "khac", late),
            cand("C3", "0", "SENDER", "anhnh", late),
            cand("D4", "15000", "DELIVERY", "anhnh", late)
        );
        Map<String, StaffDepositService.Merged> m = StaffDepositService.mergeOwn(all, "anhnh");
        assertThat(m).containsOnlyKeys("A1", "D4");
        assertThat(m.get("A1").amount()).isEqualByComparingTo("50000");
        assertThat(m.get("A1").portion()).isNull();
        assertThat(m.get("A1").collectedAt()).isEqualTo(early);
        assertThat(m.get("D4").portion()).isEqualTo("DELIVERY");
    }

    @Test
    void receiptDateIsLatestCustomerPaymentElseCreatedAt() {
        Instant created = Instant.parse("2026-10-04T05:00:00Z");
        Instant paid1 = Instant.parse("2026-10-02T09:00:00Z");
        Instant paid2 = Instant.parse("2026-10-03T16:30:00Z");
        assertThat(StaffDepositService.receiptDate(Set.of(1L, 2L, 3L), Map.of(1L, paid1, 2L, paid2), created)).isEqualTo(paid2);
        assertThat(StaffDepositService.receiptDate(Set.of(3L), Map.of(1L, paid1), created)).isEqualTo(created);
        assertThat(StaffDepositService.renderContent("{MA_NV} {NGAY}", "nv", "A", "PT", "VP", paid2)).isEqualTo("NV 031026");
    }

    private static CandidateDTO cand(String code, String due, String portion, String owner, Instant at) {
        return new CandidateDTO(code, null, null, null, null, new BigDecimal(due), "DELIVERED", null, owner, null, portion, at);
    }
}
