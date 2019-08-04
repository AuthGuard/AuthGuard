package org.auther.service.impl;

import org.auther.dal.model.AccountDO;
import org.auther.service.model.AccountBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ServiceMapper {
    ServiceMapper INSTANCE = Mappers.getMapper(ServiceMapper.class);

    AccountDO toDO(AccountBO accountBO);
    AccountBO toBO(AccountDO accountDO);
}
