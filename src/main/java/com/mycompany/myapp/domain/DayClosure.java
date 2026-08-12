package com.mycompany.myapp.domain;

import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A DayClosure.
 */
@Entity
@Table(name = "day_closure")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DayClosure implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DayClosureStatus status;

    @NotNull
    @Size(max = 50)
    @Column(name = "confirmed_by_username", length = 50, nullable = false)
    private String confirmedByUsername;

    @NotNull
    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    @Size(max = 50)
    @Column(name = "reopened_by_username", length = 50)
    private String reopenedByUsername;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @ManyToOne(optional = false)
    @NotNull
    private Office office;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public DayClosure id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getBusinessDate() {
        return this.businessDate;
    }

    public DayClosure businessDate(LocalDate businessDate) {
        this.setBusinessDate(businessDate);
        return this;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public DayClosureStatus getStatus() {
        return this.status;
    }

    public DayClosure status(DayClosureStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(DayClosureStatus status) {
        this.status = status;
    }

    public String getConfirmedByUsername() {
        return this.confirmedByUsername;
    }

    public DayClosure confirmedByUsername(String confirmedByUsername) {
        this.setConfirmedByUsername(confirmedByUsername);
        return this;
    }

    public void setConfirmedByUsername(String confirmedByUsername) {
        this.confirmedByUsername = confirmedByUsername;
    }

    public Instant getConfirmedAt() {
        return this.confirmedAt;
    }

    public DayClosure confirmedAt(Instant confirmedAt) {
        this.setConfirmedAt(confirmedAt);
        return this;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getReopenedByUsername() {
        return this.reopenedByUsername;
    }

    public DayClosure reopenedByUsername(String reopenedByUsername) {
        this.setReopenedByUsername(reopenedByUsername);
        return this;
    }

    public void setReopenedByUsername(String reopenedByUsername) {
        this.reopenedByUsername = reopenedByUsername;
    }

    public Instant getReopenedAt() {
        return this.reopenedAt;
    }

    public DayClosure reopenedAt(Instant reopenedAt) {
        this.setReopenedAt(reopenedAt);
        return this;
    }

    public void setReopenedAt(Instant reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public Office getOffice() {
        return this.office;
    }

    public void setOffice(Office office) {
        this.office = office;
    }

    public DayClosure office(Office office) {
        this.setOffice(office);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DayClosure)) {
            return false;
        }
        return getId() != null && getId().equals(((DayClosure) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DayClosure{" +
            "id=" + getId() +
            ", businessDate='" + getBusinessDate() + "'" +
            ", status='" + getStatus() + "'" +
            ", confirmedByUsername='" + getConfirmedByUsername() + "'" +
            ", confirmedAt='" + getConfirmedAt() + "'" +
            ", reopenedByUsername='" + getReopenedByUsername() + "'" +
            ", reopenedAt='" + getReopenedAt() + "'" +
            "}";
    }
}
