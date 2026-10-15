package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Nhật ký một lần BIDV gọi getbill / paybill, kể cả bị từ chối — để đối chiếu khi UAT. */
@Entity
@Table(name = "bidv_call_log")
public class BidvCallLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "called_at", nullable = false)
    private Instant calledAt;

    /** GETBILL | PAYBILL */
    @NotNull
    @Size(max = 10)
    @Column(name = "endpoint", length = 10, nullable = false)
    private String endpoint;

    @Size(max = 64)
    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Size(max = 64)
    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Size(max = 64)
    @Column(name = "trans_id", length = 64)
    private String transId;

    @Column(name = "amount", precision = 21, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Size(max = 5)
    @Column(name = "result_code", length = 5, nullable = false)
    private String resultCode;

    @Size(max = 255)
    @Column(name = "result_desc", length = 255)
    private String resultDesc;

    /** Kiểu nối chuỗi checksum khớp được (PLUS / CONCAT / PLUS_PIPE …) — để chốt công thức với BIDV. */
    @Size(max = 20)
    @Column(name = "checksum_variant", length = 20)
    private String checksumVariant;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Size(max = 4000)
    @Column(name = "request_body", length = 4000)
    private String requestBody;

    @Size(max = 4000)
    @Column(name = "response_body", length = 4000)
    private String responseBody;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(Instant calledAt) {
        this.calledAt = calledAt;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getTransId() {
        return transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public void setResultDesc(String resultDesc) {
        this.resultDesc = resultDesc;
    }

    public String getChecksumVariant() {
        return checksumVariant;
    }

    public void setChecksumVariant(String checksumVariant) {
        this.checksumVariant = checksumVariant;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
