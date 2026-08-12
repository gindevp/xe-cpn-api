package com.mycompany.myapp.service.criteria;

import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.OrderIssue} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.OrderIssueResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /order-issues?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderIssueCriteria implements Serializable, Criteria {

    /**
     * Class for filtering IssueType
     */
    public static class IssueTypeFilter extends Filter<IssueType> {

        public IssueTypeFilter() {}

        public IssueTypeFilter(IssueTypeFilter filter) {
            super(filter);
        }

        @Override
        public IssueTypeFilter copy() {
            return new IssueTypeFilter(this);
        }
    }

    /**
     * Class for filtering IssueStatus
     */
    public static class IssueStatusFilter extends Filter<IssueStatus> {

        public IssueStatusFilter() {}

        public IssueStatusFilter(IssueStatusFilter filter) {
            super(filter);
        }

        @Override
        public IssueStatusFilter copy() {
            return new IssueStatusFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private IssueTypeFilter issueType;

    private IssueStatusFilter issueStatus;

    private StringFilter reason;

    private InstantFilter openedAt;

    private StringFilter openedByUsername;

    private InstantFilter resolvedAt;

    private StringFilter resolvedByUsername;

    private StringFilter resolutionNote;

    private LongFilter orderId;

    private Boolean distinct;

    public OrderIssueCriteria() {}

    public OrderIssueCriteria(OrderIssueCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.issueType = other.optionalIssueType().map(IssueTypeFilter::copy).orElse(null);
        this.issueStatus = other.optionalIssueStatus().map(IssueStatusFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.openedAt = other.optionalOpenedAt().map(InstantFilter::copy).orElse(null);
        this.openedByUsername = other.optionalOpenedByUsername().map(StringFilter::copy).orElse(null);
        this.resolvedAt = other.optionalResolvedAt().map(InstantFilter::copy).orElse(null);
        this.resolvedByUsername = other.optionalResolvedByUsername().map(StringFilter::copy).orElse(null);
        this.resolutionNote = other.optionalResolutionNote().map(StringFilter::copy).orElse(null);
        this.orderId = other.optionalOrderId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public OrderIssueCriteria copy() {
        return new OrderIssueCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public IssueTypeFilter getIssueType() {
        return issueType;
    }

    public Optional<IssueTypeFilter> optionalIssueType() {
        return Optional.ofNullable(issueType);
    }

    public IssueTypeFilter issueType() {
        if (issueType == null) {
            setIssueType(new IssueTypeFilter());
        }
        return issueType;
    }

    public void setIssueType(IssueTypeFilter issueType) {
        this.issueType = issueType;
    }

    public IssueStatusFilter getIssueStatus() {
        return issueStatus;
    }

    public Optional<IssueStatusFilter> optionalIssueStatus() {
        return Optional.ofNullable(issueStatus);
    }

    public IssueStatusFilter issueStatus() {
        if (issueStatus == null) {
            setIssueStatus(new IssueStatusFilter());
        }
        return issueStatus;
    }

    public void setIssueStatus(IssueStatusFilter issueStatus) {
        this.issueStatus = issueStatus;
    }

    public StringFilter getReason() {
        return reason;
    }

    public Optional<StringFilter> optionalReason() {
        return Optional.ofNullable(reason);
    }

    public StringFilter reason() {
        if (reason == null) {
            setReason(new StringFilter());
        }
        return reason;
    }

    public void setReason(StringFilter reason) {
        this.reason = reason;
    }

    public InstantFilter getOpenedAt() {
        return openedAt;
    }

    public Optional<InstantFilter> optionalOpenedAt() {
        return Optional.ofNullable(openedAt);
    }

    public InstantFilter openedAt() {
        if (openedAt == null) {
            setOpenedAt(new InstantFilter());
        }
        return openedAt;
    }

    public void setOpenedAt(InstantFilter openedAt) {
        this.openedAt = openedAt;
    }

    public StringFilter getOpenedByUsername() {
        return openedByUsername;
    }

    public Optional<StringFilter> optionalOpenedByUsername() {
        return Optional.ofNullable(openedByUsername);
    }

    public StringFilter openedByUsername() {
        if (openedByUsername == null) {
            setOpenedByUsername(new StringFilter());
        }
        return openedByUsername;
    }

    public void setOpenedByUsername(StringFilter openedByUsername) {
        this.openedByUsername = openedByUsername;
    }

    public InstantFilter getResolvedAt() {
        return resolvedAt;
    }

    public Optional<InstantFilter> optionalResolvedAt() {
        return Optional.ofNullable(resolvedAt);
    }

    public InstantFilter resolvedAt() {
        if (resolvedAt == null) {
            setResolvedAt(new InstantFilter());
        }
        return resolvedAt;
    }

    public void setResolvedAt(InstantFilter resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public StringFilter getResolvedByUsername() {
        return resolvedByUsername;
    }

    public Optional<StringFilter> optionalResolvedByUsername() {
        return Optional.ofNullable(resolvedByUsername);
    }

    public StringFilter resolvedByUsername() {
        if (resolvedByUsername == null) {
            setResolvedByUsername(new StringFilter());
        }
        return resolvedByUsername;
    }

    public void setResolvedByUsername(StringFilter resolvedByUsername) {
        this.resolvedByUsername = resolvedByUsername;
    }

    public StringFilter getResolutionNote() {
        return resolutionNote;
    }

    public Optional<StringFilter> optionalResolutionNote() {
        return Optional.ofNullable(resolutionNote);
    }

    public StringFilter resolutionNote() {
        if (resolutionNote == null) {
            setResolutionNote(new StringFilter());
        }
        return resolutionNote;
    }

    public void setResolutionNote(StringFilter resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public LongFilter getOrderId() {
        return orderId;
    }

    public Optional<LongFilter> optionalOrderId() {
        return Optional.ofNullable(orderId);
    }

    public LongFilter orderId() {
        if (orderId == null) {
            setOrderId(new LongFilter());
        }
        return orderId;
    }

    public void setOrderId(LongFilter orderId) {
        this.orderId = orderId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OrderIssueCriteria that = (OrderIssueCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(issueType, that.issueType) &&
            Objects.equals(issueStatus, that.issueStatus) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(openedAt, that.openedAt) &&
            Objects.equals(openedByUsername, that.openedByUsername) &&
            Objects.equals(resolvedAt, that.resolvedAt) &&
            Objects.equals(resolvedByUsername, that.resolvedByUsername) &&
            Objects.equals(resolutionNote, that.resolutionNote) &&
            Objects.equals(orderId, that.orderId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            issueType,
            issueStatus,
            reason,
            openedAt,
            openedByUsername,
            resolvedAt,
            resolvedByUsername,
            resolutionNote,
            orderId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderIssueCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalIssueType().map(f -> "issueType=" + f + ", ").orElse("") +
            optionalIssueStatus().map(f -> "issueStatus=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalOpenedAt().map(f -> "openedAt=" + f + ", ").orElse("") +
            optionalOpenedByUsername().map(f -> "openedByUsername=" + f + ", ").orElse("") +
            optionalResolvedAt().map(f -> "resolvedAt=" + f + ", ").orElse("") +
            optionalResolvedByUsername().map(f -> "resolvedByUsername=" + f + ", ").orElse("") +
            optionalResolutionNote().map(f -> "resolutionNote=" + f + ", ").orElse("") +
            optionalOrderId().map(f -> "orderId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
