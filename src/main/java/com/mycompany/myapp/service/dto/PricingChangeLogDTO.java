package com.mycompany.myapp.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.PricingChangeLog} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PricingChangeLogDTO implements Serializable {

    private Long id;

    @NotNull
    private Instant changedAt;

    @NotNull
    @Size(max = 50)
    private String changedByUsername;

    @Lob
    private String beforeJson;

    @Lob
    private String afterJson;

    @NotNull
    private PricingRuleDTO pricingRule;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedByUsername() {
        return changedByUsername;
    }

    public void setChangedByUsername(String changedByUsername) {
        this.changedByUsername = changedByUsername;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public void setBeforeJson(String beforeJson) {
        this.beforeJson = beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public void setAfterJson(String afterJson) {
        this.afterJson = afterJson;
    }

    public PricingRuleDTO getPricingRule() {
        return pricingRule;
    }

    public void setPricingRule(PricingRuleDTO pricingRule) {
        this.pricingRule = pricingRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PricingChangeLogDTO)) {
            return false;
        }

        PricingChangeLogDTO pricingChangeLogDTO = (PricingChangeLogDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, pricingChangeLogDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PricingChangeLogDTO{" +
            "id=" + getId() +
            ", changedAt='" + getChangedAt() + "'" +
            ", changedByUsername='" + getChangedByUsername() + "'" +
            ", beforeJson='" + getBeforeJson() + "'" +
            ", afterJson='" + getAfterJson() + "'" +
            ", pricingRule=" + getPricingRule() +
            "}";
    }
}
