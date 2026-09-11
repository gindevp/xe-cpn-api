package com.mycompany.myapp.service.invoice;

/**
 * Domain event: đơn đã chuyển sang DELIVERED (sau khi transaction commit).
 */
public record OrderDeliveredEvent(String orderCode) {}
