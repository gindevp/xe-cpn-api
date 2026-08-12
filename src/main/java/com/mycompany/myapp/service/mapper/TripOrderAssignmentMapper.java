package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.TripOrderAssignment;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import com.mycompany.myapp.service.dto.TripDTO;
import com.mycompany.myapp.service.dto.TripOrderAssignmentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link TripOrderAssignment} and its DTO {@link TripOrderAssignmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface TripOrderAssignmentMapper extends EntityMapper<TripOrderAssignmentDTO, TripOrderAssignment> {
    @Mapping(target = "trip", source = "trip", qualifiedByName = "tripTripCode")
    @Mapping(target = "order", source = "order", qualifiedByName = "shipmentOrderOrderCode")
    TripOrderAssignmentDTO toDto(TripOrderAssignment s);

    @Named("tripTripCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tripCode", source = "tripCode")
    TripDTO toDtoTripTripCode(Trip trip);

    @Named("shipmentOrderOrderCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    ShipmentOrderDTO toDtoShipmentOrderOrderCode(ShipmentOrder shipmentOrder);
}
