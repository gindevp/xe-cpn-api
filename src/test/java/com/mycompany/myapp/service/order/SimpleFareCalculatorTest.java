package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Branch;
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
    void estimate_usesFixedUnitPriceInBand() {
        PricingRule rule = routeRule(99L, "0", "5", "10000", "2000");
        when(pricingRuleRepository.findAll()).thenReturn(List.of(rule));

        SimpleFareCalculator.FareBreakdown b = calc.estimate(new BigDecimal("1"), false, false, gp, nb);

        assertThat(b.base()).isEqualByComparingTo("10000");
        assertThat(b.surcharge()).isEqualByComparingTo("2000");
        assertThat(b.total()).isEqualByComparingTo("12000");
        assertThat(b.pricingRuleId()).isEqualTo(99L);

        SimpleFareCalculator.FareBreakdown b2 = calc.estimate(new BigDecimal("4"), false, false, gp, nb);
        assertThat(b2.base()).isEqualByComparingTo("10000");
        assertThat(b2.total()).isEqualByComparingTo("12000");
    }

    @Test
    void estimate_exclusiveMinInclusiveMax() {
        PricingRule first = routeRule(1L, "0", "3", "15000", "0");
        PricingRule second = routeRule(2L, "3", "10", "25000", "0");
        when(pricingRuleRepository.findAll()).thenReturn(List.of(first, second));

        assertThat(calc.estimate(new BigDecimal("3"), false, false, gp, nb).base()).isEqualByComparingTo("15000");
        assertThat(calc.estimate(new BigDecimal("3.001"), false, false, gp, nb).base()).isEqualByComparingTo("25000");
    }

    @Test
    void estimate_overageUsesAddFeePerKgWhenNoStep() {
        PricingRule rule = routeRule(99L, "0", "5", "10000", "0");
        rule.setAddFeeAmount(new BigDecimal("2000"));
        when(pricingRuleRepository.findAll()).thenReturn(List.of(rule));

        // extra = 8-5 = 3kg × 2000
        SimpleFareCalculator.FareBreakdown b = calc.estimate(new BigDecimal("8"), false, false, gp, nb);
        assertThat(b.base()).isEqualByComparingTo("16000");
        assertThat(b.total()).isEqualByComparingTo("16000");
    }

    @Test
    void estimate_overageUsesStepGram() {
        PricingRule rule = routeRule(99L, "0", "3", "12000", "0");
        rule.setStepGram(500);
        rule.setAddFeeAmount(new BigDecimal("1000"));
        when(pricingRuleRepository.findAll()).thenReturn(List.of(rule));

        // extra 1kg = 1000g / 500 = 2 steps × 1000
        SimpleFareCalculator.FareBreakdown b = calc.estimate(new BigDecimal("4"), false, false, gp, nb);
        assertThat(b.base()).isEqualByComparingTo("14000");
    }

    @Test
    void estimate_prefersBranchOverOfficeRoute() {
        PricingRule officeRule = routeRule(1L, "0", "10", "99999", "0");
        PricingRule branchRule = new PricingRule();
        branchRule.setId(2L);
        branchRule.setActive(true);
        branchRule.setEffectiveFrom(Instant.now().minus(1, ChronoUnit.DAYS));
        branchRule.setMinKg(new BigDecimal("0"));
        branchRule.setMaxKg(new BigDecimal("10"));
        branchRule.setUnitPrice(new BigDecimal("22000"));
        branchRule.setSurchargeAmount(BigDecimal.ZERO);
        Branch branch = new Branch();
        branch.setCode("YB");
        branch.setName("Yên Bái");
        branchRule.setBranch(branch);
        when(pricingRuleRepository.findAll()).thenReturn(List.of(officeRule, branchRule));

        SimpleFareCalculator.FareBreakdown b = calc.estimate(new BigDecimal("2"), false, false, gp, nb, "YB");
        assertThat(b.base()).isEqualByComparingTo("22000");
        assertThat(b.pricingRuleId()).isEqualTo(2L);
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

    private PricingRule routeRule(Long id, String min, String max, String unit, String surcharge) {
        PricingRule rule = new PricingRule();
        rule.setId(id);
        rule.setActive(true);
        rule.setEffectiveFrom(Instant.now().minus(1, ChronoUnit.DAYS));
        rule.setMinKg(new BigDecimal(min));
        rule.setMaxKg(new BigDecimal(max));
        rule.setUnitPrice(new BigDecimal(unit));
        rule.setSurchargeAmount(new BigDecimal(surcharge));
        Route route = new Route();
        route.setFromOffice(gp);
        route.setToOffice(nb);
        rule.setRoute(route);
        return rule;
    }

    private static Office office(Long id, String code) {
        Office o = new Office();
        o.setId(id);
        o.setCode(code);
        return o;
    }
}
