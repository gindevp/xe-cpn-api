package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderLeg;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.dto.OrderLegDTO;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import com.mycompany.myapp.service.dto.TripDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderLeg} and its DTO {@link OrderLegDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderLegMapper extends EntityMapper<OrderLegDTO, OrderLeg> {
    @Mapping(target = "order", source = "order", qualifiedByName = "shipmentOrderOrderCode")
    @Mapping(target = "fromOffice", source = "fromOffice", qualifiedByName = "officeCode")
    @Mapping(target = "toOffice", source = "toOffice", qualifiedByName = "officeCode")
    @Mapping(target = "trip", source = "trip", qualifiedByName = "tripTripCode")
    OrderLegDTO toDto(OrderLeg s);

    @Named("shipmentOrderOrderCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    ShipmentOrderDTO toDtoShipmentOrderOrderCode(ShipmentOrder shipmentOrder);

    @Named("officeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    OfficeDTO toDtoOfficeCode(Office office);

    @Named("tripTripCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tripCode", source = "tripCode")
    TripDTO toDtoTripTripCode(Trip trip);
}
