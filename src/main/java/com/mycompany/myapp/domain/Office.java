package com.mycompany.myapp.domain;

import com.mycompany.myapp.domain.enumeration.OfficeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A Office.
 */
@Entity
@Table(name = "office")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Office implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 2, max = 20)
    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "office_type", nullable = false)
    private OfficeType officeType;

    @NotNull
    @Column(name = "is_hub", nullable = false)
    private Boolean isHub;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    /** ID danh mục nguồn (Excel / hệ thống VP). */
    @Column(name = "source_id", unique = true)
    private Long sourceId;

    @Size(max = 500)
    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Office id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public Office code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public Office name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OfficeType getOfficeType() {
        return this.officeType;
    }

    public Office officeType(OfficeType officeType) {
        this.setOfficeType(officeType);
        return this;
    }

    public void setOfficeType(OfficeType officeType) {
        this.officeType = officeType;
    }

    public Boolean getIsHub() {
        return this.isHub;
    }

    public Office isHub(Boolean isHub) {
        this.setIsHub(isHub);
        return this;
    }

    public void setIsHub(Boolean isHub) {
        this.isHub = isHub;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Office active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getSourceId() {
        return this.sourceId;
    }

    public Office sourceId(Long sourceId) {
        this.setSourceId(sourceId);
        return this;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getAddress() {
        return this.address;
    }

    public Office address(String address) {
        this.setAddress(address);
        return this;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return this.latitude;
    }

    public Office latitude(BigDecimal latitude) {
        this.setLatitude(latitude);
        return this;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return this.longitude;
    }

    public Office longitude(BigDecimal longitude) {
        this.setLongitude(longitude);
        return this;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Office)) {
            return false;
        }
        return getId() != null && getId().equals(((Office) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Office{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", officeType='" + getOfficeType() + "'" +
            ", isHub='" + getIsHub() + "'" +
            ", active='" + getActive() + "'" +
            ", sourceId=" + getSourceId() +
            ", address='" + getAddress() + "'" +
            ", latitude=" + getLatitude() +
            ", longitude=" + getLongitude() +
            "}";
    }
}
