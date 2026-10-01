package com.mycompany.myapp.service.dto.order;

/**
 * Đổi hình thức thanh toán của đơn.
 *
 * @param method GUI_TRA | NHAN_TRA | CONG_NO (công nợ = người gửi trả, ghi nợ)
 * @param reason lý do bắt buộc, lưu vào lịch sử đơn
 */
public record ChangePaymentTermRequest(String method, String reason) {}
