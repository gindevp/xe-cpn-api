package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A Vehicle.
 */
@Entity
@Table(name = "vehicle")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Vehicle implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 20)
    @Column(name = "plate_number", length = 20, nullable = false, unique = true)
    private String plateNumber;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "capacity_kg", precision = 21, scale = 2, nullable = false)
    private BigDecimal capacityKg;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Vehicle id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlateNumber() {
        return this.plateNumber;
    }

    public Vehicle plateNumber(String plateNumber) {
        this.setPlateNumber(plateNumber);
        return this;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public BigDecimal getCapacityKg() {
        return this.capacityKg;
    }

    public Vehicle capacityKg(BigDecimal capacityKg) {
        this.setCapacityKg(capacityKg);
        return this;
    }

    public void setCapacityKg(BigDecimal capacityKg) {
        this.capacityKg = capacityKg;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Vehicle active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle)) {
            return false;
        }
        return getId() != null && getId().equals(((Vehicle) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Vehicle{" +
            "id=" + getId() +
            ", plateNumber='" + getPlateNumber() + "'" +
            ", capacityKg=" + getCapacityKg() +
            ", active='" + getActive() + "'" +
            "}";
    }
}
