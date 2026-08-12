package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderEvent} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderEventDTO implements Serializable {

    private Long id;

    @NotNull
    private Instant eventAt;

    @NotNull
    @Size(max = 100)
    private String action;

    @Size(max = 255)
    private String detail;

    @NotNull
    @Size(max = 50)
    private String actorUsername;

    @NotNull
    private ShipmentOrderDTO order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getEventAt() {
        return eventAt;
    }

    public void setEventAt(Instant eventAt) {
        this.eventAt = eventAt;
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

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public ShipmentOrderDTO getOrder() {
        return order;
    }

    public void setOrder(ShipmentOrderDTO order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderEventDTO)) {
            return false;
        }

        OrderEventDTO orderEventDTO = (OrderEventDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderEventDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderEventDTO{" +
            "id=" + getId() +
            ", eventAt='" + getEventAt() + "'" +
            ", action='" + getAction() + "'" +
            ", detail='" + getDetail() + "'" +
            ", actorUsername='" + getActorUsername() + "'" +
            ", order=" + getOrder() +
            "}";
    }
}
