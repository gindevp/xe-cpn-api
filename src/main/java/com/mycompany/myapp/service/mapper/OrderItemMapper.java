package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.OrderItem;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.service.dto.OrderItemDTO;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderItem} and its DTO {@link OrderItemDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderItemMapper extends EntityMapper<OrderItemDTO, OrderItem> {
    @Mapping(target = "order", source = "order", qualifiedByName = "shipmentOrderOrderCode")
    OrderItemDTO toDto(OrderItem s);

    @Named("shipmentOrderOrderCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    ShipmentOrderDTO toDtoShipmentOrderOrderCode(ShipmentOrder shipmentOrder);
}
