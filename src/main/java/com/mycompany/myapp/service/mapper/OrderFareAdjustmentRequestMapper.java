package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.OrderFareAdjustmentRequest;
import com.mycompany.myapp.service.dto.OrderFareAdjustmentRequestDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderFareAdjustmentRequest} and its DTO {@link OrderFareAdjustmentRequestDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderFareAdjustmentRequestMapper extends EntityMapper<OrderFareAdjustmentRequestDTO, OrderFareAdjustmentRequest> {}
