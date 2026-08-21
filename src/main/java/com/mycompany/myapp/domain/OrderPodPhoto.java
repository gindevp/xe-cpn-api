package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A OrderPodPhoto.
 */
@Entity
@Table(name = "order_pod_photo")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderPodPhoto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Lob
    @Column(name = "photo_url", nullable = false, columnDefinition = "LONGTEXT")
    private String photoUrl;

    @NotNull
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "captured_by_username", length = 50, nullable = false)
    private String capturedByUsername;

    @Min(value = 1)
    @Max(value = 3)
    @Column(name = "sequence_no")
    private Integer sequenceNo;

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

    public OrderPodPhoto id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhotoUrl() {
        return this.photoUrl;
    }

    public OrderPodPhoto photoUrl(String photoUrl) {
        this.setPhotoUrl(photoUrl);
        return this;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Instant getCapturedAt() {
        return this.capturedAt;
    }

    public OrderPodPhoto capturedAt(Instant capturedAt) {
        this.setCapturedAt(capturedAt);
        return this;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getCapturedByUsername() {
        return this.capturedByUsername;
    }

    public OrderPodPhoto capturedByUsername(String capturedByUsername) {
        this.setCapturedByUsername(capturedByUsername);
        return this;
    }

    public void setCapturedByUsername(String capturedByUsername) {
        this.capturedByUsername = capturedByUsername;
    }

    public Integer getSequenceNo() {
        return this.sequenceNo;
    }

    public OrderPodPhoto sequenceNo(Integer sequenceNo) {
        this.setSequenceNo(sequenceNo);
        return this;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public ShipmentOrder getOrder() {
        return this.order;
    }

    public void setOrder(ShipmentOrder shipmentOrder) {
        this.order = shipmentOrder;
    }

    public OrderPodPhoto order(ShipmentOrder shipmentOrder) {
        this.setOrder(shipmentOrder);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderPodPhoto)) {
            return false;
        }
        return getId() != null && getId().equals(((OrderPodPhoto) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderPodPhoto{" +
            "id=" + getId() +
            ", photoUrl='" + getPhotoUrl() + "'" +
            ", capturedAt='" + getCapturedAt() + "'" +
            ", capturedByUsername='" + getCapturedByUsername() + "'" +
            ", sequenceNo=" + getSequenceNo() +
            "}";
    }
}
