package com.nexblocks.authguard.rest.mappers;

import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.service.model.*;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface RestMapper {
    AccountBO toBO(CreateAccountRequestDTO createAccountRequestDTO);
    AccountBO toBO(AccountDTO accountDTO);
    AccountDTO toDTO(AccountBO accountBO);

    CredentialsBO toBO(CreateCredentialsRequestDTO createAccountRequestDTO);
    CredentialsBO toBO(CredentialsDTO credentialsDTO);
    CredentialsDTO toDTO(CredentialsBO credentialsBO);

    AuthRequestBO toBO(AuthRequestDTO authRequestDTO);
    AuthRequestDTO toDO(AuthRequestBO authRequestBO);

    TokensBO toBO(TokensDTO tokensDTO);
    TokensDTO toDTO(TokensBO tokensBO);

    TokenRestrictionsBO toBO(TokenRestrictionsDTO tokenRestrictionsDTO);
    TokenRestrictionsDTO toDTO(TokenRestrictionsBO tokenRestrictionsBO);

    PermissionBO toBO(PermissionDTO permissionDTO);
    PermissionDTO toDTO(PermissionBO permissionBO);

    RoleBO toBO(RoleDTO roleDTO);
    RoleBO toBO(CreateRoleRequestDTO createRoleRequestDTO);
    RoleDTO toDTO(RoleBO roleBO);

    AccountEmailBO toBO(AccountEmailDTO accountEmailDTO);
    AccountEmailDTO toDTO(AccountEmailBO accountEmailBO);

    UserIdentifierBO toBO(UserIdentifierDTO userIdentifierDTO);
    UserIdentifierDTO toDTO(UserIdentifierBO userIdentifierBO);

    @Mapping(target = "parentAccountId", source = "accountId")
    AppBO toBO(CreateAppRequestDTO createAppRequestDTO);
    @Mapping(target = "parentAccountId", source = "accountId")
    AppBO toBO(AppDTO appDTO);
    @InheritInverseConfiguration
    AppDTO toDTO(AppBO appBO);

    ApiKeyBO toBO(ApiKeyDTO apiKeyDO);
    ApiKeyDTO toDTO(ApiKeyBO apiKeyBO);

    AccountLockBO toBO(AccountLockDTO accountLockDTO);
    AccountLockDTO toDTO(AccountLockBO accountLockBO);

    ExchangeAttemptDTO toDTO(ExchangeAttemptBO exchangeAttemptBO);
}
