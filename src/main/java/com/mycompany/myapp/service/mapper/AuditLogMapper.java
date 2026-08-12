package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.AuditLog;
import com.mycompany.myapp.service.dto.AuditLogDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AuditLog} and its DTO {@link AuditLogDTO}.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper extends EntityMapper<AuditLogDTO, AuditLog> {}
