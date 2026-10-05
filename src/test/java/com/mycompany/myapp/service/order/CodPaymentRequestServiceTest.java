package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CodPaymentRequestServiceTest {

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
    void html_fillsForm() {
        String html = CodPaymentRequestService.html(
            order(),
            new CodPaymentRequestService.Requester("Nguyễn Văn Kế", "Kế toán"),
            LocalDate.of(2026, 10, 5),
            "data:image/png;base64,AA=="
        );
        assertThat(html).contains("GIẤY ĐỀ NGHỊ THANH TOÁN").contains("BMTT - 01").contains("src=\"data:image/png;base64,AA==\"");
        assertThat(html).contains("Ngày 5 Tháng 10 Năm 2026");
        assertThat(html).contains("Tên tôi là: <b>Nguyễn Văn Kế</b>").contains("Chức vụ: Kế toán");
        assertThat(html).doesNotContain("Phòng ban");
        assertThat(html).contains("Thanh toán tiền thu hộ COD cho người gửi đơn BC0310IEMH");
        assertThat(html).contains(">2.440.000<");
        assertThat(html).contains("TRẦN A &amp; B - 0912345678");
        assertThat(html).contains("Hai triệu bốn trăm bốn mươi nghìn đồng");
        assertThat(html).contains("Hình thức thanh toán: CK").contains("Số tài khoản: 0393883210").contains("Mở tại: VPBANK");
        assertThat(html).contains("Trưởng Bộ phận</b><i>(Ký, ghi rõ họ tên)</i><div class=\"name\">Nguyễn Tuấn Việt");
        assertThat(html).contains("Người đề nghị</b><i>(Ký, ghi rõ họ tên)</i><div class=\"name\">Nguyễn Văn Kế");
    }

    @Test
    void html_cashWhenNoBankAccount() {
        ShipmentOrder o = order();
        o.setBankAccountNo(null);
        String html = CodPaymentRequestService.html(o, new CodPaymentRequestService.Requester("A", ""), LocalDate.of(2026, 1, 2), "");
        assertThat(html).contains("Hình thức thanh toán: TM").doesNotContain("<img");
    }

    @Test
    void moneyFormatAndWords() {
        assertThat(CodPaymentRequestService.money(new BigDecimal("2440000.00"))).isEqualTo("2.440.000");
        assertThat(CodPaymentRequestService.money(new BigDecimal("1500.50"))).isEqualTo("1.500,5");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("2440000"))).isEqualTo("Hai triệu bốn trăm bốn mươi nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("2160000"))).isEqualTo("Hai triệu một trăm sáu mươi nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("1005000"))).isEqualTo("Một triệu không trăm linh năm nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("15500000"))).isEqualTo("Mười lăm triệu năm trăm nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("321000"))).isEqualTo("Ba trăm hai mươi mốt nghìn đồng");
        assertThat(VietnameseMoneyWords.of(new BigDecimal("2000000000"))).isEqualTo("Hai tỷ đồng");
        assertThat(VietnameseMoneyWords.of(BigDecimal.ZERO)).isEqualTo("Không đồng");
    }
}
