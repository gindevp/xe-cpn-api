package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.OrderPodPhoto} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderPodPhotoDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 500)
    private String photoUrl;

    @NotNull
    private Instant capturedAt;

    @NotNull
    @Size(max = 50)
    private String capturedByUsername;

    @Min(value = 1)
    @Max(value = 3)
    private Integer sequenceNo;

    @NotNull
    private ShipmentOrderDTO order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getCapturedByUsername() {
        return capturedByUsername;
    }

    public void setCapturedByUsername(String capturedByUsername) {
        this.capturedByUsername = capturedByUsername;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
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
        if (!(o instanceof OrderPodPhotoDTO)) {
            return false;
        }

        OrderPodPhotoDTO orderPodPhotoDTO = (OrderPodPhotoDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderPodPhotoDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderPodPhotoDTO{" +
            "id=" + getId() +
            ", photoUrl='" + getPhotoUrl() + "'" +
            ", capturedAt='" + getCapturedAt() + "'" +
            ", capturedByUsername='" + getCapturedByUsername() + "'" +
            ", sequenceNo=" + getSequenceNo() +
            ", order=" + getOrder() +
            "}";
    }
}
