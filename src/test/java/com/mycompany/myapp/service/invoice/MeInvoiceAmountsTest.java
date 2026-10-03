package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MeInvoiceAmountsTest {

    @Test
    void splitGross_vat8_example32400() {
        MeInvoiceAmounts.Split s = MeInvoiceAmounts.splitGross(new BigDecimal("32400"));
        assertThat(s.gross()).isEqualByComparingTo("32400");
        assertThat(s.net()).isEqualByComparingTo("30000");
        assertThat(s.vat()).isEqualByComparingTo("2400");
        assertThat(s.net().add(s.vat())).isEqualByComparingTo(s.gross());
    }

    @Test
    void fromOrder_sumsShippingCodFeeDeclared() {
        ShipmentOrder o = new ShipmentOrder();
        o.setGoodsFareAmount(new BigDecimal("20000"));
        o.setPickupFeeAmount(BigDecimal.ZERO);
        o.setDeliveryFeeAmount(BigDecimal.ZERO);
        o.setCodFeeAmount(new BigDecimal("8000"));
        o.setDeclaredFeeAmount(new BigDecimal("5000"));
        o.setCodAmount(new BigDecimal("500000")); // không cộng vào HĐ

        MeInvoiceAmounts.Breakdown b = MeInvoiceAmounts.fromOrder(o);
        assertThat(b.gross()).isEqualByComparingTo("33000");
        assertThat(b.net()).isEqualByComparingTo("30556");
        assertThat(b.vat()).isEqualByComparingTo("2444");
        assertThat(b.shipping()).isEqualByComparingTo("20000");
    }

    @Test
    void fromOrder_includesPickupDeliveryInShipping() {
        ShipmentOrder o = new ShipmentOrder();
        o.setGoodsFareAmount(new BigDecimal("10000"));
        o.setPickupFeeAmount(new BigDecimal("3000"));
        o.setDeliveryFeeAmount(new BigDecimal("2000"));
        o.setCodFeeAmount(BigDecimal.ZERO);
        o.setDeclaredFeeAmount(BigDecimal.ZERO);

        MeInvoiceAmounts.Breakdown b = MeInvoiceAmounts.fromOrder(o);
        assertThat(b.shipping()).isEqualByComparingTo("15000");
        assertThat(b.gross()).isEqualByComparingTo("15000");
        assertThat(b.net().add(b.vat())).isEqualByComparingTo(b.gross());
    }

    @Test
    void refId_prefixed() {
        assertThat(MeInvoiceAmounts.refIdFor("GP260911001")).isEqualTo("XE-GP260911001");
    }

    @Test
    void itemName_fromSenderReceiverAddresses() {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode("GP260922001");
        o.setPickupAddress("12 Giải Phóng, Giáp Bát, Hoàng Mai, Hà Nội");
        o.setDeliveryAddress("45 Trần Hưng Đạo, Phố Hiến, Hưng Yên");

        assertThat(MeInvoiceAmounts.itemNameFor(o)).isEqualTo(
            "Dịch vụ bưu chính chuyển phát hàng hóa từ Hà Nội đến Hưng Yên Bill: GP260922001"
        );
    }

    @Test
    void itemName_fallsBackToOfficeWhenAddressMissing() {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode("GP260922002");
        Office from = new Office().name("VP Giải Phóng").address("1 Giải Phóng, Hà Nội");
        Office to = new Office().name("VP Hưng Yên").address("2 Phố Hiến, Tỉnh Hưng Yên");
        o.setFromOffice(from);
        o.setToOffice(to);

        assertThat(MeInvoiceAmounts.itemNameFor(o)).isEqualTo(
            "Dịch vụ bưu chính chuyển phát hàng hóa từ Hà Nội đến Hưng Yên Bill: GP260922002"
        );
    }

    @Test
    void itemName_oldProvinceOfficesMappedToMergedProvince() {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode("ND021001");
        o.setFromOffice(
            new Office()
                .name("104 Song Hào - Nam Định")
                .address("Số 104, đường Song Hào, Phường Quang Trung, Thành phố Nam Định, Tỉnh Nam Định")
        );
        o.setToOffice(new Office().name("18 Vũ Trọng Khánh - Hà Nội").address("18 Vũ Trọng Khánh - HN"));
        assertThat(MeInvoiceAmounts.itemNameFor(o)).isEqualTo(
            "Dịch vụ bưu chính chuyển phát hàng hóa từ Ninh Bình đến Hà Nội Bill: ND021001"
        );

        o.setFromOffice(new Office().name("34 Trần Phú - Thái Bình").address("Số 34, Thành phố Thái Bình, Tỉnh Thái Bình"));
        o.setToOffice(new Office().name("Văn Phú - Yên Bái").address("Gần Cầu Văn Phú, Xã Văn Phú, Thành phố Yên Bái, Tỉnh Yên Bái"));
        assertThat(MeInvoiceAmounts.itemNameFor(o)).isEqualTo(
            "Dịch vụ bưu chính chuyển phát hàng hóa từ Hưng Yên đến Lào Cai Bill: ND021001"
        );
    }

    @Test
    void provinceFromAddress_stripsPrefix() {
        assertThat(MeInvoiceAmounts.provinceFromAddress("1 ABC, Phường X, Thành phố Hà Nội")).isEqualTo("Hà Nội");
        assertThat(MeInvoiceAmounts.provinceFromAddress("1 ABC, TP. Hưng Yên")).isEqualTo("Hưng Yên");
    }
}
