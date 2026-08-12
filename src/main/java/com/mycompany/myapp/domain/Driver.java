package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * A Driver.
 */
@Entity
@Table(name = "driver")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Driver implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 30)
    @Column(name = "driver_code", length = 30, nullable = false, unique = true)
    private String driverCode;

    @NotNull
    @Size(max = 100)
    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Driver id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDriverCode() {
        return this.driverCode;
    }

    public Driver driverCode(String driverCode) {
        this.setDriverCode(driverCode);
        return this;
    }

    public void setDriverCode(String driverCode) {
        this.driverCode = driverCode;
    }

    public String getFullName() {
        return this.fullName;
    }

    public Driver fullName(String fullName) {
        this.setFullName(fullName);
        return this;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return this.phone;
    }

    public Driver phone(String phone) {
        this.setPhone(phone);
        return this;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Driver active(Boolean active) {
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
        if (!(o instanceof Driver)) {
            return false;
        }
        return getId() != null && getId().equals(((Driver) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Driver{" +
            "id=" + getId() +
            ", driverCode='" + getDriverCode() + "'" +
            ", fullName='" + getFullName() + "'" +
            ", phone='" + getPhone() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
