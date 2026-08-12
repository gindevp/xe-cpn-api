package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.LegStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderLeg} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderLegDTO implements Serializable {

    private Long id;

    @NotNull
    @Min(value = 0)
    private Integer legIndex;

    @NotNull
    private LegStatus status;

    private Instant departedAt;

    private Instant arrivedAt;

    @NotNull
    private ShipmentOrderDTO order;

    @NotNull
    private OfficeDTO fromOffice;

    @NotNull
    private OfficeDTO toOffice;

    private TripDTO trip;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLegIndex() {
        return legIndex;
    }

    public void setLegIndex(Integer legIndex) {
        this.legIndex = legIndex;
    }

    public LegStatus getStatus() {
        return status;
    }

    public void setStatus(LegStatus status) {
        this.status = status;
    }

    public Instant getDepartedAt() {
        return departedAt;
    }

    public void setDepartedAt(Instant departedAt) {
        this.departedAt = departedAt;
    }

    public Instant getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(Instant arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public ShipmentOrderDTO getOrder() {
        return order;
    }

    public void setOrder(ShipmentOrderDTO order) {
        this.order = order;
    }

    public OfficeDTO getFromOffice() {
        return fromOffice;
    }

    public void setFromOffice(OfficeDTO fromOffice) {
        this.fromOffice = fromOffice;
    }

    public OfficeDTO getToOffice() {
        return toOffice;
    }

    public void setToOffice(OfficeDTO toOffice) {
        this.toOffice = toOffice;
    }

    public TripDTO getTrip() {
        return trip;
    }

    public void setTrip(TripDTO trip) {
        this.trip = trip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderLegDTO)) {
            return false;
        }

        OrderLegDTO orderLegDTO = (OrderLegDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderLegDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderLegDTO{" +
            "id=" + getId() +
            ", legIndex=" + getLegIndex() +
            ", status='" + getStatus() + "'" +
            ", departedAt='" + getDepartedAt() + "'" +
            ", arrivedAt='" + getArrivedAt() + "'" +
            ", order=" + getOrder() +
            ", fromOffice=" + getFromOffice() +
            ", toOffice=" + getToOffice() +
            ", trip=" + getTrip() +
            "}";
    }
}
