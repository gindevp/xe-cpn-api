package com.mycompany.myapp.domain.enumeration;

/**
 * The IssueType enumeration.
 */
public enum IssueType {
    EXCEPTION,
    LOST,
    DAMAGED,
    /** Điều phối yêu cầu huỷ — đơn ẩn khỏi danh sách tới khi admin duyệt / từ chối. */
    CANCEL_REQUEST,
}
