package com.mycompany.myapp.domain.enumeration;

/**
 * The OrderStatus enumeration.
 */
public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    WAITING,
    IN_TRANSIT,
    AT_DEST,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED_DELIVERY,
    CANCELLED,
    RETURNING,
    RETURNED,
}
