package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class CodPaymentRequestServiceTest {

    private static Map<String, String> unzip(byte[] xlsx) throws Exception {
        Map<String, String> out = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(xlsx), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                out.put(e.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return out;
    }

    private static ShipmentOrder order() {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode("BC0310IEMH");
        o.setStatus(OrderStatus.DELIVERED);
        o.setCodAmount(new BigDecimal("2440000.00"));
        o.setSenderName("TRẦN A & B");
        o.setSenderPhone("0912345678");
        o.setBankName("VPBANK");
        o.setBankAccountNo("0393883210");
        o.setBankAccountName("TRAN VAN A");
        return o;
    }

    @Test
    void fillsTemplateCells_keepsOtherParts() throws Exception {
        byte[] xlsx = CodPaymentRequestService.fill(
            order(),
            new CodPaymentRequestService.Requester("Nguyễn Văn Kế", "Kế toán"),
            LocalDate.of(2026, 10, 5)
        );
        Map<String, String> parts = unzip(xlsx);
        assertThat(parts).containsKeys("xl/workbook.xml", "xl/styles.xml", "xl/media/image1.png", "xl/drawings/drawing1.xml");
        assertThat(parts.get("xl/workbook.xml")).contains("ĐNTT").doesNotContain("ĐNTU").doesNotContain("hidden");

        String sheet = parts.get("xl/worksheets/sheet1.xml");
        assertThat(sheet).contains(
            "<c r=\"B6\" s=\"11\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Ngày 5 Tháng 10 Năm 2026</t></is></c>"
        );
        assertThat(sheet).contains("Nguyễn Văn Kế").contains(">Kế toán<");
        assertThat(sheet).contains("<row r=\"9\" ht=\"30.75\" hidden=\"1\"").doesNotContain("<c r=\"B9\"");
        assertThat(sheet).contains("<c r=\"E32\" s=\"34\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Nguyễn Tuấn Việt</t></is></c>");
        assertThat(sheet).contains("<mergeCell ref=\"E32:G32\"/>");
        assertThat(sheet).contains("<c r=\"H14\" s=\"59\"><v>2440000.00</v></c>");
        assertThat(sheet).contains("<c r=\"H20\" s=\"61\"><f>SUM(H14:H19)</f><v>2440000.00</v></c>");
        assertThat(sheet).contains("Hai triệu bốn trăm bốn mươi nghìn đồng");
        assertThat(sheet).contains("TRẦN A &amp; B - 0912345678");
        assertThat(sheet).contains("Hình thức thanh toán: CK").contains(">0393883210<").contains(">VPBANK<");
        assertThat(sheet).doesNotContain("<f>&quot;Lý do");
    }

    @Test
    void moneyWords() {
        assertThat(VietnameseMoneyWords.of(new BigDecimal("2440000"))).isEqualTo("Hai triệu bốn trăm bốn mươi nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("2160000"))).isEqualTo("Hai triệu một trăm sáu mươi nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("1005000"))).isEqualTo("Một triệu không trăm linh năm nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("15500000"))).isEqualTo("Mười lăm triệu năm trăm nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("321000"))).isEqualTo("Ba trăm hai mươi mốt nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("2000000000"))).isEqualTo("Hai tỷ đồng");
        assertThat(VietnameseMoneyWords.of(BigDecimal.ZERO)).isEqualTo("Không đồng");
    }

    @Test
    void missingCell_fails() {
        assertThatThrownBy(() -> CodPaymentRequestService.applyCells("<sheetData/>", Map.of("Z99", CodPaymentRequestService.Cell.text("x")))
        ).isInstanceOf(IllegalStateException.class);
    }
}
