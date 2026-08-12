package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.OrderReturnRequest;
import com.mycompany.myapp.service.dto.OrderReturnRequestDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderReturnRequest} and its DTO {@link OrderReturnRequestDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderReturnRequestMapper extends EntityMapper<OrderReturnRequestDTO, OrderReturnRequest> {}
