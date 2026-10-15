package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Nhật ký mọi lệnh CPN gọi OnePay (OUT) và OnePay gọi IPN về (IN), kể cả lệnh lỗi / bị từ chối. */
@Entity
@Table(name = "onepay_call_log")
public class OnePayCallLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "called_at", nullable = false)
    private Instant calledAt;

    @NotNull
    @Size(max = 3)
    @Column(name = "direction", length = 3, nullable = false)
    private String direction;

    @NotNull
    @Size(max = 16)
    @Column(name = "action", length = 16, nullable = false)
    private String action;

    @Column(name = "http_status")
    private Integer httpStatus;

    @NotNull
    @Size(max = 20)
    @Column(name = "result", length = 20, nullable = false)
    private String result;

    @Size(max = 255)
    @Column(name = "message", length = 255)
    private String message;

    @Size(max = 64)
    @Column(name = "reference", length = 64)
    private String reference;

    @Size(max = 64)
    @Column(name = "trans_id", length = 64)
    private String transId;

    @Column(name = "amount", precision = 21, scale = 2)
    private BigDecimal amount;

    @Size(max = 64)
    @Column(name = "client_ip", length = 64)
    private String clientIp;

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

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
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

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
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
