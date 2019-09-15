package org.auther.service.impl.mappers;

import org.auther.dal.model.*;
import org.auther.service.model.*;
import org.mapstruct.Mapper;

@Mapper
public interface ServiceMapper {
    CredentialsDO toDO(CredentialsBO credentialsBO);
    CredentialsBO toBO(CredentialsDO credentialsDO);

    CredentialsAuditDO toDO(CredentialsAuditBO credentialsBO);
    CredentialsAuditBO toBO(CredentialsAuditDO credentialsDO);

    AccountDO toDO(AccountBO accountBO);
    AccountBO toBO(AccountDO accountDO);

    PermissionGroupDO toDO(PermissionGroupBO permissionGroupDO);
    PermissionGroupBO toBO(PermissionGroupDO permissionGroupBO);

    PermissionDO toDO(PermissionBO permissionBO);
    PermissionBO toBO(PermissionDO permissionDO);

    RoleDO toDO(RoleBO roleBO);
    RoleBO toBO(RoleDO roleDO);
}
