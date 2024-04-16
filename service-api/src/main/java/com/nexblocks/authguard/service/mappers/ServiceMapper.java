package com.nexblocks.authguard.service.mappers;

import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.dal.model.*;
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

    ClientDO toDO(ClientBO clientBO);
    ClientBO toBO(ClientDO clientDO);

    ApiKeyDO toDO(ApiKeyBO apiKeyBO);
    ApiKeyBO toBO(ApiKeyDO apiKeyDO);

    PermissionDO toDO(PermissionBO permissionBO);
    PermissionBO toBO(PermissionDO permissionDO);

    RoleDO toDO(RoleBO roleBO);
    RoleBO toBO(RoleDO roleDO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "key", expression = "java(tokensBO.getToken().toString())")
    @Mapping(target = "appId", source = "appBO.id")
    ApiKeyDO toDO(AuthResponseBO tokensBO, AppBO appBO);

    OneTimePasswordDO toDO(OneTimePasswordBO oneTimePasswordBO);
    OneTimePasswordBO toBO(OneTimePasswordDO oneTimePasswordDO);

    SessionDO toDO(SessionBO sessionBO);
    SessionBO toBO(SessionDO sessionDO);

    IdempotentRecordDO toDO(IdempotentRecordBO idempotentRecordBO);
    IdempotentRecordBO toBO(IdempotentRecordDO idempotentRecordDO);

    TokenRestrictionsDO toDO(TokenRestrictionsBO tokenRestrictionsBO);
    TokenRestrictionsBO toBO(TokenRestrictionsDO tokenRestrictionsDO);

    AccountLockDO toDO(AccountLockBO accountLockBO);
    AccountLockBO toBO(AccountLockDO accountLockDO);

    ExchangeAttemptDO toDO(ExchangeAttemptBO exchangeAttemptBO);
    ExchangeAttemptBO toBO(ExchangeAttemptDO exchangeAttemptDO);

    EventDO toDO(EventBO eventBO);
    EventBO toBO(EventDO eventDO);
}
