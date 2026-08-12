package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.DoorFeeRule;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.PricingRule;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.domain.enumeration.DoorFeeKind;
import com.mycompany.myapp.repository.DoorFeeRuleRepository;
import com.mycompany.myapp.repository.PricingRuleRepository;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Fare estimate aligned with FE {@code calcFare} / {@code calcDoorFee} (TaoDon / bang-gia path).
 * Uses PricingRule (by route from→to + weight tier), DoorFeeRule, SurchargePolicy defaults.
 * Falls back to legacy flat constants when no matching active rule exists.
 */
@Component
public class SimpleFareCalculator {

    private static final BigDecimal FALLBACK_BASE = BigDecimal.valueOf(30_000);
    private static final BigDecimal FALLBACK_PER_KG = BigDecimal.valueOf(5_000);
    private static final BigDecimal FALLBACK_DOOR = BigDecimal.valueOf(10_000);
    private static final BigDecimal DEFAULT_KM_MIN = BigDecimal.valueOf(2);
    private static final BigDecimal DEFAULT_KM_RATE = BigDecimal.valueOf(5_000);

    private final PricingRuleRepository pricingRuleRepository;
    private final DoorFeeRuleRepository doorFeeRuleRepository;
    private final SurchargePolicyRepository surchargePolicyRepository;

    public SimpleFareCalculator(
        PricingRuleRepository pricingRuleRepository,
        DoorFeeRuleRepository doorFeeRuleRepository,
        SurchargePolicyRepository surchargePolicyRepository
    ) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.doorFeeRuleRepository = doorFeeRuleRepository;
        this.surchargePolicyRepository = surchargePolicyRepository;
    }

    /** Legacy signature — no route context → fallback constants (or global first matching tier). */
    public FareBreakdown estimate(BigDecimal weightKg, boolean homePickup, boolean homeDelivery) {
        return estimate(weightKg, homePickup, homeDelivery, null, null, null, null);
    }

    public FareBreakdown estimate(BigDecimal weightKg, boolean homePickup, boolean homeDelivery, Office fromOffice, Office toOffice) {
        return estimate(weightKg, homePickup, homeDelivery, fromOffice, toOffice, null, null);
    }

    public FareBreakdown estimate(
        BigDecimal weightKg,
        boolean homePickup,
        boolean homeDelivery,
        Office fromOffice,
        Office toOffice,
        BigDecimal pickupKm,
        BigDecimal deliveryKm
    ) {
        BigDecimal kg = weightKg == null ? BigDecimal.ZERO : weightKg.max(BigDecimal.ZERO);
        Instant now = Instant.now();

        PricingRule rule = findRule(fromOffice, toOffice, kg, now);
        BigDecimal base;
        BigDecimal surcharge;
        BigDecimal kmMin = DEFAULT_KM_MIN;
        BigDecimal kmRate = DEFAULT_KM_RATE;
        if (rule != null) {
            BigDecimal minKg = nz(rule.getMinKg());
            BigDecimal billable = kg.max(minKg.compareTo(BigDecimal.ZERO) > 0 ? minKg : new BigDecimal("0.1"));
            base = nz(rule.getUnitPrice()).multiply(billable).setScale(0, RoundingMode.HALF_UP);
            surcharge = nz(rule.getSurchargeAmount());
            if (rule.getKmMin() != null) {
                kmMin = rule.getKmMin();
            }
            if (rule.getKmRate() != null) {
                kmRate = rule.getKmRate();
            }
        } else {
            base = FALLBACK_BASE.add(kg.multiply(FALLBACK_PER_KG)).setScale(0, RoundingMode.HALF_UP);
            surcharge = BigDecimal.ZERO;
        }

        BigDecimal pickupFee = homePickup
            ? doorFee(DoorFeeKind.PICKUP, kg, pickupKm != null ? pickupKm : kmMin, kmMin, kmRate)
            : BigDecimal.ZERO;
        BigDecimal deliveryFee = homeDelivery
            ? doorFee(DoorFeeKind.DELIVERY, kg, deliveryKm != null ? deliveryKm : kmMin, kmMin, kmRate)
            : BigDecimal.ZERO;

        BigDecimal total = base.add(surcharge).add(pickupFee).add(deliveryFee);
        return new FareBreakdown(base, pickupFee, deliveryFee, total, surcharge, rule != null ? rule.getId() : null);
    }

    private PricingRule findRule(Office from, Office to, BigDecimal chargeKg, Instant now) {
        if (from == null || to == null || from.getCode() == null || to.getCode() == null) {
            return null;
        }
        List<PricingRule> active = pricingRuleRepository
            .findAll()
            .stream()
            .filter(p -> Boolean.TRUE.equals(p.getActive()))
            .filter(p -> p.getEffectiveFrom() == null || !p.getEffectiveFrom().isAfter(now))
            .filter(p -> p.getEffectiveTo() == null || p.getEffectiveTo().isAfter(now))
            .filter(p -> matchesRoute(p, from, to))
            .sorted(Comparator.comparing(PricingRule::getMinKg, Comparator.nullsFirst(Comparator.naturalOrder())))
            .toList();
        if (active.isEmpty()) {
            return null;
        }
        return active
            .stream()
            .filter(p -> inKgBand(chargeKg, p.getMinKg(), p.getMaxKg()))
            .findFirst()
            .orElse(active.get(active.size() - 1));
    }

    private static boolean matchesRoute(PricingRule p, Office from, Office to) {
        if (p.getRoute() == null) {
            return false;
        }
        Office rf = p.getRoute().getFromOffice();
        Office rt = p.getRoute().getToOffice();
        if (rf == null || rt == null || rf.getCode() == null || rt.getCode() == null) {
            return false;
        }
        return rf.getCode().equalsIgnoreCase(from.getCode()) && rt.getCode().equalsIgnoreCase(to.getCode());
    }

    private BigDecimal doorFee(DoorFeeKind kind, BigDecimal kg, BigDecimal km, BigDecimal kmMin, BigDecimal kmRate) {
        BigDecimal useKm = km == null ? kmMin : km.max(kmMin);
        DoorFeeRule hit = doorFeeRuleRepository
            .findAll()
            .stream()
            .filter(r -> Boolean.TRUE.equals(r.getActive()))
            .filter(r -> r.getKind() == kind)
            .filter(r -> inBand(kg, r.getMinKg(), r.getMaxKg()))
            .filter(r -> inBand(useKm, r.getMinKm(), r.getMaxKm()))
            .findFirst()
            .orElse(null);
        if (hit != null) {
            return nz(hit.getFeeAmount());
        }
        SurchargePolicy policy = surchargePolicyRepository.findAll().stream().findFirst().orElse(null);
        if (kind == DoorFeeKind.DELIVERY && policy != null && Boolean.TRUE.equals(policy.getHomeDeliveryEnabled())) {
            return nz(policy.getDefaultHomeDeliveryAmount());
        }
        if (kmRate != null && useKm != null) {
            return useKm.multiply(kmRate).setScale(0, RoundingMode.HALF_UP);
        }
        return FALLBACK_DOOR;
    }

    /** FE-aligned: value > min - eps && value <= max + eps */
    private static boolean inBand(BigDecimal value, BigDecimal min, BigDecimal max) {
        BigDecimal v = nz(value);
        BigDecimal lo = nz(min).subtract(new BigDecimal("0.001"));
        BigDecimal hi = nz(max).add(new BigDecimal("0.001"));
        return v.compareTo(lo) > 0 && v.compareTo(hi) <= 0;
    }

    private static boolean inKgBand(BigDecimal kg, BigDecimal min, BigDecimal max) {
        return inBand(kg, min, max);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public record FareBreakdown(
        BigDecimal base,
        BigDecimal pickupFee,
        BigDecimal deliveryFee,
        BigDecimal total,
        BigDecimal surcharge,
        Long pricingRuleId
    ) {
        public FareBreakdown(BigDecimal base, BigDecimal pickupFee, BigDecimal deliveryFee, BigDecimal total) {
            this(base, pickupFee, deliveryFee, total, BigDecimal.ZERO, null);
        }
    }
}
