package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.dto.ReceiptDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Receipt} and its DTO {@link ReceiptDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReceiptMapper extends EntityMapper<ReceiptDTO, Receipt> {
    @Mapping(target = "office", source = "office", qualifiedByName = "officeCode")
    ReceiptDTO toDto(Receipt s);

    @Named("officeCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    OfficeDTO toDtoOfficeCode(Office office);
}
