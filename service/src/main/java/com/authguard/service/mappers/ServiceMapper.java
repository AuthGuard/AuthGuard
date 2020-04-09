package com.authguard.service.mappers;

import com.authguard.dal.model.*;
import com.authguard.service.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ServiceMapper {
    CredentialsDO toDO(CredentialsBO credentialsBO);
    CredentialsBO toBO(CredentialsDO credentialsDO);

    CredentialsAuditDO toDO(CredentialsAuditBO credentialsBO);
    CredentialsAuditBO toBO(CredentialsAuditDO credentialsDO);

    AccountDO toDO(AccountBO accountBO);
    AccountBO toBO(AccountDO accountDO);

    AppDO toDO(AppBO appBO);
    AppBO toBO(AppDO appDO);

    PermissionDO toDO(PermissionBO permissionBO);
    PermissionBO toBO(PermissionDO permissionDO);

    RoleDO toDO(RoleBO roleBO);
    RoleBO toBO(RoleDO roleDO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jti", source = "tokensBO.id")
    @Mapping(target = "key", source = "tokensBO.token")
    @Mapping(target = "appId", source = "appBO.id")
    ApiKeyDO toDO(TokensBO tokensBO, AppBO appBO);

    OneTimePasswordDO toDO(OneTimePasswordBO oneTimePasswordBO);
    OneTimePasswordBO toBO(OneTimePasswordDO oneTimePasswordDO);
}
