package com.mycompany.myapp.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.AuditLog} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.AuditLogResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /audit-logs?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AuditLogCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter action;

    private StringFilter entityType;

    private StringFilter entityId;

    private StringFilter detail;

    private InstantFilter actedAt;

    private StringFilter actedByUsername;

    private Boolean distinct;

    public AuditLogCriteria() {}

    public AuditLogCriteria(AuditLogCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.action = other.optionalAction().map(StringFilter::copy).orElse(null);
        this.entityType = other.optionalEntityType().map(StringFilter::copy).orElse(null);
        this.entityId = other.optionalEntityId().map(StringFilter::copy).orElse(null);
        this.detail = other.optionalDetail().map(StringFilter::copy).orElse(null);
        this.actedAt = other.optionalActedAt().map(InstantFilter::copy).orElse(null);
        this.actedByUsername = other.optionalActedByUsername().map(StringFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public AuditLogCriteria copy() {
        return new AuditLogCriteria(this);
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

    public StringFilter getAction() {
        return action;
    }

    public Optional<StringFilter> optionalAction() {
        return Optional.ofNullable(action);
    }

    public StringFilter action() {
        if (action == null) {
            setAction(new StringFilter());
        }
        return action;
    }

    public void setAction(StringFilter action) {
        this.action = action;
    }

    public StringFilter getEntityType() {
        return entityType;
    }

    public Optional<StringFilter> optionalEntityType() {
        return Optional.ofNullable(entityType);
    }

    public StringFilter entityType() {
        if (entityType == null) {
            setEntityType(new StringFilter());
        }
        return entityType;
    }

    public void setEntityType(StringFilter entityType) {
        this.entityType = entityType;
    }

    public StringFilter getEntityId() {
        return entityId;
    }

    public Optional<StringFilter> optionalEntityId() {
        return Optional.ofNullable(entityId);
    }

    public StringFilter entityId() {
        if (entityId == null) {
            setEntityId(new StringFilter());
        }
        return entityId;
    }

    public void setEntityId(StringFilter entityId) {
        this.entityId = entityId;
    }

    public StringFilter getDetail() {
        return detail;
    }

    public Optional<StringFilter> optionalDetail() {
        return Optional.ofNullable(detail);
    }

    public StringFilter detail() {
        if (detail == null) {
            setDetail(new StringFilter());
        }
        return detail;
    }

    public void setDetail(StringFilter detail) {
        this.detail = detail;
    }

    public InstantFilter getActedAt() {
        return actedAt;
    }

    public Optional<InstantFilter> optionalActedAt() {
        return Optional.ofNullable(actedAt);
    }

    public InstantFilter actedAt() {
        if (actedAt == null) {
            setActedAt(new InstantFilter());
        }
        return actedAt;
    }

    public void setActedAt(InstantFilter actedAt) {
        this.actedAt = actedAt;
    }

    public StringFilter getActedByUsername() {
        return actedByUsername;
    }

    public Optional<StringFilter> optionalActedByUsername() {
        return Optional.ofNullable(actedByUsername);
    }

    public StringFilter actedByUsername() {
        if (actedByUsername == null) {
            setActedByUsername(new StringFilter());
        }
        return actedByUsername;
    }

    public void setActedByUsername(StringFilter actedByUsername) {
        this.actedByUsername = actedByUsername;
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
        final AuditLogCriteria that = (AuditLogCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(action, that.action) &&
            Objects.equals(entityType, that.entityType) &&
            Objects.equals(entityId, that.entityId) &&
            Objects.equals(detail, that.detail) &&
            Objects.equals(actedAt, that.actedAt) &&
            Objects.equals(actedByUsername, that.actedByUsername) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, action, entityType, entityId, detail, actedAt, actedByUsername, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AuditLogCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalAction().map(f -> "action=" + f + ", ").orElse("") +
            optionalEntityType().map(f -> "entityType=" + f + ", ").orElse("") +
            optionalEntityId().map(f -> "entityId=" + f + ", ").orElse("") +
            optionalDetail().map(f -> "detail=" + f + ", ").orElse("") +
            optionalActedAt().map(f -> "actedAt=" + f + ", ").orElse("") +
            optionalActedByUsername().map(f -> "actedByUsername=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
