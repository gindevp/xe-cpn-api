package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.DoorFeeRule;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.PricingRule;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.domain.enumeration.DoorFeeKind;
import com.mycompany.myapp.repository.DoorFeeRuleRepository;
import com.mycompany.myapp.repository.PricingRuleRepository;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimpleFareCalculatorTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private DoorFeeRuleRepository doorFeeRuleRepository;

    @Mock
    private SurchargePolicyRepository surchargePolicyRepository;

    private SimpleFareCalculator calc;
    private Office gp;
    private Office nb;

    @BeforeEach
    void setUp() {
        calc = new SimpleFareCalculator(pricingRuleRepository, doorFeeRuleRepository, surchargePolicyRepository);
        gp = office(5L, "GP");
        nb = office(8L, "NB");
    }

    @Test
    void estimate_usesFallbackWhenNoRules() {
        when(pricingRuleRepository.findAll()).thenReturn(List.of());

        SimpleFareCalculator.FareBreakdown b = calc.estimate(new BigDecimal("2"), false, false, gp, nb);

        // 30000 + 2*5000 = 40000
        assertThat(b.total()).isEqualByComparingTo("40000");
        assertThat(b.pricingRuleId()).isNull();
    }

    @Test
    void estimate_usesPricingRuleForRouteAndWeight() {
        PricingRule rule = new PricingRule();
        rule.setId(99L);
        rule.setActive(true);
        rule.setEffectiveFrom(Instant.now().minus(1, ChronoUnit.DAYS));
        rule.setMinKg(new BigDecimal("0"));
        rule.setMaxKg(new BigDecimal("5"));
        rule.setUnitPrice(new BigDecimal("10000"));
        rule.setSurchargeAmount(new BigDecimal("2000"));
        Route route = new Route();
        route.setFromOffice(gp);
        route.setToOffice(nb);
        rule.setRoute(route);
        when(pricingRuleRepository.findAll()).thenReturn(List.of(rule));

        // billable = max(1, 0.1) = 1 → base 10000 + surcharge 2000
        SimpleFareCalculator.FareBreakdown b = calc.estimate(new BigDecimal("1"), false, false, gp, nb);

        assertThat(b.base()).isEqualByComparingTo("10000");
        assertThat(b.surcharge()).isEqualByComparingTo("2000");
        assertThat(b.total()).isEqualByComparingTo("12000");
        assertThat(b.pricingRuleId()).isEqualTo(99L);
    }

    @Test
    void estimate_usesDoorFeeRuleWhenHomeDelivery() {
        when(pricingRuleRepository.findAll()).thenReturn(List.of());
        DoorFeeRule door = new DoorFeeRule();
        door.setActive(true);
        door.setKind(DoorFeeKind.DELIVERY);
        door.setMinKg(new BigDecimal("0"));
        door.setMaxKg(new BigDecimal("10"));
        door.setMinKm(new BigDecimal("0"));
        door.setMaxKm(new BigDecimal("20"));
        door.setFeeAmount(new BigDecimal("15000"));
        when(doorFeeRuleRepository.findAll()).thenReturn(List.of(door));

        SimpleFareCalculator.FareBreakdown b = calc.estimate(new BigDecimal("1"), false, true, gp, nb);

        assertThat(b.deliveryFee()).isEqualByComparingTo("15000");
        assertThat(b.total()).isEqualByComparingTo("50000"); // fallback base 35000 + door 15000
    }

    private static Office office(Long id, String code) {
        Office o = new Office();
        o.setId(id);
        o.setCode(code);
        return o;
    }
}
