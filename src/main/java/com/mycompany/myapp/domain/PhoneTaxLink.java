package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/** Một MST gắn với một số điện thoại. Mỗi SĐT giữ tối đa 5 MST. */
@Entity
@Table(
    name = "phone_tax_link",
    uniqueConstraints = { @UniqueConstraint(name = "uk_phone_tax_link_phone_tax", columnNames = { "phone", "tax_code" }) }
)
public class PhoneTaxLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 20)
    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @NotNull
    @Size(max = 20)
    @Column(name = "tax_code", length = 20, nullable = false)
    private String taxCode;

    @NotNull
    @Size(max = 255)
    @Column(name = "company_name", length = 255, nullable = false)
    private String companyName;

    @Size(max = 500)
    @Column(name = "address", length = 500)
    private String address;

    @Size(max = 100)
    @Column(name = "email", length = 100)
    private String email;

    @Size(max = 100)
    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Size(max = 40)
    @Column(name = "from_order_code", length = 40)
    private String fromOrderCode;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getFromOrderCode() {
        return fromOrderCode;
    }

    public void setFromOrderCode(String fromOrderCode) {
        this.fromOrderCode = fromOrderCode;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
