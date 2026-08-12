package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.service.dto.OrderIssueDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OrderIssue} and its DTO {@link OrderIssueDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderIssueMapper extends EntityMapper<OrderIssueDTO, OrderIssue> {}
