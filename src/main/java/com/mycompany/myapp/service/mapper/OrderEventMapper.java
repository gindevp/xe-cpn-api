package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderEvent} and its DTO {@link OrderEventDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderEventMapper extends EntityMapper<OrderEventDTO, OrderEvent> {
    @Mapping(target = "order", source = "order", qualifiedByName = "shipmentOrderOrderCode")
    OrderEventDTO toDto(OrderEvent s);

    @Named("shipmentOrderOrderCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    ShipmentOrderDTO toDtoShipmentOrderOrderCode(ShipmentOrder shipmentOrder);
}
