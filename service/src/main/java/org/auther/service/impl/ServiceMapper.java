package org.auther.service.impl;

import org.auther.dal.model.AccountDO;
import org.auther.dal.model.PermissionDO;
import org.auther.dal.model.PermissionGroupDO;
import org.auther.service.PermissionGroupBO;
import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ServiceMapper {
    ServiceMapper INSTANCE = Mappers.getMapper(ServiceMapper.class);

    AccountDO toDO(AccountBO accountBO);
    AccountBO toBO(AccountDO accountDO);

    PermissionGroupDO toDO(PermissionGroupBO permissionGroupDO);
    PermissionGroupBO toBO(PermissionGroupDO permissionGroupBO);

    PermissionDO toDO(PermissionBO permissionBO);
    PermissionBO toBO(PermissionDO permissionDO);
}
