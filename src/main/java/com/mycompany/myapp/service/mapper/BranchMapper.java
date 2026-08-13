package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Branch;
import com.mycompany.myapp.service.dto.BranchDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Branch} and its DTO {@link BranchDTO}.
 */
@Mapper(componentModel = "spring")
public interface BranchMapper extends EntityMapper<BranchDTO, Branch> {}
