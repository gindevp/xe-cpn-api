package com.mycompany.myapp.service.trip;

import com.mycompany.myapp.domain.enumeration.TripStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Aligned with FE {@code TRIP_TRANSITIONS}. */
public final class TripStatusTransitions {

    private static final Map<TripStatus, Set<TripStatus>> ALLOWED = new EnumMap<>(TripStatus.class);

    static {
        ALLOWED.put(TripStatus.CREATED, Set.of(TripStatus.LOADING, TripStatus.CANCELLED));
        ALLOWED.put(TripStatus.LOADING, Set.of(TripStatus.DEPARTED, TripStatus.CANCELLED));
        ALLOWED.put(TripStatus.DEPARTED, Set.of(TripStatus.UNLOADING));
        ALLOWED.put(TripStatus.UNLOADING, Set.of(TripStatus.CLOSED));
        ALLOWED.put(TripStatus.CLOSED, Set.of());
        ALLOWED.put(TripStatus.CANCELLED, Set.of());
    }

    private TripStatusTransitions() {}

    public static boolean canTransition(TripStatus from, TripStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static List<TripStatus> allowedTargets(TripStatus from) {
        return List.copyOf(ALLOWED.getOrDefault(from, Set.of()));
    }
}
