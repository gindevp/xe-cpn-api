package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Driver;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.service.dto.DriverDTO;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.dto.RouteDTO;
import com.mycompany.myapp.service.dto.TripDTO;
import com.mycompany.myapp.service.dto.VehicleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Trip} and its DTO {@link TripDTO}.
 */
@Mapper(componentModel = "spring")
public interface TripMapper extends EntityMapper<TripDTO, Trip> {
    @Mapping(target = "office", source = "office", qualifiedByName = "officeCode")
    @Mapping(target = "route", source = "route", qualifiedByName = "routeCode")
    @Mapping(target = "vehicle", source = "vehicle", qualifiedByName = "vehiclePlateNumber")
    @Mapping(target = "driver", source = "driver", qualifiedByName = "driverFullName")
    TripDTO toDto(Trip s);

    @Named("officeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    OfficeDTO toDtoOfficeCode(Office office);

    @Named("routeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    RouteDTO toDtoRouteCode(Route route);

    @Named("vehiclePlateNumber")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "plateNumber", source = "plateNumber")
    VehicleDTO toDtoVehiclePlateNumber(Vehicle vehicle);

    @Named("driverFullName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fullName", source = "fullName")
    DriverDTO toDtoDriverFullName(Driver driver);
}
