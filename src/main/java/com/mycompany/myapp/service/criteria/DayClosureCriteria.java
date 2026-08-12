package com.mycompany.myapp.service.criteria;

import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.DayClosure} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.DayClosureResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /day-closures?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DayClosureCriteria implements Serializable, Criteria {

    /**
     * Class for filtering DayClosureStatus
     */
    public static class DayClosureStatusFilter extends Filter<DayClosureStatus> {

        public DayClosureStatusFilter() {}

        public DayClosureStatusFilter(DayClosureStatusFilter filter) {
            super(filter);
        }

        @Override
        public DayClosureStatusFilter copy() {
            return new DayClosureStatusFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private LocalDateFilter businessDate;

    private DayClosureStatusFilter status;

    private StringFilter confirmedByUsername;

    private InstantFilter confirmedAt;

    private StringFilter reopenedByUsername;

    private InstantFilter reopenedAt;

    private LongFilter officeId;

    private Boolean distinct;

    public DayClosureCriteria() {}

    public DayClosureCriteria(DayClosureCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.businessDate = other.optionalBusinessDate().map(LocalDateFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(DayClosureStatusFilter::copy).orElse(null);
        this.confirmedByUsername = other.optionalConfirmedByUsername().map(StringFilter::copy).orElse(null);
        this.confirmedAt = other.optionalConfirmedAt().map(InstantFilter::copy).orElse(null);
        this.reopenedByUsername = other.optionalReopenedByUsername().map(StringFilter::copy).orElse(null);
        this.reopenedAt = other.optionalReopenedAt().map(InstantFilter::copy).orElse(null);
        this.officeId = other.optionalOfficeId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DayClosureCriteria copy() {
        return new DayClosureCriteria(this);
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

    public LocalDateFilter getBusinessDate() {
        return businessDate;
    }

    public Optional<LocalDateFilter> optionalBusinessDate() {
        return Optional.ofNullable(businessDate);
    }

    public LocalDateFilter businessDate() {
        if (businessDate == null) {
            setBusinessDate(new LocalDateFilter());
        }
        return businessDate;
    }

    public void setBusinessDate(LocalDateFilter businessDate) {
        this.businessDate = businessDate;
    }

    public DayClosureStatusFilter getStatus() {
        return status;
    }

    public Optional<DayClosureStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public DayClosureStatusFilter status() {
        if (status == null) {
            setStatus(new DayClosureStatusFilter());
        }
        return status;
    }

    public void setStatus(DayClosureStatusFilter status) {
        this.status = status;
    }

    public StringFilter getConfirmedByUsername() {
        return confirmedByUsername;
    }

    public Optional<StringFilter> optionalConfirmedByUsername() {
        return Optional.ofNullable(confirmedByUsername);
    }

    public StringFilter confirmedByUsername() {
        if (confirmedByUsername == null) {
            setConfirmedByUsername(new StringFilter());
        }
        return confirmedByUsername;
    }

    public void setConfirmedByUsername(StringFilter confirmedByUsername) {
        this.confirmedByUsername = confirmedByUsername;
    }

    public InstantFilter getConfirmedAt() {
        return confirmedAt;
    }

    public Optional<InstantFilter> optionalConfirmedAt() {
        return Optional.ofNullable(confirmedAt);
    }

    public InstantFilter confirmedAt() {
        if (confirmedAt == null) {
            setConfirmedAt(new InstantFilter());
        }
        return confirmedAt;
    }

    public void setConfirmedAt(InstantFilter confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public StringFilter getReopenedByUsername() {
        return reopenedByUsername;
    }

    public Optional<StringFilter> optionalReopenedByUsername() {
        return Optional.ofNullable(reopenedByUsername);
    }

    public StringFilter reopenedByUsername() {
        if (reopenedByUsername == null) {
            setReopenedByUsername(new StringFilter());
        }
        return reopenedByUsername;
    }

    public void setReopenedByUsername(StringFilter reopenedByUsername) {
        this.reopenedByUsername = reopenedByUsername;
    }

    public InstantFilter getReopenedAt() {
        return reopenedAt;
    }

    public Optional<InstantFilter> optionalReopenedAt() {
        return Optional.ofNullable(reopenedAt);
    }

    public InstantFilter reopenedAt() {
        if (reopenedAt == null) {
            setReopenedAt(new InstantFilter());
        }
        return reopenedAt;
    }

    public void setReopenedAt(InstantFilter reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public LongFilter getOfficeId() {
        return officeId;
    }

    public Optional<LongFilter> optionalOfficeId() {
        return Optional.ofNullable(officeId);
    }

    public LongFilter officeId() {
        if (officeId == null) {
            setOfficeId(new LongFilter());
        }
        return officeId;
    }

    public void setOfficeId(LongFilter officeId) {
        this.officeId = officeId;
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
        final DayClosureCriteria that = (DayClosureCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(businessDate, that.businessDate) &&
            Objects.equals(status, that.status) &&
            Objects.equals(confirmedByUsername, that.confirmedByUsername) &&
            Objects.equals(confirmedAt, that.confirmedAt) &&
            Objects.equals(reopenedByUsername, that.reopenedByUsername) &&
            Objects.equals(reopenedAt, that.reopenedAt) &&
            Objects.equals(officeId, that.officeId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, businessDate, status, confirmedByUsername, confirmedAt, reopenedByUsername, reopenedAt, officeId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DayClosureCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalBusinessDate().map(f -> "businessDate=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalConfirmedByUsername().map(f -> "confirmedByUsername=" + f + ", ").orElse("") +
            optionalConfirmedAt().map(f -> "confirmedAt=" + f + ", ").orElse("") +
            optionalReopenedByUsername().map(f -> "reopenedByUsername=" + f + ", ").orElse("") +
            optionalReopenedAt().map(f -> "reopenedAt=" + f + ", ").orElse("") +
            optionalOfficeId().map(f -> "officeId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
