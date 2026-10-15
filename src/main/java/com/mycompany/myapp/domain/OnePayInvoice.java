package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Hoá đơn CPN đã tạo trên OnePay: mỗi hoá đơn một khách (user_reference) và một tài khoản ảo riêng, nên IPN được
 * khớp theo user_reference chứ không theo nội dung chuyển khoản.
 */
@Entity
@Table(name = "onepay_invoice")
public class OnePayInvoice implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PURPOSE_TEST = "TEST";
    public static final String STATE_UNPAID = "UNPAID";
    public static final String STATE_PAID = "PAID";
    public static final String STATE_EXPIRED = "EXPIRED";
    /** Có tiền về nhưng khác số tiền hoá đơn — kế toán xử lý tay. */
    public static final String STATE_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 16)
    @Column(name = "purpose", length = 16, nullable = false)
    private String purpose = PURPOSE_TEST;

    @NotNull
    @Size(max = 50)
    @Column(name = "user_reference", length = 50, nullable = false, unique = true)
    private String userReference;

    @NotNull
    @Size(max = 20)
    @Column(name = "invoice_reference", length = 20, nullable = false, unique = true)
    private String invoiceReference;

    @NotNull
    @Size(max = 70)
    @Column(name = "payer_name", length = 70, nullable = false)
    private String payerName;

    @NotNull
    @Column(name = "amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal amount;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @NotNull
    @Size(max = 16)
    @Column(name = "state", length = 16, nullable = false)
    private String state = STATE_UNPAID;

    @Size(max = 40)
    @Column(name = "onepay_user_id", length = 40)
    private String onepayUserId;

    @Size(max = 40)
    @Column(name = "onepay_invoice_id", length = 40)
    private String onepayInvoiceId;

    @Size(max = 40)
    @Column(name = "account_number", length = 40)
    private String accountNumber;

    @Size(max = 60)
    @Column(name = "bank_name", length = 60)
    private String bankName;

    /** Chuỗi VietQR OnePay trả về; FE tự vẽ ảnh QR. */
    @Size(max = 1000)
    @Column(name = "qr_data", length = 1000)
    private String qrData;

    /** active | inactive — trạng thái tài khoản ảo bên OnePay. */
    @Size(max = 16)
    @Column(name = "va_state", length = 16)
    private String vaState;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Size(max = 64)
    @Column(name = "paid_trans_id", length = 64)
    private String paidTransId;

    @Column(name = "paid_amount", precision = 21, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Size(max = 255)
    @Column(name = "last_error", length = 255)
    private String lastError;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getUserReference() {
        return userReference;
    }

    public void setUserReference(String userReference) {
        this.userReference = userReference;
    }

    public String getInvoiceReference() {
        return invoiceReference;
    }

    public void setInvoiceReference(String invoiceReference) {
        this.invoiceReference = invoiceReference;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getOnepayUserId() {
        return onepayUserId;
    }

    public void setOnepayUserId(String onepayUserId) {
        this.onepayUserId = onepayUserId;
    }

    public String getOnepayInvoiceId() {
        return onepayInvoiceId;
    }

    public void setOnepayInvoiceId(String onepayInvoiceId) {
        this.onepayInvoiceId = onepayInvoiceId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    public String getVaState() {
        return vaState;
    }

    public void setVaState(String vaState) {
        this.vaState = vaState;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getPaidTransId() {
        return paidTransId;
    }

    public void setPaidTransId(String paidTransId) {
        this.paidTransId = paidTransId;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
