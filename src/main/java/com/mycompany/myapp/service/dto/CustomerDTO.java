package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.CustomerType;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Customer} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CustomerDTO implements Serializable {

    private Long id;

    @NotNull
    private CustomerType customerType;

    @NotNull
    @Pattern(regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$")
    private String phone;

    @Size(max = 100)
    private String name;

    private Instant lastOrderAt;

    @Min(value = 0)
    private Integer orderCount;

    @NotNull
    private Boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getLastOrderAt() {
        return lastOrderAt;
    }

    public void setLastOrderAt(Instant lastOrderAt) {
        this.lastOrderAt = lastOrderAt;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomerDTO)) {
            return false;
        }

        CustomerDTO customerDTO = (CustomerDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, customerDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CustomerDTO{" +
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
