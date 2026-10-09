package com.mycompany.myapp.domain.enumeration;

/**
 * The ReturnStage enumeration.
 */
public enum ReturnStage {
    RETURN_PENDING,
    /** Hoàn tại VP gửi: khách vừa nhập rồi huỷ, hàng chưa lên xe — giao trả tại chỗ. */
    RT_ORIGIN_WH_IN,
    RT_TRANSFER_PENDING,
    RT_TRANSFERRING,
    RT_WH_IN,
    RT_DELIVERING,
    RT_FAILED,
    RT_DONE,
}
