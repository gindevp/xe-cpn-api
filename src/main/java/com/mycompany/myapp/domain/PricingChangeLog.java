package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A PricingChangeLog.
 */
@Entity
@Table(name = "pricing_change_log")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PricingChangeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "changed_by_username", length = 50, nullable = false)
    private String changedByUsername;

    @Lob
    @Column(name = "before_json")
    private String beforeJson;

    @Lob
    @Column(name = "after_json", nullable = false)
    private String afterJson;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "route" }, allowSetters = true)
    private PricingRule pricingRule;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public PricingChangeLog id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getChangedAt() {
        return this.changedAt;
    }

    public PricingChangeLog changedAt(Instant changedAt) {
        this.setChangedAt(changedAt);
        return this;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedByUsername() {
        return this.changedByUsername;
    }

    public PricingChangeLog changedByUsername(String changedByUsername) {
        this.setChangedByUsername(changedByUsername);
        return this;
    }

    public void setChangedByUsername(String changedByUsername) {
        this.changedByUsername = changedByUsername;
    }

    public String getBeforeJson() {
        return this.beforeJson;
    }

    public PricingChangeLog beforeJson(String beforeJson) {
        this.setBeforeJson(beforeJson);
        return this;
    }

    public void setBeforeJson(String beforeJson) {
        this.beforeJson = beforeJson;
    }

    public String getAfterJson() {
        return this.afterJson;
    }

    public PricingChangeLog afterJson(String afterJson) {
        this.setAfterJson(afterJson);
        return this;
    }

    public void setAfterJson(String afterJson) {
        this.afterJson = afterJson;
    }

    public PricingRule getPricingRule() {
        return this.pricingRule;
    }

    public void setPricingRule(PricingRule pricingRule) {
        this.pricingRule = pricingRule;
    }

    public PricingChangeLog pricingRule(PricingRule pricingRule) {
        this.setPricingRule(pricingRule);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PricingChangeLog)) {
            return false;
        }
        return getId() != null && getId().equals(((PricingChangeLog) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PricingChangeLog{" +
            "id=" + getId() +
            ", changedAt='" + getChangedAt() + "'" +
            ", changedByUsername='" + getChangedByUsername() + "'" +
            ", beforeJson='" + getBeforeJson() + "'" +
            ", afterJson='" + getAfterJson() + "'" +
            "}";
    }
}
