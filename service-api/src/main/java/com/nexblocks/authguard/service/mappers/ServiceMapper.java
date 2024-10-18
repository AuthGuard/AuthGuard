package com.nexblocks.authguard.service.mappers;

import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.dal.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Base64;

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

    CryptoKeyDO toDO(PersistedKeyBO cryptoKeyDO);
    PersistedKeyBO toBO(CryptoKeyDO cryptoKeyDO);

    @Mapping(source = "key", target = "encryptedKey")
    TotpKeyDO toDO(TotpKeyBO totpKeyBO);
    @Mapping(source = "encryptedKey", target = "key")
    TotpKeyBO toBO(TotpKeyDO totpKeyDO);

    default byte[] toBlob(final String base64) {
        return base64 == null ? null : Base64.getDecoder().decode(base64);
    }

    default String toBase64(final byte[] blob) {
        return blob == null ? null : Base64.getEncoder().encodeToString(blob);
    }
}
