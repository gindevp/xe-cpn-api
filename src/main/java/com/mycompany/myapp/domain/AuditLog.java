package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A AuditLog.
 */
@Entity
@Table(name = "audit_log")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @NotNull
    @Size(max = 100)
    @Column(name = "entity_type", length = 100, nullable = false)
    private String entityType;

    @NotNull
    @Size(max = 100)
    @Column(name = "entity_id", length = 100, nullable = false)
    private String entityId;

    @Size(max = 255)
    @Column(name = "detail", length = 255)
    private String detail;

    @NotNull
    @Column(name = "acted_at", nullable = false)
    private Instant actedAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "acted_by_username", length = 50, nullable = false)
    private String actedByUsername;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public AuditLog id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return this.action;
    }

    public AuditLog action(String action) {
        this.setAction(action);
        return this;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return this.entityType;
    }

    public AuditLog entityType(String entityType) {
        this.setEntityType(entityType);
        return this;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public AuditLog entityId(String entityId) {
        this.setEntityId(entityId);
        return this;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDetail() {
        return this.detail;
    }

    public AuditLog detail(String detail) {
        this.setDetail(detail);
        return this;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getActedAt() {
        return this.actedAt;
    }

    public AuditLog actedAt(Instant actedAt) {
        this.setActedAt(actedAt);
        return this;
    }

    public void setActedAt(Instant actedAt) {
        this.actedAt = actedAt;
    }

    public String getActedByUsername() {
        return this.actedByUsername;
    }

    public AuditLog actedByUsername(String actedByUsername) {
        this.setActedByUsername(actedByUsername);
        return this;
    }

    public void setActedByUsername(String actedByUsername) {
        this.actedByUsername = actedByUsername;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditLog)) {
            return false;
        }
        return getId() != null && getId().equals(((AuditLog) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AuditLog{" +
            "id=" + getId() +
            ", action='" + getAction() + "'" +
            ", entityType='" + getEntityType() + "'" +
            ", entityId='" + getEntityId() + "'" +
            ", detail='" + getDetail() + "'" +
            ", actedAt='" + getActedAt() + "'" +
            ", actedByUsername='" + getActedByUsername() + "'" +
            "}";
    }
}
