package com.mycompany.myapp.domain.enumeration;

/**
 * The ForwardStage enumeration.
 */
public enum ForwardStage {
    PICKED,
    WH_IN,
    TRANSFER_PENDING,
    TRANSFERRING,
    DEST_WH_IN,
    DELIVERING,
    FAILED,
    /** Giao thất bại, điều phối đã xếp vào danh sách chờ giao lại (vẫn FAILED_DELIVERY). */
    REDELIVER_WAIT,
}
