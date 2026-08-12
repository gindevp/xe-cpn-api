package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.DeliveryAttemptResult;
import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderDeliveryAttempt} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderDeliveryAttemptDTO implements Serializable {

    private Long id;

    @NotNull
    @Min(value = 1)
    private Integer attemptNo;

    @NotNull
    private Instant attemptAt;

    @NotNull
    private DeliveryAttemptResult result;

    @Size(max = 255)
    private String reason;

    @NotNull
    @Size(max = 50)
    private String handledByUsername;

    private DeliveryPartner deliveryPartner;

    @NotNull
    private ShipmentOrderDTO order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public Instant getAttemptAt() {
        return attemptAt;
    }

    public void setAttemptAt(Instant attemptAt) {
        this.attemptAt = attemptAt;
    }

    public DeliveryAttemptResult getResult() {
        return result;
    }

    public void setResult(DeliveryAttemptResult result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getHandledByUsername() {
        return handledByUsername;
    }

    public void setHandledByUsername(String handledByUsername) {
        this.handledByUsername = handledByUsername;
    }

    public DeliveryPartner getDeliveryPartner() {
        return deliveryPartner;
    }

    public void setDeliveryPartner(DeliveryPartner deliveryPartner) {
        this.deliveryPartner = deliveryPartner;
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
        if (!(o instanceof OrderDeliveryAttemptDTO)) {
            return false;
        }

        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = (OrderDeliveryAttemptDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderDeliveryAttemptDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderDeliveryAttemptDTO{" +
            "id=" + getId() +
            ", attemptNo=" + getAttemptNo() +
            ", attemptAt='" + getAttemptAt() + "'" +
            ", result='" + getResult() + "'" +
            ", reason='" + getReason() + "'" +
            ", handledByUsername='" + getHandledByUsername() + "'" +
            ", deliveryPartner='" + getDeliveryPartner() + "'" +
            ", order=" + getOrder() +
            "}";
    }
}
