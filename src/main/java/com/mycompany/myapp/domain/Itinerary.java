package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Master "Lộ trình" under a {@link Branch}. Named Itinerary to avoid clash with {@link Route}.
 */
@Entity
@Table(name = "itinerary")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Itinerary implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 2, max = 60)
    @Column(name = "code", length = 60, nullable = false, unique = true)
    private String code;

    @NotNull
    @Size(max = 120)
    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Size(max = 120)
    @Column(name = "departure_point", length = 120)
    private String departurePoint;

    @Size(max = 120)
    @Column(name = "destination_point", length = 120)
    private String destinationPoint;

    @Size(max = 120)
    @Column(name = "route_direction", length = 120)
    private String routeDirection;

    @Column(name = "route_type")
    private Integer routeType;

    @Column(name = "price", precision = 21, scale = 2)
    private BigDecimal price;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "display_order")
    private Integer displayOrder;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Size(max = 255)
    @Column(name = "shortest_itinerary", length = 255)
    private String shortestItinerary;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Branch getBranch() {
        return this.branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getDeparturePoint() {
        return this.departurePoint;
    }

    public void setDeparturePoint(String departurePoint) {
        this.departurePoint = departurePoint;
    }

    public String getDestinationPoint() {
        return this.destinationPoint;
    }

    public void setDestinationPoint(String destinationPoint) {
        this.destinationPoint = destinationPoint;
    }

    public String getRouteDirection() {
        return this.routeDirection;
    }

    public void setRouteDirection(String routeDirection) {
        this.routeDirection = routeDirection;
    }

    public Integer getRouteType() {
        return this.routeType;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return this.active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getShortestItinerary() {
        return this.shortestItinerary;
    }

    public void setShortestItinerary(String shortestItinerary) {
        this.shortestItinerary = shortestItinerary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Itinerary)) {
            return false;
        }
        return getId() != null && getId().equals(((Itinerary) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Itinerary{" +
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
