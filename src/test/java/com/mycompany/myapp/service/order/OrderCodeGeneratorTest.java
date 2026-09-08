package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Mã đơn: {VP}{ddMMyy}{STT} — STT đếm riêng từng VP, reset mỗi ngày, quá 1000 phải xác nhận. */
@ExtendWith(MockitoExtension.class)
class OrderCodeGeneratorTest {

    private static final String YB = "YB1";
    private static final String ND = "ND";

    @Mock
    private ShipmentOrderRepository shipmentOrderRepository;

    private OrderCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OrderCodeGenerator(shipmentOrderRepository);
    }

    private static String prefix(String office) {
        String day = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("ddMMyy"));
        return office + day;
    }

    @Test
    void firstOrderOfDayStartsAtZero() {
        when(shipmentOrderRepository.findOrderCodesByPrefix(prefix(YB))).thenReturn(List.of());

        assertThat(generator.nextOrderCode(YB)).isEqualTo(prefix(YB) + "000");
    }

    @Test
    void nextCodeContinuesFromHighestUsedSequence() {
        String p = prefix(YB);
        when(shipmentOrderRepository.findOrderCodesByPrefix(p)).thenReturn(List.of(p + "000", p + "007", p + "003"));

        assertThat(generator.nextOrderCode(YB)).isEqualTo(p + "008");
    }

    @Test
    void officesCountIndependently() {
        String pYb = prefix(YB);
        when(shipmentOrderRepository.findOrderCodesByPrefix(pYb)).thenReturn(List.of(pYb + "000", pYb + "001"));
        when(shipmentOrderRepository.findOrderCodesByPrefix(prefix(ND))).thenReturn(List.of());

        assertThat(generator.nextOrderCode(YB)).isEqualTo(pYb + "002");
        assertThat(generator.nextOrderCode(ND)).isEqualTo(prefix(ND) + "000");
    }

    @Test
    void legacyRandomCodesOfSameDayDoNotBreakNumbering() {
        String p = prefix(YB);
        when(shipmentOrderRepository.findOrderCodesByPrefix(p)).thenReturn(List.of(p + "A3K9M", p + "ZZ001"));

        assertThat(generator.nextOrderCode(YB)).isEqualTo(p + "000");
    }

    @Test
    void takenCodeIsSkipped() {
        String p = prefix(YB);
        when(shipmentOrderRepository.findOrderCodesByPrefix(p)).thenReturn(List.of());
        when(shipmentOrderRepository.existsByOrderCode(p + "000")).thenReturn(true);

        assertThat(generator.nextOrderCode(YB)).isEqualTo(p + "001");
    }

    @Test
    void over1000PerDayNeedsConfirmation() {
        String p = prefix(YB);
        List<String> full = IntStream.range(0, 1000).mapToObj(i -> p + String.format("%03d", i)).toList();
        when(shipmentOrderRepository.findOrderCodesByPrefix(p)).thenReturn(full);

        assertThatThrownBy(() -> generator.nextOrderCode(YB))
            .isInstanceOf(BadRequestAlertException.class)
            .hasFieldOrPropertyWithValue("errorKey", OrderCodeGenerator.OVERFLOW_ERROR_KEY);
    }

    @Test
    void confirmedOverflowGrowsToFourDigits() {
        String p = prefix(YB);
        List<String> full = IntStream.range(0, 1000).mapToObj(i -> p + String.format("%03d", i)).toList();
        when(shipmentOrderRepository.findOrderCodesByPrefix(p)).thenReturn(full);

        assertThat(generator.nextOrderCode(YB, true)).isEqualTo(p + "1000");
    }

    @Test
    void officePrefixDropsVpPrefix() {
        when(shipmentOrderRepository.findOrderCodesByPrefix(prefix("TDN"))).thenReturn(List.of());

        assertThat(generator.nextOrderCode("VP_TDN")).isEqualTo(prefix("TDN") + "000");
    }
}
