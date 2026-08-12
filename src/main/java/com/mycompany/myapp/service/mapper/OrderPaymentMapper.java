package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.service.dto.OrderPaymentDTO;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderPayment} and its DTO {@link OrderPaymentDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderPaymentMapper extends EntityMapper<OrderPaymentDTO, OrderPayment> {
    @Mapping(target = "order", source = "order", qualifiedByName = "shipmentOrderOrderCode")
    OrderPaymentDTO toDto(OrderPayment s);

    @Named("shipmentOrderOrderCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    ShipmentOrderDTO toDtoShipmentOrderOrderCode(ShipmentOrder shipmentOrder);
}
