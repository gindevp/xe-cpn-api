package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.DeliveryAttemptResult;
import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A OrderDeliveryAttempt.
 */
@Entity
@Table(name = "order_delivery_attempt")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderDeliveryAttempt implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Min(value = 1)
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @NotNull
    @Column(name = "attempt_at", nullable = false)
    private Instant attemptAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private DeliveryAttemptResult result;

    @Size(max = 255)
    @Column(name = "reason", length = 255)
    private String reason;

    @NotNull
    @Size(max = 50)
    @Column(name = "handled_by_username", length = 50, nullable = false)
    private String handledByUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_partner")
    private DeliveryPartner deliveryPartner;

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

    public OrderDeliveryAttempt id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAttemptNo() {
        return this.attemptNo;
    }

    public OrderDeliveryAttempt attemptNo(Integer attemptNo) {
        this.setAttemptNo(attemptNo);
        return this;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public Instant getAttemptAt() {
        return this.attemptAt;
    }

    public OrderDeliveryAttempt attemptAt(Instant attemptAt) {
        this.setAttemptAt(attemptAt);
        return this;
    }

    public void setAttemptAt(Instant attemptAt) {
        this.attemptAt = attemptAt;
    }

    public DeliveryAttemptResult getResult() {
        return this.result;
    }

    public OrderDeliveryAttempt result(DeliveryAttemptResult result) {
        this.setResult(result);
        return this;
    }

    public void setResult(DeliveryAttemptResult result) {
        this.result = result;
    }

    public String getReason() {
        return this.reason;
    }

    public OrderDeliveryAttempt reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getHandledByUsername() {
        return this.handledByUsername;
    }

    public OrderDeliveryAttempt handledByUsername(String handledByUsername) {
        this.setHandledByUsername(handledByUsername);
        return this;
    }

    public void setHandledByUsername(String handledByUsername) {
        this.handledByUsername = handledByUsername;
    }

    public DeliveryPartner getDeliveryPartner() {
        return this.deliveryPartner;
    }

    public OrderDeliveryAttempt deliveryPartner(DeliveryPartner deliveryPartner) {
        this.setDeliveryPartner(deliveryPartner);
        return this;
    }

    public void setDeliveryPartner(DeliveryPartner deliveryPartner) {
        this.deliveryPartner = deliveryPartner;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public OrderDeliveryAttempt order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderDeliveryAttempt)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderDeliveryAttempt) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderDeliveryAttempt{" +
            "id=" + getId() +
            ", attemptNo=" + getAttemptNo() +
            ", attemptAt='" + getAttemptAt() + "'" +
            ", result='" + getResult() + "'" +
            ", reason='" + getReason() + "'" +
            ", handledByUsername='" + getHandledByUsername() + "'" +
            ", deliveryPartner='" + getDeliveryPartner() + "'" +
            "}";
    }
}
