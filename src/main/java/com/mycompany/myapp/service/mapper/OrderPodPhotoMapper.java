package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.OrderPodPhoto;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.service.dto.OrderPodPhotoDTO;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderPodPhoto} and its DTO {@link OrderPodPhotoDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderPodPhotoMapper extends EntityMapper<OrderPodPhotoDTO, OrderPodPhoto> {
    @Mapping(target = "order", source = "order", qualifiedByName = "shipmentOrderOrderCode")
    OrderPodPhotoDTO toDto(OrderPodPhoto s);

    @Named("shipmentOrderOrderCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    ShipmentOrderDTO toDtoShipmentOrderOrderCode(ShipmentOrder shipmentOrder);
}
