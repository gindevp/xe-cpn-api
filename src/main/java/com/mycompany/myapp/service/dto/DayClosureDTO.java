package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.DayClosure} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DayClosureDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate businessDate;

    @NotNull
    private DayClosureStatus status;

    @NotNull
    @Size(max = 50)
    private String confirmedByUsername;

    @NotNull
    private Instant confirmedAt;

    @Size(max = 50)
    private String reopenedByUsername;

    private Instant reopenedAt;

    @NotNull
    private OfficeDTO office;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public DayClosureStatus getStatus() {
        return status;
    }

    public void setStatus(DayClosureStatus status) {
        this.status = status;
    }

    public String getConfirmedByUsername() {
        return confirmedByUsername;
    }

    public void setConfirmedByUsername(String confirmedByUsername) {
        this.confirmedByUsername = confirmedByUsername;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getReopenedByUsername() {
        return reopenedByUsername;
    }

    public void setReopenedByUsername(String reopenedByUsername) {
        this.reopenedByUsername = reopenedByUsername;
    }

    public Instant getReopenedAt() {
        return reopenedAt;
    }

    public void setReopenedAt(Instant reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public OfficeDTO getOffice() {
        return office;
    }

    public void setOffice(OfficeDTO office) {
        this.office = office;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DayClosureDTO)) {
            return false;
        }

        DayClosureDTO dayClosureDTO = (DayClosureDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, dayClosureDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DayClosureDTO{" +
            "id=" + getId() +
            ", businessDate='" + getBusinessDate() + "'" +
            ", status='" + getStatus() + "'" +
            ", confirmedByUsername='" + getConfirmedByUsername() + "'" +
            ", confirmedAt='" + getConfirmedAt() + "'" +
            ", reopenedByUsername='" + getReopenedByUsername() + "'" +
            ", reopenedAt='" + getReopenedAt() + "'" +
            ", office=" + getOffice() +
            "}";
    }
}
