package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.service.dto.OrderDeliveryAttemptDTO;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderDeliveryAttempt} and its DTO {@link OrderDeliveryAttemptDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderDeliveryAttemptMapper extends EntityMapper<OrderDeliveryAttemptDTO, OrderDeliveryAttempt> {
    @Mapping(target = "order", source = "order", qualifiedByName = "shipmentOrderOrderCode")
    OrderDeliveryAttemptDTO toDto(OrderDeliveryAttempt s);

    @Named("shipmentOrderOrderCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    ShipmentOrderDTO toDtoShipmentOrderOrderCode(ShipmentOrder shipmentOrder);
}
