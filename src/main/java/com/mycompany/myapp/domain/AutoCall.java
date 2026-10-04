package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * Một cuộc gọi Auto Call (HHVN Tech) gắn với đơn.
 * status: PENDING (chưa gửi) · QUEUED/CALLING/RETRYING (HHVN đang xử lý) · COMPLETED/FAILED/CANCELLED (kết quả cuối)
 * · ERROR (gửi sang HHVN thất bại) · SKIPPED (không có SĐT hợp lệ).
 */
@Entity
@Table(name = "auto_call")
public class AutoCall implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ShipmentOrder order;

    @NotNull
    @Size(max = 10)
    @Column(name = "call_type", length = 10, nullable = false)
    private String callType;

    @NotNull
    @Size(max = 64)
    @Column(name = "ref_id", length = 64, nullable = false, unique = true)
    private String refId;

    @Size(max = 64)
    @Column(name = "call_id", length = 64)
    private String callId;

    @Size(max = 64)
    @Column(name = "request_id", length = 64)
    private String requestId;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 50)
    @Column(name = "trigger_action", length = 50)
    private String triggerAction;

    @NotNull
    @Column(name = "sandbox", nullable = false)
    private Boolean sandbox = false;

    /** HHVN | VTECH — nhà cung cấp đã (hoặc sẽ) gửi cuộc gọi này. */
    @NotNull
    @Size(max = 10)
    @Column(name = "provider", length = 10, nullable = false)
    private String provider = "HHVN";

    @NotNull
    @Size(max = 20)
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Size(max = 20)
    @Column(name = "result", length = 20)
    private String result;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "first_call_at")
    private Instant firstCallAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Size(max = 500)
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    @Size(max = 50)
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Size(max = 255)
    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** 0 = cuộc gọi đầu; n = lần gọi lại thứ n do CPN tự tạo. */
    @NotNull
    @Column(name = "retry_no", nullable = false)
    private Integer retryNo = 0;

    /** Ngày thứ mấy của chuỗi gọi lại (0 = ngày đầu). */
    @NotNull
    @Column(name = "retry_day", nullable = false)
    private Integer retryDay = 0;

    /** Thứ tự trong ngày của chuỗi (0 = lần gọi đầu của ngày). */
    @NotNull
    @Column(name = "retry_day_seq", nullable = false)
    private Integer retryDaySeq = 0;

    /** Hẹn giờ CPN tạo cuộc gọi lại kế tiếp; null = không gọi lại. */
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    public Integer getRetryNo() {
        return retryNo;
    }

    public void setRetryNo(Integer retryNo) {
        this.retryNo = retryNo;
    }

    public Integer getRetryDay() {
        return retryDay;
    }

    public void setRetryDay(Integer retryDay) {
        this.retryDay = retryDay;
    }

    public Integer getRetryDaySeq() {
        return retryDaySeq;
    }

    public void setRetryDaySeq(Integer retryDaySeq) {
        this.retryDaySeq = retryDaySeq;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ShipmentOrder getOrder() {
        return order;
    }

    public void setOrder(ShipmentOrder order) {
        this.order = order;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTriggerAction() {
        return triggerAction;
    }

    public void setTriggerAction(String triggerAction) {
        this.triggerAction = triggerAction;
    }

    public Boolean getSandbox() {
        return sandbox;
    }

    public void setSandbox(Boolean sandbox) {
        this.sandbox = sandbox;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getFirstCallAt() {
        return firstCallAt;
    }

    public void setFirstCallAt(Instant firstCallAt) {
        this.firstCallAt = firstCallAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AutoCall)) {
            return false;
        }
        return id != null && id.equals(((AutoCall) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
