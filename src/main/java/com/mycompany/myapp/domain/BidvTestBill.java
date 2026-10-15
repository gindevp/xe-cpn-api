package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Hóa đơn test cho BIDV UAT gọi getbill / paybill — không gắn đơn hay phiếu thu thật. */
@Entity
@Table(name = "bidv_test_bill")
public class BidvTestBill implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã khách hàng gửi BIDV (không kèm tiền tố TK định danh), tối đa 13 ký tự theo BIDV. */
    @NotNull
    @Size(max = 20)
    @Column(name = "customer_id", length = 20, nullable = false, unique = true)
    private String customerId;

    @NotNull
    @Size(max = 100)
    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @NotNull
    @Column(name = "amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal amount;

    @Size(max = 200)
    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
