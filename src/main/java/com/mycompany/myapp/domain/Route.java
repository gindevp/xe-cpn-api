package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * A Route.
 */
@Entity
@Table(name = "route")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Route implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 3, max = 30)
    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @ManyToOne(optional = false)
    @NotNull
    private Office fromOffice;

    @ManyToOne(optional = false)
    @NotNull
    private Office toOffice;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Route id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public Route code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public Route name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Route active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Office getFromOffice() {
        return this.fromOffice;
    }

    public void setFromOffice(Office office) {
        this.fromOffice = office;
    }

    public Route fromOffice(Office office) {
        this.setFromOffice(office);
        return this;
    }

    public Office getToOffice() {
        return this.toOffice;
    }

    public void setToOffice(Office office) {
        this.toOffice = office;
    }

    public Route toOffice(Office office) {
        this.setToOffice(office);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Route)) {
            return false;
        }
        return getId() != null && getId().equals(((Route) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Route{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
