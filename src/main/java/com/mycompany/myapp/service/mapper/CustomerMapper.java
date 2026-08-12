package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Customer;
import com.mycompany.myapp.service.dto.CustomerDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Customer} and its DTO {@link CustomerDTO}.
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper extends EntityMapper<CustomerDTO, Customer> {}
