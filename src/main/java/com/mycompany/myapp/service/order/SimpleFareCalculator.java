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
 * Fare estimate aligned with FE {@code calcFare}.
 * Branch (Tuyến) rules: fixed {@code unitPrice} in band {@code (minKg, maxKg]} (min=0 → [0, max]);
 * over last max: {@code unitPrice + extra × addFee} (stepGram &gt; 0 → ceil extra grams / step).
 * Falls back to office Route match, then legacy constants.
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
        return estimate(weightKg, homePickup, homeDelivery, null, null, null, null, null);
    }

    public FareBreakdown estimate(BigDecimal weightKg, boolean homePickup, boolean homeDelivery, Office fromOffice, Office toOffice) {
        return estimate(weightKg, homePickup, homeDelivery, fromOffice, toOffice, null, null, null);
    }

    public FareBreakdown estimate(
        BigDecimal weightKg,
        boolean homePickup,
        boolean homeDelivery,
        Office fromOffice,
        Office toOffice,
        String branchKey
    ) {
        return estimate(weightKg, homePickup, homeDelivery, fromOffice, toOffice, null, null, branchKey);
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
        return estimate(weightKg, homePickup, homeDelivery, fromOffice, toOffice, pickupKm, deliveryKm, null);
    }

    public FareBreakdown estimate(
        BigDecimal weightKg,
        boolean homePickup,
        boolean homeDelivery,
        Office fromOffice,
        Office toOffice,
        BigDecimal pickupKm,
        BigDecimal deliveryKm,
        String branchKey
    ) {
        BigDecimal kg = weightKg == null ? BigDecimal.ZERO : weightKg.max(BigDecimal.ZERO);
        Instant now = Instant.now();

        PricingRuleMatch match = findRule(fromOffice, toOffice, kg, now, branchKey);
        PricingRule rule = match == null ? null : match.rule();
        BigDecimal base;
        BigDecimal surcharge;
        BigDecimal kmMin = DEFAULT_KM_MIN;
        BigDecimal kmRate = DEFAULT_KM_RATE;
        if (rule != null) {
            base = computeFixedBandFare(rule, kg, match.overage());
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

    private PricingRuleMatch findRule(Office from, Office to, BigDecimal chargeKg, Instant now, String branchKey) {
        List<PricingRule> active = pricingRuleRepository
            .findAll()
            .stream()
            .filter(p -> Boolean.TRUE.equals(p.getActive()))
            .filter(p -> p.getEffectiveFrom() == null || !p.getEffectiveFrom().isAfter(now))
            .filter(p -> p.getEffectiveTo() == null || p.getEffectiveTo().isAfter(now))
            .toList();

        if (branchKey != null && !branchKey.isBlank()) {
            PricingRuleMatch byBranch = pickBand(active.stream().filter(p -> matchesBranch(p, branchKey)).toList(), chargeKg);
            if (byBranch != null) {
                return byBranch;
            }
        }

        if (from == null || to == null || from.getCode() == null || to.getCode() == null) {
            return null;
        }
        return pickBand(active.stream().filter(p -> matchesRoute(p, from, to)).toList(), chargeKg);
    }

    private static PricingRuleMatch pickBand(List<PricingRule> rules, BigDecimal chargeKg) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        List<PricingRule> sorted = rules
            .stream()
            .sorted(Comparator.comparing(PricingRule::getMinKg, Comparator.nullsFirst(Comparator.naturalOrder())))
            .toList();
        for (PricingRule p : sorted) {
            if (inKgBand(chargeKg, p.getMinKg(), p.getMaxKg())) {
                return new PricingRuleMatch(p, false);
            }
        }
        PricingRule last = sorted.get(sorted.size() - 1);
        if (chargeKg.compareTo(nz(last.getMaxKg())) > 0) {
            return new PricingRuleMatch(last, true);
        }
        return null;
    }

    /**
     * Interval (min, max] — exclusive min, inclusive max.
     * min == 0 is [0, max] so the first band includes 0 kg.
     */
    static boolean inKgBand(BigDecimal kg, BigDecimal min, BigDecimal max) {
        BigDecimal v = nz(kg);
        BigDecimal lo = nz(min);
        BigDecimal hi = nz(max);
        boolean upperOk = v.compareTo(hi) <= 0;
        if (lo.compareTo(BigDecimal.ZERO) == 0) {
            return v.compareTo(BigDecimal.ZERO) >= 0 && upperOk;
        }
        return v.compareTo(lo) > 0 && upperOk;
    }

    static BigDecimal computeFixedBandFare(PricingRule rule, BigDecimal kg, boolean overage) {
        BigDecimal unit = nz(rule.getUnitPrice());
        if (!overage) {
            return unit.setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal extraKg = kg.subtract(nz(rule.getMaxKg())).max(BigDecimal.ZERO);
        BigDecimal add = nz(rule.getAddFeeAmount());
        Integer stepG = rule.getStepGram();
        BigDecimal extraMoney;
        if (stepG != null && stepG > 0 && add.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal extraG = extraKg.multiply(new BigDecimal("1000"));
            BigDecimal steps = extraG.divide(BigDecimal.valueOf(stepG), 0, RoundingMode.UP);
            extraMoney = steps.multiply(add);
        } else {
            extraMoney = extraKg.multiply(add).setScale(0, RoundingMode.HALF_UP);
        }
        return unit.add(extraMoney).setScale(0, RoundingMode.HALF_UP);
    }

    private static boolean matchesBranch(PricingRule p, String key) {
        if (p.getBranch() == null || key == null || key.isBlank()) {
            return false;
        }
        String code = p.getBranch().getCode();
        String name = p.getBranch().getName();
        return (code != null && key.equalsIgnoreCase(code)) || (name != null && key.equalsIgnoreCase(name));
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

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private record PricingRuleMatch(PricingRule rule, boolean overage) {}

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
