package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Customer;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderFareAdjustmentRequest;
import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.OrderReturnRequest;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.service.dto.CustomerDTO;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.dto.OrderFareAdjustmentRequestDTO;
import com.mycompany.myapp.service.dto.OrderIssueDTO;
import com.mycompany.myapp.service.dto.OrderReturnRequestDTO;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import com.mycompany.myapp.service.dto.TripDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ShipmentOrder} and its DTO {@link ShipmentOrderDTO}.
 */
@Mapper(componentModel = "spring")
public interface ShipmentOrderMapper extends EntityMapper<ShipmentOrderDTO, ShipmentOrder> {
    @Mapping(target = "issue", source = "issue", qualifiedByName = "orderIssueId")
    @Mapping(target = "returnRequest", source = "returnRequest", qualifiedByName = "orderReturnRequestId")
    @Mapping(target = "fareAdjustmentRequest", source = "fareAdjustmentRequest", qualifiedByName = "orderFareAdjustmentRequestId")
    @Mapping(target = "senderCustomer", source = "senderCustomer", qualifiedByName = "customerPhone")
    @Mapping(target = "fromOffice", source = "fromOffice", qualifiedByName = "officeCode")
    @Mapping(target = "toOffice", source = "toOffice", qualifiedByName = "officeCode")
    @Mapping(target = "hubOffice", source = "hubOffice", qualifiedByName = "officeCode")
    @Mapping(target = "finalToOffice", source = "finalToOffice", qualifiedByName = "officeCode")
    @Mapping(target = "currentTrip", source = "currentTrip", qualifiedByName = "tripTripCode")
    ShipmentOrderDTO toDto(ShipmentOrder s);

    @Named("orderIssueId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    OrderIssueDTO toDtoOrderIssueId(OrderIssue orderIssue);

    @Named("orderReturnRequestId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    OrderReturnRequestDTO toDtoOrderReturnRequestId(OrderReturnRequest orderReturnRequest);

    @Named("orderFareAdjustmentRequestId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    OrderFareAdjustmentRequestDTO toDtoOrderFareAdjustmentRequestId(OrderFareAdjustmentRequest orderFareAdjustmentRequest);

    @Named("customerPhone")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "phone", source = "phone")
    CustomerDTO toDtoCustomerPhone(Customer customer);

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
