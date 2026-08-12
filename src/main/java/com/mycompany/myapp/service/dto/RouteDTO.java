package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Route} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RouteDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 3, max = 30)
    private String code;

    @NotNull
    @Size(max = 100)
    private String name;

    @NotNull
    private Boolean active;

    @NotNull
    private OfficeDTO fromOffice;

    @NotNull
    private OfficeDTO toOffice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OfficeDTO getFromOffice() {
        return fromOffice;
    }

    public void setFromOffice(OfficeDTO fromOffice) {
        this.fromOffice = fromOffice;
    }

    public OfficeDTO getToOffice() {
        return toOffice;
    }

    public void setToOffice(OfficeDTO toOffice) {
        this.toOffice = toOffice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteDTO)) {
            return false;
        }

        RouteDTO routeDTO = (RouteDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, routeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RouteDTO{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", active='" + getActive() + "'" +
            ", fromOffice=" + getFromOffice() +
            ", toOffice=" + getToOffice() +
            "}";
    }
}
