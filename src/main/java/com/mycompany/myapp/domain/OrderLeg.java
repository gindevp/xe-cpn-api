package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.LegStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A OrderLeg.
 */
@Entity
@Table(name = "order_leg")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderLeg implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Min(value = 0)
    @Column(name = "leg_index", nullable = false)
    private Integer legIndex;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LegStatus status;

    @Column(name = "departed_at")
    private Instant departedAt;

    @Column(name = "arrived_at")
    private Instant arrivedAt;

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

    @ManyToOne(optional = false)
    @NotNull
    private Office fromOffice;

    @ManyToOne(optional = false)
    @NotNull
    private Office toOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "office", "route", "vehicle", "driver" }, allowSetters = true)
    private Trip trip;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public OrderLeg id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLegIndex() {
        return this.legIndex;
    }

    public OrderLeg legIndex(Integer legIndex) {
        this.setLegIndex(legIndex);
        return this;
    }

    public void setLegIndex(Integer legIndex) {
        this.legIndex = legIndex;
    }

    public LegStatus getStatus() {
        return this.status;
    }

    public OrderLeg status(LegStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(LegStatus status) {
        this.status = status;
    }

    public Instant getDepartedAt() {
        return this.departedAt;
    }

    public OrderLeg departedAt(Instant departedAt) {
        this.setDepartedAt(departedAt);
        return this;
    }

    public void setDepartedAt(Instant departedAt) {
        this.departedAt = departedAt;
    }

    public Instant getArrivedAt() {
        return this.arrivedAt;
    }

    public OrderLeg arrivedAt(Instant arrivedAt) {
        this.setArrivedAt(arrivedAt);
        return this;
    }

    public void setArrivedAt(Instant arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public OrderLeg order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    public Office getFromOffice() {
        return this.fromOffice;
    }

    public void setFromOffice(Office office) {
        this.fromOffice = office;
    }

    public OrderLeg fromOffice(Office office) {
        this.setFromOffice(office);
        return this;
    }

    public Office getToOffice() {
        return this.toOffice;
    }

    public void setToOffice(Office office) {
        this.toOffice = office;
    }

    public OrderLeg toOffice(Office office) {
        this.setToOffice(office);
        return this;
    }

    public Trip getTrip() {
        return this.trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public OrderLeg trip(Trip trip) {
        this.setTrip(trip);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderLeg)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderLeg) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderLeg{" +
            "id=" + getId() +
            ", legIndex=" + getLegIndex() +
            ", status='" + getStatus() + "'" +
            ", departedAt='" + getDepartedAt() + "'" +
            ", arrivedAt='" + getArrivedAt() + "'" +
            "}";
    }
}
