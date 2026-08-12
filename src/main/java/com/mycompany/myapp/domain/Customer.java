package com.mycompany.myapp.domain;

import com.mycompany.myapp.domain.enumeration.CustomerType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A Customer.
 */
@Entity
@Table(name = "customer")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private CustomerType customerType;

    @NotNull
    @Pattern(regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$")
    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Size(max = 100)
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "last_order_at")
    private Instant lastOrderAt;

    @Min(value = 0)
    @Column(name = "order_count")
    private Integer orderCount;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Customer id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerType getCustomerType() {
        return this.customerType;
    }

    public Customer customerType(CustomerType customerType) {
        this.setCustomerType(customerType);
        return this;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    public String getPhone() {
        return this.phone;
    }

    public Customer phone(String phone) {
        this.setPhone(phone);
        return this;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return this.name;
    }

    public Customer name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getLastOrderAt() {
        return this.lastOrderAt;
    }

    public Customer lastOrderAt(Instant lastOrderAt) {
        this.setLastOrderAt(lastOrderAt);
        return this;
    }

    public void setLastOrderAt(Instant lastOrderAt) {
        this.lastOrderAt = lastOrderAt;
    }

    public Integer getOrderCount() {
        return this.orderCount;
    }

    public Customer orderCount(Integer orderCount) {
        this.setOrderCount(orderCount);
        return this;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Customer active(Boolean active) {
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
        if (!(o instanceof Customer)) {
            return false;
        }
        return getId() != null && getId().equals(((Customer) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Customer{" +
            "id=" + getId() +
            ", customerType='" + getCustomerType() + "'" +
            ", phone='" + getPhone() + "'" +
            ", name='" + getName() + "'" +
            ", lastOrderAt='" + getLastOrderAt() + "'" +
            ", orderCount=" + getOrderCount() +
            ", active='" + getActive() + "'" +
            "}";
    }
}
