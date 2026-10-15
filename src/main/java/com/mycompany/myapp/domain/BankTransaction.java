package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Một lần gạch nợ thành công từ ngân hàng. {@code transId} duy nhất: ngân hàng gọi lại cùng mã thì không ghi lần hai. */
@Entity
@Table(name = "bank_transaction")
public class BankTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDER_BIDV = "BIDV";
    public static final String PROVIDER_ONEPAY = "ONEPAY";
    /** target_id = bidv_test_bill.id */
    public static final String TARGET_TEST = "TEST";
    /** target_id = onepay_invoice.id */
    public static final String TARGET_ONEPAY_TEST = "ONEPAY_TEST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 10)
    @Column(name = "provider", length = 10, nullable = false)
    private String provider;

    @NotNull
    @Size(max = 64)
    @Column(name = "trans_id", length = 64, nullable = false, unique = true)
    private String transId;

    /** Thời gian giao dịch phía ngân hàng, giữ nguyên chuỗi gửi sang (yyyyMMddHHmmss). */
    @Size(max = 20)
    @Column(name = "trans_date", length = 20)
    private String transDate;

    @NotNull
    @Size(max = 40)
    @Column(name = "customer_id", length = 40, nullable = false)
    private String customerId;

    @NotNull
    @Size(max = 40)
    @Column(name = "bill_id", length = 40, nullable = false)
    private String billId;

    @NotNull
    @Column(name = "amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal amount;

    /** TEST (hóa đơn test); sau này ORDER / RECEIPT. */
    @NotNull
    @Size(max = 16)
    @Column(name = "target_type", length = 16, nullable = false)
    private String targetType;

    @NotNull
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTransId() {
        return transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
