package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.OfficeType;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Office} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OfficeDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 2, max = 20)
    private String code;

    @NotNull
    @Size(max = 100)
    private String name;

    @NotNull
    private OfficeType officeType = OfficeType.BRANCH;

    @NotNull
    private Boolean isHub = false;

    @NotNull
    private Boolean active = true;

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

    public OfficeType getOfficeType() {
        return officeType;
    }

    public void setOfficeType(OfficeType officeType) {
        this.officeType = officeType != null ? officeType : OfficeType.BRANCH;
    }

    public Boolean getIsHub() {
        return isHub;
    }

    public void setIsHub(Boolean isHub) {
        this.isHub = isHub != null ? isHub : Boolean.FALSE;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active != null ? active : Boolean.TRUE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OfficeDTO)) {
            return false;
        }

        OfficeDTO officeDTO = (OfficeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, officeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OfficeDTO{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", officeType='" + getOfficeType() + "'" +
            ", isHub='" + getIsHub() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
