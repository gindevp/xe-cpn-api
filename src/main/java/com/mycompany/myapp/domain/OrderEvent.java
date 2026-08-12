package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A OrderEvent.
 */
@Entity
@Table(name = "order_event")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @NotNull
    @Size(max = 100)
    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Size(max = 255)
    @Column(name = "detail", length = 255)
    private String detail;

    @NotNull
    @Size(max = 50)
    @Column(name = "actor_username", length = 50, nullable = false)
    private String actorUsername;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = {
            "issue",
            "returnRequest",
            "fareAdjustmentRequest",
            "senderCustomer",
            "fromOffice",
            "toOffice",
            "hubOffice",
            "finalToOffice",
            "currentTrip",
        },
        allowSetters = true
    )
    private ShipmentOrder order;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public OrderEvent id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getEventAt() {
        return this.eventAt;
    }

    public OrderEvent eventAt(Instant eventAt) {
        this.setEventAt(eventAt);
        return this;
    }

    public void setEventAt(Instant eventAt) {
        this.eventAt = eventAt;
    }

    public String getAction() {
        return this.action;
    }

    public OrderEvent action(String action) {
        this.setAction(action);
        return this;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return this.detail;
    }

    public OrderEvent detail(String detail) {
        this.setDetail(detail);
        return this;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getActorUsername() {
        return this.actorUsername;
    }

    public OrderEvent actorUsername(String actorUsername) {
        this.setActorUsername(actorUsername);
        return this;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public OrderEvent order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderEvent)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderEvent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderEvent{" +
            "id=" + getId() +
            ", eventAt='" + getEventAt() + "'" +
            ", action='" + getAction() + "'" +
            ", detail='" + getDetail() + "'" +
            ", actorUsername='" + getActorUsername() + "'" +
            "}";
    }
}
