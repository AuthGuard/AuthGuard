package org.auther.service.impl;

import org.auther.dal.model.AccountDO;
import org.auther.dal.model.PermissionDO;
import org.auther.dal.model.PermissionGroupDO;
import org.auther.dal.model.RoleDO;
import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;
import org.auther.service.model.PermissionGroupBO;
import org.auther.service.model.RoleBO;
import org.mapstruct.Mapper;

@Mapper
public interface ServiceMapper {
    AccountDO toDO(AccountBO accountBO);
    AccountBO toBO(AccountDO accountDO);

    PermissionGroupDO toDO(PermissionGroupBO permissionGroupDO);
    PermissionGroupBO toBO(PermissionGroupDO permissionGroupBO);

    PermissionDO toDO(PermissionBO permissionBO);
    PermissionBO toBO(PermissionDO permissionDO);

    RoleDO toDO(RoleBO roleBO);
    RoleBO toBO(RoleDO roleDO);
}
