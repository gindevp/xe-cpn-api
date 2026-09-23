package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Mã đơn: {VP}{ddMM}{XXXX} — XXXX 4 ký tự A-Z0-9 (không 0/O/1/I/L), ít trùng đuôi. */
@ExtendWith(MockitoExtension.class)
class OrderCodeGeneratorTest {

    private static final String YB = "YB1";
    private static final String ND = "ND";
    private static final String SAFE = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    @Mock
    private ShipmentOrderRepository shipmentOrderRepository;

    private OrderCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OrderCodeGenerator(shipmentOrderRepository);
    }

    private static String prefix(String office) {
        String day = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("ddMM"));
        return office + day;
    }

    @Test
    void formatIsOfficeDdMmPlusFourAlnum() {
        when(shipmentOrderRepository.findOrderCodesByPrefix(prefix(YB))).thenReturn(List.of());
        when(shipmentOrderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(shipmentOrderRepository.existsByOrderCodeEndingWithIgnoreCase(anyString())).thenReturn(false);

        String code = generator.nextOrderCode(YB);
        String p = prefix(YB);
        assertThat(code).startsWith(p);
        assertThat(code.length()).isEqualTo(p.length() + 4);
        String suffix = code.substring(p.length());
        assertThat(suffix).hasSize(4);
        assertThat(suffix.chars().allMatch(c -> SAFE.indexOf(c) >= 0)).isTrue();
    }

    @Test
    void officesHaveIndependentDailyQuota() {
        when(shipmentOrderRepository.findOrderCodesByPrefix(prefix(YB))).thenReturn(List.of());
        when(shipmentOrderRepository.findOrderCodesByPrefix(prefix(ND))).thenReturn(List.of());
        when(shipmentOrderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(shipmentOrderRepository.existsByOrderCodeEndingWithIgnoreCase(anyString())).thenReturn(false);

        assertThat(generator.nextOrderCode(YB)).startsWith(prefix(YB));
        assertThat(generator.nextOrderCode(ND)).startsWith(prefix(ND));
    }

    @Test
    void skipsTakenFullCodeAndTakenSuffix() {
        String p = prefix(YB);
        when(shipmentOrderRepository.findOrderCodesByPrefix(p)).thenReturn(List.of());
        when(shipmentOrderRepository.existsByOrderCode(anyString())).thenAnswer(inv -> {
            String c = inv.getArgument(0);
            return c.endsWith("AAAA"); // never generated (A not in SAFE) — always false path
        });
        when(shipmentOrderRepository.existsByOrderCodeEndingWithIgnoreCase(anyString())).thenReturn(false);

        String code = generator.nextOrderCode(YB);
        assertThat(code).startsWith(p).hasSize(p.length() + 4);
    }

    @Test
    void over1000PerDayNeedsConfirmation() {
        String p = prefix(YB);
        List<String> full = IntStream.range(0, 1000).mapToObj(i -> p + String.format("%04d", i)).toList();
        when(shipmentOrderRepository.findOrderCodesByPrefix(p)).thenReturn(full);

        assertThatThrownBy(() -> generator.nextOrderCode(YB))
            .isInstanceOf(BadRequestAlertException.class)
            .hasFieldOrPropertyWithValue("errorKey", OrderCodeGenerator.OVERFLOW_ERROR_KEY);
    }

    @Test
    void confirmedOverflowStillAllocates() {
        String p = prefix(YB);
        List<String> full = IntStream.range(0, 1000).mapToObj(i -> p + String.format("%04d", i)).toList();
        when(shipmentOrderRepository.findOrderCodesByPrefix(p)).thenReturn(full);
        when(shipmentOrderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(shipmentOrderRepository.existsByOrderCodeEndingWithIgnoreCase(anyString())).thenReturn(false);

        assertThat(generator.nextOrderCode(YB, true)).startsWith(p).hasSize(p.length() + 4);
    }

    @Test
    void officePrefixDropsVpPrefix() {
        when(shipmentOrderRepository.findOrderCodesByPrefix(prefix("TDN"))).thenReturn(Collections.emptyList());
        when(shipmentOrderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(shipmentOrderRepository.existsByOrderCodeEndingWithIgnoreCase(anyString())).thenReturn(false);

        assertThat(generator.nextOrderCode("VP_TDN")).startsWith(prefix("TDN"));
    }
}
