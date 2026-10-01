package com.mycompany.myapp.service.dto.order;

/**
 * Đổi VP nhận khi đơn đã nằm ở kho VP nhận (hàng được chuyển tay sang VP khác giao).
 *
 * @param officeCode mã VP nhận mới
 * @param reason lý do bắt buộc, lưu vào lịch sử đơn
 */
public record RerouteDestinationRequest(String officeCode, String reason) {}
