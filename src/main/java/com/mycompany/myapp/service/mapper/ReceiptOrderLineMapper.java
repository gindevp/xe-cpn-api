package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ReceiptOrderLine;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.service.dto.ReceiptDTO;
import com.mycompany.myapp.service.dto.ReceiptOrderLineDTO;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReceiptOrderLine} and its DTO {@link ReceiptOrderLineDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReceiptOrderLineMapper extends EntityMapper<ReceiptOrderLineDTO, ReceiptOrderLine> {
    @Mapping(target = "receipt", source = "receipt", qualifiedByName = "receiptReceiptCode")
    @Mapping(target = "order", source = "order", qualifiedByName = "shipmentOrderOrderCode")
    ReceiptOrderLineDTO toDto(ReceiptOrderLine s);

    @Named("receiptReceiptCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "receiptCode", source = "receiptCode")
    ReceiptDTO toDtoReceiptReceiptCode(Receipt receipt);

    @Named("shipmentOrderOrderCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    ShipmentOrderDTO toDtoShipmentOrderOrderCode(ShipmentOrder shipmentOrder);
}
