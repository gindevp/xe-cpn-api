package com.mycompany.myapp.service.dto.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderDetailDTO extends OrderSummaryDTO {

    private String cancelReason;
    private String receiverActualName;
    private String receiverActualPhone;
    private Integer failCount;
    private List<OrderEventViewDTO> events = new ArrayList<>();
    private List<OrderIssueViewDTO> issues = new ArrayList<>();
    private Long currentIssueId;
    private List<OrderReturnViewDTO> returns = new ArrayList<>();
    private Long currentReturnId;

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getReceiverActualName() {
        return receiverActualName;
    }

    public void setReceiverActualName(String receiverActualName) {
        this.receiverActualName = receiverActualName;
    }

    public String getReceiverActualPhone() {
        return receiverActualPhone;
    }

    public void setReceiverActualPhone(String receiverActualPhone) {
        this.receiverActualPhone = receiverActualPhone;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public List<OrderEventViewDTO> getEvents() {
        return events;
    }

    public void setEvents(List<OrderEventViewDTO> events) {
        this.events = events;
    }

    public List<OrderIssueViewDTO> getIssues() {
        return issues;
    }

    public void setIssues(List<OrderIssueViewDTO> issues) {
        this.issues = issues;
    }

    public Long getCurrentIssueId() {
        return currentIssueId;
    }

    public void setCurrentIssueId(Long currentIssueId) {
        this.currentIssueId = currentIssueId;
    }

    public List<OrderReturnViewDTO> getReturns() {
        return returns;
    }

    public void setReturns(List<OrderReturnViewDTO> returns) {
        this.returns = returns;
    }

    public Long getCurrentReturnId() {
        return currentReturnId;
    }

    public void setCurrentReturnId(Long currentReturnId) {
        this.currentReturnId = currentReturnId;
    }

    public static class OrderIssueViewDTO {

        private Long id;
        private String issueType;
        private String issueStatus;
        private String reason;
        private Instant openedAt;
        private String openedByUsername;
        private Instant resolvedAt;
        private String resolvedByUsername;
        private String resolutionNote;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIssueType() {
            return issueType;
        }

        public void setIssueType(String issueType) {
            this.issueType = issueType;
        }

        public String getIssueStatus() {
            return issueStatus;
        }

        public void setIssueStatus(String issueStatus) {
            this.issueStatus = issueStatus;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public Instant getOpenedAt() {
            return openedAt;
        }

        public void setOpenedAt(Instant openedAt) {
            this.openedAt = openedAt;
        }

        public String getOpenedByUsername() {
            return openedByUsername;
        }

        public void setOpenedByUsername(String openedByUsername) {
            this.openedByUsername = openedByUsername;
        }

        public Instant getResolvedAt() {
            return resolvedAt;
        }

        public void setResolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
        }

        public String getResolvedByUsername() {
            return resolvedByUsername;
        }

        public void setResolvedByUsername(String resolvedByUsername) {
            this.resolvedByUsername = resolvedByUsername;
        }

        public String getResolutionNote() {
            return resolutionNote;
        }

        public void setResolutionNote(String resolutionNote) {
            this.resolutionNote = resolutionNote;
        }
    }

    public static class OrderReturnViewDTO {

        private Long id;
        private String reason;
        private String status;
        private Instant requestedAt;
        private String requestedByUsername;
        private Instant decidedAt;
        private String decidedByUsername;
        private String decisionNote;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getRequestedAt() {
            return requestedAt;
        }

        public void setRequestedAt(Instant requestedAt) {
            this.requestedAt = requestedAt;
        }

        public String getRequestedByUsername() {
            return requestedByUsername;
        }

        public void setRequestedByUsername(String requestedByUsername) {
            this.requestedByUsername = requestedByUsername;
        }

        public Instant getDecidedAt() {
            return decidedAt;
        }

        public void setDecidedAt(Instant decidedAt) {
            this.decidedAt = decidedAt;
        }

        public String getDecidedByUsername() {
            return decidedByUsername;
        }

        public void setDecidedByUsername(String decidedByUsername) {
            this.decidedByUsername = decidedByUsername;
        }

        public String getDecisionNote() {
            return decisionNote;
        }

        public void setDecisionNote(String decisionNote) {
            this.decisionNote = decisionNote;
        }
    }

    public static class OrderEventViewDTO {

        private Instant at;
        private String action;
        private String detail;
        private String by;

        public Instant getAt() {
            return at;
        }

        public void setAt(Instant at) {
            this.at = at;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getBy() {
            return by;
        }

        public void setBy(String by) {
            this.by = by;
        }
    }
}
