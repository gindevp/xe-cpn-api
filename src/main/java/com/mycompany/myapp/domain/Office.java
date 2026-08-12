package com.mycompany.myapp.domain;

import com.mycompany.myapp.domain.enumeration.OfficeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

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
    @Column(name = "code", length = 20, nullable = false, unique = true)
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
            "}";
    }
}
