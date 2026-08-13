package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Itinerary} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ItineraryDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 2, max = 60)
    private String code;

    @NotNull
    @Size(max = 120)
    private String name;

    @NotNull
    private BranchDTO branch;

    private String departurePoint;
    private String destinationPoint;
    private String routeDirection;
    private Integer routeType;
    private BigDecimal price;
    private Integer priority;
    private Integer displayOrder;

    @NotNull
    private Boolean active;

    private String shortestItinerary;

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

    public BranchDTO getBranch() {
        return branch;
    }

    public void setBranch(BranchDTO branch) {
        this.branch = branch;
    }

    public String getDeparturePoint() {
        return departurePoint;
    }

    public void setDeparturePoint(String departurePoint) {
        this.departurePoint = departurePoint;
    }

    public String getDestinationPoint() {
        return destinationPoint;
    }

    public void setDestinationPoint(String destinationPoint) {
        this.destinationPoint = destinationPoint;
    }

    public String getRouteDirection() {
        return routeDirection;
    }

    public void setRouteDirection(String routeDirection) {
        this.routeDirection = routeDirection;
    }

    public Integer getRouteType() {
        return routeType;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getShortestItinerary() {
        return shortestItinerary;
    }

    public void setShortestItinerary(String shortestItinerary) {
        this.shortestItinerary = shortestItinerary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItineraryDTO)) {
            return false;
        }
        ItineraryDTO that = (ItineraryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return (
            "ItineraryDTO{" +
            "id=" +
            getId() +
            ", code='" +
            getCode() +
            "'" +
            ", name='" +
            getName() +
            "'" +
            ", active='" +
            getActive() +
            "'" +
            "}"
        );
    }
}
